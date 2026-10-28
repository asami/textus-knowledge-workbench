package org.simplemodeling.textus.knowledgeworkbench

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.concurrent.{FutureTask, TimeUnit}
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}
import cats.data.NonEmptyVector
import org.goldenport.Consequence
import org.goldenport.cncf.action.{ActionCall, ProcedureActionCall, QueryAction}
import org.goldenport.cncf.bootstrap.{BootstrapConfig, CncfBootstrap, CncfHandle}
import org.goldenport.cncf.cli.RunMode
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentDescriptor, ComponentId, ComponentInit, ComponentInstanceId, ComponentOrigin}
import org.goldenport.cncf.context.{ExecutionContext, GlobalContext, GlobalRuntimeContext, PrincipalId}
import org.goldenport.cncf.entity.{EntityRevisionCarrier, EntityRevisionRepresentation}
import org.goldenport.cncf.log.LogBackendHolder
import org.goldenport.cncf.security.{AuthenticationProvider, AuthenticationRequest, AuthenticationResult, SecuritySubject}
import org.goldenport.cncf.subsystem.{GenericSubsystemAuthenticationBinding, GenericSubsystemAuthenticationProviderBinding, GenericSubsystemComponentBinding, GenericSubsystemDescriptor, GenericSubsystemSecurityBinding, Subsystem}
import org.goldenport.protocol.{Property, Protocol, Request}
import org.goldenport.protocol.operation.{OperationRequest, OperationResponse}
import org.goldenport.protocol.spec
import org.goldenport.record.Record
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.model.datatype.EntityRevision
import org.simplemodeling.textus.knowledgeworkbench.impl.{KnowledgeWorkbenchParticipantFactoryBase, MaterialEvidenceWork}

/*
 * Controlled public producer observations are not real Google Drive evidence.
 * @since   Oct. 28, 2026
 * @version Oct. 28, 2026
 */
final class MaterialEvidenceWorkSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialEvidenceWorkSpec.*
  import MaterialWorkFixture.{materialRef, resourceId, owner, otherSession}

  "Explicit native Material evidence acquisition" should {
    "dispatch only immutable selectors with the same caller and retain native correspondence and human target" in {
      Given("a Material-created Candidate with a two-resource first selection and unrelated human work")
      _with_session { (session, producer) =>
        val first = _create(session, Vector(resourceId, "resource:second"))
        val human = _edit(session, first, first.candidate.evidenceRefs :+ _human_reference, "Human content")
        val request = session.request("refreshCandidateMaterialEvidence", _refresh_fields(human) ++ Vector(
          "materialRef" -> "forged:material", "resourceIds" -> Vector("forged:resource"),
          "revision" -> "forged:version", "fileId" -> "forged:provider"))
        When("the authenticated native command acquires and applies the producer observation")
        session.call(request).TAKE shouldBe a[KnowledgeWorkbenchComponent.MaterialWorkService.RefreshCandidateMaterialEvidenceActionCall]
        val result = _work(session.executeRequest(request))
        Then("one owning source query sees the caller and exact first ordered selection without client override")
        val captured = producer.calls.last
        captured._1.component shouldBe Some(MaterialWorkFixture.producerName)
        captured._1.service shouldBe Some("Material")
        captured._1.operation shouldBe "getMaterial"
        captured._1.properties.map(property => property.name -> property.value).toMap shouldBe Map(
          "materialRef" -> materialRef, "evidenceResourceIds" -> Vector(resourceId, "resource:second"))
        captured._1.source shouldBe None
        captured._2 shouldBe owner
        result.candidate.evidenceRefs.map(_.id) shouldBe Vector("tkl:evidence:0", "tkl:evidence:1", "human:unrelated")
        result.candidate.evidenceRefs.last shouldBe _human_reference
        result.candidate.materialEvidence.get.resourceIds shouldBe Vector(resourceId, "resource:second")
        result.candidate.materialEvidence.get.applications.head.resources.map(_.resourceId) shouldBe Vector(resourceId, "resource:second")
        result.candidate.materialEvidence.get.applications.head.resources.map(_.appliedReference) shouldBe result.candidate.evidenceRefs.take(2)
        result.candidate.originalProposal shouldBe first.candidate.originalProposal
        result.candidate.content shouldBe human.candidate.content
        result.candidate.context shouldBe human.candidate.context
        result.candidate.preparationState shouldBe first.candidate.preparationState
        result.candidate.preparedMaterialRefs shouldBe first.candidate.preparedMaterialRefs
        result.candidate.sourceRunRef shouldBe first.candidate.sourceRunRef
        result.candidate.sourceContextRef shouldBe first.candidate.sourceContextRef
        result.candidate.decisions.init shouldBe human.candidate.decisions
        result.candidate.decisions.last.actor shouldBe owner
        result.candidate.decisions.last.reason.value shouldBe "Observe selected originals"
        result.state shouldBe "Draft"
        result.reviewEligible shouldBe true
        result.approvalValid shouldBe false
        session.component.isMcpReady("MaterialWork", "refreshCandidateMaterialEvidence") shouldBe false
      }
    }

    "retain the exact revision, application and decisions for identical observations even in Approved, Held and Rejected" in {
      Vector("Approved", "Held", "Rejected").foreach { state =>
        Given(s"a source-bound Candidate explicitly placed in $state")
        _with_session { (session, producer) =>
          val ready = _refresh(session, _create(session))
          val selected = state match {
            case "Approved" => _decide(session, _decide(session, ready, "review"), "approve")
            case "Held" => _decide(session, ready, "hold")
            case _ => _decide(session, ready, "reject")
          }
          When("the operator refreshes unchanged evidence once")
          val result = _refresh(session, selected)
          Then("source acquisition is observed, but no native write, reopening or extra application occurs")
          result.toRecord() shouldBe selected.toRecord()
          result.state shouldBe state
          result.candidate.materialEvidence.get.applications should have size 1
          producer.calls.count(_._1.properties.exists(_.name == "evidenceResourceIds")) shouldBe 2
        }
      }
    }

    "invalidate approval for changed source versions or backing identities while preserving unrelated human references" in {
      Vector(("2", "tkl:evidence:0"), ("1", "tkl:rebound:0")).foreach { case (version, evidenceid) =>
        Given("explicitly approved source-bound work and unrelated human edits")
        _with_session { (session, producer) =>
          val edited = _edit(session, _create(session), Vector(_original_reference, _human_reference), "Human version")
          val ready = _refresh(session, edited)
          val approved = _decide(session, _decide(session, ready, "review"), "approve")
          producer.observations = Vector(_observation(resourceId, evidenceid, Some(version)))
          When("a fresh explicit source observation changes the exact managed target")
          val changed = _refresh(session, approved)
          Then("ordinary edit invalidates approval and native application history keeps both exact correspondences")
          changed.state shouldBe "Draft"
          changed.approvalValid shouldBe false
          changed.revision should be > approved.revision
          changed.candidate.evidenceRefs.last shouldBe _human_reference
          changed.candidate.content shouldBe approved.candidate.content
          changed.candidate.decisions.init shouldBe approved.candidate.decisions
          changed.candidate.materialEvidence.get.applications should have size 2
          changed.candidate.materialEvidence.get.applications.head shouldBe approved.candidate.materialEvidence.get.applications.head
          changed.candidate.materialEvidence.get.applications.last.resources.head.appliedReference.id shouldBe evidenceid
          changed.candidate.evidenceRefs.head.revision shouldBe Some(version)
          changed.candidate.evidenceRefs.head.snapshotRef shouldBe None
          changed.candidate.evidenceRefs.head.retentionUntil shouldBe None
        }
      }
    }

    "persist detachment on changed, removed, reordered or ambiguous managed projection and never resurrect it" in {
      Given("managed references for two selected resources")
      val changes: Vector[Vector[org.simplemodeling.textus.knowledgeworkbench.value.ProposalReference] => Vector[org.simplemodeling.textus.knowledgeworkbench.value.ProposalReference]] = Vector(
        references => references.updated(0, references.head.copy(revision = Some("human:version"))),
        references => references.tail :+ _human_reference,
        references => references.reverse,
        references => references :+ references.head)
      changes.foreach { change =>
        _with_session { (session, producer) =>
          val ready = _refresh(session, _create(session, Vector(resourceId, "resource:second")))
          When("a generic human full-target edit changes the managed projection, then requests refresh")
          val edited = _edit(session, ready, change(ready.candidate.evidenceRefs), "Human choice")
          val before = producer.calls.size
          session.execute("refreshCandidateMaterialEvidence", _refresh_fields(edited)) shouldBe a[Consequence.Failure[?]]
          Then("detachment is native persistent intent and source access is refused before any IO")
          edited.candidate.materialEvidence.get.status shouldBe "detached"
          producer.calls should have size before
          _get(session, edited).toRecord() shouldBe edited.toRecord()
          val restored = _edit(session, edited, ready.candidate.evidenceRefs, "Human restores old references")
          restored.candidate.materialEvidence.get.status shouldBe "detached"
          session.execute("refreshCandidateMaterialEvidence", _refresh_fields(restored)) shouldBe a[Consequence.Failure[?]]
          producer.calls should have size before
        }
      }
    }

    "preserve managed ownership through unrelated human edits and reject new collisions rather than overwrite human references" in {
      Given("active evidence plus an unrelated human reference")
      _with_session { (session, producer) =>
        val ready = _refresh(session, _create(session))
        val edited = _edit(session, ready, ready.candidate.evidenceRefs :+ _human_reference, "Human revises content")
        edited.candidate.materialEvidence.get.status shouldBe "active"
        producer.observations = Vector(_observation(resourceId, "human:unrelated", Some("2")))
        When("TKL supplies an identity colliding with the unrelated human evidence")
        session.execute("refreshCandidateMaterialEvidence", _refresh_fields(edited)) shouldBe a[Consequence.Failure[?]]
        Then("the whole native Candidate remains unchanged")
        _get(session, edited).toRecord() shouldBe edited.toRecord()
      }
    }

    "refuse stale revision, blank reason, changed legacy source or foreign producer before source IO" in {
      Given("a valid Material Candidate before any managed binding")
      _with_session { (session, producer) =>
        val first = _create(session)
        val changed = _edit(session, first, Vector(_human_reference), "Human replaces original source")
        val before = producer.calls.size
        When("stale, invalid and no-longer-owned refresh intents are submitted")
        session.execute("refreshCandidateMaterialEvidence", _refresh_fields(first)) shouldBe a[Consequence.Failure[?]]
        session.execute("refreshCandidateMaterialEvidence", _refresh_fields(changed).map {
          case ("reason", _) => "reason" -> " "
          case field => field
        }) shouldBe a[Consequence.Failure[?]]
        session.execute("refreshCandidateMaterialEvidence", _refresh_fields(changed)) shouldBe a[Consequence.Failure[?]]
        val snapshot = _snapshot(first)
        MaterialEvidenceWork.prepare(snapshot.copy(entity = first.candidate.copy(producerRef = None)),
          _input(first)) shouldBe a[Consequence.Failure[?]]
        Then("the actual source is never acquired and native human choices remain exact")
        producer.calls should have size before
        _get(session, changed).toRecord() shouldBe changed.toRecord()
      }
    }

    "preserve authenticated source denial and every structured acquisition failure without a write or replay" in {
      Given("an authenticated owner and a producer capable of structured failure")
      _with_session { (session, producer) =>
        val first = _create(session)
        val before = producer.calls.size
        When("an anonymous or different caller requests access")
        session.execute("refreshCandidateMaterialEvidence", _refresh_fields(first), sessionId = None) shouldBe a[Consequence.Failure[?]]
        session.execute("refreshCandidateMaterialEvidence", _refresh_fields(first), sessionId = Some(otherSession)) shouldBe a[Consequence.Failure[?]]
        producer.calls should have size before
        val failures = Vector(Consequence.securityPermissionDenied[OperationResponse]("Source denied"),
          Consequence.resourceNotFound[OperationResponse]("Source absent"),
          Consequence.serviceUnavailable[OperationResponse]("Observation outcome unknown"))
        failures.foreach { failure =>
          producer.overrideResponse = Some(failure)
          session.execute("refreshCandidateMaterialEvidence", _refresh_fields(first)) shouldBe failure
        }
        Then("upstream Conclusions remain exact and one call per explicit request changes no Candidate")
        producer.calls should have size (before + failures.size)
        _get(session, first).toRecord() shouldBe first.toRecord()
      }
    }

    "reject missing, duplicate, malformed, wrongly ordered or mismatched observations atomically" in {
      Given("a two-resource immutable selection and independently malformed public producer results")
      _with_session { (session, producer) =>
        val first = _create(session, Vector(resourceId, "resource:second"))
        val good = producer.observations
        val invalid: Vector[Any] = Vector(Vector.empty[Record], Vector(good.head), good.reverse,
          Vector(good.head, good.head), Vector("not a record"), "not a collection",
          Vector(good.head, _observation("wrong:resource", "tkl:evidence:1", Some("1"))),
          Vector(good.head, _observation("resource:second", "tkl:evidence:0", Some("1"))),
          Vector(good.head, Record.data("evidenceRef" -> " ", "materialRef" -> materialRef,
            "resourceId" -> "resource:second", "retrievalState" -> "available", "limitation" -> "Missing revision")),
          Vector(good.head, Record.data("evidenceRef" -> "id", "materialRef" -> "wrong:material",
            "resourceId" -> "resource:second", "retrievalState" -> "available", "revision" -> "1", "limitation" -> "Wrong material")),
          Vector(good.head, _observation("resource:second", "id", None) ++ Record.data("revision" -> 1)),
          Vector(good.head, _observation("resource:second", "id", Some("1")) ++ Record.data("snapshotRef" -> "invented")))
        When("each incomplete or contradictory result reaches the explicit command")
        invalid.foreach { observations =>
          producer.overrideResponse = Some(Consequence.success(OperationResponse(_metadata(Vector(resourceId, "resource:second")) ++
            Record.data("observedEvidence" -> observations))))
          session.execute("refreshCandidateMaterialEvidence", _refresh_fields(first)) shouldBe a[Consequence.Failure[?]]
          _get(session, first).toRecord() shouldBe first.toRecord()
        }
        producer.overrideResponse = Some(Consequence.success(OperationResponse(Record.data("status" -> "unknown"))))
        session.execute("refreshCandidateMaterialEvidence", _refresh_fields(first)) shouldBe a[Consequence.Failure[?]]
        Then("neither partial applications nor source-dependent writes occurred")
        _get(session, first).candidate.materialEvidence shouldBe None
        _get(session, first).revision shouldBe first.revision
      }
    }

    "retain unversioned observations honestly and keep every evidence reference under the existing Review gate" in {
      Given("a completed source read without an authoritative version")
      _with_session { (session, producer) =>
        val first = _create(session)
        producer.observations = Vector(_observation(resourceId, "tkl:evidence:0", None))
        When("that unverified observation is explicitly bound to native work")
        val result = _refresh(session, first)
        Then("the binding survives, but source/Candidate revisions do not become eligibility")
        result.candidate.materialEvidence.get.applications.head.resources.head.appliedReference.revision shouldBe None
        result.candidate.evidenceRefs.head.retrievalState shouldBe Some("unverified")
        result.reviewEligible shouldBe false
        Vector("review", "approve").foreach { decision =>
          session.execute("decideCandidateWork", _decision_fields(result, decision), service = "Candidate") shouldBe a[Consequence.Failure[?]]
        }
        val unready = _human_reference.copy(revision = None, retrievalState = Some("unverified"))
        producer.observations = Vector(_observation(resourceId, "tkl:evidence:0", Some("1")))
        val ready = _refresh(session, result)
        val mixed = _edit(session, ready, ready.candidate.evidenceRefs :+ unready, "Unready human reference")
        mixed.reviewEligible shouldBe false
      }
    }

    "reconcile a lost command acknowledgement by known native identity without source replay" in {
      Given("a known native Candidate whose refresh response is lost after the write")
      _with_session { (session, producer) =>
        val first = _create(session)
        When("the source refresh executes once but the caller cannot acknowledge its response")
        val lost = session.execute("refreshCandidateMaterialEvidence", _refresh_fields(first)).flatMap(_ =>
          Consequence.serviceUnavailable[OperationResponse]("Controlled acknowledgement loss after execution"))
        lost shouldBe a[Consequence.Failure[?]]
        val calls = producer.calls.size
        val reconciled = _get(session, first)
        Then("getCandidateWork finds the same native identity, one application and ordinary edit without resubmission")
        reconciled.candidate.id shouldBe first.candidate.id
        reconciled.candidate.materialEvidence.get.applications should have size 1
        reconciled.candidate.decisions.map(_.decision.value) shouldBe Vector("edit")
        producer.calls should have size calls
      }
    }

    "maintain exact ordered resource/reference correspondence as a property over opaque source versions" in {
      Given("native Material work and distinct opaque logical resource IDs")
      _with_session { (session, producer) =>
        val first = _create(session, Vector(resourceId, "resource:second"))
        val selection = MaterialEvidenceWork.prepare(_snapshot(first), _input(first)).TAKE
        val property = Prop.forAll(Gen.nonEmptyListOf(Gen.alphaNumChar), Gen.nonEmptyListOf(Gen.alphaNumChar)) { (left, right) =>
          val versions = Vector(left.mkString, right.mkString).map(version => s" version:$version/東京 %2F ")
          val observations = selection.resourceIds.zipWithIndex.map { case (id, index) => _observation(id, s"evidence:$index", Some(versions(index))) }
          When("opaque producer versions are parsed without normalization or consumer invention")
          val result = MaterialEvidenceWork.observe(selection, _metadata(selection.resourceIds) ++ Record.data("observedEvidence" -> observations)).TAKE
          Then("all logical identities, order and exact versions correspond")
          result.map(_.resourceId) == selection.resourceIds && result.map(_.appliedReference.revision) == versions.map(Some(_)) &&
            result.forall(resource => resource.appliedReference.snapshotRef.isEmpty && resource.appliedReference.retentionUntil.isEmpty)
        }
        Test.check(Test.Parameters.default.withMinSuccessfulTests(40), property).passed shouldBe true
      }
    }

    "retain binding, first selection and attributed human history after actual PostgreSQL restart, and reject concurrent source-window changes" in {
      Given("a real PostgreSQL store, native Workbench and controlled same-caller public TKL producer")
      val database = new MaterialCandidateWorkSpec.LivePostgresql
      database.start()
      try {
        val root = Files.createTempDirectory(Files.createDirectories(Path.of("target/material-evidence-postgresql").toAbsolutePath), "case-")
        val writer = _pg_handle(root, database)
        val saved = try {
          val producer = installProducer(writer.subsystem)
          val first = _pg_create(writer.subsystem)
          val ready = _pg_refresh(writer.subsystem, first)
          val human = _pg_work(writer.subsystem, "Candidate", "editCandidateWork", _edit_fields(ready,
            ready.candidate.evidenceRefs :+ _human_reference, "Durable human content"))
          _pg_work(writer.subsystem, "Candidate", "decideCandidateWork", _decision_fields(human, "hold"))
        } finally writer.close()
        When("a fresh runtime opens the same database and reconciles by native identity")
        val reader = _pg_handle(root, database)
        try {
          val producer = installProducer(reader.subsystem)
          val reopened = _pg_work(reader.subsystem, "Candidate", "getCandidateWork", Vector("id" -> saved.candidate.id.print))
          Then("the full binding, immutable first selection, target and human history survive together")
          reopened.toRecord() shouldBe saved.toRecord()
          reopened.candidate.materialEvidence.get.status shouldBe "active"
          reopened.candidate.decisions.map(_.actor).distinct shouldBe Vector("material-evidence-postgres-user")
          _pg_refresh(reader.subsystem, reopened).toRecord() shouldBe reopened.toRecord()
          Vector(false, true).foreach { unchanged =>
            Given("a current snapshot and a concurrent human edit during its one source acquisition")
            val before = _pg_work(reader.subsystem, "Candidate", "getCandidateWork", Vector("id" -> saved.candidate.id.print))
            var committed: Option[org.simplemodeling.textus.knowledgeworkbench.value.CandidateWorkResult] = None
            producer.observations = Vector(_observation(resourceId, "tkl:evidence:0", Some(if (unchanged) "1" else "2")))
            producer.onAcquire = Some(() => {
              producer.onAcquire = None
              // A reentrant call on this thread is not an independent caller/UoW.
              val edit = new FutureTask[org.simplemodeling.textus.knowledgeworkbench.value.CandidateWorkResult](() => {
                val changed = _pg_work(reader.subsystem, "Candidate", "editCandidateWork", _edit_fields(before,
                  before.candidate.evidenceRefs, s"Concurrent edit $unchanged"))
                val confirmed = _pg_work(reader.subsystem, "Candidate", "getCandidateWork",
                  Vector("id" -> before.candidate.id.print))
                confirmed.toRecord() shouldBe changed.toRecord()
                confirmed
              })
              val thread = new Thread(edit, "material-evidence-independent-human-edit")
              thread.start()
              val confirmed = edit.get(30, TimeUnit.SECONDS)
              confirmed.revision should be > before.revision
              confirmed.candidate.content.map(_.value) shouldBe Some(s"Concurrent edit $unchanged")
              committed = Some(confirmed)
              ()
            })
            When(if (unchanged) "a would-be no-op returns from the source" else "changed evidence returns from the source")
            val calls = producer.calls.size
            val outcome = _pg_call(reader.subsystem, "MaterialWork", "refreshCandidateMaterialEvidence", _refresh_fields(before))
            committed.flatMap(_.candidate.content).map(_.value) shouldBe Some(s"Concurrent edit $unchanged")
            outcome shouldBe a[Consequence.Failure[?]]
            Then("revision reconciliation/CAS detects the concurrent write, does not replay and preserves that human edit")
            producer.calls should have size (calls + 1)
            val after = _pg_work(reader.subsystem, "Candidate", "getCandidateWork", Vector("id" -> before.candidate.id.print))
            after.candidate.content.map(_.value) shouldBe Some(s"Concurrent edit $unchanged")
            after.candidate.materialEvidence shouldBe reopened.candidate.materialEvidence
            after.candidate.evidenceRefs shouldBe reopened.candidate.evidenceRefs
          }
        } finally reader.close()
      } finally database.stop()
    }
  }
}

