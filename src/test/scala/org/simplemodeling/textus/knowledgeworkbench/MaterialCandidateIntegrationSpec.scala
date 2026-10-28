package org.simplemodeling.textus.knowledgeworkbench

import java.nio.file.{Files, Path}
import cats.effect.{Deferred, IO}
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import com.comcast.ip4s.{Host, Port}
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.bag.BinaryBag
import org.goldenport.cncf.bootstrap.{BootstrapConfig, CncfBootstrap, CncfHandle}
import org.goldenport.cncf.cli.RunMode
import org.goldenport.cncf.http.WorkbenchHttpFixture
import org.goldenport.cncf.component.{Component, ComponentId, ComponentInit, ComponentInstanceId, ComponentOrigin}
import org.goldenport.cncf.context.{ExecutionContext, PrincipalId, SubjectKind}
import org.goldenport.cncf.security.{AuthenticationProvider, AuthenticationRequest, AuthenticationResult, IngressSecurityResolver, SecuritySubject}
import org.goldenport.cncf.subsystem.{GenericSubsystemAuthenticationBinding, GenericSubsystemAuthenticationProviderBinding, GenericSubsystemComponentBinding, GenericSubsystemSecurityBinding, Subsystem}
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.http4s.{Header, HttpApp, Method, Request as WebRequest, Response, Status, Uri}
import org.http4s.ember.server.EmberServerBuilder
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgeworkbench.value as WorkbenchValue
import org.typelevel.ci.CIString
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.Using

/*
 * @since   Oct. 28, 2026
 * @version Oct. 28, 2026
 */
final class MaterialCandidateIntegrationSpec extends AnyWordSpec with Matchers with GivenWhenThen with BeforeAndAfterAll {
  import MaterialCandidateIntegrationSpec.*
  private var _postgres: Option[MaterialCandidateWorkSpec.LivePostgresql] = None
  // Reuse the existing read-only connection's admitted service caller; do not expand its ACL.
  private val _subject = if (sys.props.get("textus.test.real-material-browser").contains("true"))
    "editing-studio-mobile-development" else "material-candidate-integration-user"
  private val _token = java.util.UUID.randomUUID().toString

