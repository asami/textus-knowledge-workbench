package org.simplemodeling.textus.knowledgeworkbench

import java.nio.file.{Files, Path}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.bag.BinaryBag
import org.goldenport.cncf.bootstrap.{BootstrapConfig, CncfBootstrap, CncfHandle}
import org.goldenport.cncf.cli.RunMode
import org.goldenport.cncf.component.{Component, ComponentId, ComponentInit, ComponentInstanceId, ComponentOrigin}
import org.goldenport.cncf.context.{ExecutionContext, PrincipalId, SubjectKind}
import org.goldenport.cncf.http.WorkbenchHttpFixture
import org.goldenport.cncf.security.{AuthenticationProvider, AuthenticationRequest, AuthenticationResult, IngressSecurityResolver, SecuritySubject}
import org.goldenport.cncf.subsystem.{GenericSubsystemAuthenticationBinding, GenericSubsystemAuthenticationProviderBinding, GenericSubsystemComponentBinding, GenericSubsystemSecurityBinding, Subsystem}
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.http4s.{Header, HttpApp, Method, Request as WebRequest, Uri}
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgeworkbench.impl.CandidateAdmission
import org.simplemodeling.textus.knowledgeworkbench.value as WorkbenchValue
import org.typelevel.ci.CIString
import scala.jdk.CollectionConverters.*
import scala.util.Using

/*
 * Connected public-contract acceptance. Controlled facts and real read-only facts
 * are separate cases; neither test caller claims real end-user OAuth.
 * @since   Oct. 28, 2026
 * @version Oct. 28, 2026
 */
final class MaterialEvidenceIntegrationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialEvidenceIntegrationSpec.*

  "Versioned Material to confirmed Information" should {
    "preserve a neutral consumer's exact approved evidence and joint native/Hub trace across PostgreSQL restart" in {
      Given("an independent technical-observation consumer, controlled logical Material, PostgreSQL and the actual Hub CAR")
      _with_database { (root, database) =>
        val token = java.util.UUID.randomUUID().toString
        val subject = "material-evidence-neutral-service"
        val writer = _handle(root, database, token, subject)
        val saved = try {
          val producer = WorkbenchHttpFixture.installMaterial(writer.subsystem)
          producer.sourceVersion = Some(" opaque:version/東京 %2F ")
          val consumer = new EvidenceConsumer(writer.subsystem, token)
          val metadata = consumer.inspect(WorkbenchHttpFixture.materialRef)
          val selected = metadata.resources.map(_.resourceId)
          selected shouldBe Vector(WorkbenchHttpFixture.resourceId)
          val first = consumer.create("technical:versioned-observation", metadata.materialRef, selected)
          first.reviewEligible shouldBe false
          When("the consumer retains human work, explicitly observes source evidence, Reviews, Approves and admits once")
          val human = consumer.edit(first, "Human technical observation")
          val bound = consumer.refresh(human)
          bound.candidate.content shouldBe human.candidate.content
          bound.candidate.originalProposal shouldBe first.candidate.originalProposal
          bound.candidate.evidenceRefs.head.revision shouldBe producer.sourceVersion
          bound.candidate.evidenceRefs.head.snapshotRef shouldBe None
          val approved = consumer.decide(consumer.decide(bound, "review"), "approve")
          val admitted = consumer.admit(approved)
          Then("the actual Hub persists the exact approved target while detailed resource correspondence stays native")
          _assert_trace(consumer, admitted, approved)
          producer.subjects.distinct shouldBe Vector(subject)
          producer.evidenceRequests shouldBe 1
          val firstselection = _association(admitted)
          firstselection shouldBe (metadata.materialRef -> selected)
          val original = _json(admitted.candidate.originalProposal.get.value)
          original.asObject.get.keys.toSet.intersect(Set("book", "isbn", "studio", "provider", "connectionId")) shouldBe empty
          val canonical = consumer.information(admitted.candidate.admissions.last.informationId.print)
          When("generated canonical target substitutions attempt to claim the same original approved trace")
          val property = Prop.forAll(Gen.nonEmptyListOf(Gen.alphaNumChar)) { suffix =>
            val changed = _json(canonical.toJsonString).hcursor.downField("information").downField("target").downField("content")
              .withFocus(_ => Json.fromString("Human technical observation" + suffix.mkString)).top.get
            val foreign = new org.goldenport.record.io.RecordDecoder().json(changed.noSpaces).TAKE
            CandidateAdmission.confirm(admitted.candidate, admitted.candidate.admissions.last, foreign).isSuccess == false
          }
          Test.check(Test.Parameters.default.withMinSuccessfulTests(40), property).passed shouldBe true
          admitted
        } finally writer.close()
        When("a fresh runtime opens the same PostgreSQL Candidate and persisted Hub without a Material producer")
        val reader = _handle(root, database, token, subject)
        try {
          val consumer = new EvidenceConsumer(reader.subsystem, token)
          val restored = consumer.work(saved.candidate.id.print)
          Then("identity, versions, immutable first selection, applications, decisions and canonical outcome survive together")
          restored.toRecord() shouldBe saved.toRecord()
          consumer.confirm(restored).toRecord() shouldBe restored.toRecord()
          consumer.lookup("technical:versioned-observation").candidate.get.toRecord() shouldBe saved.candidate.toRecord()
          consumer.create("technical:versioned-observation", "irrelevant:resend", Vector("irrelevant:resource")).toRecord() shouldBe saved.toRecord()
          restored.candidate.materialEvidence.get.applications should have size 1
          restored.candidate.decisions.map(_.actor).distinct shouldBe Vector(subject)
          _assert_trace(consumer, restored, restored)
        } finally reader.close()
      }
    }

    "preserve native work through denied missing unavailable unversioned and stale evidence without acquiring or replaying mutations" in {
      Given("a provider-authenticated service caller and controlled public source failures beside actual persistence")
      _with_database { (root, database) =>
        val token = java.util.UUID.randomUUID().toString
        val handle = _handle(root, database, token, "material-evidence-failure-service")
        try {
          val producer = WorkbenchHttpFixture.installMaterial(handle.subsystem)
          val consumer = new EvidenceConsumer(handle.subsystem, token)
          val first = consumer.create("technical:failed-observation", WorkbenchHttpFixture.materialRef, Vector(WorkbenchHttpFixture.resourceId))
          val human = consumer.edit(first, "Human work survives failed acquisition")
          val calls = producer.evidenceRequests
          When("a stale refresh or an anonymous/unadmitted caller attempts the same business work")
          consumer.refreshResult(first) shouldBe a[Consequence.Failure[?]]
          Vector(None, Some("not-an-admitted-token")).foreach { supplied =>
            _result(handle.subsystem, supplied, KnowledgeWorkbenchComponent.name, "MaterialWork", "refreshCandidateMaterialEvidence",
              _refresh_fields(human)*) shouldBe a[Consequence.Failure[?]]
          }
          Then("preconditions stop before source acquisition and preserve every current native field")
          producer.evidenceRequests shouldBe calls
          consumer.work(human.candidate.id.print).toRecord() shouldBe human.toRecord()
          val failures: Vector[Consequence[OperationResponse]] = Vector(
            Consequence.securityPermissionDenied("Controlled source denial"),
            Consequence.resourceNotFound("Controlled original is missing"),
            Consequence.serviceUnavailable("Controlled source unavailable"))
          When("each structured source failure is returned to an explicit fresh attempt")
          failures.foreach { failure =>
            producer.sourceFailure = Some(failure)
            consumer.refreshResult(human) shouldBe failure
            consumer.work(human.candidate.id.print).toRecord() shouldBe human.toRecord()
          }
          producer.evidenceRequests shouldBe (calls + failures.size)
          producer.sourceFailure = None
          producer.sourceVersion = None
          val unverified = consumer.refresh(human)
          Then("a completed unversioned observation is retained honestly and cannot pass Review or Admission")
          unverified.candidate.evidenceRefs.head.revision shouldBe None
          unverified.candidate.evidenceRefs.head.retrievalState shouldBe Some("unverified")
          unverified.reviewEligible shouldBe false
          consumer.decideResult(unverified, "review") shouldBe a[Consequence.Failure[?]]
          consumer.admitResult(unverified) shouldBe a[Consequence.Failure[?]]
          consumer.work(unverified.candidate.id.print).toRecord() shouldBe unverified.toRecord()
          unverified.candidate.admissions shouldBe empty
        } finally handle.close()
      }
    }

    "reconcile the reserved canonical identity after an unconfirmed owning outcome without allocating or resending" in {
      Given("versioned neutral work approved while its Hub route is unavailable")
      _with_database { (root, database) =>
        val token = java.util.UUID.randomUUID().toString
        val subject = "material-evidence-unconfirmed-service"
        val writer = _handle(root, database, token, subject, includehub = false)
        val saved = try {
          val producer = WorkbenchHttpFixture.installMaterial(writer.subsystem)
          val consumer = new EvidenceConsumer(writer.subsystem, token)
          val bound = consumer.refresh(consumer.create("technical:uncertain-admission", WorkbenchHttpFixture.materialRef,
            Vector(WorkbenchHttpFixture.resourceId)))
          val approved = consumer.decide(consumer.decide(bound, "review"), "approve")
          When("one Admission attempt cannot observe its owning result")
          consumer.admitResult(approved) shouldBe a[Consequence.Failure[?]]
          val pending = consumer.work(approved.candidate.id.print)
          Then("the native reservation, exact approved source and one Information identity remain durable")
          pending.state shouldBe "AdmissionUnconfirmed"
          pending.candidate.admissions should have size 1
          pending.candidate.materialEvidence shouldBe approved.candidate.materialEvidence
          consumer.admitResult(pending) shouldBe a[Consequence.Failure[?]]
          consumer.work(pending.candidate.id.print).toRecord() shouldBe pending.toRecord()
          producer.evidenceRequests shouldBe 1
          pending
        } finally writer.close()
        val reader = _handle(root, database, token, subject)
        try {
          val consumer = new EvidenceConsumer(reader.subsystem, token)
          val restored = consumer.work(saved.candidate.id.print)
          When("a read initially finds no canonical outcome, then the original reserved owning write finishes")
          consumer.confirmResult(restored) shouldBe a[Consequence.Failure[?]]
          consumer.work(restored.candidate.id.print).toRecord() shouldBe restored.toRecord()
          val reservation = restored.candidate.admissions.last
          consumer.completeOriginalOwningWrite(CandidateAdmission.request(restored.candidate.id, reservation))
          consumer.work(restored.candidate.id.print).state shouldBe "AdmissionUnconfirmed"
          val confirmed = consumer.confirm(restored)
          Then("explicit lookup confirms only that exact ID and target without another Workbench Admission or source acquisition")
          confirmed.state shouldBe "Admitted"
          confirmed.candidate.admissions should have size 1
          confirmed.candidate.admissions.last.informationId shouldBe reservation.informationId
          confirmed.candidate.materialEvidence shouldBe saved.candidate.materialEvidence
          consumer.confirm(confirmed).toRecord() shouldBe confirmed.toRecord()
          _assert_trace(consumer, confirmed, restored)
        } finally reader.close()
      }
    }

    // Only the explicitly selected connected check reads the existing cloud source.
    // Normal full validation retains controlled coverage without network or browser prerequisites.
    if (sys.props.get("textus.test.material-evidence.real").contains("true")) {
      "retain actual read-only TKL evidence and byte-identical original through confirmed Information and PostgreSQL restart" in {
        Given("one existing real Material, the admitted read-only connection and the actual TKL package reader")
        val selected = _live_selection()
        _with_database { (root, database) =>
          val token = java.util.UUID.randomUUID().toString
          val subject = "editing-studio-mobile-development"
          val writer = _handle(root, database, token, subject, live = Some(selected.document -> selected.connection))
          val saved = try {
            val consumer = new EvidenceConsumer(writer.subsystem, token)
            val context = _context(writer.subsystem, Some(token)).TAKE
            SecuritySubject.current(using context).isProviderAuthenticated shouldBe true
            context.security.subjectKind shouldBe SubjectKind.Service
            SecuritySubject.current(using context).subjectId shouldBe subject
            _reject_unadmitted(writer.subsystem, selected.materialref)
            val metadata = consumer.inspect(selected.materialref)
            val publicresources = metadata.resources.filter(resource =>
              Set(selected.resourceid, selected.assetpath).contains(resource.resourceId) &&
                resource.role == selected.role && resource.mediaType == "image/jpeg")
            publicresources should have size 1
            val resourceid = publicresources.head.resourceId
            _equal_bytes(consumer.original(selected.materialref, resourceid), selected.bytes)
            _equal_bytes(_http_original(WorkbenchHttpFixture.app(writer.subsystem), token, selected.materialref, resourceid), selected.bytes)
            When("the same admitted service caller explicitly creates, edits, refreshes, Reviews, Approves and admits the actual source")
            val first = consumer.create("technical:actual-existing-material", metadata.materialRef, Vector(resourceid))
            first.reviewEligible shouldBe false
            val human = consumer.edit(first, "Human observation of an existing real Material")
            val bound = consumer.refresh(human)
            val evidence = bound.candidate.evidenceRefs.head
            evidence.retrievalState shouldBe Some("available")
            evidence.revision.exists(_.trim.nonEmpty) shouldBe true
            evidence.snapshotRef shouldBe None
            evidence.retentionUntil shouldBe None
            evidence.limitation.exists(_.trim.nonEmpty) shouldBe true
            evidence.id should startWith ("urn:textus:klake:evidence:")
            bound.candidate.originalProposal shouldBe first.candidate.originalProposal
            bound.candidate.content shouldBe human.candidate.content
            _association(bound) shouldBe (metadata.materialRef -> Vector(resourceid))
            bound.candidate.materialEvidence.get.applications.last.resources.map(_.appliedReference) shouldBe bound.candidate.evidenceRefs
            bound.candidate.materialEvidence.get.applications.last.resources.map(_.resourceId) shouldBe Vector(resourceid)
            val approved = consumer.decide(consumer.decide(bound, "review"), "approve")
            val admitted = consumer.admit(approved)
            Then("the actual Hub confirms the exact approved source version and logical trace, independently of local native revisions")
            _assert_trace(consumer, admitted, approved)
            admitted.candidate.decisions.map(_.actor).distinct shouldBe Vector(subject)
            admitted.candidate.decisions.map(_.decision.value) shouldBe Vector("edit", "edit", "review", "approve")
            _equal_bytes(consumer.original(selected.materialref, resourceid), selected.bytes)
            admitted
          } finally writer.close()
          When("a new runtime reopens PostgreSQL, the persisted Hub and the same read-only original")
          val reader = _handle(root, database, token, subject, live = Some(selected.document -> selected.connection))
          try {
            val consumer = new EvidenceConsumer(reader.subsystem, token)
            val restored = consumer.work(saved.candidate.id.print)
            restored.toRecord() shouldBe saved.toRecord()
            consumer.confirm(restored).toRecord() shouldBe restored.toRecord()
            val resourceid = restored.candidate.materialEvidence.get.resourceIds.head
            _equal_bytes(consumer.original(selected.materialref, resourceid), selected.bytes)
            _equal_bytes(_http_original(WorkbenchHttpFixture.app(reader.subsystem), token, selected.materialref, resourceid), selected.bytes)
            _assert_trace(consumer, restored, restored)
            Then("all original bytes, native source correspondence and the canonical ID/revision remain exact without remote mutation")
            val admission = restored.candidate.admissions.last
            val observation = Json.obj("kind" -> Json.fromString("actual-read-only-material-evidence"),
              "materialRef" -> Json.fromString(selected.materialref), "resourceId" -> Json.fromString(resourceid),
              "sourceRevision" -> Json.fromString(restored.candidate.evidenceRefs.head.revision.get),
              "evidenceRef" -> Json.fromString(restored.candidate.evidenceRefs.head.id),
              "candidateId" -> Json.fromString(restored.candidate.id.print), "candidateRevision" -> Json.fromLong(restored.revision),
              "approvedCandidateRevision" -> Json.fromLong(admission.candidateRevision),
              "informationId" -> Json.fromString(admission.informationId.print),
              "informationRevision" -> Json.fromLong(admission.informationRevision.get),
              "byteLength" -> Json.fromInt(selected.bytes.length), "completeOriginalEquality" -> Json.fromBoolean(true),
              "postgresqlRestartEquality" -> Json.fromBoolean(true), "caller" -> Json.fromString(subject),
              "callerKind" -> Json.fromString("provider-authenticated-service; not end-user OAuth"),
              "limitation" -> Json.fromString(restored.candidate.evidenceRefs.head.limitation.get))
            Files.writeString(Path.of("target/material-evidence-real-observation.json"), observation.noSpaces)
          } finally reader.close()
        }
      }
    }
  }

  private def _assert_trace(consumer: EvidenceConsumer, admitted: WorkbenchValue.CandidateWorkResult,
      approved: WorkbenchValue.CandidateWorkResult): Unit = {
    val admission = admitted.candidate.admissions.last
    val canonical = _json(consumer.information(admission.informationId.print).toJsonString)
    admitted.state shouldBe "Admitted"
    admission.status shouldBe "Confirmed"
    admission.informationRevision shouldBe canonical.hcursor.get[Long]("revision").toOption
    canonical.hcursor.downField("information").focus shouldBe Some(_json(CandidateAdmission.request(admitted.candidate.id, admission).toJsonString))
    canonical.hcursor.downField("information").get[String]("candidateId").toOption shouldBe Some(admitted.candidate.id.print)
    canonical.hcursor.downField("information").get[Long]("candidateRevision").toOption shouldBe Some(admission.candidateRevision)
    canonical.hcursor.downField("information").downField("materialEvidence").succeeded shouldBe false
    admission.approval.target shouldBe approved.target.get
    admitted.candidate.materialEvidence.get.applications.last.resources.map(_.appliedReference) shouldBe admission.approval.target.evidenceRefs
    admitted.candidate.materialEvidence.get.materialRef shouldBe _association(admitted)._1
    admitted.candidate.materialEvidence.get.resourceIds shouldBe _association(admitted)._2
  }

  private def _equal_bytes(actual: Array[Byte], expected: Array[Byte]): Unit =
    withClue(s"Complete original comparison (${actual.length}/${expected.length} bytes): ") {
      java.util.Arrays.equals(actual, expected) shouldBe true
    }

  private def _reject_unadmitted(subsystem: Subsystem, materialref: String): Unit = {
    Given("the live read-only connection grants only the already admitted service subject")
    Vector(None, Some("not-an-admitted-token")).foreach { token =>
      _result(subsystem, token, KnowledgeWorkbenchComponent.name, "MaterialWork", "inspectMaterial",
        "materialRef" -> materialref) shouldBe a[Consequence.Failure[?]]
    }
  }
}

