package org.simplemodeling.textus.knowledgeworkbench

import java.nio.file.{Files, Path}
import cats.data.NonEmptyVector
import io.circe.parser.parse
import org.goldenport.cncf.action.{ActionCall, ProcedureActionCall, QueryAction}
import org.goldenport.cncf.bootstrap.{BootstrapConfig, CncfBootstrap, CncfHandle}
import org.goldenport.cncf.cli.RunMode
import org.goldenport.cncf.component.{Component, ComponentId, ComponentInit, ComponentInstanceId, ComponentOrigin}
import org.goldenport.cncf.context.{ExecutionContext, Principal, PrincipalId}
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.protocol.{Property, Protocol, Request}
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.protocol.spec
import org.scalacheck.{Gen, Prop, Test}
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import org.goldenport.Consequence
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgeworkbench.value as WorkbenchValue

/*
 * @since   Oct. 28, 2026
 * @version Oct. 28, 2026
 */
final class MaterialCandidateWorkSpec extends AnyWordSpec with Matchers with GivenWhenThen with BeforeAndAfterAll {
  private var _postgres: Option[MaterialCandidateWorkSpec.LivePostgresql] = None
  import MaterialWorkFixture.*

  "Explicit Material-backed Candidate work" should {
    "dispatch the generated authenticated command and retain exact source selectors without inventing readiness" in {
      Given("the public native Workbench and a caller-authorized logical Material")
      _with_session { session =>
        val request = session.request("createCandidateFromMaterial", _fields("intent:1"))
        When("the operator explicitly creates work")
        val call = session.call(request).TAKE
        val result = _result(session.executeRequest(request))
        Then("native registration confirms identity and retains the first exact proposal and metadata")
        call shouldBe a[KnowledgeWorkbenchComponent.MaterialWorkService.CreateCandidateFromMaterialActionCall]
        result.created shouldBe true
        result.candidate.id.print should not be empty
        result.candidate.producerRef.map(_.value) shouldBe Some("workbench-material")
        result.candidate.sourceKind.map(_.value) shouldBe Some("tkl-material")
        result.candidate.preparationState.map(_.value) shouldBe Some("unprepared")
        result.candidate.context.map(_.value) shouldBe Some("")
        result.candidate.preparedMaterialRefs shouldBe empty
        val evidence = result.candidate.evidenceRefs.head
        evidence.id shouldBe materialRef
        evidence.retrievalState shouldBe Some("unverified")
        evidence.revision shouldBe None
        evidence.snapshotRef shouldBe None
        val original = parse(result.candidate.originalProposal.get.value).toOption.get
        original.hcursor.downField("materialSelection").get[String]("materialRef").toOption shouldBe Some(materialRef)
        original.hcursor.downField("materialSelection").get[Vector[String]]("resourceIds").toOption shouldBe Some(Vector(resourceId))
        session.producer.calls.map(_.request.operation) shouldBe Vector("getMaterial")
        session.producer.calls.head.subject.subjectId shouldBe owner
        session.component.isMcpReady("MaterialWork", "createCandidateFromMaterial") shouldBe false
      }
    }

    "return current human work on same-intent resend without reading a now-denied source or overwriting the original" in {
      Given("a created Candidate followed by a native revision-aware human edit and Hold")
      val handle = _pg_handle(Files.createTempDirectory(Path.of("target").toAbsolutePath, "material-resend-postgres-"))
      try {
        val producer = _install_producer(handle.subsystem)
        def _execute_(operation: String, fields: Vector[(String, Any)], service: String = "Candidate") =
          _pg_call(handle.subsystem, service, operation, fields)
        val first = _result(_execute_("createCandidateFromMaterial", _fields("intent:resend"), "MaterialWork"))
        val work = _work(_execute_("getCandidateWork", Vector("id" -> first.candidate.id.print)))
        val edited = _work(_execute_("editCandidateWork", Vector("id" -> first.candidate.id.print,
          "expectedRevision" -> work.revision, "content" -> "Human content",
          "evidenceRefs" -> Vector(Record.data("id" -> "human:replacement", "kind" -> "material",
            "retrievalState" -> "unverified", "limitation" -> "Human selected another source")),
          "groundingRefs" -> Vector.empty[Record],
          "mappingRefs" -> Vector.empty[Record], "reason" -> "Human edit")))
        val held = _work(_execute_("decideCandidateWork", Vector("id" -> first.candidate.id.print,
          "expectedRevision" -> edited.revision, "decision" -> "hold", "reason" -> "Inspect source later")))
        producer.respondWith(Consequence.securityPermissionDenied("Source is now denied"))
        When("the same intent is explicitly sent again with changed admitted content and source")
        val resent = _result(_execute_("createCandidateFromMaterial", _fields("intent:resend")
          .map { case (key, input) => key -> (if (key == "materialRef") "other:material" else if (key == "content") "Incoming replacement" else input) }, "MaterialWork"))
        Then("the existing current native Candidate and first source survive without TKL access")
        resent.created shouldBe false
        resent.candidate.id shouldBe first.candidate.id
        resent.candidate.content.map(_.value) shouldBe Some("Human content")
        resent.candidate.evidenceRefs shouldBe held.candidate.evidenceRefs
        resent.candidate.evidenceRefs.map(_.id) shouldBe Vector("human:replacement")
        resent.candidate.originalProposal shouldBe first.candidate.originalProposal
        resent.candidate.decisions shouldBe held.candidate.decisions
        producer.calls should have size 1
      } finally handle.close()
    }

    "validate the whole local command even when its intent already exists" in {
      Given("an accessible existing intent and nonblank/distinct/Name admission counterexamples")
      _with_session { session =>
        _result(session.execute("createCandidateFromMaterial", _fields("intent:validate")))
        val invalid = Vector[(String, Any)]("proposalId" -> " ", "materialRef" -> "", "resourceIds" -> Vector.empty[String],
          "resourceIds" -> Vector(resourceId, resourceId), "resourceIds" -> Vector(" "),
          "resourceIds" -> resourceId, "resourceIds" -> Vector(1), "context" -> 1, "context" -> io.circe.Json.Null,
          "name" -> "", "name" -> ("x" * 257), "content" -> "\n ")
        When("each invalid command reaches normal public dispatch")
        invalid.foreach { case (key, input) =>
          session.execute("createCandidateFromMaterial", _fields("intent:validate").map {
            case (`key`, _) => key -> input
            case field => field
          }) shouldBe a[Consequence.Failure[?]]
        }
        Then("none reads the source or introduces another Candidate")
        session.producer.calls should have size 1
        WorkbenchValue.ListCandidatesResult.createC(_record(session.candidates())).TAKE.items should have size 1
      }
    }

    "create nothing for absent, denied, unavailable, malformed or noncorresponding exact sources" in {
      Given("structured public failures and metadata with wrong references, bad resources or duplicate resource identities")
      _with_session { session =>
        val responses: Vector[Consequence[OperationResponse]] = Vector(
          Consequence.resourceNotFound("Missing source"), Consequence.securityPermissionDenied("Denied source"),
          Consequence.serviceUnavailable("Source unavailable"),
          Consequence.success(OperationResponse(Record.data("materialRef" -> "different", "title" -> "Source", "resources" -> Vector.empty[Record], "provenance" -> Vector.empty[Record]))),
          Consequence.success(OperationResponse(Record.data("materialRef" -> materialRef, "title" -> "Source", "resources" -> "malformed", "provenance" -> Vector.empty[Record]))),
          Consequence.success(OperationResponse(_metadata(Vector.empty))),
          Consequence.success(OperationResponse(_metadata(Vector(resourceId, resourceId)))))
        When("a new explicit intent encounters each source outcome")
        responses.zipWithIndex.foreach { case (response, index) =>
          session.producer.respondWith(response)
          session.execute("createCandidateFromMaterial", _fields(s"failure:$index")) shouldBe a[Consequence.Failure[?]]
        }
        Then("the native Candidate collection stays empty and no original/materialize operation is attempted")
        WorkbenchValue.ListCandidatesResult.createC(_record(session.candidates())).TAKE.items shouldBe empty
        session.producer.calls.map(_.request.operation) shouldBe Vector.fill(responses.size)("getMaterial")
      }
    }

    "require authentication and preserve independent producer authorization" in {
      Given("an anonymous caller and a separately authenticated subject denied by the producer")
      _with_session { session =>
        When("each invokes the public command")
        session.execute("createCandidateFromMaterial", _fields("anonymous"), sessionId = None) shouldBe a[Consequence.Failure[?]]
        session.producer.calls shouldBe empty
        val denied = Consequence.securityPermissionDenied[OperationResponse]("Owner denied source")
        session.producer.respondWith(denied)
        session.execute("createCandidateFromMaterial", _fields("other"), sessionId = Some(otherSession)) shouldBe denied
        Then("the source sees the actual other caller and neither creates work")
        session.producer.calls.head.subject.subjectId shouldBe otherSubject
        WorkbenchValue.ListCandidatesResult.createC(_record(session.candidates())).TAKE.items shouldBe empty
      }
    }

    "preserve ordered opaque selectors and optional context as properties of the original observation" in {
      Given("generated commands over distinct opaque resource strings and both optional-context states")
      val property = Prop.forAll(Gen.nonEmptyListOf(Gen.alphaNumStr.suchThat(_.nonEmpty)), Gen.option(Gen.alphaNumStr)) { (ids, context) =>
        val selected = ids.distinct.toVector.map(id => s" opaque:$id/東京 %2F ")
        val fields = _fields("property").filterNot { case (key, _) => key == "resourceIds" || key == "context" } ++
          Vector("resourceIds" -> selected) ++ context.map(text => "context" -> text)
        val command = WorkbenchValue.CreateCandidateFromMaterial.createC(Record.data(fields*)).TAKE
        val metadata = _metadata(selected.reverse)
        When("the admitted proposal is formed from differently ordered public metadata")
        val proposal = impl.MaterialCandidateWork.validate(command).flatMap(_ => impl.MaterialCandidateWork.proposal(command, metadata)).TAKE
        val original = parse(proposal).toOption.get
        Then("selection order and exact context presence remain caller-owned without source version fabrication")
        original.hcursor.downField("materialSelection").get[Vector[String]]("resourceIds").toOption.contains(selected) &&
          original.hcursor.get[String]("context").toOption == context &&
          !original.hcursor.downField("evidenceRefs").downArray.downField("revision").succeeded
      }
      Test.check(Test.Parameters.default.withMinSuccessfulTests(40), property).passed shouldBe true
    }

    "reject unknown registration acknowledgements without reporting success or replay" in {
      Given("a logical response without a confirmed native Candidate")
      val unknown = OperationResponse(Record.data("status" -> "unknown"))
      When("the registration acknowledgement is interpreted")
      val result = impl.MaterialCandidateWork.confirmed(unknown)
      Then("a structured failure remains instead of a manufactured identity")
      result shouldBe a[Consequence.Failure[?]]
    }

    "retain native PostgreSQL identity, exact first association and human revisions across runtime restart" in {
      Given("an actual PostgreSQL datastore and ordinary Workbench native runtime with a controlled public Material producer")
      val root = Files.createTempDirectory(Path.of("target").toAbsolutePath, "material-candidate-postgres-")
      val writer = _pg_handle(root)
      val first = try {
        _install_producer(writer.subsystem)
        val created = _result(_pg_call(writer.subsystem, "MaterialWork", "createCandidateFromMaterial", _fields("postgres:restart")))
        val before = _work(_pg_call(writer.subsystem, "Candidate", "getCandidateWork", Vector("id" -> created.candidate.id.print)))
        val changed = _work(_pg_call(writer.subsystem, "Candidate", "editCandidateWork", _edit_fields(before, "Durable human content")))
        val stale = _pg_call(writer.subsystem, "Candidate", "editCandidateWork", _edit_fields(before, "Stale overwrite"))
        stale shouldBe a[Consequence.Failure[?]]
        changed.revision should be > before.revision
        val held = _work(_pg_call(writer.subsystem, "Candidate", "decideCandidateWork", Vector("id" -> changed.candidate.id.print,
          "expectedRevision" -> changed.revision, "decision" -> "hold", "reason" -> "Await source inspection")))
        held.candidate.decisions.last.actor shouldBe "material-candidate-postgres-user"
        // Raw unversioned Material must never become Review/Approve-ready through creation.
        Vector("review", "approve").foreach { decision =>
          _pg_call(writer.subsystem, "Candidate", "decideCandidateWork", Vector("id" -> held.candidate.id.print,
            "expectedRevision" -> held.revision, "decision" -> decision, "reason" -> "Attempt without source version")) shouldBe a[Consequence.Failure[?]]
        }
        held
      } finally writer.close()
      When("a fresh runtime opens the same PostgreSQL store without any TKL component and receives the retained intent")
      val reader = _pg_handle(root)
      try {
        val resent = _result(_pg_call(reader.subsystem, "MaterialWork", "createCandidateFromMaterial", _fields("postgres:restart")))
        val reopened = _work(_pg_call(reader.subsystem, "Candidate", "getCandidateWork", Vector("id" -> first.candidate.id.print)))
        val component = reader.subsystem.components.find(_.name == KnowledgeWorkbenchComponent.name).get
        val base = component.logic.executionContext()
        val otherprincipal = new Principal {
          val id = PrincipalId("material-candidate-other-user")
          val attributes = base.security.principal.attributes
        }
        val othercontext = ExecutionContext.withSecurityContext(base, base.security.copy(principal = otherprincipal))
        val denied = reader.subsystem.executeOperationResponseInContext(Request.of(component = KnowledgeWorkbenchComponent.name,
          service = "Candidate", operation = "getCandidateWork",
          properties = List(Property("id", first.candidate.id.print, None))), othercontext)
        Then("source-independent reconciliation returns the exact current persisted work and native revision while another caller cannot load it")
        denied shouldBe a[Consequence.Failure[?]]
        resent.created shouldBe false
        resent.candidate.toRecord() shouldBe first.candidate.toRecord()
        reopened.revision shouldBe first.revision
        reopened.candidate.originalProposal shouldBe first.candidate.originalProposal
        reopened.candidate.content.map(_.value) shouldBe Some("Durable human content")
      } finally reader.close()
    }

    "keep deliberate new intent distinct on the same source" in {
      Given("the same source and exact ordered selection")
      _with_session { session =>
        When("two deliberately distinct caller intent keys are submitted")
        val results = Vector("intent:a", "intent:b").map(key => _result(session.execute("createCandidateFromMaterial", _fields(key))))
        Then("both native creations retain independent identities")
        results.map(_.created) shouldBe Vector(true, true)
        results.map(_.candidate.id).distinct should have size 2
      }
    }
  }

