package org.simplemodeling.textus.knowledgeworkbench.impl

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.simplemodeling.textus.knowledgeworkbench.{KnowledgeWorkbenchComponent, entity, value}

/*
 * @since   Oct. 28, 2026
 * @version Oct. 28, 2026
 */
private[knowledgeworkbench] object MaterialEvidenceWorkActions {
  import KnowledgeWorkbenchComponent.MaterialWorkService.*

  def refresh(core: ActionCall.Core, action: RefreshCandidateMaterialEvidence): RefreshCandidateMaterialEvidenceActionCall =
    RefreshAction(core, action)

  private def _actor(core: ActionCall.Core): Consequence[String] = {
    val subject = SecuritySubject.from(core.executionContext.security)
    if (subject.isAuthenticated) Consequence.success(subject.subjectId)
    else Consequence.securityAuthenticationRequired("Material evidence work requires an authenticated user")
  }

  private def _acquire(core: ActionCall.Core, selection: MaterialEvidenceWork.Selection): Consequence[OperationResponse] = {
    val request = Request.of(component = "org.simplemodeling.textus.KnowledgeLake", service = "Material",
      operation = "getMaterial", properties = List(Property("materialRef", selection.materialRef, None),
        Property("evidenceResourceIds", selection.resourceIds, None)))
    core.component.flatMap(_.subsystem) match {
      case Some(subsystem) => subsystem.executeOperationResponseInChildContext(request, core.executionContext)
      case None => Consequence.serviceUnavailable("Material work owning subsystem is unavailable")
    }
  }

  private final case class RefreshAction(core: ActionCall.Core, override val action: RefreshCandidateMaterialEvidence)
      extends RefreshCandidateMaterialEvidenceActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        actor <- exec_from(_actor(core))
        input <- exec_from(value.RefreshCandidateMaterialEvidence.createC(action.request.toRecord))
        snapshot <- entity_load_detached[entity.InformationCandidate](input.id)
        selection <- exec_from(MaterialEvidenceWork.prepare(snapshot, input))
        response <- exec_from(_acquire(core, selection))
        record <- exec_from(MaterialCandidateWork.record(response))
        resources <- exec_from(MaterialEvidenceWork.observe(selection, record))
        changed <- exec_from(MaterialEvidenceWork.applyEvidence(snapshot, input, selection, resources, actor, current_instant))
        persisted <- changed match {
          case Some(candidate) => entity_update_detached(candidate, snapshot.revision)
          case None =>
            for {
              current <- entity_load_detached[entity.InformationCandidate](input.id)
              _ <- exec_from(MaterialEvidenceWork.checkRevision(current, snapshot.revision.value))
            } yield current
        }
      } yield OperationResponse(CandidateFormation.result(persisted).toRecord())
  }
}
