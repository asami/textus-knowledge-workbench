package org.simplemodeling.textus.knowledgeworkbench

import java.nio.file.{Files, Path}
import java.time.Instant
import cats.free.Free
import org.goldenport.{Consequence, ConsequenceT}
import org.goldenport.cncf.{Program}
import org.goldenport.cncf.bootstrap.{BootstrapConfig, CncfBootstrap}
import org.goldenport.cncf.cli.RunMode
import org.goldenport.cncf.context.{ExecutionContext, IdGenerationContext, PrincipalId}
import org.goldenport.cncf.entity.{EntityPersistent, EntityPersistentCreate, EntityRevisionCarrier}
import org.goldenport.cncf.unitofwork.{ExecUowM, UnitOfWork, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.simplemodeling.model.datatype.{EntityCollectionId, EntityRevision}
import org.goldenport.record.Record
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/*
 * Controlled semantic work seeds are executable fixture data, never human grants.
 * @since   Oct. 30, 2026
 * @version Oct. 30, 2026
 */
object SemanticFormationWorkFixture {
  val instant: Instant = Instant.parse("2026-10-30T00:00:00Z")
  val ids: IdGenerationContext = IdGenerationContext.deterministic(IdGenerationContext.DEFAULT_NAMESPACE, "semantic-work-spec")
  def target(): value.SemanticFormationTarget = value.SemanticFormationTarget(
    ids.entityId(EntityCollectionId("major", "minor", "information")), 3L, "textus:publication", "1", "publication", "Saved Book",
    Vector(value.SemanticBinding("isbn", "publication", "9784122025660")), "known", "Controlled source assertion", "manual-correction")
  def snapshot(): EntityRevisionCarrier[entity.SemanticFormationWork] = {
    val selected = target()
    EntityRevisionCarrier(entity.SemanticFormationWork(ids.entityId(entity.SemanticFormationWork.collectionId),
      selected.informationId, selected.informationRevision, Some(selected), Vector.empty, Vector.empty), EntityRevision.createC(7L).TAKE)
  }
  final class Database extends PostgreSQLContainer(DockerImageName.parse("postgres:17.11-bookworm")) {
    withDatabaseName("workbench_semantic")
    withUsername("workbench_semantic")
    withPassword("controlled-test-only")
    withCommand("postgres", "-c", "fsync=on", "-c", "synchronous_commit=on")
  }
  def root(): Path = Files.createTempDirectory(Files.createDirectories(Path.of("target/semantic-work-spec").toAbsolutePath), "case-")
  final class Session(database: Database, root: Path) extends AutoCloseable {
    private[SemanticFormationWorkFixture] val hub = new ControlledHub(target())
    private val _descriptor = root.resolve("assembly.yaml")
    Files.writeString(_descriptor, s"""subsystem: semantic-work-spec
version: 0.1.0-SNAPSHOT
components:
  - namespace: org.simplemodeling.textus
    id: KnowledgeWorkbench
    version: 0.1.0-SNAPSHOT
subsystemCapabilities:
  providers:
    - name: fixture-runtime
      component: org.simplemodeling.textus.KnowledgeWorkbench
      provides:
        - datastore.optimistic-concurrency@1
        - datastore.persistent@1
        - datastore.transactional@1
        - user-context.current@1
config:
  textus.subsystem.user-mode: multi-user
  textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.kind: postgresql
  textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.jdbc.url: ${database.getJdbcUrl}
  textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.jdbc.user: ${database.getUsername}
  textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.jdbc.password: ${database.getPassword}
  textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.jdbc.driver: org.postgresql.Driver
security:
  authentication:
    convention: enabled
    fallbackPrivilege: disabled
    providers:
      - name: controlled-semantic-work-provider
        component: org.simplemodeling.textus.KnowledgeHub
        enabled: true
""")
    val handle = CncfBootstrap.initialize(BootstrapConfig(cwd = root, args = Array("--discover=classes",
      "--component-dev-dir=" + Path.of(".").toAbsolutePath.normalize(), "--textus.assembly.descriptor=" + _descriptor, "command"),
      modeHint = Some(RunMode.Command), extraComponents = subsystem => {
        val componentid = org.goldenport.cncf.component.ComponentId("org.simplemodeling.textus.KnowledgeHub")
        val operations = Vector("proposeSemantics", "validateSemanticTarget", "formKnowledge", "getFormedKnowledge")
          .map(name => ControlledHubOperation(name, hub))
        val protocol = org.goldenport.protocol.Protocol(services = org.goldenport.protocol.spec.ServiceDefinitionGroup(Vector(
          org.goldenport.protocol.spec.ServiceDefinition(name = "KnowledgeFormation",
            operations = org.goldenport.protocol.spec.OperationDefinitionGroup(cats.data.NonEmptyVector.fromVectorUnsafe(operations))))))
        hub.initialize(org.goldenport.cncf.component.ComponentInit(subsystem,
          org.goldenport.cncf.component.Component.Core.create(name = componentid.name, componentId = componentid,
            instanceId = org.goldenport.cncf.component.ComponentInstanceId.default(componentid), protocol = protocol),
          org.goldenport.cncf.component.ComponentOrigin.Builtin))
        Vector(hub)
      })).TAKE
    private val _base_context = handle.subsystem.components.find(_.componentId == KnowledgeWorkbenchComponent.componentId).get.logic.executionContext()
    // Seed and reconstructed public reads use the initially admitted controlled provider.
    val context = org.goldenport.cncf.security.IngressSecurityResolver.resolve(_base_context,
      Map("authorization" -> "Bearer semantic-owner-token")).TAKE.executionContext
    private val _datasource = new org.postgresql.ds.PGSimpleDataSource
    _datasource.setURL(database.getJdbcUrl)
    _datasource.setUser(database.getUsername)
    _datasource.setPassword(database.getPassword)
    private val _store = new org.goldenport.cncf.datastore.sql.SqlDataStore(
      org.goldenport.cncf.datastore.sql.PostgreSqlDialectDriver, _datasource)
    context.dataStoreSpace.useDataStore(_store)
    def seed(work: entity.SemanticFormationWork): Consequence[Unit] = for {
      draft <- entity.create.SemanticFormationWork.createC(work.toRecord())
      _ <- new UnitOfWorkInterpreter(new UnitOfWork(context)).evaluateInActiveUnitOfWorkC(
        ConsequenceT.liftF[[X] =>> Program[UnitOfWorkOp, X], org.goldenport.cncf.entity.CreateResult[entity.create.SemanticFormationWork]](
          Free.liftF[UnitOfWorkOp, org.goldenport.cncf.entity.CreateResult[entity.create.SemanticFormationWork]](UnitOfWorkOp.EntityStoreCreate(draft,
            summon[EntityPersistentCreate[entity.create.SemanticFormationWork]]))))
    } yield ()
    def close(): Unit = try handle.close() finally _store.closeC().TAKE
  }

  enum OwnerOutcome { case Success, Unavailable, LostSuccess }
  final class LostOwnerAcknowledgement extends scala.util.control.ControlThrowable

  /** Controlled Hub contracts only; real composed Hub/Studio acceptance is separate. */
  final class PublicBoundary(val session: Session, initialTarget: value.SemanticFormationTarget = target()) {
    import org.goldenport.cncf.component.*
    import org.goldenport.cncf.subsystem.*
    import org.goldenport.protocol.{Protocol, Request}
    import org.goldenport.protocol.spec
    private val _hub = session.hub
    _hub.select(initialTarget)
    private val _engine = new org.goldenport.cncf.http.HttpExecutionEngine(session.handle.subsystem)
    def informationId: org.simplemodeling.model.datatype.EntityId = initialTarget.informationId
    def formationCalls: Int = _hub.formationCalls
    def resultReads: Vector[String] = _hub.resultReads
    def retainedResult: Option[Record] = _hub.retainedResult
    def ownerSubjects: Vector[String] = _hub.ownerSubjects
    def beforeFormation(observe: Request => Unit): Unit = _hub.beforeFormation(observe)
    def outcome(selected: OwnerOutcome): Unit = _hub.outcome(selected)
    def restoreResult(result: Record): Unit = _hub.restoreResult(result)
    def http(operation: String, fields: Record, command: Boolean = true,
        token: Option[String] = Some("semantic-owner-token"), service: String = "semantic-work"): org.goldenport.http.HttpResponse = {
      val headers = Record.data("Accept" -> "application/json", "Content-Type" -> "application/x-www-form-urlencoded") ++
        token.map(text => Record.data("Authorization" -> ("Bearer " + text))).getOrElse(Record.empty)
      _engine.execute(org.goldenport.http.HttpRequest.fromPath(
        if (command) org.goldenport.http.HttpRequest.POST else org.goldenport.http.HttpRequest.GET,
        "/knowledge-workbench/" + service + "/" + operation + ".json",
        query = if (command) Record.empty else fields, form = if (command) fields else Record.empty, header = headers))
    }
    def json(response: org.goldenport.http.HttpResponse): io.circe.Json =
      io.circe.parser.parse(response.bag.asString().TAKE).fold(throw _, scala.Predef.identity)
    def record(response: org.goldenport.http.HttpResponse): Record =
      org.goldenport.schema.XSemiStructuredData.fromJson(json(response)).TAKE
    def execute(operation: String, fields: Record): Consequence[org.goldenport.protocol.operation.OperationResponse] = {
      val request = Request.of(component = KnowledgeWorkbenchComponent.name, service = "SemanticWork", operation = operation,
        properties = fields.asMap.toList.map { case (key, datum) => org.goldenport.protocol.Property(key, datum, None) })
      org.goldenport.cncf.security.IngressSecurityResolver.resolve(session.context,
        Map("authorization" -> "Bearer semantic-owner-token")).flatMap(admitted =>
        session.handle.subsystem.executeOperationResponseInContext(request, admitted.executionContext))
    }
    def get(id: org.simplemodeling.model.datatype.EntityId): value.SemanticWorkResult =
      execute("getSemanticWork", Record.data("id" -> id.value)).TAKE match {
        case org.goldenport.protocol.operation.OperationResponse.RecordResponse(record) => value.SemanticWorkResult.createC(record).TAKE
        case other => throw new IllegalStateException("Expected public semantic work result: " + other)
      }
  }

  /** New native component/realm/context and pool; preserve the caller's process-global runtime. */
  def independentlyRead(database: Database, root: Path, id: org.simplemodeling.model.datatype.EntityId): value.SemanticWorkResult = {
    val previousruntime = org.goldenport.cncf.context.GlobalRuntimeContext.current
    val previousglobal = scala.util.Try(org.goldenport.cncf.context.GlobalContext.globalContext).toOption
    val previouslog = org.goldenport.cncf.log.LogBackendHolder.backend
    val reader = new Session(database, root)
    try {
      val request = org.goldenport.protocol.Request.of(component = KnowledgeWorkbenchComponent.name, service = "SemanticWork",
        operation = "getSemanticWork", properties = List(org.goldenport.protocol.Property("id", id.value, None)))
      reader.handle.subsystem.executeOperationResponseInContext(request, reader.context).TAKE match {
        case org.goldenport.protocol.operation.OperationResponse.RecordResponse(record) => value.SemanticWorkResult.createC(record).TAKE
        case other => throw new IllegalStateException("Expected independently reconstructed public work: " + other)
      }
    } finally {
      try reader.close() finally {
        org.goldenport.cncf.context.GlobalRuntimeContext.current = previousruntime
        previousglobal.foreach(org.goldenport.cncf.context.GlobalContext.set)
        previouslog.foreach(org.goldenport.cncf.log.LogBackendHolder.install)
      }
    }
  }

  private object ControlledAuthentication extends org.goldenport.cncf.security.AuthenticationProvider {
    def name: String = "controlled-semantic-work-provider"
    def authenticate(request: org.goldenport.cncf.security.AuthenticationRequest)(using ExecutionContext):
        Consequence[Option[org.goldenport.cncf.security.AuthenticationResult]] =
      Consequence.success(request.accessToken.flatMap {
        case "semantic-owner-token" => Some(org.goldenport.cncf.security.AuthenticationResult(PrincipalId("controlled-semantic-editor")))
        case "semantic-foreign-token" => Some(org.goldenport.cncf.security.AuthenticationResult(PrincipalId("foreign-semantic-editor")))
        case _ => None
      })
  }
  private final class ControlledHub(initial: value.SemanticFormationTarget) extends org.goldenport.cncf.component.Component {
    private var _selected = initial
    def select(target: value.SemanticFormationTarget): Unit = _selected = target
    private var _formation_calls = 0
    private var _result_reads = Vector.empty[String]
    private var _subjects = Vector.empty[String]
    private var _result: Option[Record] = None
    private var _outcome = OwnerOutcome.Success
    private var _before: org.goldenport.protocol.Request => Unit = _ => ()
    override def authenticationProviders: Vector[org.goldenport.cncf.security.AuthenticationProvider] = Vector(ControlledAuthentication)
    def formationCalls: Int = _formation_calls
    def resultReads: Vector[String] = _result_reads
    def ownerSubjects: Vector[String] = _subjects
    def retainedResult: Option[Record] = _result
    def outcome(selected: OwnerOutcome): Unit = _outcome = selected
    def beforeFormation(observe: org.goldenport.protocol.Request => Unit): Unit = _before = observe
    def restoreResult(result: Record): Unit = _result = Some(result)
    def receive(request: org.goldenport.protocol.Request, context: ExecutionContext): Consequence[org.goldenport.protocol.operation.OperationResponse] = {
      import org.goldenport.protocol.operation.OperationResponse
      val subject = org.goldenport.cncf.security.SecuritySubject.from(context.security)
      _subjects = _subjects :+ subject.subjectId
      if (!subject.isAuthenticated) Consequence.securityAuthenticationRequired("Controlled Hub requires authentication")
      else if (subject.subjectId != "controlled-semantic-editor") Consequence.securityPermissionDenied("Controlled Hub source/result is private")
      else request.operation match {
        case "proposeSemantics" if request.toRecord.getString("informationId").contains(_selected.informationId.value) => Consequence.success(OperationResponse(Record.data("informationId" -> _selected.informationId.value,
          "informationRevision" -> _selected.informationRevision, "outcome" -> "known",
          "context" -> Record.data("id" -> _selected.contextId, "revision" -> _selected.contextRevision,
            "purpose" -> "Controlled publication qualification", "provider" -> _selected.provider,
            "concepts" -> Vector("publication", "work", "person", "organization"),
            "predicates" -> Vector("isbn", "title", "author", "publisher"), "limitation" -> "Controlled owning contract"),
          "alternatives" -> Vector(_selected.toRecord()))))
        case "proposeSemantics" => Consequence.resourceNotFound("Controlled saved source is missing")
        case "validateSemanticTarget" =>
          request.toRecord.getRecord("target").fold[Consequence[value.SemanticFormationTarget]](
            Consequence.argumentInvalid("Controlled target is missing"))(value.SemanticFormationTarget.createC).flatMap { target =>
            if (target.informationId == _selected.informationId && target.informationRevision == _selected.informationRevision &&
                target.contextId == _selected.contextId && target.contextRevision == _selected.contextRevision && target.outcome == "known")
              Consequence.success(OperationResponse(target.toRecord()))
            else Consequence.stateConflict("Controlled Hub source/context/target is unavailable")
          }
        case "formKnowledge" =>
          // At the owner boundary, before any effect, independently observe the durable reservation.
          _before(request)
          _formation_calls += 1
          if (_outcome == OwnerOutcome.Unavailable) Consequence.serviceUnavailable("Controlled owning formation unavailable")
          else {
            val read = org.goldenport.protocol.Request.of(component = KnowledgeWorkbenchComponent.name,
              service = "SemanticWork", operation = "getSemanticWork", properties = List(
                org.goldenport.protocol.Property("id", request.toRecord.getString("workId").get, None)))
            subsystem.get.executeOperationResponseInChildContext(read, context).flatMap {
              case OperationResponse.RecordResponse(record) =>
                value.SemanticWorkResult.createC(record).map { current =>
                  val reservation = current.work.formations.last
                  val decision = reservation.decision
                  val target = reservation.target
                  val result = Record.data("id" -> reservation.id.value, "revision" -> 1L,
                    "informationId" -> target.informationId.value, "informationRevision" -> target.informationRevision,
                    "workId" -> current.work.id.value, "workRevision" -> current.revision,
                    "contextId" -> target.contextId, "contextRevision" -> target.contextRevision,
                    "nodeIds" -> Vector("controlled-canonical-node"), "decisionActor" -> decision.actor,
                    "decisionAt" -> decision.occurredAt, "decisionReason" -> decision.reason)
                  _result = Some(result)
                  if (_outcome == OwnerOutcome.LostSuccess) throw new LostOwnerAcknowledgement
                  OperationResponse(result)
                }
              case _ => Consequence.stateConflict("Owning work response is not a Record")
            }
          }
        case "getFormedKnowledge" =>
          val id = request.toRecord.getString("id").get
          _result_reads = _result_reads :+ id
          _result.filter(_.getString("id").contains(id)).fold[Consequence[OperationResponse]](
            Consequence.resourceNotFound("Controlled canonical result missing"))(result => Consequence.success(OperationResponse(result)))
        case _ => Consequence.notImplemented(request.operation)
      }
    }
  }
  private final case class ControlledHubOperation(opname: String, producer: ControlledHub) extends org.goldenport.protocol.spec.OperationDefinition {
    override val specification = org.goldenport.protocol.spec.OperationDefinition.Specification(name = opname,
      request = org.goldenport.protocol.spec.RequestDefinition(), response = org.goldenport.protocol.spec.ResponseDefinition.void)
    override def createOperationRequest(req: org.goldenport.protocol.Request): Consequence[org.goldenport.protocol.operation.OperationRequest] =
      Consequence.success(if (opname == "formKnowledge") ControlledHubCommand(req, producer) else ControlledHubQuery(req, producer))
  }
  private final case class ControlledHubQuery(request: org.goldenport.protocol.Request, producer: ControlledHub)
      extends org.goldenport.cncf.action.QueryAction {
    override def createCall(core: org.goldenport.cncf.action.ActionCall.Core): org.goldenport.cncf.action.ActionCall = ControlledHubCall(core, producer)
  }
  private final case class ControlledHubCommand(request: org.goldenport.protocol.Request, producer: ControlledHub)
      extends org.goldenport.cncf.action.CommandAction {
    override def createCall(core: org.goldenport.cncf.action.ActionCall.Core): org.goldenport.cncf.action.ActionCall = ControlledHubCall(core, producer)
  }
  private final case class ControlledHubCall(core: org.goldenport.cncf.action.ActionCall.Core, producer: ControlledHub)
      extends org.goldenport.cncf.action.ProcedureActionCall {
    override def execute(): Consequence[org.goldenport.protocol.operation.OperationResponse] = producer.receive(request, core.executionContext)
  }
}