  "Connected Material Candidate work" should {
    "retain a second domain-neutral consumer's exact source and human work across PostgreSQL restart" in {
      Given("a generic technical-observation consumer, controlled public Material and actual PostgreSQL Candidate persistence")
      val root = _root()
      try {
        val writer = _handle(root)
        val saved = try {
          WorkbenchHttpFixture.installMaterial(writer.subsystem)
          val consumer = new TechnicalObservationConsumer(writer.subsystem, _token)
          val metadata = consumer.inspect(WorkbenchHttpFixture.materialRef)
          metadata.resources.map(_.resourceId) should contain (WorkbenchHttpFixture.resourceId)
          WorkbenchValue.ListCandidatesResult.createC(_call(writer.subsystem, _token, "Candidate", "listCandidates")).TAKE.items shouldBe empty
          When("the neutral consumer explicitly creates a technical observation then a person edits and Holds the same native work")
          val created = consumer.create("neutral:observation", metadata.materialRef, Vector(WorkbenchHttpFixture.resourceId))
          created.created shouldBe true
          val initial = consumer.work(created.candidate.id.print)
          initial.reviewEligible shouldBe false
          val edited = _edit(writer.subsystem, initial, "Human technical observation")
          val held = _hold(writer.subsystem, edited)
          Then("native identity, first association, actual revision and attributed human history remain distinct from source readiness")
          held.revision should be > initial.revision
          held.candidate.decisions.map(_.actor).distinct shouldBe Vector(_subject)
          held.candidate.decisions.map(_.decision.value) shouldBe Vector("edit", "hold")
          _association(held) shouldBe (metadata.materialRef -> Vector(WorkbenchHttpFixture.resourceId))
          val original = io.circe.parser.parse(held.candidate.originalProposal.get.value).toOption.get
          original.asObject.get.keys.toSet.intersect(Set("book", "isbn", "studio", "provider", "connectionId")) shouldBe empty
          held
        } finally writer.close()
        When("a fresh ordinary runtime reopens the same PostgreSQL store with no Material producer")
        val reader = _handle(root)
        try {
          val consumer = new TechnicalObservationConsumer(reader.subsystem, _token)
          val found = consumer.lookup("neutral:observation").candidate.get
          val resent = consumer.create("neutral:observation", "other:incoming-source", Vector("other:resource"))
          Then("public lookup and same-intent creation return current human work without source reads or replacement")
          found.toRecord() shouldBe saved.candidate.toRecord()
          resent.created shouldBe false
          resent.candidate.toRecord() shouldBe saved.candidate.toRecord()
          consumer.work(found.id.print).toRecord() shouldBe saved.toRecord()
        } finally reader.close()
      } finally _cleanup(root)
    }

    // Only the explicitly configured connected check requires an existing read-only cloud connection and a browser.
    if (sys.props.get("textus.test.real-material-browser").contains("true")) {
      "inspect an existing real original and persist browser Edit Hold and source correspondence across restart" in {
        Given("the explicitly selected existing Material, actual TKL package reader and disposable local PostgreSQL Candidate store")
        val selectionpath = Path.of(sys.props.getOrElse("textus.test.real-material-selection", fail("Missing private selection locator")))
        val connection = Path.of(sys.props.getOrElse("textus.test.drive-connection", fail("Missing read-only connection locator")))
        selectionpath.isAbsolute shouldBe true
        connection.isAbsolute shouldBe true
        Files.isRegularFile(connection) shouldBe true // Never read credential contents in this consumer fixture.
        val selected = io.circe.parser.parse(Files.readString(selectionpath)).toOption.get
        selected.hcursor.get[String]("schema") shouldBe Right("ess3.actual-app-material-read-selection/1")
        val materialref = selected.hcursor.get[String]("materialRef").toOption.get
        val original = selected.hcursor.get[Vector[Json]]("originals").toOption.get.head
        val sourceid = original.hcursor.get[String]("resourceId").toOption.get
        val sourcepath = Path.of(original.hcursor.get[String]("sourcePath").toOption.get)
        val sourceroot = Path.of(selected.hcursor.get[String]("sourceRoot").toOption.get)
        sourcepath shouldBe sourceroot.resolve("materials").resolve(sourceid + ".bin")
        Files.size(sourcepath) should be <= 16777216L
        val expected = Files.readAllBytes(sourcepath)
        expected.length shouldBe original.hcursor.get[Int]("byteLength").toOption.get
        expected.length should be > 0
        expected.length should be <= 16777216
        val manifestassets = selected.hcursor.downField("expectedManifest").get[Vector[Json]]("assets").toOption.get
        val manifestasset = manifestassets.filter(asset =>
          asset.hcursor.get[String]("path").contains("assets/" + sourceid) &&
            asset.hcursor.get[String]("role") == original.hcursor.get[String]("role"))
        manifestasset should have size 1
        val assetpath = manifestasset.head.hcursor.get[String]("path").toOption.get
        val root = _root()
        val ready = Path.of("target/material-candidate-real-browser-ready.json").toAbsolutePath
        Files.deleteIfExists(ready)
        try {
          val writer = _handle(root, Some(selected -> connection))
          val saved = try {
            val subsystem = writer.subsystem
            val consumer = new TechnicalObservationConsumer(subsystem, _token)
            val caller = _context(subsystem, Some(_token)).TAKE
            val subject = SecuritySubject.current(using caller)
            subject.isProviderAuthenticated shouldBe true
            subject.subjectId shouldBe _subject
            caller.security.subjectKind shouldBe SubjectKind.Service
            _reject_unadmitted(subsystem, materialref, sourceid)
            val metadata = consumer.inspect(materialref)
            metadata.materialRef shouldBe materialref
            val matches = metadata.resources.filter(resource =>
              Set(sourceid, assetpath).contains(resource.resourceId) &&
                resource.role == original.hcursor.get[String]("role").toOption.get && resource.mediaType == "image/jpeg")
            matches should have size 1
            val resourceid = matches.head.resourceId // Actual public metadata wins; do not construct a selector from storage layout.
            val app = WorkbenchHttpFixture.app(subsystem)
            _equal_bytes(_original(subsystem, materialref, resourceid), expected)
            _equal_bytes(_http_original(app, materialref, resourceid), expected)
            When("the coordinated browser searches previews explicitly creates edits Holds and reopens the original source")
            val program = for {
              done <- Deferred[IO, Unit]
              combined = HttpApp[IO](request => if (request.uri.path.renderString == "/__real_material_complete")
                done.complete(()).as(Response[IO](Status.NoContent)) else app(request))
              _ <- EmberServerBuilder.default[IO].withHost(Host.fromString("127.0.0.1").get)
                .withPort(Port.fromInt(0).get).withHttpApp(combined).build.use { server =>
                  IO.blocking(_write_ready(ready, Json.obj(
                    "baseUrl" -> Json.fromString(s"http://127.0.0.1:${server.address.getPort}"),
                    "applicationPath" -> Json.fromString("/web/knowledge-workbench/textus-knowledge-workbench"),
                    "completePath" -> Json.fromString("/__real_material_complete"),
                    "materialRef" -> Json.fromString(materialref), "resourceId" -> Json.fromString(resourceid),
                    "byteLength" -> Json.fromInt(expected.length), "actor" -> Json.fromString(_subject),
                    "callerKind" -> Json.fromString("provider-authenticated-service"),
                    "testToken" -> Json.fromString(_token), "tokenSelector" -> Json.fromString("#token"),
                    "procedure" -> Json.fromString("Enter testToken in #token (disposable loopback fixture service caller, not end-user OAuth). Search existing Material; inspect and select the supplied exact resourceId; preview real JPEG. Start explicit new intent; create name Real Material observation, content Real source initial observation, present-empty context. Confirm Candidate ID and unavailable Review/Approve. Edit content Real source human observation with reason; Hold with reason. Reload same Candidate, open first source and preview again. Signal completePath. Do not add evidence versions or invoke admission."),
                    "selectors" -> Json.fromString("#material-search #material-list #material-detail #original-preview #new-intent #creation-name #creation-content #creation-context-present #create-candidate #content #edit-reason #decision #decision-reason #reload #candidate-source")
                  ))) *> done.get.timeout(12.minutes)
                }
            } yield ()
            program.unsafeRunSync()
            Then("actual native work records the same unversioned source and attributed Edit/Hold while every original byte is unchanged")
            val candidates = WorkbenchValue.ListCandidatesResult.createC(_call(subsystem, _token, "Candidate", "listCandidates")).TAKE.items
            candidates should have size 1
            val result = consumer.work(candidates.head.id.print)
            result.state shouldBe "Held"
            result.reviewEligible shouldBe false
            result.approvalValid shouldBe false
            result.candidate.content.map(_.value) shouldBe Some("Real source human observation")
            result.candidate.context.map(_.value) shouldBe Some("")
            result.candidate.decisions.map(_.decision.value) shouldBe Vector("edit", "hold")
            result.candidate.decisions.map(_.actor).distinct shouldBe Vector(_subject)
            result.candidate.evidenceRefs.foreach { reference =>
              reference.revision shouldBe None
              reference.snapshotRef shouldBe None
              reference.retrievalState shouldBe Some("unverified")
            }
            _association(result) shouldBe (materialref -> Vector(resourceid))
            _equal_bytes(_original(subsystem, materialref, resourceid), expected)
            _equal_bytes(_http_original(app, materialref, resourceid), expected)
            _equal_bytes(Files.readAllBytes(sourcepath), expected)
            result
          } finally writer.close()
          When("the native runtime restarts with the same PostgreSQL store and caller")
          val reader = _handle(root, Some(selected -> connection))
          try {
            val consumer = new TechnicalObservationConsumer(reader.subsystem, _token)
            val restored = consumer.work(saved.candidate.id.print)
            Then("revision, first association, human edits and Hold survive ordinary native reload and exact public intent lookup")
            restored.toRecord() shouldBe saved.toRecord()
            consumer.lookup(saved.candidate.proposalId.get.value).candidate.get.toRecord() shouldBe saved.candidate.toRecord()
            val association = _association(restored)
            _equal_bytes(_original(reader.subsystem, association._1, association._2.head), expected)
          } finally reader.close()
        } finally { Files.deleteIfExists(ready); _cleanup(root) }
      }
    }
  }

