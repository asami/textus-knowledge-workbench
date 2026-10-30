package org.simplemodeling.textus.knowledgeworkbench.impl

import cats.syntax.all.*
import org.simplemodeling.textus.knowledgeworkbench.{KnowledgeWorkbenchComponent, entity, value}
import org.goldenport.cncf.component.{Component, ComponentCreate, ComponentId}
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.entity.runtime.EntityQueryFieldResolver
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record

/*
 * @since   Oct.  3, 2026
 * @version Oct. 30, 2026
 */
final class ComponentFactory extends Component.BundleFactory {
  def primaryFactory: Component.PrimaryComponentFactory =
    KnowledgeWorkbenchPrimaryFactory

  override def componentletFactories: Vector[Component.ComponentletFactory] =
    Vector.empty
}

abstract class KnowledgeWorkbenchParticipantFactoryBase extends KnowledgeWorkbenchComponent.Factory {
  protected final val shared_services =
    Vector(
      KnowledgeWorkbenchComponent.CandidateService,
      KnowledgeWorkbenchComponent.SemanticWorkService,
      KnowledgeWorkbenchComponent.MaterialWorkService,
      KnowledgeWorkbenchComponent.AggregateService,
      KnowledgeWorkbenchComponent.ViewService,
      KnowledgeWorkbenchComponent.EntityService
    )

  protected final def component_core(
    name: String,
    componentId: ComponentId
  ): Component.Core =
    spec_create(name, componentId, shared_services)

  override val SemanticWork: KnowledgeWorkbenchComponent.SemanticWorkServiceFactory = new SemanticFormationWorkServiceFactory

  override val Candidate: KnowledgeWorkbenchComponent.CandidateServiceFactory = DefaultCandidateServiceFactory()
  override val MaterialWork: KnowledgeWorkbenchComponent.MaterialWorkServiceFactory = MaterialWorkServiceFactory()
  override val aggregate: KnowledgeWorkbenchComponent.AggregateServiceFactory = AggregateServiceFactoryImpl()
  override val view: KnowledgeWorkbenchComponent.ViewServiceFactory = ViewServiceFactoryImpl()
  override val entity: KnowledgeWorkbenchComponent.EntityServiceFactory = DefaultEntityServiceFactory()
}

final class KnowledgeWorkbenchPrimaryComponent extends KnowledgeWorkbenchComponent with CandidateDisplayRuntime {
  override def componentDescriptors: Vector[org.goldenport.cncf.component.ComponentDescriptor] =
    super.componentDescriptors.map { descriptor =>
      descriptor.copy(entityRuntimeDescriptors = descriptor.entityRuntimeDescriptors.map { declaration =>
        if ((declaration.collectionId == org.simplemodeling.textus.knowledgeworkbench.entity.InformationCandidate.collectionId ||
            declaration.collectionId == org.simplemodeling.textus.knowledgeworkbench.entity.SemanticFormationWork.collectionId))
          declaration.copy(
            revisionRepresentation = Some(org.goldenport.cncf.entity.EntityRevisionRepresentation.Detached),
            concurrencyPolicy = Some(org.goldenport.cncf.entity.EntityConcurrencyPolicy.Optimistic))
        else declaration
      })
    }

  override def mcpReadyServices: Set[String] =
    Set.empty
}

object KnowledgeWorkbenchPrimaryFactory extends KnowledgeWorkbenchParticipantFactoryBase with Component.PrimaryComponentFactory {
  override protected def create_Component(params: ComponentCreate): Component =
    new KnowledgeWorkbenchPrimaryComponent()

  override protected def create_Core(
    params: ComponentCreate,
    comp: Component
  ): Component.Core =
    component_core(KnowledgeWorkbenchComponent.name, KnowledgeWorkbenchComponent.componentId)
}

final class DefaultCandidateServiceFactory extends KnowledgeWorkbenchComponent.CandidateServiceFactory {
  import KnowledgeWorkbenchComponent.CandidateService.*
  import DefaultCandidateServiceFactory.*

  override def createAdmitCandidateWorkActionCall(
    core: org.goldenport.cncf.action.ActionCall.Core,
    action: AdmitCandidateWork
  ): AdmitCandidateWorkActionCall = CandidateAdmissionActions.admit(core, action)

  override def createConfirmCandidateAdmissionActionCall(
    core: org.goldenport.cncf.action.ActionCall.Core,
    action: ConfirmCandidateAdmission
  ): ConfirmCandidateAdmissionActionCall = CandidateAdmissionActions.confirm(core, action)

  override def createGetCandidateWorkActionCall(
    core: org.goldenport.cncf.action.ActionCall.Core,
    action: CandidateWorkSelection
  ): GetCandidateWorkActionCall = CandidateWorkActions.get(core, action)

