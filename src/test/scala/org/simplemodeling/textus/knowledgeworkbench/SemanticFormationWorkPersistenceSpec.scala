package org.simplemodeling.textus.knowledgeworkbench

import org.simplemodeling.textus.knowledgeworkbench.{value as workbenchvalue}

import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 30, 2026
 * @version Oct. 30, 2026
 */
final class SemanticFormationWorkPersistenceSpec extends AnyWordSpec with Matchers with GivenWhenThen with BeforeAndAfterAll {
  private val _database = new SemanticFormationWorkFixture.Database
  override protected def beforeAll(): Unit = { super.beforeAll(); _database.start() }
  override protected def afterAll(): Unit = try _database.stop() finally super.afterAll()
  "Native semantic work persistence" should {
    "retain complete corrected target, decisions and the same uncertain formation identity after restart" in {
      Given("required real PostgreSQL and explicitly controlled approved-work fixture data")
      val initial = SemanticFormationWorkFixture.snapshot().entity
      val selected = initial.target.get
      val approval = workbenchvalue.SemanticFormationDecision("approve", "controlled-editor", SemanticFormationWorkFixture.instant.toString,
        "Controlled fixture approval, not human acceptance", selected)
      val reservation = workbenchvalue.SemanticFormationReservation(SemanticFormationWorkFixture.ids.entityId(
        impl.SemanticFormationWork.resultCollection), selected, approval, "Unconfirmed", None)
      val seeded = initial.copy(decisions = Vector(approval), formations = Vector(reservation))
      val root = SemanticFormationWorkFixture.root()
      val writer = new SemanticFormationWorkFixture.Session(_database, root)
      try {
        When("the fixture seeds actual native typed work storage")
        writer.seed(seeded).isSuccess shouldBe true
      } finally writer.close()
      val reader = new SemanticFormationWorkFixture.Session(_database, root)
      try {
        When("a fresh runtime reads through the actual public work Operation")
        val response = reader.handle.subsystem.executeOperationResponseInContext(Request.of(component = KnowledgeWorkbenchComponent.name,
          service = "SemanticWork", operation = "getSemanticWork", properties = List(Property("id", seeded.id.value, None))), reader.context).TAKE
        val record = response match {
          case OperationResponse.RecordResponse(value) => value
          case _ => fail("Native semantic work Record is required")
        }
        val restored = workbenchvalue.SemanticWorkResult.createC(record).TAKE
        Then("source, exact target, attributed history and retained identity remain current without replay")
        restored.work shouldBe seeded
        restored.work.formations.last.status shouldBe "Unconfirmed"
        restored.revision should be > 0L
      } finally reader.close()
    }
    "make the Requested reservation visible before the owner effect and retain unsuccessful confirmation without replay" in {
      Given("required PostgreSQL and semantic work created, corrected, reviewed and approved through public Operations")
      val root = SemanticFormationWorkFixture.root()
      val session = new SemanticFormationWorkFixture.Session(_database, root)
      try {
        val boundary = new SemanticFormationWorkFixture.PublicBoundary(session)
        val approved = _approved(boundary)
        var before = Option.empty[workbenchvalue.SemanticWorkResult]
        boundary.beforeFormation { request =>
          val observed = SemanticFormationWorkFixture.independentlyRead(_database, root, approved.work.id)
          boundary.formationCalls shouldBe 0
          observed.work.formations should have size 1
          observed.work.formations.last.status shouldBe "Requested"
          observed.work.formations.last.target shouldBe approved.work.target.get
          observed.work.formations.last.decision shouldBe approved.work.decisions.last
          observed.revision should be > approved.revision
          request.toRecord.getLong("expectedWorkRevision") shouldBe Some(observed.revision)
          before = Some(observed)
        }
        boundary.outcome(SemanticFormationWorkFixture.OwnerOutcome.Unavailable)
        When("the owner boundary independently observes committed PostgreSQL reservation before returning actual failure")
        val failed = boundary.execute("formSemanticWork", org.goldenport.record.Record.data("id" -> approved.work.id.value,
          "expectedRevision" -> approved.revision))
        _expect_failure(failed, org.goldenport.Consequence.serviceUnavailable[Unit]("controlled owner unavailable"))
        val retained = SemanticFormationWorkFixture.independentlyRead(_database, root, approved.work.id)
        Then("the same identity and decision survive Unconfirmed with no fabricated canonical result")
        before should not be empty
        retained.work.formations.last.id shouldBe before.get.work.formations.last.id
        retained.work.formations.last.status shouldBe "Unconfirmed"
        retained.work.formations.last.resultRevision shouldBe None
        retained.work.target shouldBe approved.work.target
        boundary.retainedResult shouldBe None
        When("public confirmation can only read that identity and the owner still has no result")
        _expect_failure(boundary.execute("confirmSemanticFormation", org.goldenport.record.Record.data(
          "id" -> retained.work.id.value, "expectedRevision" -> retained.revision)),
          org.goldenport.Consequence.resourceNotFound[Unit]("controlled result absent"))
        _expect_failure(boundary.execute("formSemanticWork", org.goldenport.record.Record.data(
          "id" -> retained.work.id.value, "expectedRevision" -> retained.revision)),
          org.goldenport.Consequence.stateConflict[Unit]("retained approval cannot create twice"))
        Then("failed owner read preserves exact native storage and never allocates or resends creation")
        boundary.get(retained.work.id).toRecord() shouldBe retained.toRecord()
        boundary.resultReads shouldBe Vector(retained.work.formations.last.id.value)
        boundary.formationCalls shouldBe 1
      } finally session.close()
    }

    "confirm a successful owner effect after lost acknowledgement using the original publicly reserved identity" in {
      Given("required PostgreSQL and public approved work, with no seeded terminal reservation")
      val root = SemanticFormationWorkFixture.root()
      val writer = new SemanticFormationWorkFixture.Session(_database, root)
      val boundary = new SemanticFormationWorkFixture.PublicBoundary(writer)
      val approved = _approved(boundary)
      var before = Option.empty[workbenchvalue.SemanticWorkResult]
      boundary.beforeFormation { request =>
        val observed = SemanticFormationWorkFixture.independentlyRead(_database, root, approved.work.id)
        observed.work.formations.last.status shouldBe "Requested"
        observed.revision should be > approved.revision
        request.toRecord.getLong("expectedWorkRevision") shouldBe Some(observed.revision)
        boundary.formationCalls shouldBe 0
        before = Some(observed)
      }
      boundary.outcome(SemanticFormationWorkFixture.OwnerOutcome.LostSuccess)
      val canonical = try {
        When("the controlled owner retains its successful result but ControlThrowable loses the acknowledgement before Workbench post-call save")
        intercept[SemanticFormationWorkFixture.LostOwnerAcknowledgement] {
          boundary.execute("formSemanticWork", org.goldenport.record.Record.data("id" -> approved.work.id.value,
            "expectedRevision" -> approved.revision))
        }
        boundary.formationCalls shouldBe 1
        before should not be empty
        boundary.retainedResult should not be empty
        boundary.retainedResult.get
      } finally writer.close()
      val reader = new SemanticFormationWorkFixture.Session(_database, root)
      try {
        val reconstructed = new SemanticFormationWorkFixture.PublicBoundary(reader)
        // The separately controlled owning Hub retained its result; no new formation is simulated.
        reconstructed.restoreResult(canonical)
        val requested = reconstructed.get(approved.work.id)
        Then("independent native reconstruction sees the pre-call Requested reservation and its original revision")
        requested.toRecord() shouldBe before.get.toRecord()
        val id = requested.work.formations.last.id
        canonical.getString("id") shouldBe Some(id.value)
        When("public confirmation twice reads the owning result identity without sending another form command")
        val confirmed = _result(reconstructed.execute("confirmSemanticFormation", org.goldenport.record.Record.data(
          "id" -> requested.work.id.value, "expectedRevision" -> requested.revision)))
        val second = _result(reconstructed.execute("confirmSemanticFormation", org.goldenport.record.Record.data(
          "id" -> confirmed.work.id.value, "expectedRevision" -> confirmed.revision)))
        Then("native target, attribution, one result identity and decision survive while confirmation alone records success")
        confirmed.work.formations should have size 1
        confirmed.work.formations.last.id shouldBe id
        confirmed.work.formations.last.status shouldBe "Confirmed"
        confirmed.work.formations.last.resultRevision shouldBe Some(1L)
        confirmed.work.formations.last.target shouldBe approved.work.target.get
        confirmed.work.formations.last.decision shouldBe approved.work.decisions.last
        confirmed.work.informationId shouldBe approved.work.informationId
        confirmed.work.informationRevision shouldBe approved.work.informationRevision
        confirmed.work.decisions shouldBe approved.work.decisions
        second.toRecord() shouldBe confirmed.toRecord()
        reconstructed.get(confirmed.work.id).toRecord() shouldBe confirmed.toRecord()
        reconstructed.formationCalls shouldBe 0
        reconstructed.resultReads shouldBe Vector(id.value, id.value)
        boundary.formationCalls shouldBe 1
      } finally reader.close()
    }
  }

