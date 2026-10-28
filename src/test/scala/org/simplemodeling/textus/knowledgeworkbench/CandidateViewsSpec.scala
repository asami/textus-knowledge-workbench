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
import org.goldenport.cncf.display.{DisplayModel, DisplayProjection, DisplayValue, GeneratedLogicalUiAbi}
import org.goldenport.cncf.http.WorkbenchHttpFixture
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.http4s.{HttpApp, Method, Request as WebRequest, Uri}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.`Content-Type`
import org.http4s.MediaType
import org.simplemodeling.textus.knowledgeworkbench.{datatype, value as workbenchvalue}
import org.simplemodeling.textus.knowledgeworkbench.impl.CandidateDisplay
import org.simplemodeling.textus.knowledgeworkbench.logicalui.KnowledgeWorkbenchLogicalUiDefinitions
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.concurrent.duration.*

/*
 * @since   Oct. 27, 2026
 * @version Oct. 28, 2026
 */
final class CandidateViewsSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Candidate Workbench views" should {
    "native List and Detail" which {
      "show an explicit empty list without creating work" in {
        Given("a new normal persistent Workbench runtime")
        _with_runtime { subsystem =>
          When("the ordinary Display List is queried")
          val display = _display(subsystem)
          Then("the generated List identifies its Detail target and contains no invented Candidate")
          display.hcursor.downField("model").get[Vector[Json]]("items").toOption.get shouldBe empty
          display.hcursor.downField("model").downField("detailTarget").get[String]("screenId") shouldBe Right("candidate-detail")
        }
      }

      "show both producer inputs with exact evidence and visible unprepared limitations without writing" in {
        Given("the TKL prepared and Studio unprepared source fixtures in one native runtime")
        _with_runtime { subsystem =>
          val candidates = Vector("tkl-prepared.json", "editing-studio-unprepared.json").map(_register(subsystem, _))
          val before = candidates.map(candidate => _get(subsystem, candidate.id.print))
          When("the List and each selected Detail are read")
          val list = _display(subsystem)
          val details = candidates.map(candidate => _display(subsystem, Some(candidate.id.print)))
          Then("both inputs share the generated screens and retain all supplied evidence facts with no decision or revision write")
          list.hcursor.downField("model").get[Vector[Json]]("items").toOption.get.map(
            _.hcursor.downField("resource").get[String]("entityId").toOption.get).toSet shouldBe candidates.map(_.id.print).toSet
          details.zip(before).foreach { (display, work) =>
            display.hcursor.downField("model").downField("resource").get[String]("revision") shouldBe Right(work.revision.toString)
            _field(display, "evidence").hcursor.get[String]("text") shouldBe
              Right(Record.data("items" -> work.candidate.evidenceRefs.map(_.toRecord())).toJsonString)
            _field(display, "approved").hcursor.get[Boolean]("boolean") shouldBe Right(false)
            _get(subsystem, work.candidate.id.print).toRecord() shouldBe work.toRecord()
          }
          _field(details.last, "preparation").hcursor.get[String]("text") shouldBe Right("unprepared")
          _field(details.last, "eligible").hcursor.get[Boolean]("boolean") shouldBe Right(false)
        }
      }
    }

    "human workflow and correspondence" which {
      "preserve exact generated content identity revision and absence through pure Display projection" in {
        Given("one native Candidate and the admitted generated Detail definition")
        _with_runtime { subsystem =>
          val current = _get(subsystem, _register(subsystem, "tkl-prepared.json").id.print)
          val definitions = _success(GeneratedLogicalUiAbi.admitC(KnowledgeWorkbenchLogicalUiDefinitions.definitions))
          val projector = _success(DisplayProjection.detailC(definitions, "candidate-detail", "candidate.work", CandidateDisplay.detailRow))
          val text = Gen.listOfN(8, Gen.oneOf("漢", "a", "<script>", "\n", "Δ", "😀")).map(_.mkString)
          When("bounded generated exact text and native revisions are projected without provider access")
          val property = Prop.forAll(text, Gen.chooseNum(1L, 100000L)) { (suffix, revision) =>
            val content = "Human " + suffix
            val candidate = current.candidate.copy(content = Some(_success(datatype.CandidateContent.createC(content))), context = None)
            val input = current.copy(candidate = candidate, revision = revision)
            val projected = _success(projector.projectC(input)).model.asInstanceOf[DisplayModel.DetailModel]
            val fields = projected.sections.flatMap(_.fields)
            Then("identity and exact scalars are retained while absent context is explicitly Missing")
            projected.resource.entityId shouldBe current.candidate.id
            projected.resource.revision.value shouldBe revision
            fields.find(_.id == "content").get.value shouldBe DisplayValue.Text(content)
            fields.find(_.id == "context").get.value shouldBe DisplayValue.Missing
            true
          }
          val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100).withMaxDiscardRatio(1.0f), property)
          Then("at least 100 examples pass with zero discarded examples")
          withClue(checked.toString) { checked.passed shouldBe true }
          checked.succeeded should be >= 100
          checked.discarded shouldBe 0
        }
      }

      "display the same edit Review Approve and invalidation lifecycle for both fixtures" in {
        Given("both registered producer fixtures and the existing STEP-3 business Operations")
        _with_runtime { subsystem =>
          Vector("tkl-prepared.json", "editing-studio-unprepared.json").foreach { fixture =>
            val initial = _get(subsystem, _register(subsystem, fixture).id.print)
            When("explicit human edit Review and Approve precede a changed target")
            val ready = _edit(subsystem, initial, "Reviewed human content", _evidence("version-1"))
            val approved = _decide(subsystem, _decide(subsystem, ready, "review"), "approve")
            val approveddisplay = _display(subsystem, Some(initial.candidate.id.print))
            val edited = _edit(subsystem, approved, "Changed human content", _evidence("version-2"))
            val changed = _display(subsystem, Some(initial.candidate.id.print))
            Then("Display reflects the canonical approved target and its later invalidation without replacing original provenance")
            _field(approveddisplay, "approved").hcursor.get[Boolean]("boolean") shouldBe Right(true)
            _field(changed, "state").hcursor.get[String]("text") shouldBe Right("Draft")
            _field(changed, "approved").hcursor.get[Boolean]("boolean") shouldBe Right(false)
            edited.candidate.originalProposal shouldBe initial.candidate.originalProposal
            edited.candidate.decisions.map(_.decision.value) shouldBe Vector("edit", "review", "approve", "edit")
          }
        }
      }

      "describe existing business targets inertly and reject incorrect resource selectors without writes" in {
        Given("one Candidate and its exact current native revision")
        _with_runtime { subsystem =>
          val current = _get(subsystem, _register(subsystem, "tkl-prepared.json").id.print)
          When("business metadata is read and mismatched source and screen selectors are submitted")
          val descriptor = _display_call(subsystem, "describeBusiness", "candidate-detail")
          val wrongresource = _display_result(subsystem, "read", "resourceType" -> "wrong-resource",
            "screenId" -> "candidate-detail", "resourceId" -> current.candidate.id.print)
          val wrongscreen = _display_result(subsystem, "read", "resourceType" -> "information-candidate",
            "screenId" -> "candidate-list", "resourceId" -> current.candidate.id.print)
          Then("metadata points only to admitted existing business Operations and malformed reads cannot mutate the Candidate")
          descriptor.hcursor.get[Vector[Json]]("actions").toOption.get.map(
            _.hcursor.downField("operation").get[String]("operation").toOption.get).toSet shouldBe
              Set("editCandidateWork", "decideCandidateWork", "admitCandidateWork", "confirmCandidateAdmission",
                "refreshCandidateMaterialEvidence")
          Vector(wrongresource, wrongscreen).foreach(_ shouldBe a[Consequence.Failure[?]])
          _get(subsystem, current.candidate.id.print).toRecord() shouldBe current.toRecord()
        }
      }

      "restore the same displayed target history and revision after ordinary persistent reopen" in {
        Given("a normally persisted approved Candidate and its Display")
        val root = _root()
        val writer = _handle(root)
        val before = try {
          val initial = _get(writer.subsystem, _register(writer.subsystem, "tkl-prepared.json").id.print)
          val approved = _decide(writer.subsystem, _decide(writer.subsystem, initial, "review"), "approve")
          approved.candidate.id.print -> _display(writer.subsystem, Some(approved.candidate.id.print))
        } finally writer.close()
        When("a fresh normal runtime reads the same selected Detail")
        val reader = _handle(root)
        try {
          val restored = _display(reader.subsystem, Some(before._1))
          Then("the complete canonical display and approval correspond to the persisted work")
          restored shouldBe before._2
        } finally reader.close()
      }
    }

    "ordinary native HTTP transport" which {
      "create only explicit public Material work and preserve exact lookup and failure outcomes" in {
        Given("the native HTTP router and a controlled public logical Material producer")
        val handle = _handle(_root(), controlledmaterial = true)
        try {
          val subsystem = handle.subsystem
          val producer = WorkbenchHttpFixture.installMaterial(subsystem)
          val app = WorkbenchHttpFixture.app(subsystem)
          val prefix = "/rest/v1/knowledge-workbench/MaterialWork/"
          val selection = "materialRef=" + java.net.URLEncoder.encode(WorkbenchHttpFixture.materialRef, "UTF-8")
          When("search inspection and original reads precede an explicit command")
          _http(app, prefix + "searchMaterials.json")._1 shouldBe 200
          _http(app, prefix + "inspectMaterial.json?" + selection)._1 shouldBe 200
          val original = app.run(WebRequest[IO](Method.GET, Uri.unsafeFromString(prefix + "readOriginal?" + selection +
            "&resourceId=" + java.net.URLEncoder.encode(WorkbenchHttpFixture.resourceId, "UTF-8")))).unsafeRunSync()
          original.status.code shouldBe 200
          original.headers.headers.find(_.name.toString.equalsIgnoreCase("Cache-Control")).map(_.value) shouldBe Some("private, no-store")
          _display(subsystem).hcursor.downField("model").get[Vector[Json]]("items").toOption.get shouldBe empty
          val body = _material_command("http:creation")
          val created = _http(app, prefix + "createCandidateFromMaterial.json", Some(body))
          withClue(created._2) { created._1 shouldBe 200 }
          val candidateid = io.circe.parser.parse(created._2).toOption.get.hcursor.downField("candidate").get[String]("id")
            .orElse(io.circe.parser.parse(created._2).toOption.get.hcursor.downField("data").downField("candidate").get[String]("id")).toOption.get
          val initial = _get(subsystem, candidateid)
          initial.reviewEligible shouldBe false
          _http(app, prefix + "createCandidateFromMaterial.json", Some(_material_command("http:mismatched", materialref = "unknown:material")))._1 should be >= 400
          val edited = _edit(subsystem, initial, "HTTP preserved human work", initial.candidate.evidenceRefs)
          val lookup = _http(app, "/rest/v1/knowledge-workbench/candidate/findCandidateByProposal.json?producerRef=workbench-material&proposalId=http%3Acreation")
          lookup._1 shouldBe 200
          lookup._2 should include (candidateid)
          lookup._2 should include ("HTTP preserved human work")
          producer.denied = true
          Then("same-intent explicit resubmission retains edits while denied and invalid new work creates nothing")
          _http(app, prefix + "createCandidateFromMaterial.json", Some(body))._1 shouldBe 200
          _http(app, prefix + "createCandidateFromMaterial.json", Some(_material_command("http:denied")))._1 should be >= 400
          _http(app, prefix + "createCandidateFromMaterial.json", Some(_material_command("http:invalid", "")))._1 should be >= 400
          _http(app, prefix + "inspectMaterial.json?" + selection)._1 should be >= 400
          _http(app, prefix + "readOriginal?" + selection + "&resourceId=missing")._1 should be >= 400
          _get(subsystem, candidateid).toRecord() shouldBe edited.toRecord()
          _display(subsystem).hcursor.downField("model").get[Vector[Json]]("items").toOption.get should have size 1
        } finally handle.close()
      }

      "serve the application assets Display reads and existing conditional business edits through the native router" in {
        Given("the real native HTTP router over a normal Workbench runtime")
        _with_runtime { subsystem =>
          val candidate = _register(subsystem, "tkl-prepared.json")
          val current = _get(subsystem, candidate.id.print)
          val app = WorkbenchHttpFixture.app(subsystem)
          When("the served application and Display metadata are read and its existing edit Operation receives JSON")
          val page = _http(app, "/web/knowledge-workbench/textus-knowledge-workbench")
          val asset = _http(app, "/web/knowledge-workbench/textus-knowledge-workbench/assets/candidate-workbench.js")
          val display = _http(app, "/rest/v1/knowledge-workbench/display/read.json?" +
            "resourceType=information-candidate&screenId=candidate-detail&resourceId=" + java.net.URLEncoder.encode(candidate.id.print, "UTF-8"))
          val workresponse = _http(app, "/rest/v1/knowledge-workbench/candidate/getCandidateWork.json?id=" +
            java.net.URLEncoder.encode(candidate.id.print, "UTF-8"))
          val body = Record.data("id" -> candidate.id.print, "expectedRevision" -> current.revision,
            "content" -> "HTTP human edit", "context" -> "", "evidenceRefs" -> current.candidate.evidenceRefs.map(_.toRecord()),
            "groundingRefs" -> Vector.empty[Record], "mappingRefs" -> Vector.empty[Record], "reason" -> "HTTP edit reason").toJsonString
          val saved = _http(app, "/rest/v1/knowledge-workbench/candidate/editCandidateWork.json", Some(body))
          val stale = _http(app, "/rest/v1/knowledge-workbench/candidate/editCandidateWork.json", Some(body))
          Then("actual assets and standard Display are available while business validation owns mutation and rejects stale replay")
          page._1 shouldBe 200
          page._2 should include ("Candidate Workbench")
          asset._1 shouldBe 200
          asset._2 should include ("expectedRevision")
          display._1 shouldBe 200
          display._2 should include ("candidate-detail")
          workresponse._1 shouldBe 200
          workresponse._2 should include ("evidence_refs")
          withClue(workresponse._2.take(200)) { io.circe.parser.parse(workresponse._2).isRight shouldBe true }
          saved._1 shouldBe 200
          stale._1 should be >= 400
          _get(subsystem, candidate.id.print).candidate.content.map(_.value) shouldBe Some("HTTP human edit")
          _get(subsystem, candidate.id.print).candidate.context.map(_.value) shouldBe Some("")
        }
      }
    }

    // Explicitly selected human-browser acceptance; normal full suite has no external browser prerequisite.
    if (sys.props.get("textus.test.legacy-browser-probe").contains("true")) {
      "owned live browser workflow" in {
        Given("both source fixtures behind a loopback-only real native HTTP application")
        _with_runtime { subsystem =>
          val selected = _register(subsystem, "tkl-prepared.json")
          val other = _register(subsystem, "editing-studio-unprepared.json")
          val app = WorkbenchHttpFixture.app(subsystem)
          val ready = Path.of("target/candidate-view-browser-ready.json").toAbsolutePath
          Files.deleteIfExists(ready)
          When("the explicitly coordinated browser edits reviews and approves the selected Candidate exactly once")
          val program = for {
            done <- Deferred[IO, Unit]
            combined = HttpApp[IO](request => if (request.uri.path.renderString == "/__fixture_complete")
              done.complete(()).as(org.http4s.Response[IO](org.http4s.Status.NoContent)) else app(request))
            _ <- EmberServerBuilder.default[IO].withHost(Host.fromString("127.0.0.1").get)
              .withPort(Port.fromInt(0).get).withHttpApp(combined).build.use { server =>
                IO.blocking(Files.writeString(ready, Json.obj("baseUrl" -> Json.fromString(s"http://127.0.0.1:${server.address.getPort}"),
                  "selectedId" -> Json.fromString(selected.id.print), "otherId" -> Json.fromString(other.id.print)).noSpaces)) *>
                  done.get.timeout(6.minutes)
              }
          } yield ()
          program.unsafeRunSync()
          Then("native persistence records the observed explicit human workflow and leaves the unprepared Candidate unapproved")
          val result = _get(subsystem, selected.id.print)
          result.state shouldBe "Approved"
          result.candidate.content.map(_.value) shouldBe Some("Browser reviewed content")
          result.candidate.context.map(_.value) shouldBe Some("")
          result.candidate.decisions.map(_.decision.value) shouldBe Vector("edit", "review", "approve")
          _get(subsystem, other.id.print).approvalValid shouldBe false
        }
      }
    }
    if (sys.props.get("textus.test.browser-probe").contains("true")) {
      "owned Material creation browser workflow" in {
        Given("a loopback native persistent runtime and a controlled logical public Material producer")
        val root = _root()
        val handle = _handle(root, controlledmaterial = true)
        try {
          val subsystem = handle.subsystem
          val producer = WorkbenchHttpFixture.installMaterial(subsystem)
          val app = WorkbenchHttpFixture.app(subsystem)
          val ready = Path.of("target/candidate-material-browser-ready.json").toAbsolutePath
          Files.deleteIfExists(ready)
          When("the coordinated browser searches inspects previews explicitly creates edits holds and tests controlled ready evidence")
          val program = for {
            done <- Deferred[IO, Unit]
            attempts <- Ref.of[IO, Int](0)
            combined = HttpApp[IO](request => request.uri.path.renderString match {
              case "/__fixture_complete" => done.complete(()).as(org.http4s.Response[IO](org.http4s.Status.NoContent))
              case "/__fixture_deny" => IO { producer.denied = true }.as(org.http4s.Response[IO](org.http4s.Status.NoContent))
              case "/__fixture_allow" => IO { producer.denied = false }.as(org.http4s.Response[IO](org.http4s.Status.NoContent))
              case path if path.endsWith("/createCandidateFromMaterial.json") =>
                attempts.getAndUpdate(_ + 1).flatMap { count =>
                  app(request).flatMap { response =>
                    if (count == 0 && response.status.isSuccess)
                      response.body.compile.drain.as(org.http4s.Response[IO](org.http4s.Status.ServiceUnavailable))
                    else IO.pure(response)
                  }
                }
              case _ => app(request)
            })
            _ <- EmberServerBuilder.default[IO].withHost(Host.fromString("127.0.0.1").get)
              .withPort(Port.fromInt(0).get).withHttpApp(combined).build.use { server =>
                IO.blocking(Files.writeString(ready, Json.obj(
                  "baseUrl" -> Json.fromString(s"http://127.0.0.1:${server.address.getPort}"),
                  "applicationPath" -> Json.fromString("/web/knowledge-workbench/textus-knowledge-workbench"),
                  "completePath" -> Json.fromString("/__fixture_complete"),
                  "denyPath" -> Json.fromString("/__fixture_deny"),
                  "allowPath" -> Json.fromString("/__fixture_allow"),
                  "creationResponse" -> Json.fromString("Only a successful first native create response is replaced with HTTP 503. Actual failure statuses are preserved. Reconcile exact retained intent; never resubmit."),
                  "materialRef" -> Json.fromString(WorkbenchHttpFixture.materialRef),
                  "resourceId" -> Json.fromString(WorkbenchHttpFixture.resourceId),
                  "finalContent" -> Json.fromString("Browser invalidated content"),
                  "root" -> Json.fromString(root.toString),
                  "procedure" -> Json.fromString("Search; select Controlled public Material; select resource and preview; new intent; create name Browser Material with content Browser initial content and present-empty context. Observe unknown HTTP503 and preserved creation draft; reload and reconcile retained intent without resubmit. Review/Approve must be disabled. Edit content Browser held content with present-empty context; Hold. Open original source. Visit denyPath then open source: old preview/selection must clear; visit allowPath and reopen. Replace evidence with id evidence:reviewed, kind evidence, revision version-1, snapshot snapshot:version-1, retrieval available (controlled readiness only); save content Browser reviewed content. Review then Approve without target change. Save unchanged target to retain approval. Edit content Browser invalidated content to invalidate approval. Open original source again. Signal complete."),
                  "selectors" -> Json.fromString("#material-search #material-list #material-detail #original-preview #new-intent #creation-name #creation-content #creation-context-present #create-candidate #candidate-source #content #edit-reason #decision #decision-reason")
                ).noSpaces)) *> done.get.timeout(12.minutes)
              }
            count <- attempts.get
          } yield count
          program.unsafeRunSync() shouldBe 1
          Then("the same native Candidate retains first source and actual human history with invalidated approval")
          val items = _display(subsystem).hcursor.downField("model").get[Vector[Json]]("items").toOption.get
          items should have size 1
          val id = items.head.hcursor.downField("resource").get[String]("entityId").toOption.get
          val result = _get(subsystem, id)
          result.state shouldBe "Draft"
          result.approvalValid shouldBe false
          result.candidate.content.map(_.value) shouldBe Some("Browser invalidated content")
          result.candidate.context.map(_.value) shouldBe Some("")
          result.candidate.decisions.map(_.decision.value) shouldBe Vector("edit", "hold", "edit", "review", "approve", "edit")
          val original = io.circe.parser.parse(result.candidate.originalProposal.get.value).toOption.get
          original.hcursor.downField("materialSelection").get[String]("materialRef") shouldBe Right(WorkbenchHttpFixture.materialRef)
          original.hcursor.downField("materialSelection").get[Vector[String]]("resourceIds") shouldBe Right(Vector(WorkbenchHttpFixture.resourceId))
          producer.calls should contain allOf ("searchMaterials", "getMaterial", "readOriginal")
          result.candidate.decisions.map(_.actor).distinct shouldBe Vector("workbench-assembly-spec-user")
        } finally handle.close()
      }
    }
  }

  private def _display_result(subsystem: Subsystem, operation: String, properties: (String, Any)*): Consequence[OperationResponse] =
    subsystem.executeOperationResponse(Request.of(component = "org.simplemodeling.textus.KnowledgeWorkbench", service = "display",
      operation = operation, properties = properties.map { case (key, input) => Property(key, input, None) }.toList))

  private def _display_call(subsystem: Subsystem, operation: String, screen: String, id: Option[String] = None): Json = {
    val properties = Vector("resourceType" -> "information-candidate", "screenId" -> screen) ++ id.map("resourceId" -> _)
    _success(_display_result(subsystem, operation, properties*)) match {
      case OperationResponse.Json(payload) => payload
      case other => fail(s"Expected standard Display JSON: $other")
    }
  }

  private def _display(subsystem: Subsystem, id: Option[String] = None): Json =
    _display_call(subsystem, if (id.isDefined) "read" else "query", if (id.isDefined) "candidate-detail" else "candidate-list", id)

  private def _field(display: Json, id: String): Json =
    display.hcursor.downField("model").get[Vector[Json]]("sections").toOption.get.flatMap(
      _.hcursor.get[Vector[Json]]("fields").toOption.get).find(_.hcursor.get[String]("id").contains(id)).get
      .hcursor.downField("value").focus.get

  private def _http(app: HttpApp[IO], path: String, body: Option[String] = None): (Int, String) = {
    val request = WebRequest[IO](if (body.isDefined) Method.POST else Method.GET, Uri.unsafeFromString(path))
    val supplied = body.map(text => request.withEntity(text).putHeaders(`Content-Type`(MediaType.application.json))).getOrElse(request)
    val response = app.run(supplied).unsafeRunSync()
    response.status.code -> response.as[String].unsafeRunSync()
  }

  private def _success[A](result: Consequence[A]): A =
    result.toOption.getOrElse(fail(s"Expected successful native result: $result"))

  private def _evidence(revision: String): Vector[workbenchvalue.ProposalReference] =
    Vector(workbenchvalue.ProposalReference("evidence:reviewed", "evidence", Some(revision),
      Some(s"snapshot:$revision"), Some("available"), None, None))

  private def _register(subsystem: Subsystem, fixture: String): entity.InformationCandidate = {
    val stream = getClass.getResourceAsStream(s"/proposals/$fixture")
    val json = try new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) finally stream.close()
    _success(workbenchvalue.RegisterProposalResult.createC(_execute(subsystem, "registerProposal", "proposalJson" -> json))).candidate
  }

  private def _get(subsystem: Subsystem, id: String): workbenchvalue.CandidateWorkResult =
    _success(workbenchvalue.CandidateWorkResult.createC(_execute(subsystem, "getCandidateWork", "id" -> id)))

  private def _decision_call(subsystem: Subsystem, current: workbenchvalue.CandidateWorkResult, decision: String): Consequence[OperationResponse] =
    _call(subsystem, "decideCandidateWork", "id" -> current.candidate.id.print,
      "expectedRevision" -> current.revision, "decision" -> decision, "reason" -> "Human decision")

  private def _decide(subsystem: Subsystem, current: workbenchvalue.CandidateWorkResult, decision: String): workbenchvalue.CandidateWorkResult =
    _success(workbenchvalue.CandidateWorkResult.createC(_record(_success(_decision_call(subsystem, current, decision)))))

  private def _edit_properties(current: workbenchvalue.CandidateWorkResult, content: String, evidence: Vector[workbenchvalue.ProposalReference]): Vector[(String, Any)] =
    Vector("id" -> current.candidate.id.print, "expectedRevision" -> current.revision,
      "content" -> content, "evidenceRefs" -> evidence.map(_.toRecord()),
      "groundingRefs" -> current.candidate.groundingRefs.map(_.toRecord()),
      "mappingRefs" -> current.candidate.mappingRefs.map(_.toRecord()), "reason" -> "Human correction") ++
      current.candidate.context.map(c => "context" -> c.value).toVector

  private def _edit(subsystem: Subsystem, current: workbenchvalue.CandidateWorkResult, content: String, evidence: Vector[workbenchvalue.ProposalReference]): workbenchvalue.CandidateWorkResult =
    _success(workbenchvalue.CandidateWorkResult.createC(_execute(subsystem, "editCandidateWork", _edit_properties(current, content, evidence)*)))

  private def _call(subsystem: Subsystem, operation: String, properties: (String, Any)*): Consequence[OperationResponse] =
    subsystem.executeOperationResponse(Request.of(component = "org.simplemodeling.textus.KnowledgeWorkbench",
      service = "Candidate", operation = operation,
      properties = properties.map { case (name, input) => Property(name, input, None) }.toList))

  private def _record(response: OperationResponse): Record = response match {
    case OperationResponse.RecordResponse(record) => record
    case other => fail(s"Expected native Record response: $other")
  }

  private def _execute(subsystem: Subsystem, operation: String, properties: (String, Any)*): Record =
    _record(_success(_call(subsystem, operation, properties*)))

  private def _root(): Path = {
    val parent = Path.of("target/candidate-view-spec").toAbsolutePath.normalize()
    Files.createDirectories(parent)
    Files.createTempDirectory(parent, "case-")
  }

  private def _material_command(intent: String, content: String = "HTTP Material statement", materialref: String = WorkbenchHttpFixture.materialRef): String =
    Record.data("proposalId" -> intent, "materialRef" -> materialref,
      "resourceIds" -> Vector(WorkbenchHttpFixture.resourceId), "name" -> "HTTP Material",
      "content" -> content, "context" -> "").toJsonString

  private def _with_runtime[A](body: Subsystem => A): A = {
    val handle = _handle(_root())
    try body(handle.subsystem) finally handle.close()
  }

  private def _handle(root: Path, controlledmaterial: Boolean = false): CncfHandle = {
    val repository = Path.of(".").toAbsolutePath.normalize()
    val lake = Path.of(sys.props.getOrElse("textus.test.knowledge-lake.repository",
      repository.resolveSibling("textus-knowledge-lake").toString)).toAbsolutePath.normalize()
    val authored = Files.readString(repository.resolve("src/main/car/assembly-descriptor.yaml")).replace(
      "  - namespace: org.simplemodeling.textus\n    id: KnowledgeHub\n    version: 0.1.0-SNAPSHOT\n", "")
    val selected = if (controlledmaterial) authored.replace(
      "  - namespace: org.simplemodeling.textus\n    id: KnowledgeLake\n    version: 0.1.0-SNAPSHOT\n", "") else authored
    val isolated = selected.replace("  textus.subsystem.user-mode: standalone\n",
      s"""  textus.subsystem.user-mode: standalone
  textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.kind: sqlite
  textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.path: ${root.resolve("workbench.db")}
  textus.component.org.simplemodeling.textus.knowledge-hub.datastores.application.kind: sqlite
  textus.component.org.simplemodeling.textus.knowledge-hub.datastores.application.path: ${root.resolve("hub.db")}
""")
    val instance = root.resolve("assembly-instance.yaml")
    Files.writeString(instance, isolated.replace("    fallbackPrivilege: disabled\n",
      "    fallbackPrivilege: disabled\n    localSubject:\n      id: workbench-assembly-spec-user\n"))
    val discovery = Vector("--discover=classes", s"--component-dev-dir=$repository") ++
      (if (controlledmaterial) Vector.empty else Vector(s"--component-dev-dir=$lake"))
    _success(CncfBootstrap.initialize(BootstrapConfig(cwd = root,
      args = (discovery ++ Vector(s"--textus.assembly.descriptor=$instance", s"--textus.local-data.root=$root", "command")).toArray,
      modeHint = Some(RunMode.Command))))
  }
}