  override def createEditCandidateWorkActionCall(
    core: org.goldenport.cncf.action.ActionCall.Core,
    action: CandidateWorkEdit
  ): EditCandidateWorkActionCall = CandidateWorkActions.edit(core, action)

  override def createDecideCandidateWorkActionCall(
    core: org.goldenport.cncf.action.ActionCall.Core,
    action: CandidateWorkDecision
  ): DecideCandidateWorkActionCall = CandidateWorkActions.decide(core, action)

  override def createRegisterCandidateActionCall(
    core: org.goldenport.cncf.action.ActionCall.Core,
    action: RegisterCandidate
  ): RegisterCandidateActionCall =
    RegisterCandidateActionCallImpl(core, action)

  override def createListCandidatesActionCall(
    core: org.goldenport.cncf.action.ActionCall.Core,
    action: ListCandidates
  ): ListCandidatesActionCall =
    ListCandidatesActionCallImpl(core, action)

  override def createRegisterProposalActionCall(
    core: org.goldenport.cncf.action.ActionCall.Core,
    action: RegisterProposal
  ): RegisterProposalActionCall =
    RegisterProposalActionCallImpl(core, action)

  override def createFindCandidateByProposalActionCall(
    core: org.goldenport.cncf.action.ActionCall.Core,
    action: FindCandidateByProposal
  ): FindCandidateByProposalActionCall =
    FindCandidateByProposalActionCallImpl(core, action)
}

object DefaultCandidateServiceFactory {
  import KnowledgeWorkbenchComponent.CandidateService.*

  def apply(): DefaultCandidateServiceFactory = new DefaultCandidateServiceFactory()

  private final case class RegisterProposalActionCallImpl(
    core: org.goldenport.cncf.action.ActionCall.Core,
    override val action: RegisterProposal
  ) extends RegisterProposalActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        input <- exec_from(value.RegisterProposal.createC(action.request.toRecord))
        proposal <- exec_from(CandidateProposal.parse(input.proposalJson.value))
        query <- exec_from(CandidateProposal.key(proposal.producerref, proposal.proposalid))
        rewrittenquery = EntityQueryFieldResolver(core.component, "InformationCandidate").rewrite(query)
        matches <- entity_search[entity.InformationCandidate](entity.query.InformationCandidate.collectionId, rewrittenquery)
        current <- exec_from(CandidateProposal.current(matches.data))
        output <- current match {
          case Some(candidate) => exec_pure(value.RegisterProposalResult(candidate, false))
          case None =>
            for {
              created <- entity_create(proposal.candidate)
              candidate <- entity_load[entity.InformationCandidate](created.id)
            } yield value.RegisterProposalResult(candidate, true)
        }
      } yield OperationResponse(output.toRecord())
  }

  private final case class FindCandidateByProposalActionCallImpl(
    core: org.goldenport.cncf.action.ActionCall.Core,
    override val action: FindCandidateByProposal
  ) extends FindCandidateByProposalActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        input <- exec_from(value.FindCandidateByProposal.createC(action.request.toRecord))
        query <- exec_from(CandidateProposal.key(input.producerRef.value, input.proposalId.value))
        rewrittenquery = EntityQueryFieldResolver(core.component, "InformationCandidate").rewrite(query)
        matches <- entity_search[entity.InformationCandidate](entity.query.InformationCandidate.collectionId, rewrittenquery)
        candidate <- exec_from(CandidateProposal.current(matches.data))
      } yield OperationResponse(value.FindCandidateByProposalResult(candidate).toRecord())
  }

  private final case class RegisterCandidateActionCallImpl(
    core: org.goldenport.cncf.action.ActionCall.Core,
    override val action: RegisterCandidate
  ) extends RegisterCandidateActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        candidate <- exec_from(entity.create.InformationCandidate.createC(action.request.toRecord))
        result <- entity_create(candidate)
        output <- exec_from(value.RegisterCandidateResult.createC(action.request.toRecord ++ result.toRecord))
      } yield OperationResponse(output.toRecord())
  }

  private final case class ListCandidatesActionCallImpl(
    core: org.goldenport.cncf.action.ActionCall.Core,
    override val action: ListCandidates
  ) extends ListCandidatesActionCall {
    protected def build_Program: ExecUowM[OperationResponse] =
      for {
        input <- exec_from(value.ListCandidates.createC(action.request.toRecord))
        query = Query.plan(
          Record.empty,
          where = input.text.map(t => Query.Or(Vector(
            Query.Contains("name", t.value, caseInsensitive = true),
            Query.Contains("description", t.value, caseInsensitive = true)
          ))).getOrElse(Query.True),
          offset = input.offset,
          limit = input.limit
        )
        rewrittenquery = EntityQueryFieldResolver(core.component, "InformationCandidate").rewrite(query)
        result <- entity_search[entity.InformationCandidate](entity.query.InformationCandidate.collectionId, rewrittenquery)
      } yield OperationResponse(value.ListCandidatesResult(result.data).toRecord())
  }
}

