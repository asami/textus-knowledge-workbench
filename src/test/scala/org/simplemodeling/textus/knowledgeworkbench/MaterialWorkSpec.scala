package org.simplemodeling.textus.knowledgeworkbench

import scala.util.Using
import org.goldenport.Consequence
import org.goldenport.bag.BinaryBag
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.configuration.ConfigurationValue
import org.goldenport.http.{HttpPath, HttpRequest, HttpResponse}
import org.goldenport.observation.Taxonomy
import org.goldenport.protocol.Request
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 21, 2026
 * @version Oct. 21, 2026
 */
final class MaterialWorkSpec extends AnyWordSpec with Matchers with GivenWhenThen with TableDrivenPropertyChecks {
  import MaterialWorkFixture.*

  "Material work through the public native route" should {
    "factory registration and Candidate compatibility" which {
      "expose all three implemented query handlers alongside the existing services without MCP publication" in {
        Given("an actual Workbench participant factory in a test-owned native subsystem with a synthetic public Material producer")
        withSession() { session =>
          val requests = Vector(
            session.request("searchMaterials"),
            session.request("inspectMaterial", Vector("materialRef" -> materialRef)),
            session.request("readOriginal", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))
          )
          When("the generated actions select their handlers and the public subsystem authorizes and executes all three queries")
          val calls = requests.map(request => session.call(request).TAKE)
          val results = requests.map(request => session.executeRequest(request))
          val candidates = session.candidates()

          Then("each generated handler reaches the producer and the Candidate and native services remain available without MCP")
          calls(0) shouldBe a[KnowledgeWorkbenchComponent.MaterialWorkService.SearchMaterialsActionCall]
          calls(1) shouldBe a[KnowledgeWorkbenchComponent.MaterialWorkService.InspectMaterialActionCall]
          calls(2) shouldBe a[KnowledgeWorkbenchComponent.MaterialWorkService.ReadOriginalActionCall]
          results.map(_.TAKE) shouldBe Vector(session.producer.search, session.producer.metadata, session.producer.original)
          session.component.protocol.services.services.map(_.name) should contain allOf (
            "Candidate", "MaterialWork", "aggregate", "view", "entity"
          )
          session.component.operationDefinitions.map(_.name) should contain allOf (
            "registerCandidate", "listCandidates", "registerProposal", "findCandidateByProposal"
          )
          candidates shouldBe a[Consequence.Success[?]]
          org.simplemodeling.textus.knowledgeworkbench.value.ListCandidatesResult.createC(_record(candidates)).TAKE.items shouldBe empty
          Vector("searchMaterials", "inspectMaterial", "readOriginal").foreach { operation =>
            session.component.isMcpReady("MaterialWork", operation) shouldBe false
          }
          Vector("registerCandidate", "listCandidates", "registerProposal", "findCandidateByProposal").foreach { operation =>
            session.component.isMcpReady("Candidate", operation) shouldBe false
          }
        }
      }
    }

