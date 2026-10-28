package org.simplemodeling.textus.knowledgeworkbench.impl

import java.time.Instant
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.cncf.entity.EntityRevisionCarrier
import org.goldenport.record.Record
import org.simplemodeling.textus.knowledgeworkbench.{entity, value}

/*
 * @since   Oct. 28, 2026
 * @version Oct. 28, 2026
 */
private[knowledgeworkbench] object MaterialEvidenceWork {
  final case class Selection(materialRef: String, resourceIds: Vector[String], current: Vector[value.ProposalReference])

  def prepare(snapshot: EntityRevisionCarrier[entity.InformationCandidate],
      input: value.RefreshCandidateMaterialEvidence): Consequence[Selection] =
    for {
      _ <- checkRevision(snapshot, input.expectedRevision)
      _ <- if (input.reason.value.trim.nonEmpty) Consequence.unit
           else Consequence.argumentFieldInvalid("reason", "nonblank decision reason", input.reason.value)
      selection <- _original(snapshot.entity)
      current <- snapshot.entity.materialEvidence match {
        case None =>
          val reference = _original_reference(selection.materialRef)
          if (_projection(snapshot.entity.evidenceRefs, Vector(reference))) Consequence.success(Vector(reference))
          else Consequence.stateConflict("Original Material evidence was changed, removed or is ambiguous")
        case Some(binding) =>
          if (binding.status == "active" && binding.materialRef == selection.materialRef &&
              binding.resourceIds == selection.resourceIds && binding.applications.nonEmpty &&
              binding.applications.forall(application => _application(selection, application))) {
            val references = binding.applications.last.resources.map(_.appliedReference)
            if (_projection(snapshot.entity.evidenceRefs, references)) Consequence.success(references)
            else Consequence.stateConflict("Managed Material evidence projection was changed or is ambiguous")
          } else Consequence.stateConflict("Material evidence binding is detached or does not own the original selection")
      }
    } yield selection.copy(current = current)

  def checkRevision(snapshot: EntityRevisionCarrier[entity.InformationCandidate], expectedRevision: Long): Consequence[Unit] =
    if (snapshot.revision.value == expectedRevision) Consequence.unit
    else Consequence.stateConflict("Candidate revision differs from the observed revision")

  def observe(selection: Selection, response: Record): Consequence[Vector[value.MaterialResourceEvidence]] = {
    val observations = response.getAny("observedEvidence") match {
      case Some(items: Vector[?]) if items.forall(_.isInstanceOf[Record]) => Some(items.map(_.asInstanceOf[Record]))
      case _ => None
    }
    if (!response.getAny("materialRef").contains(selection.materialRef)) _invalid_observation
    else observations match {
      case Some(items) if items.size == selection.resourceIds.size =>
        items.zip(selection.resourceIds).foldLeft(Consequence.success(Vector.empty[value.MaterialResourceEvidence])) {
          case (accumulated, (item, resourceid)) =>
            accumulated.flatMap(resources => _observation(selection.materialRef, resourceid, item).map(resources :+ _))
        }.flatMap { resources =>
            if (resources.map(_.appliedReference.id).distinct.size == resources.size) Consequence.success(resources)
            else _invalid_observation
          }
      case _ => _invalid_observation
    }
  }

  def applyEvidence(snapshot: EntityRevisionCarrier[entity.InformationCandidate],
      input: value.RefreshCandidateMaterialEvidence, selection: Selection,
      resources: Vector[value.MaterialResourceEvidence], actor: String,
      instant: Instant): Consequence[Option[entity.InformationCandidate]] = {
    val references = resources.map(_.appliedReference)
    val unmanaged = snapshot.entity.evidenceRefs.filterNot(reference => selection.current.exists(_.id == reference.id))
    if (references.exists(reference => unmanaged.exists(_.id == reference.id)))
      Consequence.stateConflict("Observed Material evidence collides with a human reference")
    else if (references == selection.current) Consequence.success(None)
    else {
      val first = snapshot.entity.evidenceRefs.indexWhere(reference => selection.current.exists(_.id == reference.id))
      val replacement = snapshot.entity.evidenceRefs.zipWithIndex.flatMap { case (reference, index) =>
        if (index == first) references
        else if (selection.current.exists(_.id == reference.id)) Vector.empty
        else Vector(reference)
      }
      snapshot.entity.content match {
        case None => Consequence.stateConflict("Candidate content is required for Material evidence application")
        case Some(content) =>
          val edit = value.CandidateWorkEdit(snapshot.entity.id, input.expectedRevision, content,
            snapshot.entity.context, replacement, snapshot.entity.groundingRefs, snapshot.entity.mappingRefs, input.reason)
          CandidateFormation.edit(snapshot, edit, actor, instant).map(_.map { candidate =>
            val applications = snapshot.entity.materialEvidence.map(_.applications).getOrElse(Vector.empty)
            candidate.copy(materialEvidence = Some(value.MaterialEvidenceBinding(selection.materialRef,
              selection.resourceIds, "active", applications :+ value.MaterialEvidenceApplication(resources))))
          })
      }
    }
  }

  /** Only human-declared full-target edits may detach ownership. */
  def detachOnHumanEdit(before: entity.InformationCandidate,
      after: entity.InformationCandidate): entity.InformationCandidate = {
    val binding = before.materialEvidence.map { binding =>
      if (binding.status == "active" && !binding.applications.lastOption.exists(application =>
          _projection(after.evidenceRefs, application.resources.map(_.appliedReference)))) binding.copy(status = "detached")
      else binding
    }
    after.copy(materialEvidence = binding)
  }

  private def _original(candidate: entity.InformationCandidate): Consequence[Selection] = {
    val parsed = candidate.originalProposal.flatMap(proposal => io.circe.parser.parse(proposal.value).toOption)
    val selection = for {
      document <- parsed
      producer <- document.hcursor.get[String]("producerRef").toOption
      proposalid <- document.hcursor.get[String]("proposalId").toOption
      sourcekind <- document.hcursor.get[String]("sourceKind").toOption
      materialref <- document.hcursor.downField("materialSelection").get[String]("materialRef").toOption
      resourceids <- document.hcursor.downField("materialSelection").get[Vector[String]]("resourceIds").toOption
      metadata <- document.hcursor.downField("materialSelection").downField("metadata").focus
      metadataref <- metadata.hcursor.get[String]("materialRef").toOption
      metadataresources <- metadata.hcursor.get[Vector[Json]]("resources").toOption
      originalrefs <- document.hcursor.get[Vector[Json]]("evidenceRefs").toOption
      if producer == MaterialCandidateWork.producerRef && candidate.producerRef.exists(_.value == producer)
      if proposalid.trim.nonEmpty && candidate.proposalId.exists(_.value == proposalid)
      if sourcekind == MaterialCandidateWork.sourceKind && candidate.sourceKind.exists(_.value == sourcekind)
      if materialref.trim.nonEmpty && metadataref == materialref
      if resourceids.nonEmpty && resourceids.forall(_.trim.nonEmpty) && resourceids.distinct == resourceids
      metadataids = metadataresources.flatMap(_.hcursor.get[String]("resourceId").toOption)
      if metadataids.size == metadataresources.size && metadataids.forall(_.trim.nonEmpty) && metadataids.distinct == metadataids
      if resourceids.forall(metadataids.contains)
      if originalrefs == Vector(_original_json(materialref))
    } yield Selection(materialref, resourceids, Vector.empty)
    selection.map(Consequence.success).getOrElse(
      Consequence.stateConflict("Candidate does not retain an exact original Workbench Material selection"))
  }

  private def _original_reference(materialref: String): value.ProposalReference =
    value.ProposalReference(materialref, "material", None, None, Some("unverified"), None, Some(MaterialCandidateWork.limitation))

  private def _original_json(materialref: String): Json = Json.obj(
    "id" -> Json.fromString(materialref), "kind" -> Json.fromString("material"),
    "retrievalState" -> Json.fromString("unverified"), "limitation" -> Json.fromString(MaterialCandidateWork.limitation))

  private def _projection(all: Vector[value.ProposalReference], managed: Vector[value.ProposalReference]): Boolean = {
    val ids = managed.map(_.id)
    managed.nonEmpty && ids.distinct == ids && all.filter(reference => ids.contains(reference.id)) == managed
  }

  private def _application(selection: Selection, application: value.MaterialEvidenceApplication): Boolean =
    application.resources.map(_.resourceId) == selection.resourceIds &&
      application.resources.map(_.appliedReference.id).distinct.size == selection.resourceIds.size &&
      application.resources.forall(resource => _reference(resource.appliedReference))

  private def _reference(reference: value.ProposalReference): Boolean =
    reference.id.trim.nonEmpty && reference.kind == "material-resource" && reference.snapshotRef.isEmpty &&
      reference.retentionUntil.isEmpty && reference.limitation.exists(_.trim.nonEmpty) &&
      ((reference.retrievalState.contains("available") && reference.revision.exists(_.trim.nonEmpty)) ||
        (reference.retrievalState.contains("unverified") && reference.revision.isEmpty))

  private def _observation(materialref: String, resourceid: String, item: Record): Consequence[value.MaterialResourceEvidence] = {
    val reference = for {
      evidenceid <- _string(item, "evidenceRef")
      observedmaterial <- _string(item, "materialRef") if observedmaterial == materialref
      observedresource <- _string(item, "resourceId") if observedresource == resourceid
      state <- _string(item, "retrievalState")
      limitation <- _string(item, "limitation")
      if !item.keySet.contains("revision") || _string(item, "revision").nonEmpty
      if !item.keySet.contains("snapshotRef") && !item.keySet.contains("retentionUntil")
      reference = value.ProposalReference(evidenceid, "material-resource", _string(item, "revision"), None,
        Some(state), None, Some(limitation))
      if _reference(reference)
    } yield value.MaterialResourceEvidence(resourceid, reference)
    reference.map(Consequence.success).getOrElse(_invalid_observation)
  }

  private def _string(record: Record, key: String): Option[String] = record.getAny(key).collect {
    case text: String if text.trim.nonEmpty => text
  }

  private def _invalid_observation[A]: Consequence[A] =
    Consequence.resourceInvalid("Material evidence observation must exactly and completely correspond to the original selected resources")
}
