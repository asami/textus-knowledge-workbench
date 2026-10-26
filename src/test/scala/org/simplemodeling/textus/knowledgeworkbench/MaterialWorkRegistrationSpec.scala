package org.simplemodeling.textus.knowledgeworkbench

import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.cncf.action.{Action, ActionCall, CommandAction, QueryAction}
import org.goldenport.cncf.datastore.EntityVersionedMutationFailure
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.configuration.ConfigurationValue
import org.goldenport.http.{HttpPath, HttpRequest}
import org.goldenport.observation.{Descriptor, Taxonomy}
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * Controlled producer responses specify Workbench forwarding, not native TKL persistence.
 *
 * @since   Oct. 22, 2026
 * @version Oct. 22, 2026
 */
final class MaterialWorkRegistrationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  import MaterialWorkFixture.*
  import KnowledgeWorkbenchComponent.MaterialWorkService.*

  private val _utf16: Gen[String] = for {
    size <- Gen.choose(0, 64)
    units <- Gen.listOfN(size, Gen.choose(0, 65535))
  } yield units.map(_.toChar).mkString

  "Material registration work through the public native route" should {
    "generated factory and public compatibility" which {
      "select the three generated handler and action kinds alongside existing reads and Candidates" in {
        Given("an ordinary Workbench participant and a controlled public producer with an empty native Candidate collection")
        withSession() { session =>
          val before = session.candidates()
          val requests = _requests(session)

          When("public generated actions select their real adapter calls and all three work operations are interpreted")
          val calls = requests.map(request => session.call(request).TAKE)
          val actions = requests.map(request => session.component.logic.makeOperationRequest(request).TAKE)
          val responses = requests.map(request => session.executeRequest(request).TAKE)
          val after = session.candidates()

          Then("the declared command/query kinds, exact producer responses and prior public inventory remain available without MCP")
          calls(0) shouldBe a[CreateMaterialRegistrationActionCall]
          calls(1) shouldBe a[GetMaterialRegistrationActionCall]
          calls(2) shouldBe a[MaterializeMaterialActionCall]
          actions(0) shouldBe a[CommandAction]
          actions(1) shouldBe a[QueryAction]
          actions(2) shouldBe a[CommandAction]
          responses.zip(Vector(session.producer.prepared, session.producer.materializing, session.producer.registered)).foreach {
            case (actual, expected) => actual should be theSameInstanceAs expected
          }
          session.component.operationDefinitions.map(_.name) should contain allOf (
            "createMaterialRegistration", "getMaterialRegistration", "materializeMaterial",
            "searchMaterials", "inspectMaterial", "readOriginal",
            "registerCandidate", "listCandidates", "registerProposal", "findCandidateByProposal"
          )
          requests.foreach(request => session.component.isMcpReady("MaterialWork", request.operation) shouldBe false)
          session.producer.calls.map(_.request.operation) shouldBe requests.map(_.operation)
          _empty_candidates(before)
          _empty_candidates(after)
          _record(after) shouldBe _record(before)
        }
      }
    }

    "complete neutral JSON" which {
      "retain deterministic Unicode edges and opaque nested content while forwarding only contribution to the fixed owner" in {
        Given("a complete contribution containing NUL, a surrogate pair, isolated high/low surrogates and unknown nested source/history data")
        withSession() { session =>
          val contribution = _contribution("\u0000\uD83D\uDE00\uD800x\uDC00")
          val before = session.candidates()
          val request = session.request("createMaterialRegistration", Vector(
            "contribution" -> contribution, "isbn" -> "unrelated-book", "sourceEntityId" -> "unrelated-app-id",
            "owner" -> "forged-owner", "component" -> "org.example.Other", "service" -> "Other",
            "operation" -> "delete", "connectionId" -> "untrusted-connection", "access_token" -> "synthetic-token",
            "fileId" -> "untrusted-provider-id", "transport" -> "untrusted-transport"
          )).copy(source = Some(Request.Source.Http(HttpRequest(HttpPath(Vector("synthetic-ingress")), HttpRequest.GET,
            Record.data("Authorization" -> "synthetic-header"), Record.empty, Record.empty))))

          When("the normal generated public create action forwards the complete typed JSON")
          val result = session.executeRequest(request)
          val after = session.candidates()

          Then("one canonical request contains only the unchanged JSON, with no transport source or authority inferred from opaque content")
          result.TAKE should be theSameInstanceAs session.producer.prepared
          session.producer.calls should have size 1
          _forwarded(session.producer.calls.head.request, "createMaterialRegistration", "contribution", contribution)
          session.producer.calls.head.request.properties.head.value shouldBe a[Json]
          session.producer.calls.head.subject.subjectId shouldBe owner
          _empty_candidates(before)
          _empty_candidates(after)
          _record(after) shouldBe _record(before)
        }
      }

      "preserve at least one hundred undiscarded arbitrary bounded UTF16 contributions through generated public create" in {
        Given("a property preserving every generated bounded UTF16 contribution without filtering or discarding samples")
        val property = Prop.forAll(_utf16) { text =>
          Given("a complete neutral contribution embedding an arbitrary sequence of zero to sixty-four UTF16 code units")
          withSession() { session =>
            val contribution = _contribution(text)
            val before = session.candidates()

            When("one explicit generated create request carries the nested JSON as a typed value")
            val result = session.execute("createMaterialRegistration", Vector("contribution" -> contribution))
            val after = session.candidates()

            Then("the full JSON and response survive exactly with one producer call and an unchanged empty Candidate collection")
            result.TAKE should be theSameInstanceAs session.producer.prepared
            session.producer.calls should have size 1
            _forwarded(session.producer.calls.head.request, "createMaterialRegistration", "contribution", contribution)
            session.producer.calls.head.request.properties.head.value shouldBe a[Json]
            _empty_candidates(before)
            _empty_candidates(after)
            _record(after) shouldBe _record(before)
          }
          true
        }

        When("the property checks at least one hundred successful samples with one worker")
        val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100).withWorkers(1), property)

        Then("the property passes with at least one hundred successful samples and exactly zero discarded samples")
        checked.passed shouldBe true
        checked.succeeded should be >= 100
        checked.discarded shouldBe 0
      }
    }

    "opaque selection and logical results" which {
      "preserve exact selectors and every controlled saved result through a second domain-neutral consumer" in {
        Given("a neutral consumer, opaque Unicode/space/encoded-separator IDs and controlled prepared/materializing/registered responses")
        withSession() { session =>
          val consumer = new NeutralConsumer(session)
          val before = session.candidates()
          val identifiers = Vector(" opaque:登録/東京 %2F ", "URN:Registration:%2f/A", "\u0000\uD800x\uDC00")
          val outcomes = Vector(session.producer.prepared, session.producer.materializing, session.producer.registered)

          identifiers.foreach { id =>
            outcomes.foreach { expected =>
              Given("one producer-supplied logical snapshot without a fixture lifecycle or revision manager")
              session.producer.respondWith(Consequence.success(expected))
              val count = session.producer.calls.size

              When("the neutral consumer explicitly creates, reads status and requests materialization")
              val results = Vector(consumer.createMaterialRegistration(_contribution("neutral")),
                consumer.getMaterialRegistration(id), consumer.materializeMaterial(id))
              val after = session.candidates()

              Then("all four logical result fields and the exact response instance remain producer-owned while selectors stay opaque")
              results.foreach { result =>
                result.TAKE should be theSameInstanceAs expected
                _record(result) should be theSameInstanceAs _record(Consequence.success(expected))
                org.simplemodeling.textus.knowledgeworkbench.value.MaterialRegistrationResult.createC(_record(result)).TAKE shouldBe
                  org.simplemodeling.textus.knowledgeworkbench.value.MaterialRegistrationResult.createC(_record(Consequence.success(expected))).TAKE
              }
              val captures = session.producer.calls.drop(count)
              captures should have size 3
              _forwarded(captures(0).request, "createMaterialRegistration", "contribution", _contribution("neutral"))
              _forwarded(captures(1).request, "getMaterialRegistration", "registrationId", id)
              _forwarded(captures(2).request, "materializeMaterial", "registrationId", id)
              _empty_candidates(before)
              _empty_candidates(after)
              _record(after) shouldBe _record(before)
            }
          }
        }
      }
    }

    "native caller admission and context" which {
      "reject anonymous requests with the native authenticated-only vocabulary before reaching any producer" in {
        Given("a host with provider authentication and no anonymous fallback privilege")
        withSession() { session =>
          When("an anonymous caller invokes all three generated registration operations and the existing reference query")
          val results = _requests(session).map(request => session.executeRequest(request, sessionId = None))
          val reference = session.execute("searchMaterials", sessionId = None)

          Then("each registration failure uses the existing generated authentication-required admission and no producer is called")
          _conclusion(reference).observation.taxonomy shouldBe Taxonomy.securityAuthenticationRequired
          results.foreach { result =>
            _conclusion(result).observation.taxonomy shouldBe _conclusion(reference).observation.taxonomy
            _conclusion(result).interpretation shouldBe _conclusion(reference).interpretation
            _conclusion(result).disposition shouldBe _conclusion(reference).disposition
          }
          session.producer.calls shouldBe empty
        }
      }

      "preserve the actual other subject and inherited execution assumptions while the producer independently denies authorization" in {
        Given("another provider-authenticated caller and the producer's own structured permission failure")
        withSession() { session =>
          val denied = Consequence.securityPermissionDenied[OperationResponse]("Controlled producer policy denied this subject")
          session.producer.respondWith(denied)
          _requests(session).foreach { request =>
            When("that caller invokes a normal generated registration action")
            val result = session.executeRequest(request, sessionId = Some(otherSession))
            val caller = session.contexts.last
            val captured = session.producer.calls.last

            Then("the child sees the same subject, security, core, resolved configuration and observability without privilege substitution")
            _conclusion(result) should be theSameInstanceAs _conclusion(denied)
            captured.subject.subjectId shouldBe otherSubject
            captured.subject.isProviderAuthenticated shouldBe true
            captured.subject shouldBe SecuritySubject.current(using caller)
            captured.context.security should be theSameInstanceAs caller.security
            captured.context.core should be theSameInstanceAs caller.core
            Vector(
              MaterialWorkFixture.configurationKey -> ConfigurationValue.StringValue(session.root.resolve("data").toString),
              "textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.policy" ->
                ConfigurationValue.StringValue("local-default")
            ).foreach { case (key, expected) =>
              val callerconfiguration = caller.resolvedParameters.get(key)
              callerconfiguration.map(_.value) shouldBe Some(expected)
              captured.context.resolvedParameters.get(key) shouldBe callerconfiguration
            }
            captured.context.observability.traceId shouldBe caller.observability.traceId
            captured.context.observability.correlationId shouldBe caller.observability.correlationId
            captured.context.observability.sagaId shouldBe caller.observability.sagaId
            captured.request.source shouldBe None
          }
          session.producer.calls should have size 3
        }
      }
    }

    "structured failures without replay" which {
      "retain full producer Conclusion identity for each explicit request without retry, cleanup or Candidate mutation" in {
        Given("controlled permission, missing-resource, unavailable, state-conflict and native uncertain-acknowledgement failures")
        withSession() { session =>
          val failures: Vector[Consequence[OperationResponse]] = Vector(
            Consequence.securityPermissionDenied("Controlled producer permission denied"),
            Consequence.resourceNotFound("Controlled registration absent"),
            Consequence.serviceUnavailable("Controlled producer unavailable"),
            Consequence.operationConflict("materializeMaterial", Vector(Descriptor.Facet.Reason("registration-not-prepared"))),
            EntityVersionedMutationFailure.transactionIndeterminate("Controlled native acknowledgement unknown")
          )
          val before = session.candidates()
          failures.foreach { expected =>
            Given("one supplied failure whose exact Conclusion is authoritative for all retained metadata")
            session.producer.respondWith(expected)
            _requests(session).foreach { request =>
              val count = session.producer.calls.size

              When("one explicit work request receives that producer failure")
              val result = session.executeRequest(request)
              val after = session.candidates()

              Then("the same full Conclusion, including observation, details and previous chain, survives with exactly one requested producer call")
              _conclusion(result) should be theSameInstanceAs _conclusion(expected)
              result shouldBe expected
              session.producer.calls.drop(count).map(_.request.operation) shouldBe Vector(request.operation)
              _empty_candidates(before)
              _empty_candidates(after)
              _record(after) shouldBe _record(before)
            }
          }
        }
      }
    }

    "unavailable native routes and generated input" which {
      "return each native missing-target Conclusion without a substitute route" in {
        Given("a Workbench subsystem with no owning KnowledgeLake component installed")
        withSession(includeProducer = false) { session =>
          _requests(session).foreach { request =>
            When("the work operation and its corresponding fixed public producer request encounter the absent target")
            val result = session.executeRequest(request)
            val native = session.subsystem.executeOperationResponseInChildContext(
              Request.of(component = producerName, service = "Material", operation = request.operation,
                properties = request.properties), session.contexts.last)

            Then("native routing failure semantics and metadata remain visible without fallback")
            _conclusion(result).isMatch(_conclusion(native)) shouldBe true
            _conclusion(result).observation.taxonomy shouldBe _conclusion(native).observation.taxonomy
            _conclusion(result).interpretation shouldBe _conclusion(native).interpretation
            _conclusion(result).disposition shouldBe _conclusion(native).disposition
            _conclusion(result).previous shouldBe _conclusion(native).previous
            session.producer.calls shouldBe empty
          }
        }
      }

      "report the existing structured unavailable result when each real generated adapter call has no owning component" in {
        Given("real generated actions and adapter calls whose ActionCall Core has component None")
        withSession() { session =>
          _requests(session).foreach { request =>
            val context = session.context().TAKE
            val action = session.component.logic.makeOperationRequest(request).TAKE.asInstanceOf[Action]
            val core = ActionCall.Core(action, context, None, context.observability.correlationId)
            val factory = new impl.MaterialWorkServiceFactory()
            val call = action match {
              case input: MaterialRegistrationCreateCommand => factory.createCreateMaterialRegistrationActionCall(core, input)
              case input: MaterialRegistrationSelectionQuery => factory.createGetMaterialRegistrationActionCall(core, input)
              case input: MaterialRegistrationMaterializeCommand => factory.createMaterializeMaterialActionCall(core, input)
              case other => fail(s"Expected a generated registration action: $other")
            }

            When("the real FunctionalActionCall authorizes and interprets its generated input")
            val result = call.authorize()(using context).flatMap(_ => call.execute())

            Then("safe service unavailability preserves the existing message and classification without producer calls")
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

      "leave missing and malformed input failures with the original generated admission before producer dispatch" in {
        Given("missing required inputs for all three operations and malformed JSON for generated create")
        withSession() { session =>
          val requests = Vector(session.request("createMaterialRegistration"), session.request("getMaterialRegistration"),
            session.request("materializeMaterial"), session.request("createMaterialRegistration", Vector("contribution" -> "{broken-json")))
          requests.foreach { request =>
            When("the normal public path and the generated operation definition interpret the same invalid input")
            val result = session.executeRequest(request)
            val generated = request.operation match {
              case "createMaterialRegistration" => CreateMaterialRegistrationOperation.createOperationRequest(request)
              case "getMaterialRegistration" => GetMaterialRegistrationOperation.createOperationRequest(request)
              case "materializeMaterial" => MaterializeMaterialOperation.createOperationRequest(request)
            }

            Then("the generated native failure is preserved without application validation or producer effects")
            _conclusion(result).isMatch(_conclusion(generated)) shouldBe true
            _conclusion(result).observation.taxonomy shouldBe _conclusion(generated).observation.taxonomy
            _conclusion(result).disposition shouldBe _conclusion(generated).disposition
            session.producer.calls shouldBe empty
          }
        }
      }
    }
  }

  private def _requests(session: Session): Vector[Request] = Vector(
    session.request("createMaterialRegistration", Vector("contribution" -> _contribution("fixed"))),
    session.request("getMaterialRegistration", Vector("registrationId" -> " opaque:registration/東京 %2F ")),
    session.request("materializeMaterial", Vector("registrationId" -> " opaque:registration/東京 %2F "))
  )

  private def _contribution(text: String): Json = {
    val opaque = Json.obj("unknown" -> Json.fromString(text), "history" -> Json.arr(Json.fromString(text), Json.Null,
      Json.obj("human" -> Json.fromString(text))), "empty" -> Json.arr(), "nested" -> Json.arr(Json.arr(Json.fromString(text))))
    Json.obj("schema" -> Json.fromString("textus.material-contribution/1"), "title" -> Json.fromString("Stable neutral title"),
      "createdAt" -> Json.fromString("2026-10-22T00:00:00Z"), "classifications" -> Json.arr(Json.fromString("reference")),
      "provenance" -> Json.obj("unknownSource" -> opaque, "owner" -> Json.fromString("opaque-no-authority")),
      "originals" -> Json.arr(Json.obj("resourceId" -> Json.fromString("original-1"), "role" -> Json.fromString("original"),
        "mediaType" -> Json.fromString("image/png"), "byteLength" -> Json.fromLong(0L), "sourceReference" -> opaque)),
      "documents" -> Json.arr(
        Json.obj("resourceId" -> Json.fromString("capture-1"), "role" -> Json.fromString("capture"), "body" -> opaque,
          "derivedFrom" -> Json.arr(Json.fromString("original-1"))),
        Json.obj("resourceId" -> Json.fromString("null-1"), "role" -> Json.fromString("opaque-null"), "body" -> Json.Null,
          "derivedFrom" -> Json.arr())))
  }

  private def _forwarded(request: Request, operation: String, name: String, expected: Any): Unit = {
    request.component shouldBe Some(producerName)
    request.service shouldBe Some("Material")
    request.operation shouldBe operation
    request.properties shouldBe List(Property(name, expected, None))
    request.arguments shouldBe empty
    request.switches shouldBe empty
    request.source shouldBe None
  }

  private def _record(result: Consequence[OperationResponse]): Record = result.TAKE match {
    case OperationResponse.RecordResponse(record) => record
    case other => fail(s"Expected a logical RecordResponse: $other")
  }

  private def _empty_candidates(result: Consequence[OperationResponse]): Unit =
    org.simplemodeling.textus.knowledgeworkbench.value.ListCandidatesResult.createC(_record(result)).TAKE.items shouldBe empty

  private def _conclusion(result: Consequence[?]): org.goldenport.Conclusion = result match {
    case Consequence.Failure(conclusion) => conclusion
    case other => fail(s"Expected a native structured failure: $other")
  }
}
