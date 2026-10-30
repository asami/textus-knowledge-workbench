package org.simplemodeling.textus.knowledgeworkbench.impl

import java.time.Instant
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.cncf.entity.EntityRevisionCarrier
import org.goldenport.record.Record
import org.simplemodeling.model.datatype.{EntityCollectionId, EntityId}
import org.simplemodeling.textus.knowledgeworkbench.{entity, value}

/*
 * Workbench owns decisions; SemanticFormationTarget is a typed wire binding of
 * the Hub-owned semantic contract, never an alternative canonical Knowledge.
 * @since   Oct. 30, 2026
 * @version Oct. 30, 2026
 */
object SemanticFormationWork {
  val resultCollection: EntityCollectionId = EntityCollectionId("textus", "knowledge_hub", "KnowledgeMaterialization")

  def result(snapshot: EntityRevisionCarrier[entity.SemanticFormationWork]): value.SemanticWorkResult = {
    val work = snapshot.entity
    val state = work.decisions.lastOption.filter(event => work.target.contains(event.target)).map(_.decision) match {
      case Some("review") => "Reviewing"
      case Some("approve") => "Approved"
      case Some("reject") => "Rejected"
      case _ => "Draft"
    }
    value.SemanticWorkResult(work, snapshot.revision.value, state,
      state == "Approved" && work.target.exists(_.outcome == "known"))
  }

  def revision(snapshot: EntityRevisionCarrier[entity.SemanticFormationWork], expectedRevision: Long): Consequence[Unit] =
    if (snapshot.revision.value == expectedRevision) Consequence.unit
    else Consequence.stateConflict("Semantic work revision differs from the observed revision")

  def correct(snapshot: EntityRevisionCarrier[entity.SemanticFormationWork], target: value.SemanticFormationTarget,
    expectedRevision: Long, reason: String, actor: String, instant: Instant): Consequence[entity.SemanticFormationWork] = for {
    _ <- revision(snapshot, expectedRevision)
    _ <- _reason(reason)
    _ <- if (target.informationId == snapshot.entity.informationId && target.informationRevision == snapshot.entity.informationRevision)
      Consequence.unit else Consequence.stateConflict[Unit]("Correction cannot replace the immutable source Information")
    _ <- if (snapshot.entity.formations.lastOption.forall(_.status == "Confirmed")) Consequence.unit
      else Consequence.stateConflict[Unit]("Reconcile the retained formation before changing its target")
  } yield if (snapshot.entity.target.contains(target)) snapshot.entity else snapshot.entity.copy(target = Some(target),
    decisions = snapshot.entity.decisions :+ value.SemanticFormationDecision("correct", actor, instant.toString, reason, target))

  def decide(snapshot: EntityRevisionCarrier[entity.SemanticFormationWork], input: value.SemanticWorkDecision,
    actor: String, instant: Instant): Consequence[entity.SemanticFormationWork] = for {
    _ <- revision(snapshot, input.expectedRevision)
    _ <- _reason(input.reason)
    target <- snapshot.entity.target.fold[Consequence[value.SemanticFormationTarget]](
      Consequence.stateConflict("Select and save a semantic target before review"))(Consequence.success)
    current = result(snapshot)
    allowed = input.decision match {
      case "review" => target.outcome == "known" && (current.state == "Draft" || current.state == "Rejected")
      case "approve" => target.outcome == "known" && current.state == "Reviewing"
      case "reject" => true
      case _ => false
    }
    _ <- if (allowed) Consequence.unit else Consequence.stateConflict[Unit]("Decision is not eligible for the current semantic target")
    _ <- if (snapshot.entity.formations.lastOption.forall(_.status == "Confirmed")) Consequence.unit
      else Consequence.stateConflict[Unit]("Reconcile the retained formation before changing its decision")
  } yield snapshot.entity.copy(decisions = snapshot.entity.decisions :+
    value.SemanticFormationDecision(input.decision, actor, instant.toString, input.reason, target))

  def reserve(snapshot: EntityRevisionCarrier[entity.SemanticFormationWork], expectedRevision: Long,
    identity: => EntityId): Consequence[entity.SemanticFormationWork] = for {
    _ <- revision(snapshot, expectedRevision)
    _ <- if (result(snapshot).approvalValid) Consequence.unit
      else Consequence.stateConflict[Unit]("Formation requires current explicit exact-target semantic approval")
    approval <- snapshot.entity.decisions.lastOption.fold[Consequence[value.SemanticFormationDecision]](
      Consequence.stateConflict("Semantic approval is missing"))(Consequence.success)
    _ <- if (!snapshot.entity.formations.exists(_.decision == approval)) Consequence.unit
      else Consequence.stateConflict[Unit]("This approval already has a retained formation identity; confirm it without resubmission")
  } yield snapshot.entity.copy(formations = snapshot.entity.formations :+
    value.SemanticFormationReservation(identity, approval.target, approval, "Requested", None))

  def observe(work: entity.SemanticFormationWork, reservation: value.SemanticFormationReservation): entity.SemanticFormationWork =
    work.copy(formations = work.formations.map(previous => if (previous.id == reservation.id) reservation else previous))

  def confirm(work: entity.SemanticFormationWork, reservation: value.SemanticFormationReservation,
    response: Record): Consequence[entity.SemanticFormationWork] = for {
    json <- response.toJsonStringC.flatMap(text => io.circe.parser.parse(text).fold(
      _ => Consequence.stateConflict[Json]("Invalid canonical formation acknowledgement"), Consequence.success))
    revision <- json.hcursor.get[Long]("revision").toOption.filter(_ > 0).fold[Consequence[Long]](
      Consequence.stateConflict("Canonical result revision is missing"))(Consequence.success)
    _ <- if (json.hcursor.get[String]("id").toOption.contains(reservation.id.value) &&
        json.hcursor.get[String]("workId").toOption.contains(work.id.value) &&
        json.hcursor.get[String]("informationId").toOption.contains(reservation.target.informationId.value) &&
        json.hcursor.get[Long]("informationRevision").toOption.contains(reservation.target.informationRevision) &&
        json.hcursor.get[String]("contextId").toOption.contains(reservation.target.contextId) &&
        json.hcursor.get[String]("contextRevision").toOption.contains(reservation.target.contextRevision) &&
        json.hcursor.get[String]("decisionActor").toOption.contains(reservation.decision.actor) &&
        json.hcursor.get[String]("decisionAt").toOption.contains(reservation.decision.occurredAt) &&
        json.hcursor.get[String]("decisionReason").toOption.contains(reservation.decision.reason)) Consequence.unit
      else Consequence.stateConflict[Unit]("Canonical result differs from the retained identity/source/context/decision")
  } yield observe(work, reservation.copy(status = "Confirmed", resultRevision = Some(revision)))

  private def _reason(reason: String): Consequence[Unit] =
    if (reason.trim.nonEmpty) Consequence.unit else Consequence.argumentFieldInvalid("reason", "nonblank explicit reason", reason)
}
