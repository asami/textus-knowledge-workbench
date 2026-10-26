package org.simplemodeling.textus.knowledgeworkbench

import cats.data.NonEmptyVector
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}
import org.goldenport.Consequence
import org.goldenport.bag.Bag
import org.goldenport.cncf.action.{Action, ActionCall, CommandAction, ProcedureActionCall, QueryAction}
import org.goldenport.cncf.bootstrap.{BootstrapConfig, CncfBootstrap}
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentDescriptor, ComponentId, ComponentInit, ComponentInstanceId, ComponentOrigin}
import org.goldenport.cncf.context.{CorrelationId, ExecutionContext, GlobalContext, GlobalRuntimeContext, ObservabilityContext, PrincipalId, SpanId, TraceId}
import org.goldenport.cncf.log.LogBackendHolder
import org.goldenport.cncf.security.{AuthenticationProvider, AuthenticationRequest, AuthenticationResult, IngressSecurityResolver, SecuritySubject}
import org.goldenport.cncf.subsystem.{GenericSubsystemAuthenticationBinding, GenericSubsystemAuthenticationProviderBinding, GenericSubsystemComponentBinding, GenericSubsystemDescriptor, GenericSubsystemSecurityBinding, Subsystem}
import org.goldenport.datatype.{ContentType, MimeType}
import org.goldenport.http.{HttpResponse, HttpStatus}
import org.goldenport.protocol.{Property, Protocol, Request}
import org.goldenport.protocol.operation.{OperationRequest, OperationResponse}
import org.goldenport.protocol.spec
import org.goldenport.record.Record
import org.simplemodeling.textus.knowledgeworkbench.impl.KnowledgeWorkbenchParticipantFactoryBase

/*
 * Synthetic producer evidence only; actual TKL acceptance belongs to integration.
 *
 * @since   Oct. 21, 2026
 * @version Oct. 22, 2026
 */
