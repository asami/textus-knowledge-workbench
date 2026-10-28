package org.simplemodeling.textus.knowledgeworkbench.impl

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.cncf.action.ActionCall
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.simplemodeling.textus.knowledgeworkbench.{KnowledgeWorkbenchComponent, value}

/*
 * @since   Oct. 21, 2026
 * @version Oct. 28, 2026
 */
private[knowledgeworkbench] final class MaterialWorkServiceFactory
    extends KnowledgeWorkbenchComponent.MaterialWorkServiceFactory {
  import KnowledgeWorkbenchComponent.MaterialWorkService.*
  import MaterialWorkServiceFactory.*

  override def createCreateCandidateFromMaterialActionCall(
    core: ActionCall.Core,
    action: CreateCandidateFromMaterial
  ): CreateCandidateFromMaterialActionCall = MaterialCandidateWorkActions.create(core, action)

  override def createRefreshCandidateMaterialEvidenceActionCall(
    core: ActionCall.Core,
    action: RefreshCandidateMaterialEvidence
  ): RefreshCandidateMaterialEvidenceActionCall = MaterialEvidenceWorkActions.refresh(core, action)

  override def createSearchMaterialsActionCall(
    core: ActionCall.Core,
    action: MaterialSearchQuery
  ): SearchMaterialsActionCall = SearchMaterialsActionCallImpl(core, action)

  override def createInspectMaterialActionCall(
    core: ActionCall.Core,
    action: MaterialSelectionQuery
  ): InspectMaterialActionCall = InspectMaterialActionCallImpl(core, action)

  override def createReadOriginalActionCall(
    core: ActionCall.Core,
    action: OriginalSelectionQuery
  ): ReadOriginalActionCall = ReadOriginalActionCallImpl(core, action)

  override def createCreateMaterialRegistrationActionCall(
    core: ActionCall.Core,
    action: MaterialRegistrationCreateCommand
  ): CreateMaterialRegistrationActionCall = CreateMaterialRegistrationActionCallImpl(core, action)

  override def createGetMaterialRegistrationActionCall(
    core: ActionCall.Core,
    action: MaterialRegistrationSelectionQuery
  ): GetMaterialRegistrationActionCall = GetMaterialRegistrationActionCallImpl(core, action)

  override def createMaterializeMaterialActionCall(
    core: ActionCall.Core,
    action: MaterialRegistrationMaterializeCommand
  ): MaterializeMaterialActionCall = MaterializeMaterialActionCallImpl(core, action)
}

private[knowledgeworkbench] object MaterialWorkServiceFactory {
  import KnowledgeWorkbenchComponent.MaterialWorkService.*

  def apply(): MaterialWorkServiceFactory = new MaterialWorkServiceFactory()

  private def _dispatch(
    core: ActionCall.Core,
    operation: String,
    properties: List[Property]
  ): Consequence[OperationResponse] = {
    val request = Request.of(
      component = "org.simplemodeling.textus.KnowledgeLake",
      service = "Material",
      operation = operation,
      properties = properties
    )
    core.component.flatMap(_.subsystem) match {
      case Some(subsystem) =>
        subsystem.executeOperationResponseInChildContext(request, core.executionContext)
      case None => Consequence.serviceUnavailable("Material work owning subsystem is unavailable")
    }
  }

  private final case class SearchMaterialsActionCallImpl(
    core: ActionCall.Core,
    override val action: MaterialSearchQuery
  ) extends SearchMaterialsActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = {
      for {
        input <- exec_from(value.MaterialSearchQuery.createC(action.request.toRecord))
        properties = input.text.map(Property("text", _, None)).toList ++
          input.offset.map(Property("offset", _, None)).toList ++
          input.limit.map(Property("limit", _, None)).toList
        response <- exec_from(_dispatch(core, "searchMaterials", properties))
      } yield response
    }
  }

  private final case class InspectMaterialActionCallImpl(
    core: ActionCall.Core,
    override val action: MaterialSelectionQuery
  ) extends InspectMaterialActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = {
      for {
        input <- exec_from(value.MaterialSelectionQuery.createC(action.request.toRecord))
        response <- exec_from(_dispatch(core, "getMaterial", List(Property("materialRef", input.materialRef, None))))
      } yield response
    }
  }

  private final case class ReadOriginalActionCallImpl(
    core: ActionCall.Core,
    override val action: OriginalSelectionQuery
  ) extends ReadOriginalActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = {
      for {
        input <- exec_from(value.OriginalSelectionQuery.createC(action.request.toRecord))
        response <- exec_from(_dispatch(core, "readOriginal", List(
          Property("materialRef", input.materialRef, None),
          Property("resourceId", input.resourceId, None)
        )))
      } yield response
    }
  }

  private final case class CreateMaterialRegistrationActionCallImpl(
    core: ActionCall.Core,
    override val action: MaterialRegistrationCreateCommand
  ) extends CreateMaterialRegistrationActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = {
      for {
        response <- exec_from(_dispatch(core, "createMaterialRegistration", List(Property("contribution", action.contribution, None))))
      } yield response
    }
  }

  private final case class GetMaterialRegistrationActionCallImpl(
    core: ActionCall.Core,
    override val action: MaterialRegistrationSelectionQuery
  ) extends GetMaterialRegistrationActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = {
      for {
        input <- exec_from(value.MaterialRegistrationSelectionQuery.createC(action.request.toRecord))
        response <- exec_from(_dispatch(core, "getMaterialRegistration", List(Property("registrationId", input.registrationId, None))))
      } yield response
    }
  }

  private final case class MaterializeMaterialActionCallImpl(
    core: ActionCall.Core,
    override val action: MaterialRegistrationMaterializeCommand
  ) extends MaterializeMaterialActionCall {
    protected def build_Program: ExecUowM[OperationResponse] = {
      for {
        input <- exec_from(value.MaterialRegistrationMaterializeCommand.createC(action.request.toRecord))
        response <- exec_from(_dispatch(core, "materializeMaterial", List(Property("registrationId", input.registrationId, None))))
      } yield response
    }
  }
}