final class DefaultEntityServiceFactory extends KnowledgeWorkbenchComponent.EntityServiceFactory {
  import KnowledgeWorkbenchComponent.EntityService.*
  override def createCreateSemanticFormationWorkActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: CreateSemanticFormationWorkCommand): CreateSemanticFormationWorkActionCall = SemanticFormationEntityGuard.createSemanticFormationWork(core, action)
  override def createCreateInformationCandidateActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: CreateInformationCandidateCommand): CreateInformationCandidateActionCall =
    { val owningcore = core; val owningaction = action
    if (org.simplemodeling.model.datatype.EntityId.createC(action.request.toRecord).toOption.exists(_.collection == entity.SemanticFormationWork.collectionId))
      new CreateInformationCandidateActionCall {
        val core: org.goldenport.cncf.action.ActionCall.Core = owningcore
        override val action: CreateInformationCandidateCommand = owningaction
        protected def build_Program: ExecUowM[OperationResponse] = exec_from(org.goldenport.Consequence.securityPermissionDenied("Semantic work mutations require the owning service"))
      }
    else super.createCreateInformationCandidateActionCall(core, action)
    }
  override def createCreateSemanticFormationWorkRecordActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: CreateSemanticFormationWorkRecordCommand): CreateSemanticFormationWorkRecordActionCall = SemanticFormationEntityGuard.createSemanticFormationWorkRecord(core, action)
  override def createCreateInformationCandidateRecordActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: CreateInformationCandidateRecordCommand): CreateInformationCandidateRecordActionCall =
    { val owningcore = core; val owningaction = action
    if (org.simplemodeling.model.datatype.EntityId.createC(action.request.toRecord).toOption.exists(_.collection == entity.SemanticFormationWork.collectionId))
      new CreateInformationCandidateRecordActionCall {
        val core: org.goldenport.cncf.action.ActionCall.Core = owningcore
        override val action: CreateInformationCandidateRecordCommand = owningaction
        protected def build_Program: ExecUowM[OperationResponse] = exec_from(org.goldenport.Consequence.securityPermissionDenied("Semantic work mutations require the owning service"))
      }
    else super.createCreateInformationCandidateRecordActionCall(core, action)
    }
  override def createSaveSemanticFormationWorkActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: SaveSemanticFormationWorkCommand): SaveSemanticFormationWorkActionCall = SemanticFormationEntityGuard.saveSemanticFormationWork(core, action)
  override def createSaveInformationCandidateActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: SaveInformationCandidateCommand): SaveInformationCandidateActionCall =
    { val owningcore = core; val owningaction = action
    if (org.simplemodeling.model.datatype.EntityId.createC(action.request.toRecord).toOption.exists(_.collection == entity.SemanticFormationWork.collectionId))
      new SaveInformationCandidateActionCall {
        val core: org.goldenport.cncf.action.ActionCall.Core = owningcore
        override val action: SaveInformationCandidateCommand = owningaction
        protected def build_Program: ExecUowM[OperationResponse] = exec_from(org.goldenport.Consequence.securityPermissionDenied("Semantic work mutations require the owning service"))
      }
    else super.createSaveInformationCandidateActionCall(core, action)
    }
  override def createSaveSemanticFormationWorkRecordActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: SaveSemanticFormationWorkRecordCommand): SaveSemanticFormationWorkRecordActionCall = SemanticFormationEntityGuard.saveSemanticFormationWorkRecord(core, action)
  override def createSaveInformationCandidateRecordActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: SaveInformationCandidateRecordCommand): SaveInformationCandidateRecordActionCall =
    { val owningcore = core; val owningaction = action
    if (org.simplemodeling.model.datatype.EntityId.createC(action.request.toRecord).toOption.exists(_.collection == entity.SemanticFormationWork.collectionId))
      new SaveInformationCandidateRecordActionCall {
        val core: org.goldenport.cncf.action.ActionCall.Core = owningcore
        override val action: SaveInformationCandidateRecordCommand = owningaction
        protected def build_Program: ExecUowM[OperationResponse] = exec_from(org.goldenport.Consequence.securityPermissionDenied("Semantic work mutations require the owning service"))
      }
    else super.createSaveInformationCandidateRecordActionCall(core, action)
    }
  override def createUpdateSemanticFormationWorkActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: UpdateSemanticFormationWorkCommand): UpdateSemanticFormationWorkActionCall = SemanticFormationEntityGuard.updateSemanticFormationWork(core, action)
  override def createUpdateInformationCandidateActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: UpdateInformationCandidateCommand): UpdateInformationCandidateActionCall =
    { val owningcore = core; val owningaction = action
    if (org.simplemodeling.model.datatype.EntityId.createC(action.request.toRecord).toOption.exists(_.collection == entity.SemanticFormationWork.collectionId))
      new UpdateInformationCandidateActionCall {
        val core: org.goldenport.cncf.action.ActionCall.Core = owningcore
        override val action: UpdateInformationCandidateCommand = owningaction
        protected def build_Program: ExecUowM[OperationResponse] = exec_from(org.goldenport.Consequence.securityPermissionDenied("Semantic work mutations require the owning service"))
      }
    else super.createUpdateInformationCandidateActionCall(core, action)
    }
  override def createUpdateSemanticFormationWorkRecordActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: UpdateSemanticFormationWorkRecordCommand): UpdateSemanticFormationWorkRecordActionCall = SemanticFormationEntityGuard.updateSemanticFormationWorkRecord(core, action)
  override def createUpdateInformationCandidateRecordActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: UpdateInformationCandidateRecordCommand): UpdateInformationCandidateRecordActionCall =
    { val owningcore = core; val owningaction = action
    if (org.simplemodeling.model.datatype.EntityId.createC(action.request.toRecord).toOption.exists(_.collection == entity.SemanticFormationWork.collectionId))
      new UpdateInformationCandidateRecordActionCall {
        val core: org.goldenport.cncf.action.ActionCall.Core = owningcore
        override val action: UpdateInformationCandidateRecordCommand = owningaction
        protected def build_Program: ExecUowM[OperationResponse] = exec_from(org.goldenport.Consequence.securityPermissionDenied("Semantic work mutations require the owning service"))
      }
    else super.createUpdateInformationCandidateRecordActionCall(core, action)
    }
  override def createDeleteSemanticFormationWorkActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: DeleteSemanticFormationWorkCommand): DeleteSemanticFormationWorkActionCall = SemanticFormationEntityGuard.deleteSemanticFormationWork(core, action)
  override def createDeleteInformationCandidateActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: DeleteInformationCandidateCommand): DeleteInformationCandidateActionCall =
    { val owningcore = core; val owningaction = action
    if (org.simplemodeling.model.datatype.EntityId.createC(action.request.toRecord).toOption.exists(_.collection == entity.SemanticFormationWork.collectionId))
      new DeleteInformationCandidateActionCall {
        val core: org.goldenport.cncf.action.ActionCall.Core = owningcore
        override val action: DeleteInformationCandidateCommand = owningaction
        protected def build_Program: ExecUowM[OperationResponse] = exec_from(org.goldenport.Consequence.securityPermissionDenied("Semantic work mutations require the owning service"))
      }
    else super.createDeleteInformationCandidateActionCall(core, action)
    }
  override def createDeleteSemanticFormationWorkHardActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: DeleteSemanticFormationWorkHardCommand): DeleteSemanticFormationWorkHardActionCall = SemanticFormationEntityGuard.deleteSemanticFormationWorkHard(core, action)
  override def createDeleteInformationCandidateHardActionCall(core: org.goldenport.cncf.action.ActionCall.Core, action: DeleteInformationCandidateHardCommand): DeleteInformationCandidateHardActionCall =
    { val owningcore = core; val owningaction = action
    if (org.simplemodeling.model.datatype.EntityId.createC(action.request.toRecord).toOption.exists(_.collection == entity.SemanticFormationWork.collectionId))
      new DeleteInformationCandidateHardActionCall {
        val core: org.goldenport.cncf.action.ActionCall.Core = owningcore
        override val action: DeleteInformationCandidateHardCommand = owningaction
        protected def build_Program: ExecUowM[OperationResponse] = exec_from(org.goldenport.Consequence.securityPermissionDenied("Semantic work mutations require the owning service"))
      }
    else super.createDeleteInformationCandidateHardActionCall(core, action)
    }
}

object DefaultEntityServiceFactory {
  def apply(): DefaultEntityServiceFactory = new DefaultEntityServiceFactory()
  }

final class AggregateServiceFactoryImpl extends KnowledgeWorkbenchComponent.AggregateServiceFactory

object AggregateServiceFactoryImpl {
  def apply(): AggregateServiceFactoryImpl = new AggregateServiceFactoryImpl()
}

final class ViewServiceFactoryImpl extends KnowledgeWorkbenchComponent.ViewServiceFactory

object ViewServiceFactoryImpl {
  def apply(): ViewServiceFactoryImpl = new ViewServiceFactoryImpl()
}
