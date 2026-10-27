package org.simplemodeling.textus.knowledgeworkbench.impl

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.knowledgeworkbench.{KnowledgeWorkbenchComponent, entity, value}

/*
 * @since   Oct. 28, 2026
 * @version Oct. 28, 2026
 */
private[impl] object CandidateAdmissionActions {
  import KnowledgeWorkbenchComponent.CandidateService.*

  def admit(core: ActionCall.Core, action: AdmitCandidateWork): AdmitCandidateWorkActionCall =
    AdmitAction(core, action)

  def confirm(core: ActionCall.Core, action: ConfirmCandidateAdmission): ConfirmCandidateAdmissionActionCall =
    ConfirmAction(core, action)

  private def _actor(core: ActionCall.Core): Consequence[String] = {
    val subject = SecuritySubject.from(core.executionContext.security)
    if (subject.isAuthenticated) Consequence.success(subject.subjectId)
    else Consequence.securityAuthenticationRequired("Candidate Admission requires an authenticated user")
  }

  private def _dispatch(core: ActionCall.Core, operation: String, properties: List[Property]): Consequence[Record] =
    core.component.flatMap(_.subsystem) match {
      case None => Consequence.serviceUnavailable("KnowledgeHub owning subsystem is unavailable")
      case Some(subsystem) => subsystem.executeOperationResponseInChildContext(Request.of(
        component = "org.simplemodeling.textus.KnowledgeHub", service = "Information",
        operation = operation, properties = properties), core.executionContext).flatMap {
          case OperationResponse.RecordResponse(record) => Consequence.success(record)
          case _ => Consequence.stateConflict("KnowledgeHub did not return a confirmed Information result")
        }
    }

  private final case class AdmitAction(core: ActionCall.Core, override val action: AdmitCandidateWork)
      extends AdmitCandidateWorkActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        actor <- exec_from(_actor(core))
        input <- exec_from(value.AdmitCandidateWork.createC(action.request.toRecord))
        snapshot <- entity_load_detached[entity.InformationCandidate](input.id)
        candidate <- exec_from(CandidateAdmission.begin(snapshot, input.expectedRevision,
          entity_id(CandidateAdmission.informationCollection, "information-admission"), actor, current_instant))
        // The native conditional mutation persists the reservation before the owning call.
        reserved <- entity_update_detached(candidate, snapshot.revision)
        admission = reserved.entity.admissions.last
        request = CandidateAdmission.request(input.id, admission)
        observed = _dispatch(core, "admitInformation", request.asMap.toList.map {
          case (name, input) => Property(name, input, None)
        })
        changed = observed.flatMap(result => CandidateAdmission.confirm(reserved.entity, admission, result))
        retained = changed match {
          case Consequence.Success(candidate) => candidate
          case Consequence.Failure(_) => CandidateAdmission.observe(reserved.entity, admission.copy(status = "Unconfirmed"))
        }
        persisted <- entity_update_detached(retained, reserved.revision)
        _ <- exec_from(changed.map(_ => ()))
      } yield OperationResponse(CandidateFormation.result(persisted).toRecord())
  }

  private final case class ConfirmAction(core: ActionCall.Core, override val action: ConfirmCandidateAdmission)
      extends ConfirmCandidateAdmissionActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(_actor(core))
        input <- exec_from(value.ConfirmCandidateAdmission.createC(action.request.toRecord))
        snapshot <- entity_load_detached[entity.InformationCandidate](input.id)
        _ <- exec_from(CandidateAdmission.revision(snapshot, input.expectedRevision))
        admission <- exec_from(snapshot.entity.admissions.lastOption.map(Consequence.success).getOrElse(
          Consequence.stateConflict("Candidate has no reserved Information identity to confirm")))
        received <- exec_from(_dispatch(core, "getInformation", List(Property("id", admission.informationId, None))))
        changed <- exec_from(CandidateAdmission.confirm(snapshot.entity, admission, received))
        persisted <- if (changed == snapshot.entity) exec_pure(snapshot)
          else entity_update_detached(changed, snapshot.revision)
      } yield OperationResponse(CandidateFormation.result(persisted).toRecord())
  }
}
