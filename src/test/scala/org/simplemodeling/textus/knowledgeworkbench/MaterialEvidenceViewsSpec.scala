package org.simplemodeling.textus.knowledgeworkbench

import java.nio.file.{Files, Path}
import cats.effect.{Deferred, IO, Ref}
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import com.comcast.ip4s.{Host, Port}
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.cncf.bootstrap.{BootstrapConfig, CncfBootstrap, CncfHandle}
import org.goldenport.cncf.cli.RunMode
import org.goldenport.cncf.display.{DisplayProjection, GeneratedLogicalUiAbi}
import org.goldenport.cncf.http.WorkbenchHttpFixture
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.http4s.{HttpApp, Method, Request as WebRequest, Response, Status, Uri}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.`Content-Type`
import org.http4s.MediaType
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgeworkbench.{value as WorkbenchValue}
import org.simplemodeling.textus.knowledgeworkbench.impl.CandidateDisplay
import org.simplemodeling.textus.knowledgeworkbench.logicalui.KnowledgeWorkbenchLogicalUiDefinitions
import scala.concurrent.duration.*

/*
 * Controlled producer/loopback observations are not real Google Drive or user OAuth.
 * @since   Oct. 28, 2026
 * @version Oct. 28, 2026
 */
final class MaterialEvidenceViewsSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialEvidenceViewsSpec.*

  "Material evidence in the ordinary Workbench" should {
    "project exact correspondence and inert owning action availability without source acquisition" in {
      Given("a native metadata-only Material Candidate beside the real Hub CAR")
      _with_runtime { (subsystem, producer) =>
        val current = _create(subsystem, "view:initial")
        When("Detail and business metadata are read")
        val display = _display(subsystem, current.candidate.id.print)
        val descriptor = _business(subsystem)
        Then("the existing MaterialWork operation is the only refresh target and source acquisition is still absent")
        _availability(display) shouldBe "enabled"
        _field(display, "material-evidence").hcursor.get[String]("text") shouldBe Right(Record.data("items" -> Vector.empty[Record]).toJsonString)
        val action = descriptor.hcursor.get[Vector[Json]]("actions").toOption.get.find(_.hcursor.get[String]("id").contains("refresh-evidence")).get
        action.hcursor.downField("operation").get[String]("service") shouldBe Right("MaterialWork")
        action.hcursor.downField("operation").get[String]("operation") shouldBe Right("refreshCandidateMaterialEvidence")
        producer.evidenceRequests shouldBe 0
        _get(subsystem, current.candidate.id.print).toRecord() shouldBe current.toRecord()
        _field(display, "eligible").hcursor.get[Boolean]("boolean") shouldBe Right(false)
      }
    }

    "carry native observed source versions through distinct HTTP refresh Review Approve and real Hub Admission" in {
      Given("an unverified native Candidate and the normal HTTP router")
      _with_runtime { (subsystem, producer) =>
        val first = _create(subsystem, "view:admission")
        val app = WorkbenchHttpFixture.app(subsystem)
        When("an explicit HTTP refresh applies a public producer observation")
        _http(app, _refresh_path, Some(_refresh_body(first)))._1 shouldBe 200
        val bound = _get(subsystem, first.candidate.id.print)
        Then("Detail shows the exact original/resource/reference correspondence and truthful limitations")
        bound.candidate.materialEvidence.get.materialRef shouldBe WorkbenchHttpFixture.materialRef
        bound.candidate.materialEvidence.get.resourceIds shouldBe Vector(WorkbenchHttpFixture.resourceId)
        bound.candidate.evidenceRefs.head.revision shouldBe Some("version-1")
        val display = _display(subsystem, first.candidate.id.print)
        _field(display, "material-evidence").hcursor.get[String]("text") shouldBe
          Right(Record.data("items" -> bound.candidate.materialEvidence.toVector.map(_.toRecord())).toJsonString)
        producer.subjects.distinct shouldBe Vector(caller)
        val approved = _decide(subsystem, _decide(subsystem, bound, "review"), "approve")
        When("an unchanged observation is explicitly acquired, then the producer version actually changes")
        _http(app, _refresh_path, Some(_refresh_body(approved)))._1 shouldBe 200
        _get(subsystem, first.candidate.id.print).toRecord() shouldBe approved.toRecord()
        producer.sourceVersion = Some("version-2")
        _http(app, _refresh_path, Some(_refresh_body(approved)))._1 shouldBe 200
        val changed = _get(subsystem, first.candidate.id.print)
        Then("the changed evidence requires fresh human Review and Approve before Information submission")
        changed.state shouldBe "Draft"
        changed.approvalValid shouldBe false
        changed.candidate.decisions.init shouldBe approved.candidate.decisions
        _http(app, _candidate_path("admitCandidateWork"), Some(_selection_body(changed)))._1 should be >= 400
        val renewed = _decide(subsystem, _decide(subsystem, changed, "review"), "approve")
        _http(app, _candidate_path("admitCandidateWork"), Some(_selection_body(renewed)))._1 shouldBe 200
        val admitted = _get(subsystem, first.candidate.id.print)
        admitted.state shouldBe "Admitted"
        admitted.candidate.admissions.last.status shouldBe "Confirmed"
        admitted.candidate.admissions.last.approval.target shouldBe renewed.target.get
        admitted.candidate.materialEvidence shouldBe renewed.candidate.materialEvidence
        producer.evidenceRequests shouldBe 3
      }
    }

    "show a successful but unversioned observation as unverified without enabling Review" in {
      Given("an original whose public producer cannot attest its version")
      _with_runtime { (subsystem, producer) =>
        val first = _create(subsystem, "view:unversioned")
        producer.sourceVersion = None
        When("the explicit source observation is applied and displayed")
        val current = _refresh(subsystem, first)
        val display = _display(subsystem, first.candidate.id.print)
        Then("missing version remains absent and every-reference eligibility remains false")
        current.candidate.evidenceRefs.head.revision shouldBe None
        current.candidate.evidenceRefs.head.retrievalState shouldBe Some("unverified")
        _field(display, "eligible").hcursor.get[Boolean]("boolean") shouldBe Right(false)
        current.reviewEligible shouldBe false
        _field(display, "evidence").hcursor.get[String]("text").toOption.get should include ("unverified")
        _call(subsystem, "Candidate", "decideCandidateWork", Vector("id" -> current.candidate.id.print,
          "expectedRevision" -> current.revision, "decision" -> "review", "reason" -> "Must remain ineligible")) shouldBe
          a[Consequence.Failure[?]]
        _get(subsystem, first.candidate.id.print).toRecord() shouldBe current.toRecord()
      }
    }

    "keep detached human references unavailable for refresh without a source read" in {
      Given("a source-bound Candidate and a human replacement of managed references")
      _with_runtime { (subsystem, producer) =>
        val first = _refresh(subsystem, _create(subsystem, "view:detached"))
        val edited = _edit(subsystem, first, first.candidate.evidenceRefs.map(_.copy(revision = Some("human:revision"))))
        val count = producer.evidenceRequests
        When("the ordinary screen and refresh command are requested")
        val display = _display(subsystem, first.candidate.id.print)
        val response = _http(WorkbenchHttpFixture.app(subsystem), _refresh_path, Some(_refresh_body(edited)))
        Then("native detachment stays visible, inert availability is disabled and the server still refuses the command")
        edited.candidate.materialEvidence.get.status shouldBe "detached"
        _availability(display) shouldBe "disabled"
        _field(display, "material-evidence").hcursor.get[String]("text").toOption.get should include ("detached")
        response._1 should be >= 400
        producer.evidenceRequests shouldBe count
        _get(subsystem, first.candidate.id.print).toRecord() shouldBe edited.toRecord()
      }
    }

    "retain safe structured denied changed unavailable and stale failures without mutation or source replay" in {
      Given("an accessible Candidate and a controlled public producer behind native HTTP")
      _with_runtime { (subsystem, producer) =>
        val first = _create(subsystem, "view:failures")
        val app = WorkbenchHttpFixture.app(subsystem)
        val failures = Vector(
          Consequence.securityPermissionDenied[OperationResponse]("Controlled source denied"),
          Consequence.serviceUnavailable[OperationResponse]("Controlled source unavailable"),
          Consequence.resourceInvalid[OperationResponse]("Controlled original changed during observation"))
        failures.foreach { failure =>
          producer.sourceFailure = Some(failure)
          val count = producer.evidenceRequests
          When("one explicit refresh receives the producer's structured failure")
          val response = _http(app, _refresh_path, Some(_refresh_body(first)))
          val error = _json(response._2).hcursor.downField("error")
          Then("the public HTTP error retains disclosed Conclusion information and no Candidate change or replay occurs")
          response._1 should be >= 400
          error.get[String]("message").toOption.get should include ("Controlled")
          error.downField("conclusion").focus shouldBe defined
          producer.evidenceRequests shouldBe count + 1
          _get(subsystem, first.candidate.id.print).toRecord() shouldBe first.toRecord()
        }
        producer.sourceFailure = None
        val current = _refresh(subsystem, first)
        val count = producer.evidenceRequests
        When("the original stale revision is reused")
        _http(app, _refresh_path, Some(_refresh_body(first)))._1 should be >= 400
        Then("revision checking refuses stale work before producer IO")
        producer.evidenceRequests shouldBe count
        _get(subsystem, first.candidate.id.print).toRecord() shouldBe current.toRecord()
      }
    }

    "reconcile a lost successful refresh response using only the known Candidate read" in {
      Given("a known unverified Candidate and one controlled lost success acknowledgement")
      _with_runtime { (subsystem, producer) =>
        val first = _create(subsystem, "view:lost-ack")
        val app = WorkbenchHttpFixture.app(subsystem)
        val lost = HttpApp[IO](request => app(request).flatMap { response =>
          if (request.uri.path.renderString.endsWith("/refreshCandidateMaterialEvidence.json") && response.status.isSuccess)
            response.body.compile.drain.as(Response[IO](Status.ServiceUnavailable))
          else IO.pure(response)
        })
        When("one refresh commits but its success cannot be acknowledged")
        _http(lost, _refresh_path, Some(_refresh_body(first)))._1 shouldBe 503
        val count = producer.evidenceRequests
        val read = _http(app, _candidate_path("getCandidateWork") + "?id=" + _encode(first.candidate.id.print))
        Then("read reconciliation sees the same identity and one application without replaying source or mutation")
        read._1 shouldBe 200
        val current = _get(subsystem, first.candidate.id.print)
        current.candidate.materialEvidence.get.applications should have size 1
        current.candidate.decisions.map(_.decision.value) shouldBe Vector("edit")
        current.candidate.id shouldBe first.candidate.id
        producer.evidenceRequests shouldBe count
      }
    }

    "preserve opaque source versions and markup as exact Display text under generated cases" in {
      Given("one real native binding and the generated Detail contract")
      _with_runtime { (subsystem, producer) =>
        val current = _refresh(subsystem, _create(subsystem, "view:opaque"))
        val count = producer.evidenceRequests
        val definitions = _success(GeneratedLogicalUiAbi.admitC(KnowledgeWorkbenchLogicalUiDefinitions.definitions))
        val projector = _success(DisplayProjection.detailC(definitions, "candidate-detail", "candidate.work", CandidateDisplay.detailRow))
        val fragments = Gen.listOfN(7, Gen.oneOf("資料", "\n", "<script>", "&", "😀", " %2F "))
        When("opaque exact version strings are projected without source access or HTML interpretation")
        val property = Prop.forAll(fragments) { pieces =>
          val version = "version:" + pieces.mkString
          val reference = current.candidate.evidenceRefs.head.copy(revision = Some(version))
          val binding = current.candidate.materialEvidence.get
          val application = binding.applications.last.copy(resources = binding.applications.last.resources.map(_.copy(appliedReference = reference)))
          val changed = current.copy(candidate = current.candidate.copy(evidenceRefs = Vector(reference),
            materialEvidence = Some(binding.copy(applications = Vector(application)))))
          val projected = _success(projector.projectC(changed)).model
          // The projection value is the exact Record JSON text; renderer uses text nodes.
          val fields = projected.asInstanceOf[org.goldenport.cncf.display.DisplayModel.DetailModel].sections.flatMap(_.fields)
          Then("the original opaque version is retained verbatim in the logical evidence record")
          fields.find(_.id == "evidence").get.value == org.goldenport.cncf.display.DisplayValue.Text(
            Record.data("items" -> Vector(reference.toRecord())).toJsonString)
        }
        val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(50), property)
        checked.passed shouldBe true
        checked.succeeded should be >= 50
        checked.discarded shouldBe 0
        producer.evidenceRequests shouldBe count
      }
    }

    if (sys.props.get("textus.test.material-evidence.browser-probe").contains("true")) {
      "serve the coordinated explicit evidence workflow to a real loopback browser" in {
        Given("the served Workbench, a controlled producer and actual local Hub CAR")
        val handle = _handle(_root())
        try {
          val subsystem = handle.subsystem
          val producer = WorkbenchHttpFixture.installMaterial(subsystem)
          val initial = _create(subsystem, "browser:material-evidence")
          val app = WorkbenchHttpFixture.app(subsystem)
          val ready = Path.of("target/material-evidence-browser-ready.json").toAbsolutePath
          Files.deleteIfExists(ready)
          When("the caller performs the retained browser procedure without command replay")
          val program = for {
            done <- Deferred[IO, Unit]
            refreshes <- Ref.of[IO, Int](0)
            admissions <- Ref.of[IO, Int](0)
            lose <- Ref.of[IO, Boolean](false)
            combined = HttpApp[IO](request => request.uri.path.renderString match {
              case "/__fixture_complete" => done.complete(()).as(Response[IO](Status.NoContent))
              case "/__fixture_deny" => IO { producer.denied = true }.as(Response[IO](Status.NoContent))
              case "/__fixture_allow" => IO { producer.denied = false }.as(Response[IO](Status.NoContent))
              case "/__fixture_version2" => IO { producer.sourceVersion = Some("version-2") }.as(Response[IO](Status.NoContent))
              case "/__fixture_version3_lost_ack" =>
                IO { producer.sourceVersion = Some("version-3") } *> lose.set(true).as(Response[IO](Status.NoContent))
              case path if path.endsWith("/refreshCandidateMaterialEvidence.json") =>
                refreshes.update(_ + 1) *> app(request).flatMap { response =>
                  if (response.status.isSuccess) lose.getAndSet(false).flatMap { selected =>
                    if (selected) response.body.compile.drain.as(Response[IO](Status.ServiceUnavailable)) else IO.pure(response)
                  } else IO.pure(response)
                }
              case path if path.endsWith("/admitCandidateWork.json") => admissions.update(_ + 1) *> app(request)
              case _ => app(request)
            })
            _ <- EmberServerBuilder.default[IO].withHost(Host.fromString("127.0.0.1").get)
              .withPort(Port.fromInt(0).get).withHttpApp(combined).build.use { server =>
                IO.blocking(Files.writeString(ready, Json.obj(
                  "baseUrl" -> Json.fromString(s"http://127.0.0.1:${server.address.getPort}"),
                  "applicationPath" -> Json.fromString("/web/knowledge-workbench/textus-knowledge-workbench"),
                  "candidateId" -> Json.fromString(initial.candidate.id.print),
                  "completePath" -> Json.fromString("/__fixture_complete"),
                  "denyPath" -> Json.fromString("/__fixture_deny"),
                  "allowPath" -> Json.fromString("/__fixture_allow"),
                  "version2Path" -> Json.fromString("/__fixture_version2"),
                  "version3LostAckPath" -> Json.fromString("/__fixture_version3_lost_ack"),
                  "finalState" -> Json.fromString("Admitted"),
                  "finalContent" -> Json.fromString("Browser evidence content"),
                  "expectedRefreshAttempts" -> Json.fromInt(5),
                  "expectedAdmissions" -> Json.fromInt(1),
                  "procedure" -> Json.fromString(browserProcedure),
                  "selectors" -> Json.fromString("#load-list #list #detail #content #edit-reason #evidence-state #evidence-reason #refresh-evidence #decision #decision-reason #admit #reload #candidate-source #original-preview")
                ).noSpaces)) *> done.get.timeout(12.minutes)
              }
            count <- refreshes.get
            admitted <- admissions.get
          } yield count -> admitted
          program.unsafeRunSync() shouldBe (5 -> 1)
          Then("native persistence and counts prove the explicit sequence, no-op, failed/unknown reconciliation and one confirmed Information")
          val result = _get(subsystem, initial.candidate.id.print)
          result.state shouldBe "Admitted"
          result.candidate.content.map(_.value) shouldBe Some("Browser evidence content")
          result.candidate.evidenceRefs.head.revision shouldBe Some("version-3")
          result.candidate.materialEvidence.get.applications should have size 3
          result.candidate.decisions.map(_.decision.value) shouldBe Vector("edit", "edit", "review", "approve",
            "edit", "review", "approve", "edit", "review", "approve")
          result.candidate.admissions should have size 1
          result.candidate.admissions.last.status shouldBe "Confirmed"
          producer.evidenceRequests shouldBe 5
          producer.subjects.distinct shouldBe Vector(caller)
        } finally handle.close()
      }
    }
  }
}