    "fixed logical selectors" which {
      "send only the exact present search fields to the canonical producer and discard transport and unrelated input" in {
        Given("a generated search request with exact logical options plus unrelated business, identity and transport material")
        withSession() { session =>
          val request = session.request("searchMaterials", Vector(
            "text" -> " 東京 %2F ", "offset" -> 7, "limit" -> 13,
            "component" -> "org.example.Other", "service" -> "Other", "operation" -> "write",
            "unknownBusinessField" -> "unused", "principalId" -> "forged-principal",
            "x-textus-session" -> "untrusted-session", "access_token" -> "synthetic-token",
            "fileId" -> "synthetic-provider-id", "parentId" -> "synthetic-provider-parent"
          )).copy(source = Some(Request.Source.Http(HttpRequest(
            HttpPath(Vector("synthetic-ingress")), HttpRequest.GET,
            Record.data("Authorization" -> "synthetic-header"),
            Record.data("Authorization" -> "synthetic-header"),
            Record.data("Authorization" -> "synthetic-header")
          ))))

          When("the normal generated Workbench action dispatches the search")
          val result = session.executeRequest(request)

          Then("one canonical Material search receives precisely the logical values and the producer response remains identical")
          result.TAKE should be theSameInstanceAs session.producer.search
          session.producer.calls should have size 1
          val captured = session.producer.calls.head
          _selector(captured.request) shouldBe ((Some(producerName), Some("Material"), "searchMaterials"))
          captured.request.properties.map(property => property.name -> property.value).toMap shouldBe
            Map("text" -> " 東京 %2F ", "offset" -> 7, "limit" -> 13)
          captured.request.arguments shouldBe empty
          captured.request.switches shouldBe empty
          captured.request.source shouldBe None
          captured.subject.subjectId shouldBe owner
        }
      }

      "omit absent search options rather than introducing nulls or consumer defaults" in {
        Given("the same native Workbench binding and the deterministic combinations of absent logical options")
        withSession() { session =>
          val options: Vector[Vector[(String, Any)]] = Vector(
            Vector.empty, Vector("text" -> ""), Vector("offset" -> 0), Vector("limit" -> 1)
          )

          When("each generated search is executed with only its supplied options")
          val results = options.map(fields => session.execute("searchMaterials", fields))

          Then("each producer request has exactly those keys and values without null placeholders or injected defaults")
          results.foreach(_.TAKE should be theSameInstanceAs session.producer.search)
          session.producer.calls.map(_.request.properties.map(property => property.name -> property.value).toMap) shouldBe options.map(_.toMap)
          session.producer.calls.map(capture => _selector(capture.request)).distinct shouldBe
            Vector((Some(producerName), Some("Material"), "searchMaterials"))
        }
      }

      "preserve opaque Material and resource references across repeated inspect and original queries" in {
        Given("opaque logical selections with whitespace, case, punctuation, encoded separators and non-ASCII text")
        withSession() { session =>
          val selections = Table(
            ("materialRef", "resourceId"),
            materialRef -> resourceId,
            "URN:opaque:%2F/A" -> "resource:?a=b#c",
            "urn:opaque:%2f/a" -> " resource:001 ",
            "資料:一/二" -> "原本:三"
          )

          forAll(selections) { (reference, resource) =>
            Given(s"the exact opaque logical selection $reference and $resource from the deterministic table")
            val selection = Vector[(String, Any)]("materialRef" -> reference)

            When("the selection is inspected and read using only the two public logical fields")
            val results = Vector(session.execute("inspectMaterial", selection),
              session.execute("readOriginal", selection :+ ("resourceId" -> resource)))

            Then("the fixed getMaterial/readOriginal selectors retain every input exactly without domain or provider prerequisites")
            results.foreach(_ shouldBe a[Consequence.Success[?]])
            val captures = session.producer.calls.takeRight(2)
            captures.map(_.request.operation) shouldBe Vector("getMaterial", "readOriginal")
            captures.map(_.request.properties.map(property => property.name -> property.value).toMap) shouldBe Vector(
              Map("materialRef" -> reference), Map("materialRef" -> reference, "resourceId" -> resource)
            )
            captures.foreach { capture =>
              capture.request.component shouldBe Some(producerName)
              capture.request.service shouldBe Some("Material")
            }
          }
        }
      }
    }

