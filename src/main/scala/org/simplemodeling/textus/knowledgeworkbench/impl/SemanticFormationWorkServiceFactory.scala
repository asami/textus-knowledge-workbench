package org.simplemodeling.textus.knowledgeworkbench.impl

import org.goldenport.cncf.action.ActionCall
import org.simplemodeling.textus.knowledgeworkbench.KnowledgeWorkbenchComponent

/*
 * @since   Oct. 30, 2026
 * @version Oct. 30, 2026
 */
final class SemanticFormationWorkServiceFactory extends KnowledgeWorkbenchComponent.SemanticWorkServiceFactory {
  import KnowledgeWorkbenchComponent.SemanticWorkService.*
  override def createOpenSemanticWorkActionCall(core: ActionCall.Core, action: SemanticWorkOpen): OpenSemanticWorkActionCall = SemanticFormationWorkActions.open(core, action)
  override def createGetSemanticWorkActionCall(core: ActionCall.Core, action: SemanticWorkSelection): GetSemanticWorkActionCall = SemanticFormationWorkActions.get(core, action)
  override def createCorrectSemanticWorkActionCall(core: ActionCall.Core, action: SemanticWorkCorrection): CorrectSemanticWorkActionCall = SemanticFormationWorkActions.correct(core, action)
  override def createDecideSemanticWorkActionCall(core: ActionCall.Core, action: SemanticWorkDecision): DecideSemanticWorkActionCall = SemanticFormationWorkActions.decide(core, action)
  override def createFormSemanticWorkActionCall(core: ActionCall.Core, action: SemanticWorkFormation): FormSemanticWorkActionCall = SemanticFormationWorkActions.form(core, action)
  override def createConfirmSemanticFormationActionCall(core: ActionCall.Core, action: ConfirmSemanticFormation): ConfirmSemanticFormationActionCall = SemanticFormationWorkActions.confirm(core, action)
}

private object SemanticFormationEntityGuard {
  import KnowledgeWorkbenchComponent.EntityService.*
  def createSemanticFormationWork(core: ActionCall.Core, action: CreateSemanticFormationWorkCommand): CreateSemanticFormationWorkActionCall = CreateSemanticFormationWorkDenied(core, action)
  private final case class CreateSemanticFormationWorkDenied(core: ActionCall.Core, override val action: CreateSemanticFormationWorkCommand) extends CreateSemanticFormationWorkActionCall {
    protected def build_Program: org.goldenport.cncf.unitofwork.ExecUowM[org.goldenport.protocol.operation.OperationResponse] =
      exec_from(org.goldenport.Consequence.securityPermissionDenied("Use SemanticWork operations for exact-target decisions and formation"))
  }
  def createSemanticFormationWorkRecord(core: ActionCall.Core, action: CreateSemanticFormationWorkRecordCommand): CreateSemanticFormationWorkRecordActionCall = CreateSemanticFormationWorkRecordDenied(core, action)
  private final case class CreateSemanticFormationWorkRecordDenied(core: ActionCall.Core, override val action: CreateSemanticFormationWorkRecordCommand) extends CreateSemanticFormationWorkRecordActionCall {
    protected def build_Program: org.goldenport.cncf.unitofwork.ExecUowM[org.goldenport.protocol.operation.OperationResponse] =
      exec_from(org.goldenport.Consequence.securityPermissionDenied("Use SemanticWork operations for exact-target decisions and formation"))
  }
  def saveSemanticFormationWork(core: ActionCall.Core, action: SaveSemanticFormationWorkCommand): SaveSemanticFormationWorkActionCall = SaveSemanticFormationWorkDenied(core, action)
  private final case class SaveSemanticFormationWorkDenied(core: ActionCall.Core, override val action: SaveSemanticFormationWorkCommand) extends SaveSemanticFormationWorkActionCall {
    protected def build_Program: org.goldenport.cncf.unitofwork.ExecUowM[org.goldenport.protocol.operation.OperationResponse] =
      exec_from(org.goldenport.Consequence.securityPermissionDenied("Use SemanticWork operations for exact-target decisions and formation"))
  }
  def saveSemanticFormationWorkRecord(core: ActionCall.Core, action: SaveSemanticFormationWorkRecordCommand): SaveSemanticFormationWorkRecordActionCall = SaveSemanticFormationWorkRecordDenied(core, action)
  private final case class SaveSemanticFormationWorkRecordDenied(core: ActionCall.Core, override val action: SaveSemanticFormationWorkRecordCommand) extends SaveSemanticFormationWorkRecordActionCall {
    protected def build_Program: org.goldenport.cncf.unitofwork.ExecUowM[org.goldenport.protocol.operation.OperationResponse] =
      exec_from(org.goldenport.Consequence.securityPermissionDenied("Use SemanticWork operations for exact-target decisions and formation"))
  }
  def updateSemanticFormationWork(core: ActionCall.Core, action: UpdateSemanticFormationWorkCommand): UpdateSemanticFormationWorkActionCall = UpdateSemanticFormationWorkDenied(core, action)
  private final case class UpdateSemanticFormationWorkDenied(core: ActionCall.Core, override val action: UpdateSemanticFormationWorkCommand) extends UpdateSemanticFormationWorkActionCall {
    protected def build_Program: org.goldenport.cncf.unitofwork.ExecUowM[org.goldenport.protocol.operation.OperationResponse] =
      exec_from(org.goldenport.Consequence.securityPermissionDenied("Use SemanticWork operations for exact-target decisions and formation"))
  }
  def updateSemanticFormationWorkRecord(core: ActionCall.Core, action: UpdateSemanticFormationWorkRecordCommand): UpdateSemanticFormationWorkRecordActionCall = UpdateSemanticFormationWorkRecordDenied(core, action)
  private final case class UpdateSemanticFormationWorkRecordDenied(core: ActionCall.Core, override val action: UpdateSemanticFormationWorkRecordCommand) extends UpdateSemanticFormationWorkRecordActionCall {
    protected def build_Program: org.goldenport.cncf.unitofwork.ExecUowM[org.goldenport.protocol.operation.OperationResponse] =
      exec_from(org.goldenport.Consequence.securityPermissionDenied("Use SemanticWork operations for exact-target decisions and formation"))
  }
  def deleteSemanticFormationWork(core: ActionCall.Core, action: DeleteSemanticFormationWorkCommand): DeleteSemanticFormationWorkActionCall = DeleteSemanticFormationWorkDenied(core, action)
  private final case class DeleteSemanticFormationWorkDenied(core: ActionCall.Core, override val action: DeleteSemanticFormationWorkCommand) extends DeleteSemanticFormationWorkActionCall {
    protected def build_Program: org.goldenport.cncf.unitofwork.ExecUowM[org.goldenport.protocol.operation.OperationResponse] =
      exec_from(org.goldenport.Consequence.securityPermissionDenied("Use SemanticWork operations for exact-target decisions and formation"))
  }
  def deleteSemanticFormationWorkHard(core: ActionCall.Core, action: DeleteSemanticFormationWorkHardCommand): DeleteSemanticFormationWorkHardActionCall = DeleteSemanticFormationWorkHardDenied(core, action)
  private final case class DeleteSemanticFormationWorkHardDenied(core: ActionCall.Core, override val action: DeleteSemanticFormationWorkHardCommand) extends DeleteSemanticFormationWorkHardActionCall {
    protected def build_Program: org.goldenport.cncf.unitofwork.ExecUowM[org.goldenport.protocol.operation.OperationResponse] =
      exec_from(org.goldenport.Consequence.securityPermissionDenied("Use SemanticWork operations for exact-target decisions and formation"))
  }
}
