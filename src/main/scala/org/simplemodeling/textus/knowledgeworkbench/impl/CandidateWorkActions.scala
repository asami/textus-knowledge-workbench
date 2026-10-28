package org.simplemodeling.textus.knowledgeworkbench.impl

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.simplemodeling.textus.knowledgeworkbench.{KnowledgeWorkbenchComponent, entity, value}

/*
 * @since   Oct. 27, 2026
 * @version Oct. 28, 2026
 */
private[impl] object CandidateWorkActions {
  import KnowledgeWorkbenchComponent.CandidateService.*

  def get(core: ActionCall.Core, action: CandidateWorkSelection): GetCandidateWorkActionCall =
    GetAction(core, action)

  def edit(core: ActionCall.Core, action: CandidateWorkEdit): EditCandidateWorkActionCall =
    EditAction(core, action)

  def decide(core: ActionCall.Core, action: CandidateWorkDecision): DecideCandidateWorkActionCall =
    DecideAction(core, action)

  private def _actor(core: ActionCall.Core): Consequence[String] = {
    val subject = SecuritySubject.from(core.executionContext.security)
    if (subject.isAuthenticated) Consequence.success(subject.subjectId)
    else Consequence.securityAuthenticationRequired("Candidate work requires an authenticated user")
  }

  private final case class GetAction(core: ActionCall.Core, override val action: CandidateWorkSelection)
      extends GetCandidateWorkActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        _ <- exec_from(_actor(core))
        input <- exec_from(value.CandidateWorkSelection.createC(action.request.toRecord))
        snapshot <- entity_load_detached[entity.InformationCandidate](input.id)
      } yield OperationResponse(CandidateFormation.result(snapshot).toRecord())
  }

  private final case class EditAction(core: ActionCall.Core, override val action: CandidateWorkEdit)
      extends EditCandidateWorkActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        actor <- exec_from(_actor(core))
        input <- exec_from(value.CandidateWorkEdit.createC(action.request.toRecord))
        snapshot <- entity_load_detached[entity.InformationCandidate](input.id)
        changed <- exec_from(CandidateFormation.edit(snapshot, input, actor, current_instant))
        persisted <- changed match {
          case Some(candidate) => entity_update_detached(MaterialEvidenceWork.detachOnHumanEdit(snapshot.entity, candidate), snapshot.revision)
          case None => exec_pure(snapshot)
        }
      } yield OperationResponse(CandidateFormation.result(persisted).toRecord())
  }

  private final case class DecideAction(core: ActionCall.Core, override val action: CandidateWorkDecision)
      extends DecideCandidateWorkActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        actor <- exec_from(_actor(core))
        input <- exec_from(value.CandidateWorkDecision.createC(action.request.toRecord))
        snapshot <- entity_load_detached[entity.InformationCandidate](input.id)
        changed <- exec_from(CandidateFormation.decide(snapshot, input, actor, current_instant))
        persisted <- entity_update_detached(changed, snapshot.revision)
      } yield OperationResponse(CandidateFormation.result(persisted).toRecord())
  }
}
