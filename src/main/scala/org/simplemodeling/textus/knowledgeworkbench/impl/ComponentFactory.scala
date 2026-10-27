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
 * @version Oct. 27, 2026
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

  override val Candidate: KnowledgeWorkbenchComponent.CandidateServiceFactory = DefaultCandidateServiceFactory()
  override val MaterialWork: KnowledgeWorkbenchComponent.MaterialWorkServiceFactory = MaterialWorkServiceFactory()
  override val aggregate: KnowledgeWorkbenchComponent.AggregateServiceFactory = AggregateServiceFactoryImpl()
  override val view: KnowledgeWorkbenchComponent.ViewServiceFactory = ViewServiceFactoryImpl()
  override val entity: KnowledgeWorkbenchComponent.EntityServiceFactory = DefaultEntityServiceFactory()
}

final class KnowledgeWorkbenchPrimaryComponent extends KnowledgeWorkbenchComponent {
  override def componentDescriptors: Vector[org.goldenport.cncf.component.ComponentDescriptor] =
    super.componentDescriptors.map { descriptor =>
      descriptor.copy(entityRuntimeDescriptors = descriptor.entityRuntimeDescriptors.map { declaration =>
        if (declaration.collectionId == org.simplemodeling.textus.knowledgeworkbench.entity.InformationCandidate.collectionId)
          declaration.copy(revisionRepresentation = Some(org.goldenport.cncf.entity.EntityRevisionRepresentation.Detached))
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

final class DefaultEntityServiceFactory extends KnowledgeWorkbenchComponent.EntityServiceFactory

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