  override protected def afterAll(): Unit =
    try _postgres.foreach(_.stop()) finally super.afterAll()

  private def _write_ready(path: Path, payload: Json): Unit = {
    Files.createFile(path, java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
      java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")))
    Files.writeString(path, payload.noSpaces)
    ()
  }

  private def _reject_unadmitted(subsystem: Subsystem, materialref: String, resourceid: String): Unit = {
    val app = WorkbenchHttpFixture.app(subsystem)
    val query = "materialRef=" + java.net.URLEncoder.encode(materialref, "UTF-8") +
      "&resourceId=" + java.net.URLEncoder.encode(resourceid, "UTF-8")
    val wrong = java.util.UUID.randomUUID().toString
    Vector(None, Some(wrong)).foreach { token =>
      withClue("Missing or incorrect disposable bearer must not admit Material work: ") {
        _result(subsystem, token, "MaterialWork", "inspectMaterial", "materialRef" -> materialref).isSuccess shouldBe false
        _result(subsystem, token, "MaterialWork", "readOriginal",
          "materialRef" -> materialref, "resourceId" -> resourceid).isSuccess shouldBe false
        _result(subsystem, token, "MaterialWork", "createCandidateFromMaterial",
          "proposalId" -> "denied:integration", "materialRef" -> materialref,
          "resourceIds" -> Vector(resourceid), "name" -> "Denied observation",
          "content" -> "Must not persist").isSuccess shouldBe false
        val read = WebRequest[IO](Method.GET,
          Uri.unsafeFromString("/rest/v1/knowledge-workbench/MaterialWork/readOriginal.json?" + query))
        val create = WebRequest[IO](Method.POST,
          Uri.unsafeFromString("/rest/v1/knowledge-workbench/MaterialWork/createCandidateFromMaterial.json"))
          .withEntity(Json.obj("proposalId" -> Json.fromString("denied:http-integration"),
            "materialRef" -> Json.fromString(materialref), "resourceIds" -> Json.arr(Json.fromString(resourceid)),
            "name" -> Json.fromString("Denied observation"), "content" -> Json.fromString("Must not persist")).noSpaces)
          .putHeaders(Header.Raw(CIString("Content-Type"), "application/json"))
        Vector(read, create).foreach { request =>
          val authenticated = token.fold(request)(input => request.putHeaders(Header.Raw(CIString("Authorization"), "Bearer " + input)))
          val response = app.run(authenticated).unsafeRunSync()
          Set(401, 403) should contain (response.status.code)
          response.body.compile.drain.unsafeRunSync()
        }
        WorkbenchValue.ListCandidatesResult.createC(_call(subsystem, _token, "Candidate", "listCandidates")).TAKE.items shouldBe empty
      }
    }
  }

  private def _edit(subsystem: Subsystem, current: WorkbenchValue.CandidateWorkResult, content: String): WorkbenchValue.CandidateWorkResult =
    WorkbenchValue.CandidateWorkResult.createC(_call(subsystem, _token, "Candidate", "editCandidateWork",
      "id" -> current.candidate.id.print, "expectedRevision" -> current.revision, "content" -> content,
      "context" -> "", "evidenceRefs" -> current.candidate.evidenceRefs.map(_.toRecord()),
      "groundingRefs" -> current.candidate.groundingRefs.map(_.toRecord()),
      "mappingRefs" -> current.candidate.mappingRefs.map(_.toRecord()), "reason" -> "Technical observation correction")).TAKE

  private def _hold(subsystem: Subsystem, current: WorkbenchValue.CandidateWorkResult): WorkbenchValue.CandidateWorkResult =
    WorkbenchValue.CandidateWorkResult.createC(_call(subsystem, _token, "Candidate", "decideCandidateWork",
      "id" -> current.candidate.id.print, "expectedRevision" -> current.revision,
      "decision" -> "hold", "reason" -> "Await separately versioned evidence")).TAKE

  private def _association(current: WorkbenchValue.CandidateWorkResult): (String, Vector[String]) = {
    val selected = io.circe.parser.parse(current.candidate.originalProposal.get.value).toOption.get.hcursor.downField("materialSelection")
    selected.get[String]("materialRef").toOption.get -> selected.get[Vector[String]]("resourceIds").toOption.get
  }

  private def _equal_bytes(actual: Array[Byte], expected: Array[Byte]): Unit =
    withClue(s"Complete binary comparison (${actual.length}/${expected.length} bytes): ") {
      java.util.Arrays.equals(actual, expected) shouldBe true
    }

  private def _original(subsystem: Subsystem, materialref: String, resourceid: String): Array[Byte] = {
    val response = _response(subsystem, _token, "MaterialWork", "readOriginal", "materialRef" -> materialref, "resourceId" -> resourceid) match {
      case OperationResponse.Http(binary: org.goldenport.http.HttpResponse.Binary) => binary
      case _ => fail("Expected actual public binary original response")
    }
    response.mime.value shouldBe "image/jpeg"
    response.headerValue("Cache-Control") shouldBe Some("private, no-store")
    response.headerValue("X-Content-Type-Options") shouldBe Some("nosniff")
    response.headerValue("Content-Security-Policy") shouldBe Some("default-src 'none'; sandbox")
    response.headerValue("Content-Disposition").exists(_.startsWith("attachment;")) shouldBe true
    Using.resource(response.bag.asInstanceOf[BinaryBag].openInputStream())(_.readNBytes(16777217))
  }

  private def _http_original(app: HttpApp[IO], materialref: String, resourceid: String): Array[Byte] = {
    val query = "materialRef=" + java.net.URLEncoder.encode(materialref, "UTF-8") +
      "&resourceId=" + java.net.URLEncoder.encode(resourceid, "UTF-8")
    val response = app.run(WebRequest[IO](Method.GET,
      Uri.unsafeFromString("/rest/v1/knowledge-workbench/MaterialWork/readOriginal?" + query))
      .putHeaders(Header.Raw(CIString("Authorization"), "Bearer " + _token))).unsafeRunSync()
    response.status.code shouldBe 200
    response.headers.headers.find(_.name.toString.equalsIgnoreCase("Content-Type")).map(_.value) shouldBe Some("image/jpeg")
    response.headers.headers.find(_.name.toString.equalsIgnoreCase("Cache-Control")).map(_.value) shouldBe Some("private, no-store")
    response.headers.headers.find(_.name.toString.equalsIgnoreCase("X-Content-Type-Options")).map(_.value) shouldBe Some("nosniff")
    response.headers.headers.find(_.name.toString.equalsIgnoreCase("Content-Security-Policy")).map(_.value) shouldBe Some("default-src 'none'; sandbox")
    response.headers.headers.find(_.name.toString.equalsIgnoreCase("Content-Disposition")).exists(_.value.startsWith("attachment;")) shouldBe true
    response.body.take(16777217L).compile.toVector.unsafeRunSync().toArray
  }

  private def _root(): Path = {
    val parent = Files.createDirectories(Path.of("target/material-candidate-integration/work").toAbsolutePath)
    Files.createTempDirectory(parent, "case-")
  }

  private def _cleanup(root: Path): Unit =
    try Using.resource(Files.walk(root))(_.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.deleteIfExists(_)))
    finally { _postgres.foreach(_.stop()); _postgres = None }

