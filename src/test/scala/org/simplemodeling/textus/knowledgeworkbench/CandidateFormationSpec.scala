package org.simplemodeling.textus.knowledgeworkbench

import java.nio.file.{Files, Path}
import java.time.Instant
import org.goldenport.Consequence
import org.goldenport.cncf.bootstrap.{BootstrapConfig, CncfBootstrap, CncfHandle}
import org.goldenport.cncf.cli.RunMode
import org.goldenport.cncf.entity.EntityRevisionCarrier
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.model.datatype.EntityRevision
import org.simplemodeling.textus.knowledgeworkbench.{datatype, value as workbenchvalue}
import org.simplemodeling.textus.knowledgeworkbench.impl.CandidateFormation
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
final class CandidateFormationSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Candidate formation and decisions" should {
    "target identity and supplied evidence" which {
      "retain the same target across native Review and Approve revisions for both producer fixtures" in {
        Vector("tkl-prepared.json", "editing-studio-unprepared.json").foreach { fixture =>
          Given(s"a persisted $fixture, with source limitations retained and explicit reviewed evidence")
          _with_runtime { subsystem =>
            val first = _register(subsystem, fixture)
            val initial = _get(subsystem, first.id.print)
            val ready = _edit(subsystem, initial, "Human corrected content", _evidence("1"))

            When("a human explicitly requests Review then Approve using each observed native revision")
            val reviewed = _decide(subsystem, ready, "review")
            val approved = _decide(subsystem, reviewed, "approve")

            Then("the exact reviewed target survives both revision advances without source or provenance replacement")
            initial.state shouldBe "Draft"
            initial.approvalValid shouldBe false
            reviewed.state shouldBe "Reviewing"
            approved.state shouldBe "Approved"
            approved.approvalValid shouldBe true
            reviewed.revision should be > ready.revision
            approved.revision should be > reviewed.revision
            approved.target shouldBe reviewed.target
            approved.candidate.decisions.takeRight(2).map(_.target) shouldBe Vector(reviewed.target.get, reviewed.target.get)
            approved.candidate.decisions.map(_.actor).distinct shouldBe Vector("workbench-assembly-spec-user")
            approved.candidate.decisions.foreach(event => Instant.parse(event.occurredAt).toEpochMilli should be > 0L)
            approved.candidate.originalProposal shouldBe first.originalProposal
            approved.candidate.preparedMaterialRefs shouldBe first.preparedMaterialRefs
            approved.candidate.sourceRunRef shouldBe first.sourceRunRef
            approved.candidate.sourceContextRef shouldBe first.sourceContextRef
            approved.candidate.preparationState shouldBe first.preparationState
            When("the original producer proposal is sequentially resent after approval")
            val resend = _register(subsystem, fixture)

            Then("resend preserves the current complete Candidate and its human decisions")
            resend.toRecord() shouldBe approved.candidate.toRecord()
          }
        }
      }

      "keep unavailable evidence unapproved while allowing Hold and Reject" in {
        Given("an unprepared captured proposal with unverified evidence and unknown stable version")
        _with_runtime { subsystem =>
          val original = _register(subsystem, "editing-studio-unprepared.json")
          val current = _get(subsystem, original.id.print)

          When("Review/Approve are requested before evidence is ready, then Hold and Reject are explicit")
          val review = _decision_call(subsystem, current, "review")
          val approve = _decision_call(subsystem, current, "approve")
          val held = _decide(subsystem, current, "hold")
          val rejected = _decide(subsystem, held, "reject")

          Then("invalid decisions write nothing, limitations remain, and neither transfer nor rejection grants approval")
          review shouldBe a[Consequence.Failure[?]]
          approve shouldBe a[Consequence.Failure[?]]
          held.candidate.decisions.map(_.decision.value) shouldBe Vector("hold")
          rejected.state shouldBe "Rejected"
          rejected.approvalValid shouldBe false
          rejected.candidate.evidenceRefs shouldBe original.evidenceRefs
          rejected.candidate.decisions.map(_.reason.value) shouldBe Vector("Human decision", "Human decision")
        }
      }

    }

    "human decision lifecycle" which {
      "resume Hold and require explicit reopening of Reject" in {
        Given("a ready Candidate with retained native decision history")
        _with_runtime { subsystem =>
          val initial = _get(subsystem, _register(subsystem, "tkl-prepared.json").id.print)
          val held = _decide(subsystem, initial, "hold")

          When("Hold resumes Review, Reject refuses Review, and an explicit same-target edit reopens it")
          val reviewed = _decide(subsystem, held, "review")
          val rejected = _decide(subsystem, reviewed, "reject")
          val denied = _decision_call(subsystem, rejected, "review")
          val reopened = _edit(subsystem, rejected, rejected.target.get.content.value, rejected.candidate.evidenceRefs)
          val renewed = _decide(subsystem, reopened, "review")

          Then("reopening is recorded and renewed Review is required instead of reviving the previous decision")
          denied shouldBe a[Consequence.Failure[?]]
          reopened.state shouldBe "Draft"
          reopened.revision should be > rejected.revision
          reopened.candidate.decisions.map(_.decision.value) shouldBe Vector("hold", "review", "reject", "edit")
          renewed.state shouldBe "Reviewing"
          renewed.approvalValid shouldBe false
        }
      }

      "invalidate changed targets and never resurrect approval when old content is restored" in {
        Given("a human-approved target in the native datastore")
        _with_runtime { subsystem =>
          val initial = _get(subsystem, _register(subsystem, "tkl-prepared.json").id.print)
          val approved = _decide(subsystem, _decide(subsystem, initial, "review"), "approve")

          When("an identical edit is followed by a changed edit and a return to the old content")
          val same = _edit(subsystem, approved, approved.target.get.content.value, approved.candidate.evidenceRefs)
          val changed = _edit(subsystem, same, "Changed human content", same.candidate.evidenceRefs)
          val reverted = _edit(subsystem, changed, approved.target.get.content.value, approved.candidate.evidenceRefs)

          Then("the no-op preserves revision while both actual edits invalidate approval and retain prior events")
          same.toRecord() shouldBe approved.toRecord()
          changed.state shouldBe "Draft"
          reverted.target shouldBe approved.target
          reverted.state shouldBe "Draft"
          reverted.approvalValid shouldBe false
          reverted.candidate.decisions.map(_.decision.value) shouldBe Vector("review", "approve", "edit", "edit")
          When("approval is requested again without renewing Review")
          val unreviewed = _decision_call(subsystem, reverted, "approve")

          Then("the old approval cannot authorize the reverted target")
          unreviewed shouldBe a[Consequence.Failure[?]]
        }
      }

      "reject stale revisions, invalid evidence, blank reasons and direct edit decisions without writing" in {
        Given("a ready Candidate and a newer independently observed native revision")
        _with_runtime { subsystem =>
          val initial = _get(subsystem, _register(subsystem, "tkl-prepared.json").id.print)
          val reviewed = _decide(subsystem, initial, "review")
          val before = _get(subsystem, initial.candidate.id.print)

          When("stale writes and invalid replacements attempt to mutate the Candidate")
          val stale = _decision_call(subsystem, initial, "hold")
          val staleedit = _call(subsystem, "editCandidateWork", _edit_properties(initial, "Stale content", _evidence("2"))*)
          val empty = _call(subsystem, "editCandidateWork", _edit_properties(reviewed, "Valid content", Vector.empty)*)
          val reason = _call(subsystem, "decideCandidateWork", "id" -> before.candidate.id.print,
            "expectedRevision" -> before.revision, "decision" -> "approve", "reason" -> " ")
          val directedit = _decision_call(subsystem, before, "edit")

          Then("every attempt fails and all native content, history and revision remain exact")
          Vector(stale, staleedit, empty, reason, directedit).foreach(_ shouldBe a[Consequence.Failure[?]])
          _get(subsystem, before.candidate.id.print).toRecord() shouldBe before.toRecord()
        }
      }

    }

    "persistence and complete target invalidation" which {
      "retain approval and exact history after normal runtime shutdown and persistent reopen" in {
        Given("a normal persistent runtime and an approved captured Candidate")
        val root = _root()
        val writer = _handle(root)
        val accepted = try {
          val initial = _get(writer.subsystem, _register(writer.subsystem, "editing-studio-unprepared.json").id.print)
          val ready = _edit(writer.subsystem, initial, "Human reviewed capture", _evidence("snapshot-2"))
          _decide(writer.subsystem, _decide(writer.subsystem, ready, "review"), "approve")
        } finally writer.close()

        When("a fresh normal handle reopens the same native datastore and selected user")
        val reader = _handle(root)
        try {
          val restored = _get(reader.subsystem, accepted.candidate.id.print)

          Then("native persistence restores the original, target, history, revision and effective approval")
          restored.toRecord() shouldBe accepted.toRecord()
          restored.approvalValid shouldBe true
        } finally reader.close()
      }

      "invalidate every changed content, context, evidence, grounding or mapping target without losing old approval history" in {
        Given("one actual persisted approved Candidate and bounded generated replacement data")
        _with_runtime { subsystem =>
          val initial = _get(subsystem, _register(subsystem, "tkl-prepared.json").id.print)
          val approved = _decide(subsystem, _decide(subsystem, initial, "review"), "approve")
          val snapshot = EntityRevisionCarrier(approved.candidate, _success(EntityRevision.createC(approved.revision)))
          val input = Gen.chooseNum(1, 24).flatMap(size => Gen.listOfN(size, Gen.alphaNumChar).map(_.mkString))

          When("each generated replacement changes exactly one semantic target dimension")
          val property = Prop.forAll(input, Gen.chooseNum(0, 4)) { (text, dimension) =>
            val current = approved.target.get
            val base = workbenchvalue.CandidateWorkEdit(approved.candidate.id, approved.revision,
              current.content, current.context, current.evidenceRefs, current.groundingRefs, current.mappingRefs,
              _success(datatype.CandidateDecisionReason.createC("Human correction")))
            val replacement = dimension match {
              case 0 => base.copy(content = _success(datatype.CandidateContent.createC(s"Changed $text")))
              case 1 => base.copy(context = Some(_success(datatype.CandidateContext.createC(s"Changed $text"))))
              case 2 => base.copy(evidenceRefs = _evidence(s"changed-$text"))
              case 3 => base.copy(groundingRefs = _evidence(s"ground-$text"))
              case _ => base.copy(mappingRefs = _evidence(s"map-$text"))
            }
            val change = _success(CandidateFormation.edit(snapshot, replacement,
              "workbench-assembly-spec-user", Instant.parse("2026-10-27T00:00:00Z"))).get
            val result = CandidateFormation.result(EntityRevisionCarrier(change, snapshot.revision))

            Then("the target changes, the last event is Edit, and the previously recorded approval remains history only")
            result.target should not be (approved.target)
            result.state shouldBe "Draft"
            result.approvalValid shouldBe false
            change.decisions.dropRight(1) shouldBe approved.candidate.decisions
            change.decisions.last.decision.value shouldBe "edit"
            true
          }
          val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100).withMaxDiscardRatio(1.0f), property)

          Then("at least 100 generated examples pass with zero discarded examples")
          withClue(checked.toString) { checked.passed shouldBe true }
          checked.succeeded should be >= 100
          checked.discarded shouldBe 0
        }
      }
    }
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
    val parent = Path.of("target/candidate-formation-spec").toAbsolutePath.normalize()
    Files.createDirectories(parent)
    Files.createTempDirectory(parent, "case-")
  }

  private def _with_runtime[A](body: Subsystem => A): A = {
    val handle = _handle(_root())
    try body(handle.subsystem) finally handle.close()
  }

  private def _handle(root: Path): CncfHandle = {
    val repository = Path.of(".").toAbsolutePath.normalize()
    val lake = Path.of(sys.props.getOrElse("textus.test.knowledge-lake.repository",
      repository.resolveSibling("textus-knowledge-lake").toString)).toAbsolutePath.normalize()
    val authored = Files.readString(repository.resolve("src/main/car/assembly-descriptor.yaml"))
    val instance = root.resolve("assembly-instance.yaml")
    Files.writeString(instance, authored.replace("    fallbackPrivilege: disabled\n",
      "    fallbackPrivilege: disabled\n    localSubject:\n      id: workbench-assembly-spec-user\n"))
    _success(CncfBootstrap.initialize(BootstrapConfig(cwd = root,
      args = Array("--discover=classes", s"--component-dev-dir=$repository", s"--component-dev-dir=$lake",
        s"--textus.assembly.descriptor=$instance", s"--textus.local-data.root=$root", "command"),
      modeHint = Some(RunMode.Command))))
  }
}