  override protected def afterAll(): Unit =
    try _postgres.foreach(_.stop()) finally super.afterAll()

  private def _metadata(ids: Vector[String]): Record =
    Record.data("materialRef" -> materialRef, "title" -> "Public logical Material",
      "resources" -> ids.map(id => Record.data("resourceId" -> id, "role" -> "original", "mediaType" -> "image/png")),
      "provenance" -> Vector.empty[Record])

  private def _with_session[A](body: Session => A): A = withSession() { session =>
    session.producer.respondWith(Consequence.success(OperationResponse(_metadata(Vector(resourceId)))))
    body(session)
  }

  private def _edit_fields(work: WorkbenchValue.CandidateWorkResult, content: String): Vector[(String, Any)] =
    Vector("id" -> work.candidate.id.print, "expectedRevision" -> work.revision, "content" -> content,
      "evidenceRefs" -> work.candidate.evidenceRefs.map(_.toRecord()), "groundingRefs" -> Vector.empty[Record],
      "mappingRefs" -> Vector.empty[Record], "reason" -> "Native human edit")

  private def _pg_call(subsystem: Subsystem, service: String, operation: String,
      fields: Vector[(String, Any)]): Consequence[OperationResponse] =
    subsystem.executeOperationResponse(Request.of(component = KnowledgeWorkbenchComponent.name, service = service,
      operation = operation, properties = fields.map { case (key, input) => Property(key, input, None) }.toList))

