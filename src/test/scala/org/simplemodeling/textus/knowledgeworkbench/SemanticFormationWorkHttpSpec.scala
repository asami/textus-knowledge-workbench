package org.simplemodeling.textus.knowledgeworkbench

import org.goldenport.Consequence
import org.goldenport.http.HttpResponse
import org.goldenport.record.Record
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgeworkbench.{value as workbenchvalue}

/*
 * Controlled matching Hub contract; actual composed producer acceptance is separate.
 * @since   Oct. 30, 2026
 * @version Oct. 30, 2026
 */
final class SemanticFormationWorkHttpSpec extends AnyWordSpec with Matchers with GivenWhenThen with BeforeAndAfterAll {
  private val _database = new SemanticFormationWorkFixture.Database
  override protected def beforeAll(): Unit = { super.beforeAll(); _database.start() }
  override protected def afterAll(): Unit = try _database.stop() finally super.afterAll()

  "Semantic work HTTP authority" should {
    "open, correct, review, approve, form and confirm through authenticated public operations" in {
      Given("real PostgreSQL and a controlled Hub owning proposal/validation/formation contract")
      val session = new SemanticFormationWorkFixture.Session(_database, SemanticFormationWorkFixture.root())
      try {
        val target = SemanticFormationWorkFixture.target()
        val boundary = new SemanticFormationWorkFixture.PublicBoundary(session, target)
        When("normal generated HTTP opens saved source work and saves a complete human correction")
        val opened = _result(boundary, boundary.http("openSemanticWork", Record.data("informationId" -> target.informationId.value)))
        opened.work.formations shouldBe empty
        opened.work.decisions shouldBe empty
        val correction = target.copy(label = "Explicit corrected title")
        val corrected = _result(boundary, boundary.http("correctSemanticWork", Record.data("id" -> opened.work.id.value,
          "expectedRevision" -> opened.revision, "target" -> correction.toRecord(), "reason" -> "Controlled correction", "actor" -> "forged-actor")))
        val reviewed = _decide(boundary, corrected, "review")
        val approved = _decide(boundary, reviewed, "approve")
        Then("the owning current target and native revision retain actual authenticated decision attribution")
        approved.approvalValid shouldBe true
        approved.work.target shouldBe Some(correction.copy(provider = "manual-correction"))
        approved.revision should be > opened.revision
        approved.work.decisions.map(_.decision) shouldBe Vector("correct", "review", "approve")
        approved.work.decisions.foreach { event =>
          event.actor shouldBe "controlled-semantic-editor"
          scala.util.Try(java.time.Instant.parse(event.occurredAt)).isSuccess shouldBe true
          event.reason.trim should not be empty
        }
        When("public formation reserves and invokes the owning Hub, followed by explicit public confirmation")
        val formed = _result(boundary, boundary.http("formSemanticWork", Record.data("id" -> approved.work.id.value,
          "expectedRevision" -> approved.revision)))
        val confirmed = _result(boundary, boundary.http("confirmSemanticFormation", Record.data("id" -> formed.work.id.value,
          "expectedRevision" -> formed.revision)))
        Then("one retained canonical identity and exact target/decision survive without another owner creation")
        formed.work.formations should have size 1
        formed.work.formations.last.status shouldBe "Confirmed"
        formed.work.formations.last.target shouldBe approved.work.target.get
        formed.work.formations.last.decision shouldBe approved.work.decisions.last
        formed.work.formations.last.resultRevision shouldBe Some(1L)
        confirmed.toRecord() shouldBe formed.toRecord()
        boundary.get(formed.work.id).toRecord() shouldBe formed.toRecord()
        boundary.formationCalls shouldBe 1
        boundary.resultReads shouldBe Vector(formed.work.formations.last.id.value)
        boundary.ownerSubjects.toSet shouldBe Set("controlled-semantic-editor")
      } finally session.close()
    }

    "return specific stale and authorization failures and leave stored work unchanged" in {
      Given("a public opened work with native owner rights and no fabricated seeded terminal state")
      val session = new SemanticFormationWorkFixture.Session(_database, SemanticFormationWorkFixture.root())
      try {
        val target = SemanticFormationWorkFixture.target()
        val boundary = new SemanticFormationWorkFixture.PublicBoundary(session, target)
        val current = _result(boundary, boundary.http("openSemanticWork", Record.data("informationId" -> target.informationId.value)))
        val fields = Record.data("id" -> current.work.id.value, "expectedRevision" -> (current.revision + 1),
          "decision" -> "approve", "reason" -> "Stale request", "actor" -> "forged")
        When("a stale decision, unreviewed approval and premature formation use the normal generated route")
        _expect_failure(boundary, boundary.http("decideSemanticWork", fields), Consequence.stateConflict[Unit]("stale revision"))
        _expect_failure(boundary, boundary.http("decideSemanticWork", Record.data("id" -> current.work.id.value,
          "expectedRevision" -> current.revision, "decision" -> "approve", "reason" -> "No review")),
          Consequence.stateConflict[Unit]("review required"))
        _expect_failure(boundary, boundary.http("formSemanticWork", Record.data("id" -> current.work.id.value,
          "expectedRevision" -> current.revision)), Consequence.stateConflict[Unit]("approval required"))
        When("anonymous and different authenticated callers try to read or decide the owner's work")
        _expect_failure(boundary, boundary.http("getSemanticWork", Record.data("id" -> current.work.id.value),
          command = false, token = None), Consequence.securityPermissionDenied[Unit]("authenticated ingress requires provider evidence"))
        _expect_failure(boundary, boundary.http("getSemanticWork", Record.data("id" -> current.work.id.value),
          command = false, token = Some("semantic-foreign-token")), Consequence.securityPermissionDenied[Unit]("private work"))
        _expect_failure(boundary, boundary.http("decideSemanticWork", Record.data("id" -> current.work.id.value,
          "expectedRevision" -> current.revision, "decision" -> "review", "reason" -> "Foreign decision"),
          token = Some("semantic-foreign-token")), Consequence.securityPermissionDenied[Unit]("private work"))
        When("generic Entity creation attempts to install semantic decisions outside their owning operations")
        _expect_failure(boundary, boundary.http("createSemanticFormationWork", current.work.toRecord(), service = "entity"),
          Consequence.securityPermissionDenied[Unit]("generic mutation denied"))
        Then("the exact source, target, history, native revision and absence of reservations are unchanged")
        boundary.get(current.work.id).toRecord() shouldBe current.toRecord()
        boundary.formationCalls shouldBe 0
        boundary.retainedResult shouldBe None
      } finally session.close()
    }
  }

