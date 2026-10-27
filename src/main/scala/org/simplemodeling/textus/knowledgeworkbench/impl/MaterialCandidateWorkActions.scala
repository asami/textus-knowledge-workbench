package org.simplemodeling.textus.knowledgeworkbench.impl

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.simplemodeling.textus.knowledgeworkbench.{KnowledgeWorkbenchComponent, value}

/*
 * @since   Oct. 28, 2026
 * @version Oct. 28, 2026
 */
private[knowledgeworkbench] object MaterialCandidateWorkActions {
  import KnowledgeWorkbenchComponent.MaterialWorkService.*

  def create(core: ActionCall.Core, action: CreateCandidateFromMaterial): CreateCandidateFromMaterialActionCall =
    CreateCandidateFromMaterialActionCallImpl(core, action)

  private def _dispatch(core: ActionCall.Core, service: String, operation: String,
      properties: List[Property]): Consequence[OperationResponse] = {
    val request = Request.of(component = KnowledgeWorkbenchComponent.name,
      service = service, operation = operation, properties = properties)
    core.component.flatMap(_.subsystem) match {
      case Some(subsystem) => subsystem.executeOperationResponseInChildContext(request, core.executionContext)
      case None => Consequence.serviceUnavailable("Material work owning subsystem is unavailable")
    }
  }

  private final case class CreateCandidateFromMaterialActionCallImpl(
    core: ActionCall.Core,
    override val action: CreateCandidateFromMaterial
  ) extends CreateCandidateFromMaterialActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        input <- exec_from(MaterialCandidateWork.read(action.request.toRecord))
        _ <- exec_from(MaterialCandidateWork.validate(input))
        lookup <- exec_from(_dispatch(core, "Candidate", "findCandidateByProposal", List(
          Property("producerRef", MaterialCandidateWork.producerRef, None), Property("proposalId", input.proposalId, None))))
        record <- exec_from(MaterialCandidateWork.record(lookup))
        found <- exec_from(value.FindCandidateByProposalResult.createC(record))
        response <- found.candidate match {
          case Some(candidate) => exec_pure(OperationResponse(value.RegisterProposalResult(candidate, false).toRecord()))
          case None =>
            for {
              metadata <- exec_from(_dispatch(core, "MaterialWork", "inspectMaterial", List(Property("materialRef", input.materialRef, None))))
              observed <- exec_from(MaterialCandidateWork.record(metadata))
              proposal <- exec_from(MaterialCandidateWork.proposal(input, observed))
              registered <- exec_from(_dispatch(core, "Candidate", "registerProposal", List(Property("proposalJson", proposal, None))))
              confirmed <- exec_from(MaterialCandidateWork.confirmed(registered))
            } yield confirmed
        }
      } yield response
  }
}