private[knowledgeworkbench] object MaterialWorkFixture {
  val producerName = "org.simplemodeling.textus.KnowledgeLake"
  val owner = "material-work-owner"
  val otherSubject = "material-work-other"
  val ownerSession = "material-work-owner-session"
  val otherSession = "material-work-other-session"
  val materialRef = " opaque:Material/東京 %2F "
  val resourceId = " original:頁/01 %2F "
  val bytes = Vector[Byte](0, 1, -1, 42)
  val configurationKey = "textus.local-data.root"

  final case class Capture(request: Request, subject: SecuritySubject, context: ExecutionContext)

  final class Producer extends Component {
    private var _calls = Vector.empty[Capture]
    private var _response: Option[Consequence[OperationResponse]] = None
    val metadata: OperationResponse = OperationResponse(Record.data(
      "materialRef" -> materialRef,
      "title" -> "Synthetic evidence",
      "resources" -> Vector(Record.data("resourceId" -> resourceId, "role" -> "original")),
      "provenance" -> Vector(Record.data("kind" -> "citation", "reference" -> "opaque:citation"))
    ))
    val search: OperationResponse = OperationResponse(Record.data(
      "items" -> Vector(Record.data("materialRef" -> materialRef, "title" -> "Synthetic evidence")),
      "hasMore" -> false
    ))
    // Independent controlled responses, not a simulated registration lifecycle.
    val prepared: OperationResponse = OperationResponse(value.MaterialRegistrationResult(
      "synthetic-registration-1", 1L, "prepared", materialRef).toRecord())
    val materializing: OperationResponse = OperationResponse(value.MaterialRegistrationResult(
      "synthetic-registration-1", 2L, "materializing", materialRef).toRecord())
    val registered: OperationResponse = OperationResponse(value.MaterialRegistrationResult(
      "synthetic-registration-1", 3L, "registered", materialRef).toRecord())
    val original: OperationResponse = OperationResponse.Http(HttpResponse.Binary(
      HttpStatus.Ok,
      ContentType(MimeType("image/png"), None),
      Bag.binary(bytes.toArray),
      Record.data(
        "Cache-Control" -> "private, no-store",
        "X-Content-Type-Options" -> "nosniff",
        "Content-Security-Policy" -> "default-src 'none'; sandbox",
        "Content-Disposition" -> "attachment; filename=\"original\""
      )
    ))

    def calls: Vector[Capture] = _calls
    def respondWith(response: Consequence[OperationResponse]): Unit = _response = Some(response)

    def receive(request: Request, context: ExecutionContext): Consequence[OperationResponse] = {
      _calls = _calls :+ Capture(request, SecuritySubject.current(using context), context)
      _response.getOrElse(Consequence.success(request.operation match {
        case "searchMaterials" => search
        case "getMaterial" => metadata
        case "readOriginal" => original
        case "createMaterialRegistration" => prepared
        case "getMaterialRegistration" => materializing
        case "materializeMaterial" => registered
      }))
    }
  }

  private final case class ProducerOperation(opname: String, producer: Producer) extends spec.OperationDefinition {
    override val specification: spec.OperationDefinition.Specification = spec.OperationDefinition.Specification(
      name = opname,
      request = spec.RequestDefinition(),
      response = spec.ResponseDefinition.void
    )
    override def createOperationRequest(req: Request): Consequence[OperationRequest] =
      Consequence.success(opname match {
        case "createMaterialRegistration" | "materializeMaterial" => ProducerCommand(req, producer)
        case _ => ProducerQuery(req, producer)
      })
  }

  private final case class ProducerQuery(request: Request, producer: Producer) extends QueryAction {
    override def createCall(core: ActionCall.Core): ActionCall = ProducerCall(core, producer)
  }

  private final case class ProducerCommand(request: Request, producer: Producer) extends CommandAction {
    override def createCall(core: ActionCall.Core): ActionCall = ProducerCall(core, producer)
  }

  private final case class ProducerCall(core: ActionCall.Core, producer: Producer) extends ProcedureActionCall {
    override def execute(): Consequence[OperationResponse] = producer.receive(request, core.executionContext)
  }

  private object FixtureProvider extends AuthenticationProvider {
    def name: String = "material-work-fixture-provider"
    def authenticate(request: AuthenticationRequest)(using ExecutionContext): Consequence[Option[AuthenticationResult]] =
      Consequence.success(request.sessionId.flatMap {
        case `ownerSession` => Some(AuthenticationResult(PrincipalId(owner)))
        case `otherSession` => Some(AuthenticationResult(PrincipalId(otherSubject)))
        case _ => None
      })
  }

  private final class FixtureComponent extends KnowledgeWorkbenchComponent {
    override def authenticationProviders: Vector[AuthenticationProvider] = Vector(FixtureProvider)
    override def mcpReadyServices: Set[String] = Set.empty
  }

  private object FixtureFactory extends KnowledgeWorkbenchParticipantFactoryBase with Component.PrimaryComponentFactory {
    override protected def create_Component(params: ComponentCreate): Component = new FixtureComponent
    override protected def create_Core(params: ComponentCreate, comp: Component): Component.Core =
      component_core(KnowledgeWorkbenchComponent.name, KnowledgeWorkbenchComponent.componentId)
  }

  final class Session(val component: KnowledgeWorkbenchComponent, val producer: Producer, val subsystem: Subsystem, val root: Path) {
    private var _contexts = Vector.empty[ExecutionContext]
    def contexts: Vector[ExecutionContext] = _contexts

    def request(operation: String, fields: Vector[(String, Any)] = Vector.empty, service: String = "MaterialWork"): Request =
      Request.of(component = component.name, service = service, operation = operation,
        properties = fields.map { case (name, value) => Property(name, value, None) }.toList)

    def context(sessionId: Option[String] = Some(ownerSession)): Consequence[ExecutionContext] = {
      val base = ExecutionContext.create().withScope(component.scopeContext)
      val attributes = sessionId.map(value => Map("x-textus-session" -> value)).getOrElse(Map.empty[String, String])
      IngressSecurityResolver.resolve(base, attributes).map { admitted =>
        ExecutionContext.withObservabilityContext(admitted.executionContext, ObservabilityContext(
          traceId = TraceId("material_work", "consumer"),
          spanId = Some(SpanId("material_work", "consumer", "query")),
          correlationId = Some(CorrelationId("material_work", "consumer"))
        ))
      }
    }

    def call(request: Request, sessionId: Option[String] = Some(ownerSession)): Consequence[ActionCall] =
      context(sessionId).flatMap { admitted =>
        component.logic.makeOperationRequest(request).flatMap {
          case action: Action =>
            val call = component.logic.createActionCall(action, admitted)
            _contexts = _contexts :+ call.executionContext
            Consequence.success(call)
          case _ => Consequence.argumentInvalid("Expected a generated Workbench action")
        }
      }

    def execute(operation: String, fields: Vector[(String, Any)] = Vector.empty,
        sessionId: Option[String] = Some(ownerSession), service: String = "MaterialWork"): Consequence[OperationResponse] =
      executeRequest(request(operation, fields, service), sessionId)

    def executeRequest(request: Request, sessionId: Option[String] = Some(ownerSession)): Consequence[OperationResponse] =
      context(sessionId).flatMap { admitted =>
        _contexts = _contexts :+ admitted
        subsystem.executeOperationResponseInChildContext(request, admitted)
      }

    def candidates(): Consequence[OperationResponse] = execute("listCandidates", service = "Candidate")
  }

  /** An independent application binding using only logical Material work inputs. */
  final class NeutralConsumer(session: Session) {
    def searchMaterials(): Consequence[OperationResponse] = session.execute("searchMaterials")
    def inspectMaterial(materialRef: String): Consequence[OperationResponse] =
      session.execute("inspectMaterial", Vector("materialRef" -> materialRef))
    def readOriginal(materialRef: String, resourceId: String): Consequence[OperationResponse] =
      session.execute("readOriginal", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))
    def createMaterialRegistration(contribution: io.circe.Json): Consequence[OperationResponse] =
      session.execute("createMaterialRegistration", Vector("contribution" -> contribution))
    def getMaterialRegistration(registrationId: String): Consequence[OperationResponse] =
      session.execute("getMaterialRegistration", Vector("registrationId" -> registrationId))
    def materializeMaterial(registrationId: String): Consequence[OperationResponse] =
      session.execute("materializeMaterial", Vector("registrationId" -> registrationId))
  }

  // This fixture serializes its owned process-global bootstrap lifecycle.
  def withSession[A](includeProducer: Boolean = true)(body: Session => A): A = synchronized {
    val previousruntime = GlobalRuntimeContext.current
    val previousglobal = Try(GlobalContext.globalContext).toOption
    val previouslog = LogBackendHolder.backend
    val workroot = Files.createDirectories(Path.of("target/material-work/work").toAbsolutePath.normalize())
    val root = Files.createTempDirectory(workroot, "session-")
    try {
      val config = io.circe.Json.obj(
        configurationKey -> io.circe.Json.fromString(root.resolve("data").toString),
        "textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.policy" -> io.circe.Json.fromString("local-default")
      )
      Files.writeString(Files.createDirectories(root.resolve(".textus")).resolve("config.json"), config.noSpaces, StandardCharsets.UTF_8)
      val testdescriptor = root.resolve("test.yaml")
      Files.writeString(testdescriptor, """kind: test-descriptor
        |execution:
        |  profile: controlled
        |  key: material-work
        |  time:
        |    mode: manual
        |    start-at: '2026-10-21T00:00:00Z'
        |  random:
        |    mode: seeded
        |    seed: material-work
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
        val component = FixtureFactory.createPrimaryC(ComponentCreate(handle.subsystem, ComponentOrigin.Builtin, Vector(descriptor)))
          .TAKE.asInstanceOf[KnowledgeWorkbenchComponent]
        handle.subsystem.add(component)
        val producer = new Producer
        if (includeProducer) {
          val producerid = ComponentId(producerName)
          val operations = Vector("searchMaterials", "getMaterial", "readOriginal",
            "createMaterialRegistration", "getMaterialRegistration", "materializeMaterial").map(ProducerOperation(_, producer))
          val protocol = Protocol(services = spec.ServiceDefinitionGroup(Vector(spec.ServiceDefinition(
            name = "Material", operations = spec.OperationDefinitionGroup(NonEmptyVector.fromVectorUnsafe(operations))
          ))))
          val core = Component.Core.create(name = producerid.name, componentId = producerid,
            instanceId = ComponentInstanceId.default(producerid), protocol = protocol)
          producer.initialize(ComponentInit(handle.subsystem, core, ComponentOrigin.Builtin))
          handle.subsystem.add(producer)
        }
        handle.subsystem.withDescriptor(GenericSubsystemDescriptor(
          path = root.resolve("subsystem.yaml"), subsystemName = handle.subsystem.name,
          componentBindings = Vector(GenericSubsystemComponentBinding(component.name)) ++
            (if (includeProducer) Vector(GenericSubsystemComponentBinding(producerName)) else Vector.empty),
          security = Some(GenericSubsystemSecurityBinding(authentication = Some(GenericSubsystemAuthenticationBinding(
            convention = Some("enabled"), fallbackPrivilege = Some("disabled"), providers = Vector(
              GenericSubsystemAuthenticationProviderBinding(name = FixtureProvider.name, component = component.name, enabled = Some(true))
            )
          ))))
        ))
        body(new Session(component, producer, handle.subsystem, root))
      } finally handle.close()
    } finally {
      GlobalRuntimeContext.current = previousruntime
      // Native bootstrap initializes this process global; no public absent-state reset exists.
      previousglobal.foreach(GlobalContext.set)
      previouslog.foreach(LogBackendHolder.install)
      Using.resource(Files.walk(root)) { stream =>
        stream.iterator().asScala.toVector.sortBy(_.getNameCount).reverse.foreach(Files.deleteIfExists(_))
      }
    }
  }
}