object MaterialEvidenceWorkSpec {
  import MaterialWorkFixture.{materialRef, resourceId}

  final class Producer extends Component {
    var observations: Vector[Record] = Vector(_observation(resourceId, "tkl:evidence:0", Some("1")))
    var overrideResponse: Option[Consequence[OperationResponse]] = None
    var onAcquire: Option[() => Unit] = None
    private var _calls = Vector.empty[(Request, String)]
    def calls: Vector[(Request, String)] = _calls
    def receive(request: Request, context: ExecutionContext): Consequence[OperationResponse] = {
      _calls = _calls :+ (request -> SecuritySubject.from(context.security).subjectId)
      if (request.properties.exists(_.name == "evidenceResourceIds")) {
        onAcquire.foreach(_())
        overrideResponse.getOrElse(Consequence.success(OperationResponse(_metadata(observations.flatMap(_.getString("resourceId"))) ++
          Record.data("observedEvidence" -> observations))))
      } else Consequence.success(OperationResponse(_metadata(Vector(resourceId, "resource:second"))))
    }
  }

  def installProducer(subsystem: Subsystem): Producer = {
    val producer = new Producer
    val producerid = ComponentId(MaterialWorkFixture.producerName)
    val operation = new spec.OperationDefinition {
      override val specification = spec.OperationDefinition.Specification(name = "getMaterial",
        request = spec.RequestDefinition(), response = spec.ResponseDefinition.void)
      override def createOperationRequest(req: Request): Consequence[OperationRequest] = Consequence.success(new QueryAction {
        override val request: Request = req
        override def createCall(callcore: ActionCall.Core): ActionCall = new ProcedureActionCall {
          override val core: ActionCall.Core = callcore
          override def execute(): Consequence[OperationResponse] = producer.receive(req, callcore.executionContext)
        }
      })
    }
    val protocol = Protocol(services = spec.ServiceDefinitionGroup(Vector(spec.ServiceDefinition(
      name = "Material", operations = spec.OperationDefinitionGroup(NonEmptyVector.one(operation))))))
    val core = Component.Core.create(name = producerid.name, componentId = producerid,
      instanceId = ComponentInstanceId.default(producerid), protocol = protocol)
    producer.initialize(ComponentInit(subsystem, core, ComponentOrigin.Builtin))
    subsystem.upsert(Vector(producer))
    producer
  }