  private def _result(result: org.goldenport.Consequence[OperationResponse]): workbenchvalue.SemanticWorkResult = result.TAKE match {
    case OperationResponse.RecordResponse(record) => workbenchvalue.SemanticWorkResult.createC(record).TAKE
    case other => fail("Expected public SemanticWork Record: " + other)
  }
  private def _approved(boundary: SemanticFormationWorkFixture.PublicBoundary): workbenchvalue.SemanticWorkResult = {
    val opened = _result(boundary.execute("openSemanticWork", org.goldenport.record.Record.data("informationId" -> boundary.informationId.value)))
    val selected = opened.work.target.get.copy(label = "Controlled public correction")
    val corrected = _result(boundary.execute("correctSemanticWork", org.goldenport.record.Record.data("id" -> opened.work.id.value,
      "expectedRevision" -> opened.revision, "target" -> selected.toRecord(), "reason" -> "Controlled correction")))
    val reviewed = _result(boundary.execute("decideSemanticWork", org.goldenport.record.Record.data("id" -> corrected.work.id.value,
      "expectedRevision" -> corrected.revision, "decision" -> "review", "reason" -> "Controlled review")))
    val approved = _result(boundary.execute("decideSemanticWork", org.goldenport.record.Record.data("id" -> reviewed.work.id.value,
      "expectedRevision" -> reviewed.revision, "decision" -> "approve", "reason" -> "Controlled approval")))
    approved.approvalValid shouldBe true
    approved.work.decisions.last.actor shouldBe "controlled-semantic-editor"
    approved
  }
  private def _expect_failure(result: org.goldenport.Consequence[?], expected: org.goldenport.Consequence[?]): Unit = (result, expected) match {
    case (org.goldenport.Consequence.Failure(actual), org.goldenport.Consequence.Failure(prototype)) =>
      actual.status.webCode shouldBe prototype.status.webCode
      actual.observation.taxonomy shouldBe prototype.observation.taxonomy
    case other => fail("Expected the exact structured failure categories: " + other)
  }
}
