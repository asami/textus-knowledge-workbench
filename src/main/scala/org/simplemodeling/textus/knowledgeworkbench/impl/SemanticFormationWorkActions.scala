package org.simplemodeling.textus.knowledgeworkbench.impl

import cats.syntax.all.*
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.cncf.action.{ActionCall, FunctionalActionCall}
import org.goldenport.cncf.directive.Query
import org.goldenport.cncf.entity.runtime.EntityQueryFieldResolver
import org.goldenport.cncf.security.SecuritySubject
import org.goldenport.cncf.unitofwork.ExecUowM
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.goldenport.schema.XSemiStructuredData
import org.simplemodeling.textus.knowledgeworkbench.{KnowledgeWorkbenchComponent, entity, value}
import KnowledgeWorkbenchComponent.SemanticWorkService.*

/*
 * @since   Oct. 30, 2026
 * @version Oct. 30, 2026
 */
object SemanticFormationWorkActions {
  def open(core: ActionCall.Core, action: SemanticWorkOpen): OpenSemanticWorkActionCall = OpenCall(core, action)
  def get(core: ActionCall.Core, action: SemanticWorkSelection): GetSemanticWorkActionCall = GetCall(core, action)
  def correct(core: ActionCall.Core, action: SemanticWorkCorrection): CorrectSemanticWorkActionCall = CorrectCall(core, action)
  def decide(core: ActionCall.Core, action: SemanticWorkDecision): DecideSemanticWorkActionCall = DecideCall(core, action)
  def form(core: ActionCall.Core, action: SemanticWorkFormation): FormSemanticWorkActionCall = FormCall(core, action)
  def confirm(core: ActionCall.Core, action: ConfirmSemanticFormation): ConfirmSemanticFormationActionCall = ConfirmCall(core, action)