  private object FixtureProvider extends AuthenticationProvider {
    def name: String = "material-evidence-fixture-provider"
    def authenticate(request: AuthenticationRequest)(using ExecutionContext): Consequence[Option[AuthenticationResult]] =
      Consequence.success(request.sessionId.flatMap {
        case MaterialWorkFixture.ownerSession => Some(AuthenticationResult(PrincipalId(MaterialWorkFixture.owner)))
        case MaterialWorkFixture.otherSession => Some(AuthenticationResult(PrincipalId(MaterialWorkFixture.otherSubject)))
        case _ => None
      })
  }

  private final class RevisionedComponent extends KnowledgeWorkbenchComponent {
    override def authenticationProviders: Vector[AuthenticationProvider] = Vector(FixtureProvider)
    override def mcpReadyServices: Set[String] = Set.empty
    // Match the owning production Component, rather than the metadata-only fixture.
    override def componentDescriptors: Vector[ComponentDescriptor] =
      super.componentDescriptors.map(descriptor => descriptor.copy(
        entityRuntimeDescriptors = descriptor.entityRuntimeDescriptors.map(declaration =>
          if (declaration.collectionId == org.simplemodeling.textus.knowledgeworkbench.entity.InformationCandidate.collectionId)
            declaration.copy(revisionRepresentation = Some(EntityRevisionRepresentation.Detached),
              concurrencyPolicy = Some(org.goldenport.cncf.entity.EntityConcurrencyPolicy.Optimistic))
          else declaration)))
  }