    "unchanged public producer outcomes" which {
      "return the exact logical metadata RecordResponse without projecting or rebuilding its fields" in {
        Given("a synthetic public producer record with resources and attributed provenance")
        withSession() { session =>
          val expected = session.producer.metadata

          When("the normal generated inspect action dispatches getMaterial")
          val result = session.execute("inspectMaterial", Vector("materialRef" -> materialRef))

          Then("the original response and its record are retained as the producer supplied them")
          result.TAKE should be theSameInstanceAs expected
          _record(result) should be theSameInstanceAs _record(Consequence.success(expected))
          session.producer.calls should have size 1
        }
      }

      "return the exact binary response, bytes, MIME type and private safety headers" in {
        Given("a synthetic original with deterministic bytes and the producer's private/no-store, nosniff, sandbox and attachment headers")
        withSession() { session =>
          val expected = session.producer.original

          When("the generated original query dispatches one native producer read and its returned Bag is read")
          val result = session.execute("readOriginal", Vector("materialRef" -> materialRef, "resourceId" -> resourceId))
          val response = _http(result.TAKE)
          val returned = Using.resource(response.bag.asInstanceOf[BinaryBag].openInputStream())(_.readAllBytes().toVector)

          Then("the exact response object and Bag retain all bytes and all producer safety headers")
          result.TAKE should be theSameInstanceAs expected
          response shouldBe a[HttpResponse.Binary]
          response.bag should be theSameInstanceAs _http(expected).bag
          returned shouldBe bytes
          response.mime.value shouldBe "image/png"
          response.headerValue("Cache-Control") shouldBe Some("private, no-store")
          response.headerValue("X-Content-Type-Options") shouldBe Some("nosniff")
          response.headerValue("Content-Security-Policy") shouldBe Some("default-src 'none'; sandbox")
          response.headerValue("Content-Disposition") shouldBe Some("attachment; filename=\"original\"")
          session.producer.calls should have size 1
        }
      }

      "pass permission, not-found and unavailable failures through every work query without another producer call" in {
        Given("native structured producer failures for permission denial, missing logical resources and unavailability")
        withSession() { session =>
          val failures: Vector[Consequence[OperationResponse]] = Vector(
            Consequence.securityPermissionDenied("Synthetic producer policy denied the caller"),
            Consequence.resourceNotFound("Synthetic logical resource is absent"),
            Consequence.serviceUnavailable("Synthetic producer dependency is unavailable")
          )
          val queries = Vector(
            "searchMaterials" -> Vector.empty[(String, Any)],
            "inspectMaterial" -> Vector[(String, Any)]("materialRef" -> materialRef),
            "readOriginal" -> Vector[(String, Any)]("materialRef" -> materialRef, "resourceId" -> resourceId)
          )

          When("each generated work query receives each declared producer failure")
          val results = failures.flatMap { failure =>
            session.producer.respondWith(failure)
            queries.map { case (operation, fields) => session.execute(operation, fields) -> failure }
          }

          Then("each original Conclusion survives unchanged and exactly one producer call occurs per query")
          results.foreach { case (result, expected) =>
            result shouldBe expected
            _conclusion(result) should be theSameInstanceAs _conclusion(expected)
          }
          session.producer.calls should have size (failures.size * queries.size)
        }
      }
    }

    "native caller admission and context" which {
      "reject an anonymous caller before any producer operation is invoked" in {
        Given("the host's resolved provider wiring with no session or fallback privilege")
        withSession() { session =>
          When("an anonymous consumer invokes each generated MaterialWork action")
          val results = Vector(
            session.execute("searchMaterials", sessionId = None),
            session.execute("inspectMaterial", Vector("materialRef" -> materialRef), sessionId = None),
            session.execute("readOriginal", Vector("materialRef" -> materialRef, "resourceId" -> resourceId), sessionId = None)
          )

          Then("authenticated-only action admission fails and the producer receives no calls")
          results.foreach(result => _conclusion(result).observation.taxonomy shouldBe Taxonomy.securityAuthenticationRequired)
          session.producer.calls shouldBe empty
        }
      }

      "inherit the admitted other subject, security, runtime configuration and observability while leaving authorization to the producer" in {
        Given("a different provider-authenticated subject and a producer that returns its own structured permission denial")
        withSession() { session =>
          val denied = Consequence.securityPermissionDenied[OperationResponse]("Synthetic Material policy denied this other subject")
          session.producer.respondWith(denied)

          When("that subject invokes a normal generated original query")
          val result = session.execute("readOriginal", Vector("materialRef" -> materialRef, "resourceId" -> resourceId),
            sessionId = Some(otherSession))
          val caller = session.contexts.last
          val captured = session.producer.calls.head
          val callerconfiguration = caller.resolvedParameters.get(configurationKey)
          val childconfiguration = captured.context.resolvedParameters.get(configurationKey)

          Then("producer admission observes the same caller and inherited execution assumptions without a substituted identity")
          result shouldBe denied
          session.producer.calls should have size 1
          captured.subject.subjectId shouldBe otherSubject
          captured.subject.isProviderAuthenticated shouldBe true
          captured.subject shouldBe SecuritySubject.current(using caller)
          captured.context.security should be theSameInstanceAs caller.security
          callerconfiguration.map(_.value) shouldBe Some(ConfigurationValue.StringValue(session.root.resolve("data").toString))
          childconfiguration shouldBe callerconfiguration
          captured.context.core should be theSameInstanceAs caller.core
          captured.context.observability.traceId shouldBe caller.observability.traceId
          captured.context.observability.correlationId shouldBe caller.observability.correlationId
          captured.context.observability.sagaId shouldBe caller.observability.sagaId
          captured.request.properties.map(_.name).toSet shouldBe Set("materialRef", "resourceId")
          captured.request.source shouldBe None
        }
      }
    }

