package org.simplemodeling.textus.knowledgeworkbench.impl

import org.goldenport.Consequence
import org.goldenport.cncf.display.*
import org.goldenport.record.Record
import org.goldenport.cncf.entity.EntityRevisionCarrier
import org.simplemodeling.textus.knowledgeworkbench.value.RefreshCandidateMaterialEvidence
import org.simplemodeling.model.datatype.EntityRevision
import org.simplemodeling.textus.knowledgeworkbench.value.CandidateWorkResult

/*
 * @since   Oct. 27, 2026
 * @version Oct. 28, 2026
 */
private[knowledgeworkbench] object CandidateDisplay {
  import DisplayProjection.{ActionBinding, RowBinding, ValueBinding}

  def listRow: RowBinding[CandidateWorkResult] = RowBinding(_resource,
    Vector(_text("title", _.candidate.name.value), _text("state", _.state),
      _optional("source", _.candidate.sourceKind.map(_.value)),
      _optional("preparation", _.candidate.preparationState.map(_.value)),
      _boolean("eligible", _.reviewEligible)), Vector.empty)

  def detailRow: RowBinding[CandidateWorkResult] = RowBinding(_resource,
    listRow.values ++ Vector(_optional("content", _.candidate.content.map(_.value)),
      _optional("context", _.candidate.context.map(_.value)), _boolean("approved", _.approvalValid),
      _records("evidence", _.candidate.evidenceRefs.map(_.toRecord())),
      _records("material-evidence", _.candidate.materialEvidence.toVector.map(_.toRecord())),
      _records("materials", _.candidate.preparedMaterialRefs.map(_.toRecord())),
      _records("run", _.candidate.sourceRunRef.toVector.map(_.toRecord())),
      _records("source-context", _.candidate.sourceContextRef.toVector.map(_.toRecord())),
      _records("grounding", _.candidate.groundingRefs.map(_.toRecord())),
      _records("mapping", _.candidate.mappingRefs.map(_.toRecord())),
      _records("history", _.candidate.decisions.map(_.toRecord())),
      _records("admissions", _.candidate.admissions.map(_.toRecord())),
      _records("target", _.target.toVector.map(_.toRecord()))),
    Vector("edit", "decide").map(id => ActionBinding[CandidateWorkResult](s"candidate.$id",
      _ => Consequence.success(DisplayModel.ActionAvailability.Enabled))) ++ Vector(
      ActionBinding[CandidateWorkResult]("candidate.refresh-evidence", _refresh_availability),
      ActionBinding[CandidateWorkResult]("candidate.admit", (work: CandidateWorkResult) => Consequence.success(
        if (work.approvalValid && CandidateAdmission.current(work.candidate).isEmpty) DisplayModel.ActionAvailability.Enabled
        else DisplayModel.ActionAvailability.Disabled)),
      ActionBinding[CandidateWorkResult]("candidate.confirm-admission", (work: CandidateWorkResult) => Consequence.success(
        if (work.candidate.admissions.nonEmpty) DisplayModel.ActionAvailability.Enabled
        else DisplayModel.ActionAvailability.Disabled))))

  private def _refresh_availability(work: CandidateWorkResult): Consequence[DisplayModel.ActionAvailability] =
    for {
      revision <- EntityRevision.createC(work.revision)
      input <- RefreshCandidateMaterialEvidence.createC(Record.data("id" -> work.candidate.id,
        "expectedRevision" -> work.revision, "reason" -> "Inspect source evidence availability"))
    } yield {
      if (MaterialEvidenceWork.prepare(EntityRevisionCarrier(work.candidate, revision), input).toOption.nonEmpty)
        DisplayModel.ActionAvailability.Enabled
      else DisplayModel.ActionAvailability.Disabled
    }

  private def _resource(work: CandidateWorkResult): Consequence[DisplayModel.Resource] =
    EntityRevision.createC(work.revision).map(DisplayModel.Resource(work.candidate.id, _))

  private def _text(id: String, read: CandidateWorkResult => String): ValueBinding[CandidateWorkResult] =
    ValueBinding(Vector(s"read.$id"), work => Consequence.success(Vector(DisplayValue.Text(read(work)))))

  private def _optional(id: String, read: CandidateWorkResult => Option[String]): ValueBinding[CandidateWorkResult] =
    ValueBinding(Vector(s"read.$id"), work => Consequence.success(Vector(
      read(work).map(DisplayValue.Text.apply).getOrElse(DisplayValue.Missing))))

  private def _boolean(id: String, read: CandidateWorkResult => Boolean): ValueBinding[CandidateWorkResult] =
    ValueBinding(Vector(s"read.$id"), work => Consequence.success(Vector(DisplayValue.Boolean(read(work)))))

  private def _records(id: String, read: CandidateWorkResult => Vector[Record]): ValueBinding[CandidateWorkResult] =
    ValueBinding(Vector(s"read.$id"), work => Record.data("items" -> read(work)).toJsonStringC
      .map(text => Vector(DisplayValue.Text(text))))
}