object MaterialEvidenceIntegrationSpec {
  private final case class LiveSelection(document: Json, connection: Path, materialref: String,
      resourceid: String, assetpath: String, role: String, bytes: Array[Byte])

  /** A domain-neutral application uses logical Operations, never Drive file/folder APIs. */
  final class EvidenceConsumer(subsystem: Subsystem, token: String) {
    def inspect(materialRef: String): WorkbenchValue.MaterialMetadata =
      WorkbenchValue.MaterialMetadata.createC(_call(subsystem, token, "MaterialWork", "inspectMaterial", "materialRef" -> materialRef)).TAKE
    def create(proposalId: String, materialRef: String, resourceIds: Vector[String]): WorkbenchValue.CandidateWorkResult = {
      val response = WorkbenchValue.RegisterProposalResult.createC(_call(subsystem, token, "MaterialWork", "createCandidateFromMaterial",
        "proposalId" -> proposalId, "materialRef" -> materialRef, "resourceIds" -> resourceIds,
        "name" -> "Technical source observation", "content" -> "Initial technical observation", "context" -> "")).TAKE
      work(response.candidate.id.print)
    }
    def work(id: String): WorkbenchValue.CandidateWorkResult =
      _work(_result(subsystem, Some(token), KnowledgeWorkbenchComponent.name, "Candidate", "getCandidateWork", "id" -> id))
    def lookup(proposalId: String): WorkbenchValue.FindCandidateByProposalResult =
      WorkbenchValue.FindCandidateByProposalResult.createC(_call(subsystem, token, "Candidate", "findCandidateByProposal",
        "producerRef" -> "workbench-material", "proposalId" -> proposalId)).TAKE
    def edit(current: WorkbenchValue.CandidateWorkResult, content: String): WorkbenchValue.CandidateWorkResult =
      _work(_result(subsystem, Some(token), KnowledgeWorkbenchComponent.name, "Candidate", "editCandidateWork",
        "id" -> current.candidate.id.print, "expectedRevision" -> current.revision, "content" -> content,
        "context" -> "", "evidenceRefs" -> current.candidate.evidenceRefs.map(_.toRecord()),
        "groundingRefs" -> current.candidate.groundingRefs.map(_.toRecord()),
        "mappingRefs" -> current.candidate.mappingRefs.map(_.toRecord()), "reason" -> "Human technical observation correction"))
    def refreshResult(current: WorkbenchValue.CandidateWorkResult): Consequence[OperationResponse] =
      _result(subsystem, Some(token), KnowledgeWorkbenchComponent.name, "MaterialWork", "refreshCandidateMaterialEvidence", _refresh_fields(current)*)
    def refresh(current: WorkbenchValue.CandidateWorkResult): WorkbenchValue.CandidateWorkResult = _work(refreshResult(current))
    def decideResult(current: WorkbenchValue.CandidateWorkResult, decision: String): Consequence[OperationResponse] =
      _result(subsystem, Some(token), KnowledgeWorkbenchComponent.name, "Candidate", "decideCandidateWork",
        "id" -> current.candidate.id.print, "expectedRevision" -> current.revision,
        "decision" -> decision, "reason" -> "Explicit source-aware human decision")
    def decide(current: WorkbenchValue.CandidateWorkResult, decision: String): WorkbenchValue.CandidateWorkResult = _work(decideResult(current, decision))
    def admitResult(current: WorkbenchValue.CandidateWorkResult): Consequence[OperationResponse] =
      _result(subsystem, Some(token), KnowledgeWorkbenchComponent.name, "Candidate", "admitCandidateWork", _selection_fields(current)*)
    def admit(current: WorkbenchValue.CandidateWorkResult): WorkbenchValue.CandidateWorkResult = _work(admitResult(current))
    def confirmResult(current: WorkbenchValue.CandidateWorkResult): Consequence[OperationResponse] =
      _result(subsystem, Some(token), KnowledgeWorkbenchComponent.name, "Candidate", "confirmCandidateAdmission", _selection_fields(current)*)
    def confirm(current: WorkbenchValue.CandidateWorkResult): WorkbenchValue.CandidateWorkResult = _work(confirmResult(current))
    def information(id: String): Record =
      _record(_result(subsystem, Some(token), "org.simplemodeling.textus.KnowledgeHub", "Information", "getInformation", "id" -> id).TAKE)
    def completeOriginalOwningWrite(request: Record): Unit = {
      _result(subsystem, Some(token), "org.simplemodeling.textus.KnowledgeHub", "Information", "admitInformation", request.asMap.toVector*).TAKE
      ()
    }
    def original(materialRef: String, resourceId: String): Array[Byte] = {
      val response = _result(subsystem, Some(token), KnowledgeWorkbenchComponent.name, "MaterialWork", "readOriginal",
        "materialRef" -> materialRef, "resourceId" -> resourceId).TAKE match {
        case OperationResponse.Http(binary: org.goldenport.http.HttpResponse.Binary) => binary
        case _ => throw new IllegalStateException("Expected public binary original")
      }
      require(response.mime.value == "image/jpeg", "Actual original MIME must remain JPEG")
      require(response.headerValue("Cache-Control").contains("private, no-store"), "Original must remain private")
      Using.resource(response.bag.asInstanceOf[BinaryBag].openInputStream())(_.readNBytes(16777217))
    }
  }