  private def _result(boundary: SemanticFormationWorkFixture.PublicBoundary, response: HttpResponse): workbenchvalue.SemanticWorkResult = {
    withClue(response.bag.asString().TAKE) { response.code shouldBe 200 }
    workbenchvalue.SemanticWorkResult.createC(boundary.record(response)).TAKE
  }
  private def _decide(boundary: SemanticFormationWorkFixture.PublicBoundary, current: workbenchvalue.SemanticWorkResult,
      decision: String): workbenchvalue.SemanticWorkResult =
    _result(boundary, boundary.http("decideSemanticWork", Record.data("id" -> current.work.id.value,
      "expectedRevision" -> current.revision, "decision" -> decision, "reason" -> ("Controlled " + decision), "actor" -> "forged-actor")))
  private def _expect_failure(boundary: SemanticFormationWorkFixture.PublicBoundary, response: HttpResponse,
      expected: Consequence[?]): Unit = expected match {
    case Consequence.Failure(conclusion) =>
      val json = boundary.json(response)
      response.code shouldBe conclusion.status.webCode.code
      json.hcursor.downField("error").get[Int]("status") shouldBe Right(conclusion.status.webCode.code)
      json.hcursor.downField("error").downField("conclusion").downField("observation").downField("taxonomy").focus shouldBe
        io.circe.parser.parse(conclusion.observation.taxonomy.toRecord.toJsonString).toOption
    case other => fail("Expected a structured failure prototype: " + other)
  }
}
