package org.simplemodeling.textus.knowledgeworkbench

import org.simplemodeling.textus.knowledgeworkbench.{value as workbenchvalue}

import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgeworkbench.impl.SemanticFormationWork

/*
 * @since   Oct. 30, 2026
 * @version Oct. 30, 2026
 */
final class SemanticFormationWorkSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Saved semantic work" should {
    "require separate review/approval and invalidate every meaningful changed target" in {
      Given("a selected saved target, with no prior semantic decisions")
      val initial = SemanticFormationWorkFixture.snapshot()
      val reviewed = SemanticFormationWork.decide(initial, workbenchvalue.SemanticWorkDecision(initial.entity.id, initial.revision.value,
        "review", "Explicit review"), "editor", SemanticFormationWorkFixture.instant).TAKE
      val approved = SemanticFormationWork.decide(initial.copy(entity = reviewed), workbenchvalue.SemanticWorkDecision(initial.entity.id,
        initial.revision.value, "approve", "Explicit approval"), "editor", SemanticFormationWorkFixture.instant).TAKE
      When("generated complete corrections change the selected meaning")
      val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), Prop.forAll(Gen.alphaStr) { text =>
        val changed = approved.target.get.copy(label = "Changed " + text)
        val corrected = SemanticFormationWork.correct(initial.copy(entity = approved), changed, initial.revision.value,
          "Explicit correction", "editor", SemanticFormationWorkFixture.instant).TAKE
        !SemanticFormationWork.result(initial.copy(entity = corrected)).approvalValid &&
          corrected.informationId == initial.entity.informationId && corrected.decisions.last.target == changed &&
          corrected.decisions.take(2) == approved.decisions
      })
      Then("prior approval never follows changed semantics and immutable source/history survive")
      SemanticFormationWork.result(initial).approvalValid shouldBe false
      SemanticFormationWork.result(initial.copy(entity = approved)).approvalValid shouldBe true
      checked.passed shouldBe true
      checked.succeeded should be >= 100
    }
    "retain one result reservation and refuse blind replay or mutation before reconciliation" in {
      Given("one explicitly approved complete target")
      val initial = SemanticFormationWorkFixture.snapshot()
      val target = initial.entity.target.get
      val approved = initial.copy(entity = initial.entity.copy(decisions = Vector(workbenchvalue.SemanticFormationDecision("approve", "controlled-editor",
        SemanticFormationWorkFixture.instant.toString, "Controlled explicit approval", target))))
      When("the result is reserved and its acknowledgement remains uncertain")
      val reserved = SemanticFormationWork.reserve(approved, approved.revision.value,
        SemanticFormationWorkFixture.ids.entityId(SemanticFormationWork.resultCollection)).TAKE
      val uncertain = SemanticFormationWork.observe(reserved, reserved.formations.last.copy(status = "Unconfirmed"))
      Then("the same identity remains available and another creation/correction is refused")
      uncertain.formations.map(_.id) shouldBe reserved.formations.map(_.id)
      SemanticFormationWork.reserve(approved.copy(entity = uncertain), approved.revision.value,
        SemanticFormationWorkFixture.ids.entityId(SemanticFormationWork.resultCollection)).isFaillure shouldBe true
      SemanticFormationWork.correct(approved.copy(entity = uncertain), target.copy(label = "changed"), approved.revision.value,
        "Correction", "editor", SemanticFormationWorkFixture.instant).isFaillure shouldBe true
    }
    "refuse stale, unknown, ambiguous, unreviewed and rejected formation requests" in {
      Given("a saved unapproved work and meaningful invalid variants")
      val initial = SemanticFormationWorkFixture.snapshot()
      When("approval or reservation is requested without the exact required state")
      val unreviewed = SemanticFormationWork.decide(initial, workbenchvalue.SemanticWorkDecision(initial.entity.id, initial.revision.value,
        "approve", "Cannot skip review"), "editor", SemanticFormationWorkFixture.instant)
      Then("no implicit admission or proposal grants semantic approval")
      unreviewed.isFaillure shouldBe true
      SemanticFormationWork.revision(initial, initial.revision.value - 1).isFaillure shouldBe true
      Vector("unknown", "ambiguous").foreach { outcome =>
        val work = initial.copy(entity = initial.entity.copy(target = initial.entity.target.map(_.copy(outcome = outcome))))
        SemanticFormationWork.decide(work, workbenchvalue.SemanticWorkDecision(work.entity.id, work.revision.value, "review", "Unresolved"),
          "editor", SemanticFormationWorkFixture.instant).isFaillure shouldBe true
      }
      SemanticFormationWork.reserve(initial, initial.revision.value,
        SemanticFormationWorkFixture.ids.entityId(SemanticFormationWork.resultCollection)).isFaillure shouldBe true
    }
  }
}