  private def _selection_fields(current: WorkbenchValue.CandidateWorkResult): Vector[(String, Any)] =
    Vector("id" -> current.candidate.id.print, "expectedRevision" -> current.revision)
  private def _refresh_fields(current: WorkbenchValue.CandidateWorkResult): Vector[(String, Any)] =
    _selection_fields(current) :+ ("reason" -> "Observe original evidence explicitly")
  private def _association(current: WorkbenchValue.CandidateWorkResult): (String, Vector[String]) = {
    val selected = _json(current.candidate.originalProposal.get.value).hcursor.downField("materialSelection")
    selected.get[String]("materialRef").toOption.get -> selected.get[Vector[String]]("resourceIds").toOption.get
  }
  private def _json(text: String): Json = io.circe.parser.parse(text).fold(throw _, identity)
  private def _record(response: OperationResponse): Record = response match {
    case OperationResponse.RecordResponse(record) => record
    case _ => throw new IllegalStateException("Expected native logical Record")
  }
  private def _work(result: Consequence[OperationResponse]): WorkbenchValue.CandidateWorkResult =
    WorkbenchValue.CandidateWorkResult.createC(_record(result.TAKE)).TAKE
  private def _call(subsystem: Subsystem, token: String, service: String, operation: String, fields: (String, Any)*): Record =
    _record(_result(subsystem, Some(token), KnowledgeWorkbenchComponent.name, service, operation, fields*).TAKE)
  private def _context(subsystem: Subsystem, token: Option[String]): Consequence[ExecutionContext] = {
    val component = subsystem.components.find(_.name == KnowledgeWorkbenchComponent.name)
      .getOrElse(throw new IllegalStateException("Expected owning Workbench component"))
    val attributes = token.map(input => Map("Authorization" -> ("Bearer " + input))).getOrElse(Map.empty[String, String])
    // This consumer requires provider evidence; Fixed legitimately admits its local subject without a token.
    IngressSecurityResolver.resolve(org.goldenport.cncf.subsystem.SubsystemExecutionProfile.Authenticated,
      component.logic.executionContext(), attributes).map(_.executionContext)
  }
  private def _result(subsystem: Subsystem, token: Option[String], component: String, service: String,
      operation: String, fields: (String, Any)*): Consequence[OperationResponse] =
    _context(subsystem, token).flatMap(context => subsystem.executeOperationResponseInChildContext(Request.of(
      component = component, service = service, operation = operation,
      properties = fields.map { case (key, input) => Property(key, input, None) }.toList), context))

