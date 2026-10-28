package org.goldenport.cncf.http

import cats.effect.IO
import cats.data.NonEmptyVector
import org.goldenport.Consequence
import org.goldenport.bag.Bag
import org.goldenport.cncf.action.{ActionCall, ProcedureActionCall, QueryAction}
import org.goldenport.cncf.component.{Component, ComponentId, ComponentInit, ComponentInstanceId, ComponentOrigin}
import org.goldenport.datatype.{ContentType, MimeType}
import org.goldenport.http.{HttpResponse, HttpStatus}
import org.goldenport.protocol.{Protocol, Request}
import org.goldenport.protocol.operation.{OperationRequest, OperationResponse}
import org.goldenport.protocol.spec
import org.goldenport.record.Record
import cats.effect.unsafe.implicits.global
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.security.SecuritySubject
import org.http4s.HttpApp
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.syntax.all.*

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
/** Test-scope access to the existing native HTTP router; no product API added. */
object WorkbenchHttpFixture {
  val materialRef = " opaque:browser/資料 %2F "
  val resourceId = " original:画像/01 %2F "

  /** Controlled logical producer only; this is not evidence of a real cloud original. */
  final class MaterialProducer extends Component {
    var denied: Boolean = false
    var sourceVersion: Option[String] = Some("version-1")
    var sourceFailure: Option[Consequence[OperationResponse]] = None
    private var _evidence_requests = 0
    private var _subjects = Vector.empty[String]
    def evidenceRequests: Int = _evidence_requests
    def subjects: Vector[String] = _subjects
    private var _calls = Vector.empty[String]
    def calls: Vector[String] = _calls
    def respond(request: Request, context: ExecutionContext): Consequence[OperationResponse] = {
      _subjects = _subjects :+ SecuritySubject.from(context.security).subjectId
      val evidence = request.properties.exists(_.name == "evidenceResourceIds")
      if (evidence) _evidence_requests += 1
      _calls = _calls :+ request.operation
      if (denied) Consequence.securityPermissionDenied("Controlled source denied")
      else if (evidence && sourceFailure.isDefined) sourceFailure.get
      else Consequence.success(request.operation match {
        case "searchMaterials" => OperationResponse(Record.data("items" -> Vector(
          Record.data("materialRef" -> materialRef, "title" -> "Controlled public Material")), "hasMore" -> false))
        case "getMaterial" => OperationResponse(Record.data("materialRef" -> materialRef,
          "title" -> "Controlled public Material", "resources" -> Vector(Record.data(
            "resourceId" -> resourceId, "role" -> "original", "mediaType" -> "image/png")),
          "provenance" -> Vector.empty[Record]) ++ (if (evidence) Record.data("observedEvidence" -> Vector(
            Record.data("materialRef" -> materialRef, "resourceId" -> resourceId,
              "evidenceRef" -> "urn:textus:klake:evidence:controlled-browser",
              "retrievalState" -> (if (sourceVersion.isDefined) "available" else "unverified"),
              "limitation" -> "Controlled completed read; no historical retention promise") ++
              sourceVersion.map(version => Record.data("revision" -> version)).getOrElse(Record.empty))) else Record.empty))
        case "readOriginal" => OperationResponse.Http(HttpResponse.Binary(HttpStatus.Ok,
          ContentType(MimeType("image/png"), None), Bag.binary(java.util.Base64.getDecoder.decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a6XcAAAAASUVORK5CYII=")),
          Record.data("Cache-Control" -> "private, no-store", "X-Content-Type-Options" -> "nosniff",
            "Content-Security-Policy" -> "default-src 'none'; sandbox", "Content-Disposition" -> "attachment; filename=\"original.png\"")))
      })
    }
  }

  def installMaterial(subsystem: Subsystem): MaterialProducer = {
    val producer = new MaterialProducer
    val producerid = ComponentId("org.simplemodeling.textus.KnowledgeLake")
    val operations = Vector("searchMaterials", "getMaterial", "readOriginal").map { opname =>
      new spec.OperationDefinition {
        override val specification = spec.OperationDefinition.Specification(name = opname,
          request = spec.RequestDefinition(), response = spec.ResponseDefinition.void)
        override def createOperationRequest(req: Request): Consequence[OperationRequest] = Consequence.success(new QueryAction {
          override val request: Request = req
          override def createCall(callcore: ActionCall.Core): ActionCall = new ProcedureActionCall {
            override val core: ActionCall.Core = callcore
            override def execute(): Consequence[OperationResponse] = producer.respond(req, callcore.executionContext)
          }
        })
      }
    }
    val protocol = Protocol(services = spec.ServiceDefinitionGroup(Vector(spec.ServiceDefinition(name = "Material",
      operations = spec.OperationDefinitionGroup(NonEmptyVector.fromVectorUnsafe(operations))))))
    val core = Component.Core.create(name = producerid.name, componentId = producerid,
      instanceId = ComponentInstanceId.default(producerid), protocol = protocol)
    producer.initialize(ComponentInit(subsystem, core, ComponentOrigin.Builtin))
    subsystem.add(producer)
    producer
  }

  def app(subsystem: Subsystem): HttpApp[IO] = {
    val server = new Http4sHttpServer(new HttpExecutionEngine(subsystem))
    WebSocketBuilder2[IO].map(builder => server.routes(builder).orNotFound).unsafeRunSync()
  }
}