  private def _handle(root: Path, live: Option[(Json, Path)] = None): CncfHandle = {
    val database = _postgres.getOrElse {
      val instance = new MaterialCandidateWorkSpec.LivePostgresql()
      instance.start()
      _postgres = Some(instance)
      instance
    }
    val repository = Path.of(".").toAbsolutePath.normalize()
    val lake = Path.of(sys.props.getOrElse("textus.test.knowledge-lake.repository",
      repository.resolveSibling("textus-knowledge-lake").toString)).toAbsolutePath.normalize()
    if (live.nonEmpty) {
      // Cwd host binding owns outbound authority; component parameters cannot grant HTTP.
      // Methods apply to both origins; the existing Drive token scope remains read-only.
      val httpplan = Json.obj(
        "schema" -> Json.fromString("cncf.runtime-http-execution.v1"),
        "baseUrl" -> Json.fromString("https://www.googleapis.com/"),
        "limits" -> Json.obj("connectTimeoutMillis" -> Json.fromInt(10000),
          "readTimeoutMillis" -> Json.fromInt(20000), "responseBytes" -> Json.fromInt(8388608)),
        "grants" -> Json.arr(Json.obj(
          "componentId" -> Json.fromString("org.simplemodeling.textus.KnowledgeLake"),
          "origins" -> Json.arr(Json.fromString("https://oauth2.googleapis.com"),
            Json.fromString("https://www.googleapis.com")),
          "methods" -> Json.arr(Json.fromString("GET"), Json.fromString("POST")))))
      val hostfile = Files.createDirectories(root.resolve(".textus")).resolve("config.json")
      Files.writeString(hostfile, Json.obj(
        "textus.subsystem.http-execution.plan" -> Json.fromString(httpplan.noSpaces)).noSpaces)
      Files.setPosixFilePermissions(hostfile, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"))
    }
    val lakeconfig = live.map { case (selected, connection) =>
      val storage = Json.obj("provider" -> Json.fromString("google-drive"), "connectionId" -> Json.fromString("ess3-real-drive-read"),
        "storeName" -> selected.hcursor.downField("storeName").focus.get,
        "rootKey" -> selected.hcursor.downField("rootKey").focus.get, "parentKey" -> selected.hcursor.downField("parentKey").focus.get)
      val packages = Json.obj("schema" -> Json.fromString("textus.knowledge-lake.material-packages/1"), "materials" -> Json.arr(Json.obj(
        "materialRef" -> selected.hcursor.downField("materialRef").focus.get,
        "authorizedSubjects" -> Json.arr(Json.fromString(_subject)), "storage" -> storage)))
      Vector("google-drive.service-account.configuration" -> connection.toUri.toString,
        "material.reference.mode" -> "packages", "material.reference.packages" -> packages.noSpaces)
        .map { case (key, input) => s"      $key: ${Json.fromString(input).noSpaces}\n" }.mkString
    }.getOrElse("")
    val lakecomponent = live.map(_ => "  - namespace: org.simplemodeling.textus\n    id: KnowledgeLake\n    version: 0.1.0-SNAPSHOT\n    config:\n" + lakeconfig).getOrElse("")
    val prefix = "textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application"
    val descriptor = root.resolve("assembly.yaml")
    Files.writeString(descriptor, s"""subsystem: material-candidate-integration-spec
version: 0.1.0-SNAPSHOT
components:
  - namespace: org.simplemodeling.textus
    id: KnowledgeWorkbench
    version: 0.1.0-SNAPSHOT
${lakecomponent}subsystemCapabilities:
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
      id: ${_subject}
""")
    val discovery = Vector("--discover=classes", s"--component-dev-dir=$repository") ++
      live.toVector.map(_ => s"--component-dev-dir=$lake")
    val handle = CncfBootstrap.initialize(BootstrapConfig(cwd = root,
      args = (discovery ++ Vector(s"--textus.assembly.descriptor=$descriptor", s"--textus.local-data.root=$root", "command")).toArray,
      modeHint = Some(RunMode.Command))).TAKE
    try {
      val subsystem = handle.subsystem
      live.foreach { _ =>
        val lakeinstance = subsystem.components.find(_.name == "org.simplemodeling.textus.KnowledgeLake")
          .getOrElse(fail("Expected actual KnowledgeLake component"))
        val modekey = lakeinstance.factoryOption.toVector.flatMap(_.initializationParameterDeclarations)
          .find(_.name == "material.reference.mode")
          .getOrElse(fail("Expected declared KnowledgeLake material.reference.mode parameter"))
        lakeinstance.initializationParameters.resolve(modekey).TAKE.value shouldBe Some("packages")
      }
      val component = new FixtureAuthentication(_token, _subject)
      val id = ComponentId("org.simplemodeling.textus.MaterialCandidateIntegrationAuthentication")
      val core = Component.Core.create(name = id.name, componentId = id,
        instanceId = ComponentInstanceId.default(id), protocol = org.goldenport.protocol.Protocol(
          services = org.goldenport.protocol.spec.ServiceDefinitionGroup(Vector.empty)))
      component.initialize(ComponentInit(subsystem, core, ComponentOrigin.Builtin))
      subsystem.add(component)
      val existing = subsystem.descriptor.getOrElse(throw new IllegalStateException("Expected admitted integration descriptor"))
      val security = existing.security.getOrElse(GenericSubsystemSecurityBinding())
      val authentication = security.authentication.getOrElse(GenericSubsystemAuthenticationBinding())
      subsystem.withDescriptor(existing.copy(
        componentBindings = existing.componentBindings :+ GenericSubsystemComponentBinding(component.name),
        security = Some(security.copy(authentication = Some(authentication.copy(
          convention = Some("enabled"), fallbackPrivilege = Some("disabled"),
          providers = authentication.providers :+ GenericSubsystemAuthenticationProviderBinding(
            name = component.providerName, component = component.name, enabled = Some(true))
        ))))
      ))
      handle
    } catch {
      case error: Throwable => handle.close(); throw error
    }
  }
}

object MaterialCandidateIntegrationSpec {
  /** A second consumer speaks only public logical Material and Candidate contracts. */
  final class TechnicalObservationConsumer(subsystem: Subsystem, token: String) {
    def inspect(materialRef: String): WorkbenchValue.MaterialMetadata =
      WorkbenchValue.MaterialMetadata.createC(_call(subsystem, token, "MaterialWork", "inspectMaterial", "materialRef" -> materialRef)).TAKE
    def create(proposalId: String, materialRef: String, resourceIds: Vector[String]): WorkbenchValue.RegisterProposalResult =
      WorkbenchValue.RegisterProposalResult.createC(_call(subsystem, token, "MaterialWork", "createCandidateFromMaterial",
        "proposalId" -> proposalId, "materialRef" -> materialRef, "resourceIds" -> resourceIds,
        "name" -> "Technical observation", "content" -> "Observe a technical condition", "context" -> "")).TAKE
    def lookup(proposalId: String): WorkbenchValue.FindCandidateByProposalResult =
      WorkbenchValue.FindCandidateByProposalResult.createC(_call(subsystem, token, "Candidate", "findCandidateByProposal",
        "producerRef" -> "workbench-material", "proposalId" -> proposalId)).TAKE
    def work(id: String): WorkbenchValue.CandidateWorkResult =
      WorkbenchValue.CandidateWorkResult.createC(_call(subsystem, token, "Candidate", "getCandidateWork", "id" -> id)).TAKE
  }