    "unavailable native routes" which {
      "retain the native missing-target failure without a fallback or substituted producer" in {
        Given("a native Workbench subsystem with no KnowledgeLake component installed")
        withSession(includeProducer = false) { session =>
          When("a generated search dispatches its fixed target and the same public target is queried independently")
          val result = session.execute("searchMaterials")
          val native = session.subsystem.executeOperationResponseInChildContext(
            Request.of(component = producerName, service = "Material", operation = "searchMaterials"), session.contexts.last)

          Then("the work query returns the native route Conclusion and performs no producer or fallback calls")
          _conclusion(result).isMatch(_conclusion(native)) shouldBe true
          result shouldBe a[Consequence.Failure[?]]
          session.producer.calls shouldBe empty
        }
      }

      "report structured service unavailability when a generated handler has no owning subsystem" in {
        Given("a public generated search action and the real adapter factory with no owning component in its call core")
        withSession() { session =>
          val action = KnowledgeWorkbenchComponent.MaterialWorkService.SearchMaterialsOperation.createOperationRequest(session.request("searchMaterials")).TAKE
          val context = session.context().TAKE
          val core = ActionCall.Core(action, context, None, context.observability.correlationId)
          val call = new impl.MaterialWorkServiceFactory().createSearchMaterialsActionCall(core, action)

          When("the real FunctionalActionCall interprets the generated search input")
          val result = call.authorize()(using context).flatMap(_ => call.execute())

          Then("the adapter returns a safe structured unavailable failure without invoking a producer")
          val conclusion = _conclusion(result)
          conclusion.observation.taxonomy shouldBe Taxonomy.serviceUnavailable
          conclusion.displayMessage shouldBe "Material work owning subsystem is unavailable"
          conclusion.interpretation shouldBe org.goldenport.conclusion.Interpretation.systemFailure
          conclusion.disposition shouldBe org.goldenport.conclusion.Disposition.serviceUnavailable
          conclusion.previous shouldBe None
          session.producer.calls shouldBe empty
        }
      }
    }

    "domain-neutral application binding" which {
      "use the same three public selectors without creating Candidates or requiring another domain's identity" in {
        Given("a second application binding that knows only Workbench's logical Material queries and an empty native Candidate collection")
        withSession() { session =>
          val consumer = new NeutralConsumer(session)
          val before = session.candidates()

          When("the independent consumer searches, inspects and reads an original and the Candidate collection is read again")
          val results = Vector(consumer.searchMaterials(), consumer.inspectMaterial(materialRef), consumer.readOriginal(materialRef, resourceId))
          val after = session.candidates()

          Then("the same canonical producer selectors respond and no Candidate is introduced by the Material workflow")
          results.map(_.TAKE) shouldBe Vector(session.producer.search, session.producer.metadata, session.producer.original)
          session.producer.calls.map(capture => _selector(capture.request)) shouldBe Vector(
            (Some(producerName), Some("Material"), "searchMaterials"),
            (Some(producerName), Some("Material"), "getMaterial"),
            (Some(producerName), Some("Material"), "readOriginal")
          )
          session.producer.calls.map(_.request.properties.map(_.name).toSet) shouldBe
            Vector(Set.empty[String], Set("materialRef"), Set("materialRef", "resourceId"))
          org.simplemodeling.textus.knowledgeworkbench.value.ListCandidatesResult.createC(_record(before)).TAKE.items shouldBe empty
          org.simplemodeling.textus.knowledgeworkbench.value.ListCandidatesResult.createC(_record(after)).TAKE.items shouldBe empty
          _record(after) shouldBe _record(before)
        }
      }
    }
  }

  private def _selector(request: Request): (Option[String], Option[String], String) =
    (request.component, request.service, request.operation)

  private def _record(result: Consequence[OperationResponse]): Record = result.TAKE match {
    case OperationResponse.RecordResponse(record) => record
    case other => fail(s"Expected a logical RecordResponse: $other")
  }

  private def _http(response: OperationResponse): HttpResponse = response match {
    case OperationResponse.Http(value) => value
    case other => fail(s"Expected an original Http response: $other")
  }

  private def _conclusion(result: Consequence[OperationResponse]): org.goldenport.Conclusion = result match {
    case Consequence.Failure(conclusion) => conclusion
    case other => fail(s"Expected a structured producer or admission failure: $other")
  }
}