  private final class FixtureAuthentication(token: String, subject: String) extends Component {
    val providerName: String = "material-evidence-integration-service"
    private val _provider = new AuthenticationProvider {
      def name: String = providerName
      def authenticate(request: AuthenticationRequest)(using ExecutionContext): Consequence[Option[AuthenticationResult]] =
        Consequence.success(request.accessToken.filter(_ == token).map(_ =>
          AuthenticationResult(PrincipalId(subject), subjectKind = SubjectKind.Service)))
    }
    override def authenticationProviders: Vector[AuthenticationProvider] = Vector(_provider)
  }

  private def _with_database[A](body: (Path, MaterialCandidateWorkSpec.LivePostgresql) => A): A = {
    val database = new MaterialCandidateWorkSpec.LivePostgresql
    val root = Files.createTempDirectory(Files.createDirectories(Path.of("target/material-evidence-integration/work").toAbsolutePath), "case-")
    database.start()
    try body(root, database)
    finally {
      try Using.resource(Files.walk(root))(_.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.deleteIfExists(_)))
      finally database.stop()
    }
  }

  private def _handle(root: Path, database: MaterialCandidateWorkSpec.LivePostgresql, token: String,
      subject: String, live: Option[(Json, Path)] = None, includehub: Boolean = true): CncfHandle = {
    val repository = Path.of(".").toAbsolutePath.normalize()
    val lake = Path.of(sys.props.getOrElse("textus.test.knowledge-lake.repository",
      repository.resolveSibling("textus-knowledge-lake").toString)).toAbsolutePath.normalize()
    val hubcar = Path.of(sys.props.getOrElse("textus.test.knowledge-hub.car",
      Path.of(System.getProperty("user.home"), ".cncf/local/repository/car/org/simplemodeling/textus/" +
        "textus-knowledge-hub/0.1.0-SNAPSHOT/textus-knowledge-hub-0.1.0-SNAPSHOT.car").toString)).toAbsolutePath.normalize()
    if (live.nonEmpty) {
      val httpplan = Json.obj("schema" -> Json.fromString("cncf.runtime-http-execution.v1"),
        "baseUrl" -> Json.fromString("https://www.googleapis.com/"),
        "limits" -> Json.obj("connectTimeoutMillis" -> Json.fromInt(10000), "readTimeoutMillis" -> Json.fromInt(20000),
          "responseBytes" -> Json.fromInt(8388608)),
        "grants" -> Json.arr(Json.obj("componentId" -> Json.fromString("org.simplemodeling.textus.KnowledgeLake"),
          "origins" -> Json.arr(Json.fromString("https://oauth2.googleapis.com"), Json.fromString("https://www.googleapis.com")),
          "methods" -> Json.arr(Json.fromString("GET"), Json.fromString("POST")))))
      val hostfile = Files.createDirectories(root.resolve(".textus")).resolve("config.json")
      Files.writeString(hostfile, Json.obj("textus.subsystem.http-execution.plan" -> Json.fromString(httpplan.noSpaces)).noSpaces)
      Files.setPosixFilePermissions(hostfile, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"))
    }
    val lakecomponent = live.map { case (selected, connection) =>
      val storage = Json.obj("provider" -> Json.fromString("google-drive"), "connectionId" -> Json.fromString("ess3-real-drive-read"),
        "storeName" -> selected.hcursor.downField("storeName").focus.get,
        "rootKey" -> selected.hcursor.downField("rootKey").focus.get, "parentKey" -> selected.hcursor.downField("parentKey").focus.get)
      val packages = Json.obj("schema" -> Json.fromString("textus.knowledge-lake.material-packages/1"), "materials" -> Json.arr(Json.obj(
        "materialRef" -> selected.hcursor.downField("materialRef").focus.get,
        "authorizedSubjects" -> Json.arr(Json.fromString(subject)), "storage" -> storage)))
      val config = Vector("google-drive.service-account.configuration" -> connection.toUri.toString,
        "material.reference.mode" -> "packages", "material.reference.packages" -> packages.noSpaces)
        .map { case (key, input) => s"      $key: ${Json.fromString(input).noSpaces}\n" }.mkString
      "  - namespace: org.simplemodeling.textus\n    id: KnowledgeLake\n    version: 0.1.0-SNAPSHOT\n    config:\n" + config
    }.getOrElse("")
    val hubcomponent = if (includehub) "  - namespace: org.simplemodeling.textus\n    id: KnowledgeHub\n    version: 0.1.0-SNAPSHOT\n" else ""
    val prefix = "textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application"
    val hubprefix = "textus.component.org.simplemodeling.textus.knowledge-hub.datastores.application"
    val descriptor = root.resolve("assembly.yaml")
    Files.writeString(descriptor, s"""subsystem: material-evidence-integration-spec
version: 0.1.0-SNAPSHOT
components:
  - namespace: org.simplemodeling.textus
    id: KnowledgeWorkbench
    version: 0.1.0-SNAPSHOT
${lakecomponent}${hubcomponent}subsystemCapabilities:
  providers:
    - name: workbench-runtime-facilities
      component: org.simplemodeling.textus.KnowledgeWorkbench
      provides:
        - datastore.optimistic-concurrency@1
        - datastore.persistent@1
        - datastore.transactional@1
        - user-context.current@1
config:
  textus.subsystem.user-mode: standalone
  $prefix.kind: postgresql
  $prefix.jdbc.url: ${database.getJdbcUrl}
  $prefix.jdbc.user: ${database.getUsername}
  $prefix.jdbc.password: ${database.getPassword}
  $prefix.jdbc.driver: org.postgresql.Driver
  $hubprefix.policy: local-default
  $hubprefix.kind: sqlite
  $hubprefix.path: ${root.resolve("hub.db")}
security:
  authentication:
    convention: disabled
    fallbackPrivilege: disabled
    localSubject:
      id: $subject
""")
    val discovery = Vector("--discover=classes", s"--component-dev-dir=$repository") ++
      live.toVector.map(_ => s"--component-dev-dir=$lake") ++ (if (includehub) Vector(s"--component-file=$hubcar") else Vector.empty)
    val handle = CncfBootstrap.initialize(BootstrapConfig(cwd = root,
      args = (discovery ++ Vector(s"--textus.assembly.descriptor=$descriptor", s"--textus.local-data.root=$root", "command")).toArray,
      modeHint = Some(RunMode.Command))).TAKE
    try {
      val subsystem = handle.subsystem
      live.foreach { _ =>
        val component = subsystem.components.find(_.name == "org.simplemodeling.textus.KnowledgeLake").get
        val mode = component.factoryOption.toVector.flatMap(_.initializationParameterDeclarations).find(_.name == "material.reference.mode").get
        require(component.initializationParameters.resolve(mode).TAKE.value.contains("packages"), "Actual TKL package reader must be selected")
      }
      val authentication = new FixtureAuthentication(token, subject)
      val id = ComponentId("org.simplemodeling.textus.MaterialEvidenceIntegrationAuthentication")
      val core = Component.Core.create(name = id.name, componentId = id, instanceId = ComponentInstanceId.default(id),
        protocol = org.goldenport.protocol.Protocol(services = org.goldenport.protocol.spec.ServiceDefinitionGroup(Vector.empty)))
      authentication.initialize(ComponentInit(subsystem, core, ComponentOrigin.Builtin))
      subsystem.add(authentication)
      val existing = subsystem.descriptor.get
      val security = existing.security.getOrElse(GenericSubsystemSecurityBinding())
      val ingress = security.authentication.getOrElse(GenericSubsystemAuthenticationBinding())
      subsystem.withDescriptor(existing.copy(componentBindings = existing.componentBindings :+ GenericSubsystemComponentBinding(authentication.name),
        security = Some(security.copy(authentication = Some(ingress.copy(convention = Some("enabled"), fallbackPrivilege = Some("disabled"),
          providers = ingress.providers :+ GenericSubsystemAuthenticationProviderBinding(
            name = authentication.providerName, component = authentication.name, enabled = Some(true))))))))
      handle
    } catch {
      case error: Throwable => handle.close(); throw error
    }
  }

  private def _live_selection(): LiveSelection = {
    val selectionpath = Path.of(sys.props.getOrElse("textus.test.real-material-selection",
      throw new IllegalArgumentException("Missing existing real Material selection locator")))
    val connection = Path.of(sys.props.getOrElse("textus.test.drive-connection",
      throw new IllegalArgumentException("Missing existing read-only connection locator")))
    require(selectionpath.isAbsolute && connection.isAbsolute, "Selected live locators must be absolute")
    require(Files.isRegularFile(connection), "Existing read-only connection locator is unavailable")
    // Only the TKL/CNCF connection owner reads credentials. This consumer reads no credential content.
    val document = _json(Files.readString(selectionpath))
    require(document.hcursor.get[String]("schema").contains("ess3.actual-app-material-read-selection/1"), "Wrong selection kind")
    val original = document.hcursor.get[Vector[Json]]("originals").toOption.get.head
    val resourceid = original.hcursor.get[String]("resourceId").toOption.get
    val source = Path.of(original.hcursor.get[String]("sourcePath").toOption.get)
    val sourceroot = Path.of(document.hcursor.get[String]("sourceRoot").toOption.get)
    require(source == sourceroot.resolve("materials").resolve(resourceid + ".bin"), "Unexpected selected comparison original")
    val length = original.hcursor.get[Int]("byteLength").toOption.get
    require(length > 0 && length <= 16777216 && Files.size(source) == length, "Wrong bounded original byte length")
    val role = original.hcursor.get[String]("role").toOption.get
    val assets = document.hcursor.downField("expectedManifest").get[Vector[Json]]("assets").toOption.get
    val selected = assets.filter(asset => asset.hcursor.get[String]("path").contains("assets/" + resourceid) &&
      asset.hcursor.get[String]("role").contains(role))
    require(selected.size == 1, "Ambiguous comparison asset")
    LiveSelection(document, connection, document.hcursor.get[String]("materialRef").toOption.get, resourceid,
      selected.head.hcursor.get[String]("path").toOption.get, role, Files.readAllBytes(source))
  }

  private def _http_original(app: HttpApp[IO], token: String, materialref: String, resourceid: String): Array[Byte] = {
    val query = "materialRef=" + java.net.URLEncoder.encode(materialref, "UTF-8") +
      "&resourceId=" + java.net.URLEncoder.encode(resourceid, "UTF-8")
    val response = app.run(WebRequest[IO](Method.GET, Uri.unsafeFromString(
      "/rest/v1/knowledge-workbench/MaterialWork/readOriginal?" + query)).putHeaders(
      Header.Raw(CIString("Authorization"), "Bearer " + token))).unsafeRunSync()
    require(response.status.code == 200, "Expected successful actual HTTP original")
    val headers = response.headers.headers
    require(headers.find(_.name.toString.equalsIgnoreCase("Content-Type")).exists(_.value == "image/jpeg"), "Original MIME differs")
    require(headers.find(_.name.toString.equalsIgnoreCase("Cache-Control")).exists(_.value == "private, no-store"), "Original privacy differs")
    require(headers.find(_.name.toString.equalsIgnoreCase("X-Content-Type-Options")).exists(_.value == "nosniff"), "Original sniffing policy differs")
    require(headers.find(_.name.toString.equalsIgnoreCase("Content-Security-Policy")).exists(_.value == "default-src 'none'; sandbox"), "Original CSP differs")
    require(headers.find(_.name.toString.equalsIgnoreCase("Content-Disposition")).exists(_.value.startsWith("attachment;")), "Original disposition differs")
    response.body.take(16777217L).compile.toVector.unsafeRunSync().toArray
  }
}
