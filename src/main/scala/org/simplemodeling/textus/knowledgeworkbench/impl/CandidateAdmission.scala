package org.simplemodeling.textus.knowledgeworkbench.impl

import java.time.Instant
import org.goldenport.Consequence
import org.goldenport.cncf.entity.EntityRevisionCarrier
import org.simplemodeling.model.datatype.{EntityCollectionId, EntityId}
import org.goldenport.record.Record
import org.simplemodeling.textus.knowledgeworkbench.{entity, value}

/*
 * @since   Oct. 28, 2026
 * @version Oct. 28, 2026
 */
private[knowledgeworkbench] object CandidateAdmission {
  // KnowledgeHub contract revision 1; native identity generation remains CNCF-owned.
  val informationCollection: EntityCollectionId = EntityCollectionId("major", "minor", "information")
  def current(candidate: entity.InformationCandidate): Option[value.CandidateAdmissionRecord] =
    candidate.decisions.lastOption.filter(_.decision.value == "approve").flatMap { approval =>
      candidate.admissions.reverse.find(_.approval == approval)
    }

  def state(candidate: entity.InformationCandidate, humanState: String): String =
    if (humanState != "Approved") humanState
    else current(candidate).map(_.status) match {
      case Some("Confirmed") => "Admitted"
      case Some("Requested") => "AdmissionRequested"
      case Some(_) => "AdmissionUnconfirmed"
      case None => humanState
    }

  def begin(
    snapshot: EntityRevisionCarrier[entity.InformationCandidate],
    expectedRevision: Long,
    informationId: => EntityId,
    actor: String,
    instant: Instant
  ): Consequence[entity.InformationCandidate] = {
    val work = CandidateFormation.result(snapshot)
    for {
      _ <- revision(snapshot, expectedRevision)
      _ <- if (work.approvalValid) Consequence.unit
           else Consequence.stateConflict("Admission requires the exact current eligible human-approved target")
      _ <- if (current(snapshot.entity).isEmpty) Consequence.unit
           else Consequence.stateConflict("An Admission already exists for this approval; confirm its saved Information identity without resubmitting")
      approval <- snapshot.entity.decisions.lastOption.map(Consequence.success).getOrElse(
        Consequence.stateConflict("Admission has no attributed human approval"))
      pending = value.CandidateAdmissionRecord(informationId, snapshot.revision.value, approval,
        actor, instant.toString, "Requested", None)
    } yield snapshot.entity.copy(admissions = snapshot.entity.admissions :+ pending)
  }

  def revision(snapshot: EntityRevisionCarrier[entity.InformationCandidate], expectedRevision: Long): Consequence[Unit] =
    if (snapshot.revision.value == expectedRevision) Consequence.unit
    else Consequence.stateConflict("Candidate revision differs from the observed revision")

  def request(candidateId: EntityId, admission: value.CandidateAdmissionRecord): Record =
    Record.dataAuto("id" -> admission.informationId.print, "candidateId" -> candidateId.print,
      "candidateRevision" -> admission.candidateRevision, "approval" -> _approval(admission.approval),
      "target" -> admission.approval.target.toRecord())

  def confirm(
    candidate: entity.InformationCandidate,
    admission: value.CandidateAdmissionRecord,
    received: Record
  ): Consequence[entity.InformationCandidate] =
    for {
      expectedtext <- request(candidate.id, admission).toJsonStringC
      receivedtext <- received.toJsonStringC
      expected <- Consequence(io.circe.parser.parse(expectedtext).fold(throw _, identity))
      actual <- Consequence(io.circe.parser.parse(receivedtext).fold(throw _, identity))
      revision <- actual.hcursor.get[Long]("revision").toOption.filter(_ > 0).map(Consequence.success).getOrElse(
        Consequence.stateConflict("KnowledgeHub did not return a positive persisted Information revision"))
      _ <- if (actual.hcursor.downField("information").focus.contains(expected)) Consequence.unit
           else Consequence.stateConflict("KnowledgeHub Information differs from the reserved identity or exact approved Candidate trace")
    } yield observe(candidate, admission.copy(status = "Confirmed", informationRevision = Some(revision)))

  def observe(candidate: entity.InformationCandidate, admission: value.CandidateAdmissionRecord): entity.InformationCandidate =
    candidate.copy(admissions = candidate.admissions.map { previous =>
      if (previous.informationId == admission.informationId) admission else previous
    })

  private def _approval(approval: value.CandidateReviewDecision): Record =
    Record.dataAuto("actor" -> approval.actor, "occurredAt" -> approval.occurredAt,
      "reason" -> approval.reason.value)
}