  private def _install_producer(subsystem: Subsystem): Producer = {
    val producer = new Producer
    producer.respondWith(Consequence.success(OperationResponse(_metadata(Vector(resourceId)))))
    val producerid = ComponentId(producerName)
    val operation = new spec.OperationDefinition {
      override val specification = spec.OperationDefinition.Specification(name = "getMaterial",
        request = spec.RequestDefinition(), response = spec.ResponseDefinition.void)
      override def createOperationRequest(req: Request): Consequence[OperationRequest] = Consequence.success(new QueryAction {
        override val request: Request = req
        override def createCall(callcore: ActionCall.Core): ActionCall = new ProcedureActionCall {
          override val core: ActionCall.Core = callcore
          override def execute(): Consequence[OperationResponse] =
            producer.receive(req, callcore.executionContext)
        }
      })
    }
    val protocol = Protocol(services = spec.ServiceDefinitionGroup(Vector(spec.ServiceDefinition(
      name = "Material", operations = spec.OperationDefinitionGroup(NonEmptyVector.one(operation))))))
    val core = Component.Core.create(name = producerid.name, componentId = producerid,
      instanceId = ComponentInstanceId.default(producerid), protocol = protocol)
    producer.initialize(ComponentInit(subsystem, core, ComponentOrigin.Builtin))
    subsystem.add(producer)
    producer
  }

