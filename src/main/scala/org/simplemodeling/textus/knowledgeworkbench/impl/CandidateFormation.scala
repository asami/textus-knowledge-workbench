package org.simplemodeling.textus.knowledgeworkbench.impl

import java.time.Instant
import domain.CandidateDecisionKind
import org.goldenport.Consequence
import org.goldenport.cncf.entity.EntityRevisionCarrier
import org.simplemodeling.textus.knowledgeworkbench.{entity, value}

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
private[knowledgeworkbench] object CandidateFormation {
  def target(candidate: entity.InformationCandidate): Option[value.CandidateApprovalTarget] =
    candidate.content.map(content => value.CandidateApprovalTarget(
      content, candidate.context, candidate.evidenceRefs, candidate.preparedMaterialRefs,
      candidate.sourceRunRef, candidate.sourceContextRef, candidate.groundingRefs, candidate.mappingRefs
    ))

  def eligible(target: Option[value.CandidateApprovalTarget]): Boolean =
    target.exists(t => t.evidenceRefs.nonEmpty && t.evidenceRefs.forall { reference =>
      reference.id.trim.nonEmpty && reference.kind.trim.nonEmpty &&
        reference.retrievalState.contains("available") &&
        (reference.revision.exists(_.trim.nonEmpty) || reference.snapshotRef.exists(_.trim.nonEmpty))
    })

  def result(snapshot: EntityRevisionCarrier[entity.InformationCandidate]): value.CandidateWorkResult = {
    val selected = target(snapshot.entity)
    val state = _state(snapshot.entity, selected)
    value.CandidateWorkResult(snapshot.entity, snapshot.revision.value, state,
      eligible(selected), state == "Approved" && eligible(selected), selected)
  }

  def edit(
    snapshot: EntityRevisionCarrier[entity.InformationCandidate],
    input: value.CandidateWorkEdit,
    actor: String,
    instant: Instant
  ): Consequence[Option[entity.InformationCandidate]] = {
    val candidate = snapshot.entity.copy(content = Some(input.content), context = input.context,
      evidenceRefs = input.evidenceRefs, groundingRefs = input.groundingRefs, mappingRefs = input.mappingRefs)
    for {
      _ <- _revision(snapshot, input.expectedRevision)
      _ <- _references(candidate)
      _ <- _reason(input.reason.value)
      selected <- target(candidate).map(Consequence.success).getOrElse(
        Consequence.stateConflict("Candidate content is required"))
    } yield {
      val state = result(snapshot).state
      if (target(snapshot.entity).contains(selected) && state != "Held" && state != "Rejected") None
      else Some(candidate.copy(decisions = snapshot.entity.decisions :+
        value.CandidateReviewDecision(CandidateDecisionKind.edit, actor, instant.toString, input.reason, selected)))
    }
  }

  def decide(
    snapshot: EntityRevisionCarrier[entity.InformationCandidate],
    input: value.CandidateWorkDecision,
    actor: String,
    instant: Instant
  ): Consequence[entity.InformationCandidate] = {
    val current = result(snapshot)
    val decision = input.decision.value
    val permitted = decision match {
      case "review" => (current.state == "Draft" || current.state == "Held") && current.reviewEligible
      case "approve" => current.state == "Reviewing" && current.reviewEligible
      case "hold" | "reject" => true
      case _ => false
    }
    for {
      _ <- _revision(snapshot, input.expectedRevision)
      _ <- _reason(input.reason.value)
      _ <- if (permitted) Consequence.success(())
           else Consequence.stateConflict("Candidate decision is not eligible for the current target and review state")
      selected <- current.target.map(Consequence.success).getOrElse(
        Consequence.stateConflict("Candidate content is required before a decision"))
    } yield snapshot.entity.copy(decisions = snapshot.entity.decisions :+
      value.CandidateReviewDecision(input.decision, actor, instant.toString, input.reason, selected))
  }

  private def _state(candidate: entity.InformationCandidate, selected: Option[value.CandidateApprovalTarget]): String =
    candidate.decisions.lastOption.filter(event => selected.contains(event.target)).map(_.decision.value) match {
      case Some("review") => "Reviewing"
      case Some("approve") => "Approved"
      case Some("hold") => "Held"
      case Some("reject") => "Rejected"
      case _ => "Draft"
    }

  private def _revision(snapshot: EntityRevisionCarrier[entity.InformationCandidate], expected: Long): Consequence[Unit] =
    if (expected == snapshot.revision.value) Consequence.success(())
    else Consequence.stateConflict("Candidate revision differs from the observed revision")

  private def _reason(reason: String): Consequence[Unit] =
    if (reason.trim.nonEmpty) Consequence.success(())
    else Consequence.argumentFieldInvalid("reason", "nonblank decision reason", reason)

  private def _references(candidate: entity.InformationCandidate): Consequence[Unit] = {
    val references = candidate.evidenceRefs ++ candidate.groundingRefs ++ candidate.mappingRefs
    val valid = candidate.evidenceRefs.nonEmpty && references.forall { reference =>
      reference.id.trim.nonEmpty && reference.kind.trim.nonEmpty &&
        Vector(reference.revision, reference.snapshotRef, reference.retrievalState,
          reference.retentionUntil, reference.limitation).flatten.forall(_.trim.nonEmpty)
    }
    if (valid) Consequence.success(())
    else Consequence.argumentFieldInvalid("evidenceRefs", "nonempty evidence and nonblank supplied reference fields", "invalid references")
  }
}
