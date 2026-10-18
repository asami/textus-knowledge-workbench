package org.simplemodeling.textus.knowledgeworkbench.impl

import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.cncf.directive.Query
import org.goldenport.record.Record
import org.goldenport.record.io.RecordDecoder
import org.simplemodeling.textus.knowledgeworkbench.entity

/*
 * @since   Oct. 14, 2026
 * @version Oct. 18, 2026
 */
private[impl] object CandidateProposal {
  final case class Parsed(
    producerref: String,
    proposalid: String,
    candidate: entity.create.InformationCandidate
  )

  def parse(json: String): Consequence[Parsed] =
    _validate_json_shape(json).flatMap { _ =>
      new RecordDecoder().json(json).flatMap { record =>
        Consequence {
          val producerref = _required_string(record, "producerRef")
          val proposalid = _required_string(record, "proposalId")
          val sourcekind = _required_string(record, "sourceKind")
          val name = _required_string(record, "name")
          val content = _required_string(record, "content")
          val preparation = _required_string(record, "preparationState")
          require(preparation == "prepared" || preparation == "unprepared",
            "preparationState must be prepared or unprepared")
          val evidence = _references(record, "evidenceRefs", required = true)
          require(evidence.nonEmpty, "evidenceRefs must be nonempty")
          val materials = _references(record, "preparedMaterialRefs", required = false)
          require(preparation != "prepared" || materials.nonEmpty,
            "prepared proposals require preparedMaterialRefs")
          val context = _optional_string(record, "context", nonblank = false)
          val run = _optional_reference(record, "sourceRunRef")
          val sourcecontext = _optional_reference(record, "sourceContextRef")
          if (record.keySet.contains("captureConfirmed"))
            require(record.getAny("captureConfirmed").exists(_.isInstanceOf[Boolean]),
              "captureConfirmed must be a Boolean")
          Record.dataAuto(
            "producerRef" -> producerref,
            "proposalId" -> proposalid,
            "sourceKind" -> sourcekind,
            "name" -> name,
            "content" -> content,
            "context" -> context,
            "preparationState" -> preparation,
            "originalProposal" -> json,
            "evidenceRefs" -> evidence,
            "preparedMaterialRefs" -> materials,
            "sourceRunRef" -> run,
            "sourceContextRef" -> sourcecontext
          )
        }.flatMap { input =>
          entity.create.InformationCandidate.createC(input).map { candidate =>
            Parsed(input.getString("producerRef").get, input.getString("proposalId").get, candidate)
          }
        }
      }
    }

  def key(producerref: String, proposalid: String): Consequence[Query[?]] =
    Consequence {
      require(producerref.trim.nonEmpty, "producerRef must be a nonblank string")
      require(proposalid.trim.nonEmpty, "proposalId must be a nonblank string")
      Query.plan(Record.empty, where = Query.And(Vector(
        Query.Eq("producerRef", producerref),
        Query.Eq("proposalId", proposalid)
      )), limit = Some(2))
    }

  def current(candidates: Vector[entity.InformationCandidate]): Consequence[Option[entity.InformationCandidate]] =
    if (candidates.size > 1)
      Consequence.failure("Ambiguous proposal correspondence: more than one accessible candidate matches producerRef/proposalId")
    else
      Consequence.success(candidates.headOption)

  private def _validate_json_shape(json: String): Consequence[Unit] =
    Consequence {
      val document = io.circe.parser.parse(json).fold(throw _, identity)
      val objectvalue = document.asObject.getOrElse(
        throw new IllegalArgumentException("proposalJson must be an object")
      )
      def reference(input: Json, field: String): Unit = {
        val fields = input.asObject.getOrElse(
          throw new IllegalArgumentException(s"$field must contain reference objects")
        )
        Vector("id", "kind").foreach { name =>
          require(fields(name).flatMap(_.asString).exists(_.trim.nonEmpty),
            s"$field.$name must be a nonblank string")
        }
        Vector("revision", "snapshotRef", "retrievalState", "retentionUntil", "limitation").foreach { name =>
          fields(name).foreach { value =>
            require(value.asString.exists(_.trim.nonEmpty),
              s"$field.$name must be a nonblank string")
          }
        }
      }
      Vector("evidenceRefs", "preparedMaterialRefs").foreach { field =>
        objectvalue(field).foreach { value =>
          val items = value.asArray.getOrElse(
            throw new IllegalArgumentException(s"$field must be an array of references")
          )
          items.foreach(reference(_, field))
        }
      }
      Vector("sourceRunRef", "sourceContextRef").foreach { field =>
        objectvalue(field).foreach(reference(_, field))
      }
      objectvalue("context").foreach(value =>
        require(value.isString, "context must be a string"))
      objectvalue("captureConfirmed").foreach(value =>
        require(value.isBoolean, "captureConfirmed must be a Boolean"))
    }

  private def _required_string(record: Record, field: String): String =
    record.getAny(field) match {
      case Some(text: String) if text.trim.nonEmpty => text
      case _ => throw new IllegalArgumentException(s"$field must be a nonblank string")
    }

  private def _optional_string(record: Record, field: String, nonblank: Boolean = true): Option[String] =
    if (!record.keySet.contains(field)) None
    else record.getAny(field) match {
      case Some(text: String) if !nonblank || text.trim.nonEmpty => Some(text)
      case _ => throw new IllegalArgumentException(s"$field must be ${if (nonblank) "a nonblank" else "a"} string")
    }

  private def _reference(input: Any, field: String): Record = input match {
    case record: Record =>
      val id = _required_string(record, "id")
      val kind = _required_string(record, "kind")
      val optional = Vector("revision", "snapshotRef", "retrievalState", "retentionUntil", "limitation")
        .map(name => name -> _optional_string(record, name))
      Record.dataAuto((Vector("id" -> id, "kind" -> kind) ++ optional)*)
    case _ => throw new IllegalArgumentException(s"$field must contain reference objects")
  }

  private def _references(record: Record, field: String, required: Boolean): Vector[Record] =
    if (!required && !record.keySet.contains(field)) Vector.empty
    else record.getAny(field) match {
      case Some(items: Vector[?]) => items.map(item => _reference(item, field))
      case _ => throw new IllegalArgumentException(s"$field must be an array of references")
    }

  private def _optional_reference(record: Record, field: String): Option[Record] =
    if (!record.keySet.contains(field)) None
    else Some(_reference(record.getAny(field).getOrElse(null), field))
}