  private object RevisionedFactory extends KnowledgeWorkbenchParticipantFactoryBase with Component.PrimaryComponentFactory {
    override protected def create_Component(params: ComponentCreate): Component = new RevisionedComponent
    override protected def create_Core(params: ComponentCreate, comp: Component): Component.Core =
      component_core(KnowledgeWorkbenchComponent.name, KnowledgeWorkbenchComponent.componentId)
  }

  private def _with_session[A](body: (MaterialWorkFixture.Session, Producer) => A): A = MaterialWorkFixture.synchronized {
    val previousruntime = GlobalRuntimeContext.current
    val previousglobal = Try(GlobalContext.globalContext).toOption
    val previouslog = LogBackendHolder.backend
    val workroot = Files.createDirectories(Path.of("target/material-evidence-work/work").toAbsolutePath.normalize())
    val root = Files.createTempDirectory(workroot, "session-")
    try {
      val prefix = "textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application"
      val config = io.circe.Json.obj(
        MaterialWorkFixture.configurationKey -> io.circe.Json.fromString(root.resolve("data").toString),
        s"$prefix.policy" -> io.circe.Json.fromString("local-default"))
      Files.writeString(Files.createDirectories(root.resolve(".textus")).resolve("config.json"), config.noSpaces, StandardCharsets.UTF_8)
      val testdescriptor = root.resolve("test.yaml")
      Files.writeString(testdescriptor, """kind: test-descriptor
        |execution:
        |  profile: controlled
        |  key: material-evidence-work
        |  time:
        |    mode: manual
        |    start-at: '2026-10-21T00:00:00Z'
        |  random:
        |    mode: seeded
        |    seed: material-evidence-work
        |  ids:
        |    mode: deterministic
        |  scheduler:
        |    mode: manual
        |  ordering:
        |    mode: deterministic
        |""".stripMargin, StandardCharsets.UTF_8)
      val handle = CncfBootstrap.initialize(BootstrapConfig(cwd = root,
        args = Array(s"--textus.test.descriptor=$testdescriptor"))).TAKE
      try {
        val id = KnowledgeWorkbenchComponent.componentId
        val descriptor = ComponentDescriptor(name = Some(id.name), componentName = Some(id.name), schemaVersion = Some(3), componentId = Some(id))
        val component = RevisionedFactory.createPrimaryC(ComponentCreate(handle.subsystem, ComponentOrigin.Builtin, Vector(descriptor)))
          .TAKE.asInstanceOf[KnowledgeWorkbenchComponent]
        handle.subsystem.add(component)
        val producer = installProducer(handle.subsystem)
        handle.subsystem.withDescriptor(GenericSubsystemDescriptor(
          path = root.resolve("subsystem.yaml"), subsystemName = handle.subsystem.name,
          componentBindings = Vector(GenericSubsystemComponentBinding(component.name), GenericSubsystemComponentBinding(MaterialWorkFixture.producerName)),
          security = Some(GenericSubsystemSecurityBinding(authentication = Some(GenericSubsystemAuthenticationBinding(
            convention = Some("enabled"), fallbackPrivilege = Some("disabled"), providers = Vector(
              GenericSubsystemAuthenticationProviderBinding(name = FixtureProvider.name, component = component.name, enabled = Some(true)))))))))
        body(new MaterialWorkFixture.Session(component, new MaterialWorkFixture.Producer, handle.subsystem, root), producer)
      } finally handle.close()
    } finally {
      GlobalRuntimeContext.current = previousruntime
      previousglobal.foreach(GlobalContext.set)
      previouslog.foreach(LogBackendHolder.install)
      Using.resource(Files.walk(root)) { stream =>
        stream.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.deleteIfExists(_))
      }
    }
  }

  private def _metadata(ids: Vector[String]): Record = Record.data("materialRef" -> materialRef,
    "title" -> "Controlled logical Material", "resources" -> ids.map(id => Record.data(
      "resourceId" -> id, "role" -> "original", "mediaType" -> "image/png")), "provenance" -> Vector.empty[Record])

  private def _observation(resourceid: String, evidenceid: String, revision: Option[String]): Record =
    Record.data("evidenceRef" -> evidenceid, "materialRef" -> materialRef, "resourceId" -> resourceid,
      "retrievalState" -> (if (revision.nonEmpty) "available" else "unverified"),
      "limitation" -> "Controlled completed read; no historical retention promise") ++
      revision.map(text => Record.data("revision" -> text)).getOrElse(Record.empty)

  private val _original_reference = value.ProposalReference(materialRef, "material", None, None, Some("unverified"),
    None, Some(impl.MaterialCandidateWork.limitation))
  private val _human_reference = value.ProposalReference("human:unrelated", "citation", Some("human:1"), None,
    Some("available"), None, Some("Human-declared unrelated evidence"))

  private def _create_fields(ids: Vector[String]): Vector[(String, Any)] = Vector("proposalId" -> "material:evidence:intent",
    "materialRef" -> materialRef, "resourceIds" -> ids, "name" -> "Material work", "content" -> "Initial content", "context" -> "Human context")
  private def _create(session: MaterialWorkFixture.Session, ids: Vector[String] = Vector(resourceId)): value.CandidateWorkResult = {
    val result = value.RegisterProposalResult.createC(_record(session.execute("createCandidateFromMaterial", _create_fields(ids)))).TAKE
    val producer = session.subsystem.components.find(_.name == MaterialWorkFixture.producerName).get.asInstanceOf[Producer]
    producer.observations = ids.zipWithIndex.map { case (id, index) => _observation(id, s"tkl:evidence:$index", Some("1")) }
    _work(session.execute("getCandidateWork", Vector("id" -> result.candidate.id.print), service = "Candidate"))
  }
  private def _refresh_fields(work: value.CandidateWorkResult): Vector[(String, Any)] = Vector(
    "id" -> work.candidate.id.print, "expectedRevision" -> work.revision, "reason" -> "Observe selected originals")
  private def _edit_fields(work: value.CandidateWorkResult, references: Vector[value.ProposalReference], content: String): Vector[(String, Any)] =
    Vector("id" -> work.candidate.id.print, "expectedRevision" -> work.revision, "content" -> content,
      "context" -> work.candidate.context.get.value, "evidenceRefs" -> references.map(_.toRecord()),
      "groundingRefs" -> work.candidate.groundingRefs.map(_.toRecord()), "mappingRefs" -> work.candidate.mappingRefs.map(_.toRecord()), "reason" -> "Human edit")
  private def _decision_fields(work: value.CandidateWorkResult, decision: String): Vector[(String, Any)] =
    Vector("id" -> work.candidate.id.print, "expectedRevision" -> work.revision, "decision" -> decision, "reason" -> "Human decision")
  private def _refresh(session: MaterialWorkFixture.Session, work: value.CandidateWorkResult): value.CandidateWorkResult =
    _work(session.execute("refreshCandidateMaterialEvidence", _refresh_fields(work)))
  private def _get(session: MaterialWorkFixture.Session, work: value.CandidateWorkResult): value.CandidateWorkResult =
    _work(session.execute("getCandidateWork", Vector("id" -> work.candidate.id.print), service = "Candidate"))
  private def _edit(session: MaterialWorkFixture.Session, work: value.CandidateWorkResult,
      references: Vector[value.ProposalReference], content: String): value.CandidateWorkResult =
    _work(session.execute("editCandidateWork", _edit_fields(work, references, content), service = "Candidate"))
  private def _decide(session: MaterialWorkFixture.Session, work: value.CandidateWorkResult, decision: String): value.CandidateWorkResult =
    _work(session.execute("decideCandidateWork", _decision_fields(work, decision), service = "Candidate"))
  private def _snapshot(work: value.CandidateWorkResult): EntityRevisionCarrier[entity.InformationCandidate] =
    EntityRevisionCarrier(work.candidate, EntityRevision.createC(work.revision).TAKE)
  private def _input(work: value.CandidateWorkResult): value.RefreshCandidateMaterialEvidence =
    value.RefreshCandidateMaterialEvidence.createC(Record.data(_refresh_fields(work)*)).TAKE
  private def _record(result: Consequence[OperationResponse]): Record = result.TAKE match {
    case OperationResponse.RecordResponse(record) => record
    case other => throw new IllegalStateException(s"Expected native Record: $other")
  }
  private def _work(result: Consequence[OperationResponse]): value.CandidateWorkResult = value.CandidateWorkResult.createC(_record(result)).TAKE

  private def _pg_call(subsystem: Subsystem, service: String, operation: String, fields: Vector[(String, Any)]): Consequence[OperationResponse] =
    subsystem.executeOperationResponse(Request.of(component = KnowledgeWorkbenchComponent.name, service = service,
      operation = operation, properties = fields.map { case (key, input) => Property(key, input, None) }.toList))
  private def _pg_work(subsystem: Subsystem, service: String, operation: String, fields: Vector[(String, Any)]): value.CandidateWorkResult =
    _work(_pg_call(subsystem, service, operation, fields))
  private def _pg_create(subsystem: Subsystem): value.CandidateWorkResult = {
    val created = value.RegisterProposalResult.createC(_record(_pg_call(subsystem, "MaterialWork", "createCandidateFromMaterial", _create_fields(Vector(resourceId))))).TAKE
    _pg_work(subsystem, "Candidate", "getCandidateWork", Vector("id" -> created.candidate.id.print))
  }
  private def _pg_refresh(subsystem: Subsystem, work: value.CandidateWorkResult): value.CandidateWorkResult =
    _pg_work(subsystem, "MaterialWork", "refreshCandidateMaterialEvidence", _refresh_fields(work))

  private def _pg_handle(root: Path, database: MaterialCandidateWorkSpec.LivePostgresql): CncfHandle = {
    val repository = Path.of(".").toAbsolutePath.normalize()
    val descriptor = root.resolve("assembly.yaml")
    val prefix = "textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application"
    Files.writeString(descriptor, s"""subsystem: material-evidence-postgres-spec
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
      id: material-evidence-postgres-user
""")
    CncfBootstrap.initialize(BootstrapConfig(cwd = root,
      args = Array("--discover=classes", s"--component-dev-dir=$repository", s"--textus.assembly.descriptor=$descriptor",
        s"--textus.local-data.root=$root", "command"), modeHint = Some(RunMode.Command))).TAKE
  }
}