  private def _pg_handle(root: Path): CncfHandle = {
    val database = _postgres.getOrElse {
      val instance = new MaterialCandidateWorkSpec.LivePostgresql()
      instance.start()
      _postgres = Some(instance)
      instance
    }
    val repository = Path.of(".").toAbsolutePath.normalize()
    val descriptor = root.resolve("assembly.yaml")
    val prefix = "textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application"
    Files.writeString(descriptor, s"""subsystem: material-candidate-postgres-spec
version: 0.1.0-SNAPSHOT
components:
  - namespace: org.simplemodeling.textus
    id: KnowledgeWorkbench
    version: 0.1.0-SNAPSHOT
subsystemCapabilities:
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
security:
  authentication:
    convention: disabled
    fallbackPrivilege: disabled
    localSubject:
      id: material-candidate-postgres-user
""")
    CncfBootstrap.initialize(BootstrapConfig(cwd = root,
      args = Array("--discover=classes", s"--component-dev-dir=$repository", s"--textus.assembly.descriptor=$descriptor",
        s"--textus.local-data.root=$root", "command"), modeHint = Some(RunMode.Command))).TAKE
  }

  private def _fields(intent: String): Vector[(String, Any)] = Vector("proposalId" -> intent,
    "materialRef" -> materialRef, "resourceIds" -> Vector(resourceId), "name" -> "Operator name",
    "content" -> " Exact operator statement\n", "context" -> "")

  private def _record(result: Consequence[OperationResponse]): Record = result.TAKE match {
    case OperationResponse.RecordResponse(record) => record
    case other => fail(s"Expected native Record response: $other")
  }
  private def _result(result: Consequence[OperationResponse]): WorkbenchValue.RegisterProposalResult =
    WorkbenchValue.RegisterProposalResult.createC(_record(result)).TAKE
  private def _work(result: Consequence[OperationResponse]): WorkbenchValue.CandidateWorkResult =
    WorkbenchValue.CandidateWorkResult.createC(_record(result)).TAKE
}

object MaterialCandidateWorkSpec {
  final class LivePostgresql extends PostgreSQLContainer(DockerImageName.parse("postgres:17.11-bookworm")) {
    withDatabaseName("material_candidate_work")
    withUsername("workbench")
    withPassword("workbench-owned-test")
    withImagePullPolicy(_ => false)
    withCommand("postgres", "-c", "fsync=on", "-c", "synchronous_commit=on")
  }
}
