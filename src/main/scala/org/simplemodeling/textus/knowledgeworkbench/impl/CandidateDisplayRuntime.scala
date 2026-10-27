package org.simplemodeling.textus.knowledgeworkbench.impl

import org.goldenport.Consequence
import org.goldenport.cncf.component.{Component, ComponentInit}
import org.goldenport.cncf.context.ExecutionContext
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.display.*
import org.goldenport.cncf.entity.*
import org.goldenport.cncf.entity.view.{Browser, ContextualViewBuilder, ViewCollection}
import org.goldenport.cncf.security.EntityAccessMode
import org.goldenport.cncf.unitofwork.{UnitOfWorkAuthorization, UnitOfWorkInterpreter, UnitOfWorkOp}
import org.simplemodeling.model.datatype.EntityId
import org.simplemodeling.textus.knowledgeworkbench.{entity, value}
import org.simplemodeling.textus.knowledgeworkbench.logicalui.KnowledgeWorkbenchLogicalUiMetadataProvider

/*
 * @since   Oct. 27, 2026
 * @version Oct. 27, 2026
 */
trait CandidateDisplayRuntime extends Component with KnowledgeWorkbenchLogicalUiMetadataProvider
    with DisplayBindingProvider with DisplayServiceBindingProvider with DisplayBusinessOperationBindingProvider {
  private val _candidate_browser = new CandidateBrowser(this)

  override protected def initialize_component_c(params: ComponentInit): Consequence[Unit] =
    super.initialize_component_c(params).map { _ =>
      val collection = new ViewCollection(new ContextualViewBuilder[value.CandidateWorkResult] {
        def build_with_context(id: EntityId)(using ctx: ExecutionContext): Consequence[value.CandidateWorkResult] =
          _candidate_browser.find_with_context(id)
      })
      viewSpace.register("candidate_work", collection, _candidate_browser)
    }

  def displayBindings(component: Component): Vector[DisplayRuntime.Binding] = Vector(
    DisplayRuntime.list("candidate-list", "candidate.work", _candidate_browser, CandidateDisplay.listRow, Vector.empty),
    DisplayRuntime.detail("candidate-detail", "candidate.work", _candidate_browser, CandidateDisplay.detailRow))

  def displayServiceBindings(component: Component): Vector[DisplayService.ResourceBinding] =
    Vector("candidate-list", "candidate-detail").map(DisplayService.ResourceBinding("information-candidate", _))

  def displayBusinessOperationBindings(component: Component): Vector[DisplayBusinessOperation.Binding] = Vector(
    DisplayBusinessOperation.Binding("candidate-detail", DisplayBusinessOperation.ActionScope.Screen,
      "edit", "Candidate", "editCandidateWork"),
    DisplayBusinessOperation.Binding("candidate-detail", DisplayBusinessOperation.ActionScope.Screen,
      "decide", "Candidate", "decideCandidateWork"))
}

private final class CandidateBrowser(component: Component) extends Browser[value.CandidateWorkResult] {
  private val _component = component

  def find(id: EntityId): Consequence[value.CandidateWorkResult] =
    Consequence.operationInvalid("Candidate Browser requires native execution context")
  def query(q: Query[?]): Consequence[Vector[value.CandidateWorkResult]] =
    Consequence.operationInvalid("Candidate Browser requires native execution context")

  override def find_with_context(id: EntityId)(using ctx: ExecutionContext): Consequence[value.CandidateWorkResult] =
    if (id.collection != entity.InformationCandidate.collectionId)
      Consequence.argumentFieldInvalid("resourceId", "InformationCandidate collection", id.value)
    else _persistent.flatMap { persistent =>
      new UnitOfWorkInterpreter(ctx.unitOfWork).interpret(UnitOfWorkOp.EntityStoreLoadDetached(id,
        persistent, Some(_authorization("read", Some(id)))))
        .flatMap(found => Consequence.successOrEntityNotFound(found)(id)).map(CandidateFormation.result)
    }

  override def query_with_context(q: Query[?])(using ctx: ExecutionContext): Consequence[Vector[value.CandidateWorkResult]] =
    _persistent.flatMap { persistent =>
      new UnitOfWorkInterpreter(ctx.unitOfWork).interpret(UnitOfWorkOp.EntityStoreSearchDirect(
        EntityQuery[entity.InformationCandidate](entity.InformationCandidate.collectionId, q,
          EntitySearchScope.Store, Some(EntityVisibilityScope.Owner)), persistent,
        Some(_authorization("search/list", None)))).flatMap { found =>
          found.data.foldLeft(Consequence.success(Vector.empty[value.CandidateWorkResult])) { (acc, candidate) =>
            acc.flatMap(values => find_with_context(candidate.id).map(values :+ _))
          }
        }
    }

  private def _persistent(using ctx: ExecutionContext): Consequence[EntityPersistent[entity.InformationCandidate]] =
    ctx.entitySpace.entityByNameC[entity.InformationCandidate](entity.InformationCandidate.collectionId.name).map(_.descriptor.persistent)

  private def _authorization(kind: String, identity: Option[EntityId]): UnitOfWorkAuthorization =
    UnitOfWorkAuthorization(resourceFamily = "domain", resourceType = Some("InformationCandidate"),
      collectionName = Some(entity.InformationCandidate.collectionId.name), targetId = identity, accessKind = kind,
      sourceComponentName = Some(_component.name), targetComponentName = Some(_component.name),
      entityNames = Vector("InformationCandidate"), accessMode = EntityAccessMode.UserPermission,
      visibilityScope = Some(EntityVisibilityScope.Owner))
}