  private final case class OpenCall(core: ActionCall.Core, override val action: SemanticWorkOpen)
      extends OpenSemanticWorkActionCall with SemanticWorkCallSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      _ <- exec_from(semanticActor())
      input <- exec_from(value.SemanticWorkOpen.createC(action.request.toRecord))
      query = EntityQueryFieldResolver(core.component, "SemanticFormationWork").rewrite(
        Query.plan(Record.empty, where = Query.Eq("informationId", input.informationId.value), limit = Some(2)))
      _ <- use_component_application_datastore()
      matches <- entity_search[entity.SemanticFormationWork](entity.query.SemanticFormationWork.collectionId, query)
      _ <- exec_from(if (matches.data.size <= 1) Consequence.unit else Consequence.stateConflict[Unit]("Multiple semantic works require explicit selection"))
      snapshot <- matches.data.headOption match {
        case Some(existing) => entity_load_detached[entity.SemanticFormationWork](existing.id)
        case None => for {
          proposed <- exec_from(semanticHub("proposeSemantics", Record.dataAuto("informationId" -> input.informationId.value)))
          json <- exec_from(semanticJson(proposed))
          revision <- exec_from(json.hcursor.get[Long]("informationRevision").toOption.filter(_ > 0)
            .fold[Consequence[Long]](Consequence.stateConflict("Hub did not return saved Information revision"))(Consequence.success))
          target <- exec_from(json.hcursor.get[Vector[Json]]("alternatives").toOption.flatMap(_.headOption) match {
            case Some(selected) => XSemiStructuredData.fromJson(selected).flatMap(value.SemanticFormationTarget.createC).map(Some(_))
            case None => Consequence.success(Option.empty[value.SemanticFormationTarget])
          })
          candidate <- exec_from(entity.create.SemanticFormationWork.createC(Record.dataAuto("informationId" -> input.informationId,
            "informationRevision" -> revision, "target" -> target.map(_.toRecord()), "decisions" -> Vector.empty[Record], "formations" -> Vector.empty[Record])))
          _ <- use_component_application_datastore()
          created <- entity_create(candidate)
          saved <- entity_load_detached[entity.SemanticFormationWork](created.id)
        } yield saved
      }
    } yield OperationResponse(SemanticFormationWork.result(snapshot).toRecord())
  }
  private final case class GetCall(core: ActionCall.Core, override val action: SemanticWorkSelection)
      extends GetSemanticWorkActionCall with SemanticWorkCallSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      _ <- exec_from(semanticActor())
      input <- exec_from(value.SemanticWorkSelection.createC(action.request.toRecord))
      _ <- use_component_application_datastore()
      snapshot <- entity_load_detached[entity.SemanticFormationWork](input.id)
    } yield OperationResponse(SemanticFormationWork.result(snapshot).toRecord())
  }
  private final case class CorrectCall(core: ActionCall.Core, override val action: SemanticWorkCorrection)
      extends CorrectSemanticWorkActionCall with SemanticWorkCallSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      actor <- exec_from(semanticActor())
      input <- exec_from(value.SemanticWorkCorrection.createC(action.request.toRecord))
      _ <- use_component_application_datastore()
      snapshot <- entity_load_detached[entity.SemanticFormationWork](input.id)
      _ <- exec_from(SemanticFormationWork.revision(snapshot, input.expectedRevision))
      corrected = input.target.copy(provider = "manual-correction")
      validated <- exec_from(semanticHub("validateSemanticTarget", Record.dataAuto("target" -> corrected.toRecord())))
      target <- exec_from(value.SemanticFormationTarget.createC(validated))
      changed <- exec_from(SemanticFormationWork.correct(snapshot, target, input.expectedRevision, input.reason, actor, current_instant))
      _ <- use_component_application_datastore()
      saved <- if (changed == snapshot.entity) exec_pure(snapshot) else entity_update_detached(changed, snapshot.revision)
    } yield OperationResponse(SemanticFormationWork.result(saved).toRecord())
  }
  private final case class DecideCall(core: ActionCall.Core, override val action: SemanticWorkDecision)
      extends DecideSemanticWorkActionCall with SemanticWorkCallSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      actor <- exec_from(semanticActor())
      input <- exec_from(value.SemanticWorkDecision.createC(action.request.toRecord))
      _ <- use_component_application_datastore()
      snapshot <- entity_load_detached[entity.SemanticFormationWork](input.id)
      changed <- exec_from(SemanticFormationWork.decide(snapshot, input, actor, current_instant))
      // Review/approval revalidates the actual current owning source and context.
      _ <- exec_from(changed.target.fold[Consequence[Unit]](Consequence.unit)(target =>
        semanticHub("validateSemanticTarget", Record.dataAuto("target" -> target.toRecord())).map(_ => ())))
      _ <- use_component_application_datastore()
      saved <- entity_update_detached(changed, snapshot.revision)
    } yield OperationResponse(SemanticFormationWork.result(saved).toRecord())
  }
  private final case class FormCall(core: ActionCall.Core, override val action: SemanticWorkFormation)
      extends FormSemanticWorkActionCall with SemanticWorkCallSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      _ <- exec_from(semanticActor())
      input <- exec_from(value.SemanticWorkFormation.createC(action.request.toRecord))
      _ <- use_component_application_datastore()
      snapshot <- entity_load_detached[entity.SemanticFormationWork](input.id)
      changed <- exec_from(SemanticFormationWork.reserve(snapshot, input.expectedRevision,
        entity_id(SemanticFormationWork.resultCollection, "knowledge-formation")))
      reserved <- entity_update_detached(changed, snapshot.revision)
      reservation = reserved.entity.formations.last
      observed = semanticHub("formKnowledge", Record.dataAuto("workId" -> input.id.value, "expectedWorkRevision" -> reserved.revision.value))
      confirmed = observed.flatMap(response => SemanticFormationWork.confirm(reserved.entity, reservation, response))
      retained = confirmed.toOption.getOrElse(SemanticFormationWork.observe(reserved.entity, reservation.copy(status = "Unconfirmed")))
      _ <- use_component_application_datastore()
      saved <- entity_update_detached(retained, reserved.revision)
      _ <- exec_from(confirmed.map(_ => ()))
    } yield OperationResponse(SemanticFormationWork.result(saved).toRecord())
  }
  private final case class ConfirmCall(core: ActionCall.Core, override val action: ConfirmSemanticFormation)
      extends ConfirmSemanticFormationActionCall with SemanticWorkCallSupport {
    protected def build_Program: ExecUowM[OperationResponse] = for {
      _ <- exec_from(semanticActor())
      input <- exec_from(value.ConfirmSemanticFormation.createC(action.request.toRecord))
      _ <- use_component_application_datastore()
      snapshot <- entity_load_detached[entity.SemanticFormationWork](input.id)
      _ <- exec_from(SemanticFormationWork.revision(snapshot, input.expectedRevision))
      reservation <- exec_from(snapshot.entity.formations.lastOption.fold[Consequence[value.SemanticFormationReservation]](
        Consequence.stateConflict("No retained Knowledge identity to confirm"))(Consequence.success))
      received <- exec_from(semanticHub("getFormedKnowledge", Record.dataAuto("id" -> reservation.id.value)))
      confirmed <- exec_from(SemanticFormationWork.confirm(snapshot.entity, reservation, received))
      _ <- use_component_application_datastore()
      saved <- if (confirmed == snapshot.entity) exec_pure(snapshot) else entity_update_detached(confirmed, snapshot.revision)
    } yield OperationResponse(SemanticFormationWork.result(saved).toRecord())
  }
}

private trait SemanticWorkCallSupport extends FunctionalActionCall {
  protected final def semanticActor(): Consequence[String] = {
    val subject = SecuritySubject.from(core.executionContext.security)
    if (subject.isAuthenticated) Consequence.success(subject.subjectId)
    else Consequence.securityAuthenticationRequired("Semantic work requires an authenticated caller")
  }
  protected final def semanticHub(operation: String, record: Record): Consequence[Record] =
    core.component.flatMap(_.subsystem) match {
      case Some(subsystem) => subsystem.executeOperationResponseInChildContext(Request.of(
        component = "org.simplemodeling.textus.KnowledgeHub", service = "KnowledgeFormation", operation = operation,
        properties = record.asMap.toList.map { case (key, input) => Property(key, input, None) }), core.executionContext).flatMap {
          case OperationResponse.RecordResponse(result) => Consequence.success(result)
          case _ => Consequence.stateConflict("Hub did not return an owning semantic result")
        }
      case None => Consequence.serviceUnavailable("Hub owning subsystem is unavailable")
    }
  protected final def semanticJson(record: Record): Consequence[Json] = record.toJsonStringC.flatMap(text =>
    io.circe.parser.parse(text).fold(_ => Consequence.stateConflict("Invalid Hub response"), Consequence.success))
}
