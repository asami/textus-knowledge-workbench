package org.simplemodeling.textus.knowledgeworkbench

import java.nio.file.{Files, Path}
import java.sql.DriverManager
import domain.ProposalPreparationState
import io.circe.Json
import io.circe.parser.parse
import org.goldenport.Consequence
import org.goldenport.cncf.bootstrap.{BootstrapConfig, CncfBootstrap, CncfHandle}
import org.goldenport.cncf.cli.RunMode
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.convert.NumericCodeCodec
import org.goldenport.observation.Taxonomy
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.knowledgeworkbench.{value as workbenchvalue}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 14, 2026
 * @version Oct. 27, 2026
 */
final class CandidateProposalSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _proposals = afterWord(
    "in spec:workbench-candidate-proposals; rules:PROP-01..09; phase:1; slice:S2-A"
  )

  "Candidate proposal operations" should _proposals {
    "generated preparation-state persistence correspondence" which {
      "roundtrip both typed values through the common code and numeric JSON codecs while retaining symbolic records" in {
        Given("the actual generated prepared and unprepared values with their declared common numeric codec")
        val codec: NumericCodeCodec[ProposalPreparationState] = ProposalPreparationState.numericCodeCodec
        val jsoncodec = codec.jsonCodec
        val entries = Vector(ProposalPreparationState.prepared -> 1, ProposalPreparationState.unprepared -> 2)

        When("both values are encoded and both fixed codes are decoded through the typed and JSON boundaries")
        val encoded = entries.map { case (value, _) => codec.encodeC(value).toOption }
        val decoded = entries.map { case (_, code) => codec.decodeC(code).toOption }
        val encodedjson = entries.map { case (value, _) => jsoncodec(value) }
        val decodedjson = entries.map { case (_, code) => jsoncodec.decodeJson(Json.fromInt(code)) }
        val symbolic = entries.map { case (value, _) => value.toRecord().getString("value") }

        Then("the fixed codes 1 and 2 restore the exact typed values and record presentation remains symbolic")
        encoded shouldBe Vector(Some(1), Some(2))
        decoded shouldBe entries.map { case (value, _) => Some(value) }
        encodedjson shouldBe Vector(Json.fromInt(1), Json.fromInt(2))
        decodedjson shouldBe entries.map { case (value, _) => Right(value) }
        symbolic shouldBe Vector(Some("prepared"), Some("unprepared"))
      }

      "reject unknown codes and malformed numeric JSON without selecting a default preparation state" in {
        Given("the generated common codec and inputs outside its fixed numeric correspondence")
        val codec: NumericCodeCodec[ProposalPreparationState] = ProposalPreparationState.numericCodeCodec
        val jsoncodec = codec.jsonCodec
        val invalidjson = Vector(Json.fromInt(99), Json.fromString("1"), Json.fromBigDecimal(BigDecimal("1.5")))

        When("the typed codec decodes 99 and the numeric JSON codec decodes each invalid representation")
        val unknown = codec.decodeC(99)
        val malformed = invalidjson.map(jsoncodec.decodeJson)

        Then("an unknown code is a native failure and none of the JSON inputs becomes a preparation state")
        unknown shouldBe a[Consequence.Failure[?]]
        unknown.toOption shouldBe None
        malformed.foreach(_.isLeft shouldBe true)
      }
    }

    "registration" when {
      "given a prepared TKL proposal (PROP-01)" should {
        "retain the exact original and typed source, evidence, material and run references" in {
          Given("the exact prepared TKL JSON fixture and an ordinary fixed-user production assembly")
          val json = _fixture("tkl-prepared.json")
          _with_subsystem { subsystem =>
            When("registerProposal receives the prepared proposal")
            val result = _register(subsystem, json)
            val candidate = result.candidate

            Then("a new native candidate retains content, context, correspondence and available provenance")
            result.created shouldBe true
            candidate.id.print should not be empty
            candidate.name.value shouldBe "TKL prepared research note"
            candidate.producerRef.map(_.value) shouldBe Some("tkl-fixture")
            candidate.proposalId.map(_.value) shouldBe Some("tkl-proposal-1")
            candidate.sourceKind.map(_.value) shouldBe Some("tkl")
            candidate.content.map(_.value) shouldBe Some("A prepared research note for human formation.")
            candidate.context.map(_.value) shouldBe Some("TKL preparation fixture")
            candidate.preparationState.map(_.value) shouldBe Some("prepared")
            candidate.toDataStore().getInt("preparationState") shouldBe Some(1)
            candidate.toRecord().getString("preparationState") shouldBe Some("prepared")
            candidate.originalProposal.map(_.value) shouldBe Some(json)
            candidate.evidenceRefs.map(_.id) shouldBe Vector("evidence:tkl:1")
            candidate.evidenceRefs.head.kind shouldBe "evidence"
            candidate.evidenceRefs.head.revision shouldBe Some("1")
            candidate.evidenceRefs.head.snapshotRef shouldBe Some("snapshot:tkl:1")
            candidate.evidenceRefs.head.retrievalState shouldBe Some("available")
            candidate.preparedMaterialRefs.map(_.id) shouldBe Vector("material:tkl:1")
            candidate.preparedMaterialRefs.head.kind shouldBe "prepared-material"
            candidate.preparedMaterialRefs.head.revision shouldBe Some("1")
            candidate.preparedMaterialRefs.head.retrievalState shouldBe Some("available")
            candidate.sourceRunRef.map(_.id) shouldBe Some("run:tkl:1")
            candidate.sourceRunRef.map(_.kind) shouldBe Some("processing-run")
            candidate.sourceRunRef.flatMap(_.revision) shouldBe Some("1")
            candidate.sourceContextRef shouldBe None
            _items(subsystem).map(_.id) shouldBe Vector(candidate.id)
          }
        }
      }

      "given an unprepared Editing Studio capture (PROP-02)" should {
        "retain transfer observations and evidence limits without inventing preparation" in {
          Given("the exact confirmed capture fixture with no PreparedMaterial or Run")
          val json = _fixture("editing-studio-unprepared.json")
          _with_subsystem { subsystem =>
            When("the same registerProposal operation receives the unprepared capture")
            val result = _register(subsystem, json)
            val candidate = result.candidate

            Then("the capture remains unprepared with its source context and unknown version/snapshot")
            result.created shouldBe true
            candidate.name.value shouldBe "Confirmed book capture"
            candidate.producerRef.map(_.value) shouldBe Some("editing-studio-fixture")
            candidate.proposalId.map(_.value) shouldBe Some("book-capture-1")
            candidate.sourceKind.map(_.value) shouldBe Some("editing-studio")
            candidate.content.map(_.value) shouldBe Some("A capture-derived note awaiting preparation and human review.")
            candidate.context.map(_.value) shouldBe Some("Book chapter captured through Editing Studio")
            candidate.preparationState.map(_.value) shouldBe Some("unprepared")
            candidate.toDataStore().getInt("preparationState") shouldBe Some(2)
            candidate.toRecord().getString("preparationState") shouldBe Some("unprepared")
            candidate.originalProposal.map(_.value) shouldBe Some(json)
            candidate.preparedMaterialRefs shouldBe empty
            candidate.sourceRunRef shouldBe None
            candidate.sourceContextRef.map(_.id) shouldBe Some("editing-context:book:1")
            candidate.sourceContextRef.map(_.kind) shouldBe Some("source-context")
            candidate.evidenceRefs.head.id shouldBe "capture:book:1"
            candidate.evidenceRefs.head.kind shouldBe "book-capture"
            candidate.evidenceRefs.head.revision shouldBe None
            candidate.evidenceRefs.head.snapshotRef shouldBe None
            candidate.evidenceRefs.head.retrievalState shouldBe Some("unverified")
            candidate.evidenceRefs.head.limitation shouldBe Some(
              "Reference metadata only; original scan and a stable version are not available in this fixture."
            )
            _json(candidate.originalProposal.get.value).hcursor.get[Boolean]("captureConfirmed") shouldBe Right(true)
            _items(subsystem).map(_.id) shouldBe Vector(candidate.id)
          }
        }
      }

      "given supplied evidence limits and an unfamiliar source label (PROP-01/02)" should {
        "admit unavailable or expired references and optional run data without provider inference" in {
          Vector("expired", "unavailable").foreach { state =>
            Given(s"a prepared fixture from a non-whitelisted provider without a Run, with $state evidence")
            val evidence = Json.obj(
              "id" -> Json.fromString("evidence:limited"),
              "kind" -> Json.fromString("evidence"),
              "revision" -> Json.fromString("opaque-version-v7"),
              "snapshotRef" -> Json.fromString("snapshot:retained"),
              "retrievalState" -> Json.fromString(state),
              "retentionUntil" -> Json.fromString("2026-01-01"),
              "limitation" -> Json.fromString("Original is unavailable")
            )
            val json = _json(_fixture("tkl-prepared.json")).mapObject { obj =>
              obj.remove("sourceRunRef").add("sourceKind", Json.fromString("another-provider"))
                .add("context", Json.fromString(""))
                .add("evidenceRefs", Json.arr(evidence))
            }.spaces2
            _with_subsystem { subsystem =>
              When("the proposal is registered")
              val candidate = _register(subsystem, json).candidate

              Then("the supplied limits and opaque references are retained without a fabricated Run")
              candidate.sourceKind.map(_.value) shouldBe Some("another-provider")
              candidate.context.map(_.value) shouldBe Some("")
              candidate.sourceRunRef shouldBe None
              candidate.evidenceRefs.head.revision shouldBe Some("opaque-version-v7")
              candidate.evidenceRefs.head.snapshotRef shouldBe Some("snapshot:retained")
              candidate.evidenceRefs.head.retrievalState shouldBe Some(state)
              candidate.evidenceRefs.head.retentionUntil shouldBe Some("2026-01-01")
              candidate.evidenceRefs.head.limitation shouldBe Some("Original is unavailable")
            }
          }
        }
      }
    }

    "lookup (PROP-03)" should {
      "return exact current correspondence or absence without adding rows" in {
        Given("a registered TKL candidate and the same production datastore")
        _with_subsystem { subsystem =>
          val first = _register(subsystem, _fixture("tkl-prepared.json")).candidate
          val before = _items(subsystem)

          When("existing, absent, case-changed and whitespace-changed keys are queried")
          val existing = _find(subsystem, "tkl-fixture", "tkl-proposal-1")
          val absent = _find(subsystem, "tkl-fixture", "absent")
          val changedcase = _find(subsystem, "TKL-fixture", "tkl-proposal-1")
          val changedspace = _find(subsystem, " tkl-fixture", "tkl-proposal-1")

          Then("only the exact producer/proposal key resolves, and the visible records are unchanged")
          existing.map(_.toRecord()) shouldBe Some(first.toRecord())
          absent shouldBe None
          changedcase shouldBe None
          changedspace shouldBe None
          _items(subsystem).map(_.toRecord()) shouldBe before.map(_.toRecord())
        }
      }

      "fail explicitly on ambiguous visible correspondence without selecting or changing a row" in {
        Given("two native entities with the same source key, introduced through the generated create operation")
        val json = _fixture("tkl-prepared.json")
        _with_subsystem { subsystem =>
          val first = _register(subsystem, json).candidate
          val properties = (first.toRecord().asMap - "id").toVector
          _execute(subsystem, "entity", "createInformationCandidate", properties*)
          val before = _items(subsystem).map(_.toRecord())

          When("lookup and proposal registration encounter the ambiguous source key")
          val lookup = _call(subsystem, "Candidate", "findCandidateByProposal",
            "producerRef" -> "tkl-fixture", "proposalId" -> "tkl-proposal-1")
          val resend = _call(subsystem, "Candidate", "registerProposal", "proposalJson" -> json)

          Then("both report a structured state conflict and leave the two entities unchanged")
          Vector(lookup, resend).foreach {
            case Consequence.Failure(conclusion) =>
              conclusion.causes.map(_.observation.taxonomy) should contain(Taxonomy.stateConflict)
            case other => fail(s"Expected an ambiguous correspondence state conflict: $other")
          }
          lookup.toString should include ("Ambiguous proposal correspondence")
          resend.toString should include ("Ambiguous proposal correspondence")
          before should have size 2
          _items(subsystem).map(_.toRecord()) shouldBe before
        }
      }
    }

    "sequential resend (PROP-04/05)" should {
      "return the same stored candidate for the identical valid proposal with no extra row" in {
        Given("the prepared TKL fixture registered once")
        val json = _fixture("tkl-prepared.json")
        _with_subsystem { subsystem =>
          val first = _register(subsystem, json)

          When("the identical valid JSON is submitted sequentially")
          val resend = _register(subsystem, json)

          Then("the current candidate is returned with created=false and one visible row")
          first.created shouldBe true
          resend.created shouldBe false
          resend.candidate.toRecord() shouldBe first.candidate.toRecord()
          _items(subsystem).map(_.id) shouldBe Vector(first.candidate.id)
        }
      }

      "preserve a human content edit and first metadata when the same key carries a different valid payload" in {
        Given("a TKL proposal candidate with an actual generated content-only native edit")
        val json = _fixture("tkl-prepared.json")
        _with_subsystem { subsystem =>
          val first = _register(subsystem, json).candidate
          _execute(subsystem, "entity", "updateInformationCandidate",
            "id" -> first.id.print, "content" -> "Human corrected content")
          val edited = _find(subsystem, "tkl-fixture", "tkl-proposal-1").get
          edited.content.map(_.value) shouldBe Some("Human corrected content")

          When("a different valid incoming payload reuses the exact source key")
          val resend = _register(subsystem, _changed_payload(json))

          Then("resend returns the current human edit without replacing any original content or source metadata")
          resend.created shouldBe false
          resend.candidate.id shouldBe first.id
          resend.candidate.content.map(_.value) shouldBe Some("Human corrected content")
          resend.candidate.toRecord() shouldBe edited.toRecord()
          resend.candidate.originalProposal.map(_.value) shouldBe Some(json)
          resend.candidate.name shouldBe first.name
          resend.candidate.context shouldBe first.context
          resend.candidate.sourceKind shouldBe first.sourceKind
          resend.candidate.evidenceRefs shouldBe first.evidenceRefs
          resend.candidate.preparedMaterialRefs shouldBe first.preparedMaterialRefs
          resend.candidate.sourceRunRef shouldBe first.sourceRunRef
          _items(subsystem) should have size 1
        }
      }
    }

    "restart (PROP-06)" should {
      "retain ID, edited content, original and references after shutdown and normal reopen" in {
        Given("both proposal fixtures in one test-owned SQLite root, and an actual TKL content edit")
        val root = Files.createTempDirectory("workbench-proposal-restart-")
        val json = _fixture("tkl-prepared.json")
        val firsthandle = _handle(root)
        val firstsubsystem = firsthandle.subsystem
        val stored = try {
          val first = _register(firstsubsystem, json).candidate
          val capture = _register(firstsubsystem, _fixture("editing-studio-unprepared.json")).candidate
          _execute(firstsubsystem, "entity", "updateInformationCandidate",
            "id" -> first.id.print, "content" -> "Human edit retained across restart")
          (_find(firstsubsystem, "tkl-fixture", "tkl-proposal-1").get, capture)
        } finally firsthandle.close()

        When("a new ordinary production subsystem opens the same root and fixed subject")
        val reopenedhandle = _handle(root)
        val reopened = reopenedhandle.subsystem
        try {
          val found = _find(reopened, "tkl-fixture", "tkl-proposal-1").get
          val capture = _find(reopened, "editing-studio-fixture", "book-capture-1").get
          val resend = _register(reopened, _changed_payload(json))

          Then("lookup and resend retain all persisted current fields and the original references")
          found.toRecord() shouldBe stored._1.toRecord()
          found.toDataStore().getInt("preparationState") shouldBe Some(1)
          found.toRecord().getString("preparationState") shouldBe Some("prepared")
          found.content.map(_.value) shouldBe Some("Human edit retained across restart")
          found.originalProposal.map(_.value) shouldBe Some(json)
          capture.toRecord() shouldBe stored._2.toRecord()
          capture.toDataStore().getInt("preparationState") shouldBe Some(2)
          capture.toRecord().getString("preparationState") shouldBe Some("unprepared")
          val preparedstorage = _stored_preparation(root, found.id.print)
          val unpreparedstorage = _stored_preparation(root, capture.id.print)
          preparedstorage._1 shouldBe 1
          unpreparedstorage._1 shouldBe 2
          Set("integer", "real") should contain (preparedstorage._2)
          Set("integer", "real") should contain (unpreparedstorage._2)
          resend.created shouldBe false
          resend.candidate.toRecord() shouldBe stored._1.toRecord()
          _items(reopened).map(_.id).toSet shouldBe Set(stored._1.id, stored._2.id)
        } finally reopenedhandle.close()
      }

      "fail lookup on an unknown stored preparation code without rewriting the database" in {
        Given("a prepared proposal registered in a test-owned SQLite root, then closed with its stored code changed to 99")
        val root = Files.createTempDirectory("workbench-proposal-unknown-code-")
        val firsthandle = _handle(root)
        val candidate = try {
          _register(firsthandle.subsystem, _fixture("tkl-prepared.json")).candidate
        } finally firsthandle.close()
        _set_stored_preparation(root, candidate.id.print, 99)

        When("a normal production subsystem reopens that root and looks up the existing proposal")
        val reopenedhandle = _handle(root)
        try {
          val result = _call(reopenedhandle.subsystem, "Candidate", "findCandidateByProposal",
            "producerRef" -> "tkl-fixture", "proposalId" -> "tkl-proposal-1")
          val stored = _stored_preparation(root, candidate.id.print)

          Then("lookup returns a native failure and leaves the unknown physical code unchanged")
          result shouldBe a[Consequence.Failure[?]]
          stored._1 shouldBe 99
          Set("integer", "real") should contain (stored._2)
        } finally reopenedhandle.close()
      }

      "retain actual optional preparation absence for a native manual candidate after reopen" in {
        Given("a prepared proposal materializes the preparation column before a native manual candidate is registered without preparation metadata")
        val root = Files.createTempDirectory("workbench-proposal-absent-state-")
        val firsthandle = _handle(root)
        val candidateid = try {
          _register(firsthandle.subsystem, _fixture("tkl-prepared.json"))
          _execute(firsthandle.subsystem, "Candidate", "registerCandidate",
            "name" -> "Manual candidate without preparation").getString("id").get
        } finally firsthandle.close()

        When("the normal production subsystem reopens the same root and lists the manual candidate")
        val reopenedhandle = _handle(root)
        try {
          val candidates = _items(reopenedhandle.subsystem).filter(_.id.print == candidateid)
          val stored = _stored_preparation(root, candidateid)

          Then("the candidate retains None for preparation and SQLite retains a null rather than an enum code")
          candidates.size shouldBe 1
          candidates.head.preparationState shouldBe None
          candidates.head.toRecord().getAny("preparationState") shouldBe None
          stored._2 shouldBe "null"
        } finally reopenedhandle.close()
      }
    }

    "source key isolation (PROP-07)" should {
      "distinguish each producer/proposal pair without normalizing the strings" in {
        Given("one valid proposal and alternatives changing exactly one source-key member")
        val original = _json(_fixture("tkl-prepared.json"))
        val otherproducer = original.mapObject(_.add("producerRef", Json.fromString("other-producer"))).spaces2
        val otherproposal = original.mapObject(_.add("proposalId", Json.fromString("other-proposal"))).spaces2
        val spaced = original.mapObject(_.add("producerRef", Json.fromString(" tkl-fixture"))).spaces2
        _with_subsystem { subsystem =>
          When("the four valid keys are registered")
          val results = Vector(original.spaces2, otherproducer, otherproposal, spaced).map(_register(subsystem, _))

          Then("each source pair creates and resolves its own native candidate")
          results.map(_.created) shouldBe Vector.fill(4)(true)
          results.map(_.candidate.id).distinct should have size 4
          _find(subsystem, "tkl-fixture", "tkl-proposal-1").map(_.id) shouldBe Some(results(0).candidate.id)
          _find(subsystem, "other-producer", "tkl-proposal-1").map(_.id) shouldBe Some(results(1).candidate.id)
          _find(subsystem, "tkl-fixture", "other-proposal").map(_.id) shouldBe Some(results(2).candidate.id)
          _find(subsystem, " tkl-fixture", "tkl-proposal-1").map(_.id) shouldBe Some(results(3).candidate.id)
          _items(subsystem) should have size 4
        }
      }
    }

    "validation (PROP-08)" should {
      "reject each invalid proposal before adding or changing native rows" in {
        val valid = _json(_fixture("tkl-prepared.json"))
        val required = Vector("producerRef", "proposalId", "sourceKind", "name", "content", "preparationState")
        val scalars = required.flatMap { field => Vector(
          s"missing $field" -> valid.mapObject(_.remove(field)).noSpaces,
          s"blank $field" -> valid.mapObject(_.add(field, Json.fromString("  "))).noSpaces,
          s"wrong type $field" -> valid.mapObject(_.add(field, Json.fromInt(1))).noSpaces
        ) }
        val malformed = Vector(
          "invalid JSON" -> "{",
          "array input" -> "[]",
          "null input" -> "null",
          "missing evidence" -> valid.mapObject(_.remove("evidenceRefs")).noSpaces,
          "empty evidence" -> valid.mapObject(_.add("evidenceRefs", Json.arr())).noSpaces,
          "scalar evidence" -> valid.mapObject(_.add("evidenceRefs", Json.fromString("evidence"))).noSpaces,
          "object evidence" -> valid.mapObject(_.add("evidenceRefs", Json.obj("id" -> Json.fromString("x")))).noSpaces,
          "malformed reference" -> valid.mapObject(_.add("evidenceRefs", Json.arr(Json.obj("id" -> Json.fromString("x"))))).noSpaces,
          "nonobject reference" -> valid.mapObject(_.add("evidenceRefs", Json.arr(Json.fromString("x")))).noSpaces,
          "invalid preparation" -> valid.mapObject(_.add("preparationState", Json.fromString("approved"))).noSpaces,
          "missing prepared material" -> valid.mapObject(_.remove("preparedMaterialRefs")).noSpaces,
          "empty prepared material" -> valid.mapObject(_.add("preparedMaterialRefs", Json.arr())).noSpaces,
          "malformed material" -> valid.mapObject(_.add("preparedMaterialRefs", Json.arr(Json.obj()))).noSpaces,
          "malformed run" -> valid.mapObject(_.add("sourceRunRef", Json.fromString("run"))).noSpaces,
          "malformed source context" -> valid.mapObject(_.add("sourceContextRef", Json.obj())).noSpaces,
          "nonstring context" -> valid.mapObject(_.add("context", Json.fromBoolean(true))).noSpaces,
          "null context" -> valid.mapObject(_.add("context", Json.Null)).noSpaces,
          "nonboolean transfer" -> valid.mapObject(_.add("captureConfirmed", Json.fromString("true"))).noSpaces,
          "overlong name" -> valid.mapObject(_.add("name", Json.fromString("x" * 257))).noSpaces
        )
        val referencefields = Vector("id", "kind", "revision", "snapshotRef", "retrievalState", "retentionUntil", "limitation")
        val references = referencefields.flatMap { field => Vector(Json.fromString(" "), Json.fromInt(1), Json.Null).map { bad =>
          s"invalid reference $field ${bad.noSpaces}" -> valid.mapObject(_.add("evidenceRefs", Json.arr(
            Json.obj("id" -> Json.fromString("evidence"), "kind" -> Json.fromString("evidence")).mapObject(_.add(field, bad))
          ))).noSpaces
        } }

        _with_subsystem { subsystem =>
          val existing = _register(subsystem, valid.spaces2).candidate
          (scalars ++ malformed ++ references).foreach { case (label, json) =>
            Given(s"an existing native candidate and invalid proposal case: $label")
            val before = _items(subsystem).map(_.toRecord())

            When("registerProposal receives the invalid input, even though its key may already exist")
            val result = _call(subsystem, "Candidate", "registerProposal", "proposalJson" -> json)

            Then("the native failure result prevents success and leaves all stored fields unchanged")
            withClue(label) {
              result shouldBe a[Consequence.Failure[?]]
              _items(subsystem).map(_.toRecord()) shouldBe before
              _find(subsystem, "tkl-fixture", "tkl-proposal-1").map(_.id) shouldBe Some(existing.id)
            }
          }
        }
      }

      "reject blank lookup keys without writing" in {
        _with_subsystem { subsystem =>
          Vector("producerRef", "proposalId").foreach { field =>
            Given(s"an empty native datastore and a blank lookup $field")
            val properties = Map("producerRef" -> "tkl-fixture", "proposalId" -> "tkl-proposal-1") + (field -> " ")

            When("findCandidateByProposal receives the blank source-key string")
            val result = _call(subsystem, "Candidate", "findCandidateByProposal", properties.toVector*)

            Then("native input failure returns no candidate and leaves storage empty")
            result shouldBe a[Consequence.Failure[?]]
            _items(subsystem) shouldBe empty
          }
        }
      }
    }

    "legacy compatibility (PROP-09)" should {
      "keep starter creation, description search and native paging without fabricated proposal fields" in {
        Given("an ordinary assembly and two starter name/description registrations")
        _with_subsystem { subsystem =>
          val first = _execute(subsystem, "Candidate", "registerCandidate",
            "name" -> "Legacy research", "description" -> "Evidence retained")
          val second = _execute(subsystem, "Candidate", "registerCandidate", "name" -> "Other legacy")

          When("starter listing, description/name filtering, paging and proposal-key lookup are executed")
          val all = _items(subsystem)
          val bydescription = _items(subsystem, "text" -> "EVIDENCE")
          val byname = _items(subsystem, "text" -> "RESEARCH")
          val page = _items(subsystem, "offset" -> 1, "limit" -> 1)
          val emptypage = _items(subsystem, "offset" -> 2, "limit" -> 1)
          val absent = _find(subsystem, "tkl-fixture", "tkl-proposal-1")

          Then("starter results and query behavior remain compatible and legacy rows lack proposal correspondence")
          first.getString("name") shouldBe Some("Legacy research")
          second.getString("name") shouldBe Some("Other legacy")
          first.keySet shouldBe Set("id", "name")
          all.map(_.id.print).toSet shouldBe Set(first.getString("id").get, second.getString("id").get)
          bydescription.map(_.id.print) shouldBe Vector(first.getString("id").get)
          byname.map(_.id.print) shouldBe Vector(first.getString("id").get)
          bydescription.head.description.map(_.value) shouldBe Some("Evidence retained")
          page should have size 1
          all.map(_.id) should contain (page.head.id)
          emptypage shouldBe empty
          absent shouldBe None
          all.foreach { candidate =>
            candidate.producerRef shouldBe None
            candidate.proposalId shouldBe None
            candidate.originalProposal shouldBe None
            candidate.content shouldBe None
            candidate.evidenceRefs shouldBe empty
            candidate.preparedMaterialRefs shouldBe empty
            candidate.sourceRunRef shouldBe None
            candidate.sourceContextRef shouldBe None
          }
          _items(subsystem) should have size 2
        }
      }
    }
  }

  private def _register(subsystem: Subsystem, json: String): workbenchvalue.RegisterProposalResult =
    workbenchvalue.RegisterProposalResult.createC(
      _execute(subsystem, "Candidate", "registerProposal", "proposalJson" -> json)
    ).toOption.getOrElse(fail("registerProposal did not return its declared result"))

  private def _stored_preparation(root: Path, entityid: String): (Int, String) = {
    val database = root.resolve("org.simplemodeling.textus/knowledge-workbench/datastores/application.db")
    Files.exists(database) shouldBe true
    val connection = DriverManager.getConnection(s"jdbc:sqlite:${database.toUri}?mode=ro")
    try {
      val statement = connection.prepareStatement(
        "SELECT preparationState, typeof(preparationState) FROM information_candidate WHERE id = ?"
      )
      try {
        statement.setString(1, entityid)
        val result = statement.executeQuery()
        try {
          result.next() shouldBe true
          (result.getInt(1), result.getString(2))
        } finally result.close()
      } finally statement.close()
    } finally connection.close()
  }

  private def _set_stored_preparation(root: Path, entityid: String, code: Int): Unit = {
    val database = root.resolve("org.simplemodeling.textus/knowledge-workbench/datastores/application.db")
    val connection = DriverManager.getConnection(s"jdbc:sqlite:${database.toUri}")
    try {
      val statement = connection.prepareStatement(
        "UPDATE information_candidate SET preparationState = ? WHERE id = ?"
      )
      try {
        statement.setInt(1, code)
        statement.setString(2, entityid)
        statement.executeUpdate() shouldBe 1
      } finally statement.close()
    } finally connection.close()
  }

  private def _find(subsystem: Subsystem, producerref: String, proposalid: String): Option[entity.InformationCandidate] =
    workbenchvalue.FindCandidateByProposalResult.createC(_execute(subsystem, "Candidate", "findCandidateByProposal",
      "producerRef" -> producerref, "proposalId" -> proposalid
    )).toOption.getOrElse(fail("lookup did not return its declared result")).candidate

  private def _items(subsystem: Subsystem, properties: (String, Any)*): Vector[entity.InformationCandidate] =
    workbenchvalue.ListCandidatesResult.createC(_execute(subsystem, "Candidate", "listCandidates", properties*))
      .toOption.getOrElse(fail("listCandidates did not return its declared result")).items

  private def _call(subsystem: Subsystem, service: String, operation: String, properties: (String, Any)*): Consequence[OperationResponse] =
    subsystem.executeOperationResponse(Request.of(
      component = "org.simplemodeling.textus.KnowledgeWorkbench",
      service = service,
      operation = operation,
      properties = properties.map { case (name, input) => Property(name, input, None) }.toList
    ))

  private def _execute(subsystem: Subsystem, service: String, operation: String, properties: (String, Any)*): Record = {
    val result = _call(subsystem, service, operation, properties*)
    result.toOption.getOrElse(fail(s"$service.$operation failed: $result")) match {
      case OperationResponse.RecordResponse(record) => record
      case other => fail(s"$service.$operation returned an unexpected response: $other")
    }
  }

  private def _changed_payload(json: String): String =
    _json(json).mapObject { obj =>
      obj.add("name", Json.fromString("Incoming replacement name"))
        .add("content", Json.fromString("Incoming replacement content"))
        .add("context", Json.fromString("Incoming replacement context"))
        .add("sourceKind", Json.fromString("another-provider"))
        .add("evidenceRefs", Json.arr(Json.obj(
          "id" -> Json.fromString("incoming:evidence"), "kind" -> Json.fromString("evidence")
        )))
        .add("preparedMaterialRefs", Json.arr(Json.obj(
          "id" -> Json.fromString("incoming:material"), "kind" -> Json.fromString("prepared-material")
        )))
        .add("sourceRunRef", Json.obj(
          "id" -> Json.fromString("incoming:run"), "kind" -> Json.fromString("processing-run")
        ))
    }.spaces2

  private def _json(json: String): Json =
    parse(json).getOrElse(fail("proposal fixture is not valid JSON"))

  private def _fixture(name: String): String = {
    val stream = Option(getClass.getResourceAsStream(s"/proposals/$name"))
      .getOrElse(fail(s"missing proposal fixture $name"))
    try new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
    finally stream.close()
  }

  private def _with_subsystem[A](body: Subsystem => A): A = {
    val handle = _handle(Files.createTempDirectory("workbench-proposal-spec-"))
    try body(handle.subsystem)
    finally handle.close()
  }

  private def _handle(root: Path): CncfHandle = {
    val repository = Path.of(".").toAbsolutePath.normalize()
    val lakerepository = Path.of(
      sys.props.getOrElse(
        "textus.test.knowledge-lake.repository",
        repository.resolveSibling("textus-knowledge-lake").toString
      )
    ).toAbsolutePath.normalize()
    val authored = Files.readString(repository.resolve("src/main/car/assembly-descriptor.yaml"))
    val instance = root.resolve("assembly-instance.yaml")
    Files.writeString(instance, authored.replace(
      "    fallbackPrivilege: disabled\n",
      "    fallbackPrivilege: disabled\n    localSubject:\n      id: workbench-assembly-spec-user\n"
    ))
    CncfBootstrap.initialize(BootstrapConfig(
      cwd = root,
      args = Array(
        "--discover=classes",
        s"--component-dev-dir=$repository",
        s"--component-dev-dir=$lakerepository",
        s"--textus.assembly.descriptor=$instance",
        s"--textus.local-data.root=$root",
        "command"
      ),
      modeHint = Some(RunMode.Command)
    )).toOption.getOrElse(fail("the ordinary Workbench runtime did not initialize"))
  }
}