object MaterialEvidenceViewsSpec {
  val caller = "workbench-evidence-view-user"
  val browserProcedure = "Load Candidate list and select the sole Material Candidate. Inspect unverified source. Type content Browser evidence content; verify refresh is disabled and draft preserved, then save with edit reason. Optionally inspect original source and preview. Refresh with reason (version-1). Review then Approve separately. Refresh identical source again; verify state/revision/history unchanged. Visit version2Path, then refresh once; verify Draft/version-2 and retained content, then Review and Approve. Visit denyPath, refresh once and observe safe structured denial/blocked commands; visit allowPath and explicitly reload, without replay. Visit version3LostAckPath, refresh once; observe HTTP503/unknown acknowledgement, commands blocked and known Candidate retained. Explicitly reload only, verify Draft/version-3/one added application; Review then Approve separately. Admit approved Information once and observe confirmed canonical ID/revision. Signal completePath. Total refresh HTTP attempts must be5, including denied1 and lostACK1; Admission1. No real Drive data or user OAuth is exercised."
  private val _refresh_path = "/rest/v1/knowledge-workbench/MaterialWork/refreshCandidateMaterialEvidence.json"
  private def _candidate_path(operation: String): String = s"/rest/v1/knowledge-workbench/Candidate/$operation.json"
  private def _encode(text: String): String = java.net.URLEncoder.encode(text, "UTF-8")
  private def _json(text: String): Json = io.circe.parser.parse(text).fold(throw _, identity)
  private def _success[A](result: Consequence[A]): A = result.TAKE
  private def _record(result: Consequence[OperationResponse]): Record = result.TAKE match {
    case OperationResponse.RecordResponse(record) => record
    case other => throw new IllegalStateException(s"Expected native Record response: $other")
  }
  private def _call(subsystem: Subsystem, service: String, operation: String, fields: Vector[(String, Any)]): Consequence[OperationResponse] =
    subsystem.executeOperationResponse(Request.of(component = KnowledgeWorkbenchComponent.name, service = service, operation = operation,
      properties = fields.map { case (key, input) => Property(key, input, None) }.toList))
  private def _work(subsystem: Subsystem, service: String, operation: String, fields: Vector[(String, Any)]): WorkbenchValue.CandidateWorkResult =
    WorkbenchValue.CandidateWorkResult.createC(_record(_call(subsystem, service, operation, fields))).TAKE
  private def _get(subsystem: Subsystem, id: String): WorkbenchValue.CandidateWorkResult =
    _work(subsystem, "Candidate", "getCandidateWork", Vector("id" -> id))
  private def _create(subsystem: Subsystem, intent: String): WorkbenchValue.CandidateWorkResult = {
    val response = WorkbenchValue.RegisterProposalResult.createC(_record(_call(subsystem, "MaterialWork", "createCandidateFromMaterial", Vector(
      "proposalId" -> intent, "materialRef" -> WorkbenchHttpFixture.materialRef,
      "resourceIds" -> Vector(WorkbenchHttpFixture.resourceId), "name" -> "Material evidence browser", "content" -> "Initial browser content")))).TAKE
    _get(subsystem, response.candidate.id.print)
  }
  private def _selection_body(work: WorkbenchValue.CandidateWorkResult): String =
    Record.data("id" -> work.candidate.id.print, "expectedRevision" -> work.revision).toJsonString
  private def _refresh_body(work: WorkbenchValue.CandidateWorkResult): String =
    Record.data("id" -> work.candidate.id.print, "expectedRevision" -> work.revision, "reason" -> "Observe original evidence").toJsonString
  private def _refresh(subsystem: Subsystem, work: WorkbenchValue.CandidateWorkResult): WorkbenchValue.CandidateWorkResult =
    _work(subsystem, "MaterialWork", "refreshCandidateMaterialEvidence", Vector("id" -> work.candidate.id.print,
      "expectedRevision" -> work.revision, "reason" -> "Observe original evidence"))
  private def _decide(subsystem: Subsystem, work: WorkbenchValue.CandidateWorkResult, decision: String): WorkbenchValue.CandidateWorkResult =
    _work(subsystem, "Candidate", "decideCandidateWork", Vector("id" -> work.candidate.id.print,
      "expectedRevision" -> work.revision, "decision" -> decision, "reason" -> "Explicit human decision"))
  private def _edit(subsystem: Subsystem, work: WorkbenchValue.CandidateWorkResult,
      references: Vector[WorkbenchValue.ProposalReference]): WorkbenchValue.CandidateWorkResult =
    _work(subsystem, "Candidate", "editCandidateWork", Vector("id" -> work.candidate.id.print,
      "expectedRevision" -> work.revision, "content" -> work.candidate.content.get.value,
      "evidenceRefs" -> references.map(_.toRecord()), "groundingRefs" -> Vector.empty[Record],
      "mappingRefs" -> Vector.empty[Record], "reason" -> "Human reference change"))
  private def _display(subsystem: Subsystem, id: String): Json = _call(subsystem, "display", "read", Vector(
    "resourceType" -> "information-candidate", "screenId" -> "candidate-detail", "resourceId" -> id)).TAKE match {
    case OperationResponse.Json(payload) => payload
    case other => throw new IllegalStateException(s"Expected Display JSON: $other")
  }
  private def _business(subsystem: Subsystem): Json = _call(subsystem, "display", "describeBusiness", Vector(
    "resourceType" -> "information-candidate", "screenId" -> "candidate-detail")).TAKE match {
    case OperationResponse.Json(payload) => payload
    case other => throw new IllegalStateException(s"Expected business JSON: $other")
  }
  private def _field(display: Json, id: String): Json = display.hcursor.downField("model").get[Vector[Json]]("sections").toOption.get.flatMap(
    _.hcursor.get[Vector[Json]]("fields").toOption.get).find(_.hcursor.get[String]("id").contains(id)).get.hcursor.downField("value").focus.get
  private def _availability(display: Json): String = display.hcursor.downField("model").get[Vector[Json]]("actions").toOption.get.find(
    _.hcursor.get[String]("id").contains("refresh-evidence")).get.hcursor.get[String]("availability").toOption.get
  private def _http(app: HttpApp[IO], path: String, body: Option[String] = None): (Int, String) = {
    val request = WebRequest[IO](if (body.isDefined) Method.POST else Method.GET, Uri.unsafeFromString(path))
    val supplied = body.map(text => request.withEntity(text).putHeaders(`Content-Type`(MediaType.application.json))).getOrElse(request)
    val response = app.run(supplied).unsafeRunSync()
    response.status.code -> response.as[String].unsafeRunSync()
  }
  private def _root(): Path = Files.createTempDirectory(Files.createDirectories(Path.of("target/material-evidence-view-spec").toAbsolutePath), "case-")
  private def _with_runtime[A](body: (Subsystem, WorkbenchHttpFixture.MaterialProducer) => A): A = {
    val handle = _handle(_root())
    try body(handle.subsystem, WorkbenchHttpFixture.installMaterial(handle.subsystem)) finally handle.close()
  }
  private def _handle(root: Path): CncfHandle = {
    val repository = Path.of(".").toAbsolutePath.normalize()
    val hubcar = Path.of(sys.props.getOrElse("textus.test.knowledge-hub.car",
      Path.of(System.getProperty("user.home"), ".cncf/local/repository/car/org/simplemodeling/textus/" +
        "textus-knowledge-hub/0.1.0-SNAPSHOT/textus-knowledge-hub-0.1.0-SNAPSHOT.car").toString)).toAbsolutePath.normalize()
    val authored = Files.readString(repository.resolve("src/main/car/assembly-descriptor.yaml")).replace(
      "  - namespace: org.simplemodeling.textus\n    id: KnowledgeLake\n    version: 0.1.0-SNAPSHOT\n", "")
    val isolated = authored.replace("  textus.subsystem.user-mode: standalone\n",
      s"""  textus.subsystem.user-mode: standalone
  textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.kind: sqlite
  textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.path: ${root.resolve("workbench.db")}
  textus.component.org.simplemodeling.textus.knowledge-hub.datastores.application.kind: sqlite
  textus.component.org.simplemodeling.textus.knowledge-hub.datastores.application.path: ${root.resolve("hub.db")}
""")
    val descriptor = root.resolve("assembly.yaml")
    Files.writeString(descriptor, isolated.replace("    fallbackPrivilege: disabled\n",
      s"    fallbackPrivilege: disabled\n    localSubject:\n      id: $caller\n"))
    CncfBootstrap.initialize(BootstrapConfig(cwd = root,
      args = Array("--discover=classes", s"--component-dev-dir=$repository", s"--component-file=$hubcar",
        s"--textus.assembly.descriptor=$descriptor", s"--textus.local-data.root=$root", "command"), modeHint = Some(RunMode.Command))).TAKE
  }
}