  /** Test-only service credential for the existing standalone Fixed ingress profile. */
  private final class FixtureAuthentication(token: String, subject: String) extends Component {
    val providerName: String = "material-candidate-integration-loopback"
    private val _provider = new AuthenticationProvider {
      override def name: String = providerName
      override def authenticate(request: AuthenticationRequest)(using ExecutionContext): Consequence[Option[AuthenticationResult]] =
        Consequence.success(request.accessToken.filter(_ == token).map { _ =>
          AuthenticationResult(PrincipalId(subject), subjectKind = SubjectKind.Service)
        })
    }
    override def authenticationProviders: Vector[AuthenticationProvider] = Vector(_provider)
  }

  private def _context(subsystem: Subsystem, token: Option[String]): Consequence[ExecutionContext] = {
    val component = subsystem.components.find(_.name == KnowledgeWorkbenchComponent.name)
      .getOrElse(throw new IllegalStateException("Expected ordinary Workbench component"))
    val attributes = token.map(input => Map("Authorization" -> ("Bearer " + input))).getOrElse(Map.empty[String, String])
    subsystem.executionProfileC.flatMap { profile =>
      IngressSecurityResolver.resolve(profile, component.logic.executionContext(), attributes).map(_.executionContext)
    }
  }

  private def _result(subsystem: Subsystem, token: Option[String], service: String, operation: String,
      fields: (String, Any)*): Consequence[OperationResponse] =
    _context(subsystem, token).flatMap { context =>
      subsystem.executeOperationResponseInChildContext(Request.of(component = KnowledgeWorkbenchComponent.name,
        service = service, operation = operation,
        properties = fields.map { case (key, input) => Property(key, input, None) }.toList), context)
    }

  private def _response(subsystem: Subsystem, token: String, service: String, operation: String,
      fields: (String, Any)*): OperationResponse =
    _result(subsystem, Some(token), service, operation, fields*).TAKE

  private def _call(subsystem: Subsystem, token: String, service: String, operation: String, fields: (String, Any)*): Record =
    _response(subsystem, token, service, operation, fields*) match {
      case OperationResponse.RecordResponse(record) => record
      case _ => throw new IllegalStateException("Expected native logical Record response")
    }
}
