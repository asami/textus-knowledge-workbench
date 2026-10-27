package org.simplemodeling.textus.knowledgeworkbench.impl

import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.knowledgeworkbench.value

/*
 * @since   Oct. 28, 2026
 * @version Oct. 28, 2026
 */
private[knowledgeworkbench] object MaterialCandidateWork {
  val producerRef = "workbench-material"
  val sourceKind = "tkl-material"
  val limitation = "Material metadata does not attest original retrieval or a source revision/snapshot."

  def read(input: Record): Consequence[value.CreateCandidateFromMaterial] =
    Consequence {
      require(input.getAny("resourceIds").exists {
        case items: Vector[?] => items.forall(_.isInstanceOf[String])
        case _ => false
      }, "resourceIds must be a collection of strings")
      if (input.keySet.contains("context"))
        require(input.getAny("context").exists(_.isInstanceOf[String]), "context must be a string when present")
    }.flatMap(_ => value.CreateCandidateFromMaterial.createC(input))

  def validate(input: value.CreateCandidateFromMaterial): Consequence[Unit] = Consequence {
    require(input.proposalId.trim.nonEmpty, "proposalId must be nonblank")
    require(input.materialRef.trim.nonEmpty, "materialRef must be nonblank")
    require(input.resourceIds.nonEmpty, "resourceIds must be nonempty")
    require(input.resourceIds.forall(_.trim.nonEmpty), "resourceIds must contain nonblank strings")
    require(input.resourceIds.distinct.size == input.resourceIds.size, "resourceIds must be distinct")
    require(input.content.trim.nonEmpty, "content must be nonblank")
  }

  def record(response: OperationResponse): Consequence[Record] = response match {
    case OperationResponse.RecordResponse(record) => Consequence.success(record)
    case _ => Consequence.stateConflict("Material Candidate work requires an acknowledged logical Record response")
  }

  def proposal(input: value.CreateCandidateFromMaterial, metadata: Record): Consequence[String] =
    for {
      _ <- value.MaterialMetadata.createC(metadata)
      _ <- Consequence {
        require(metadata.getAny("materialRef").contains(input.materialRef), "Material metadata must match the exact selected materialRef")
        val resources = metadata.getAny("resources") match {
          case Some(items: Vector[?]) => items.map {
            case item: Record => item.getAny("resourceId") match {
              case Some(id: String) if id.trim.nonEmpty => id
              case _ => throw new IllegalArgumentException("Material resources require nonblank logical resourceId")
            }
            case _ => throw new IllegalArgumentException("Material resources must be logical records")
          }
          case _ => throw new IllegalArgumentException("Material metadata requires a logical resources collection")
        }
        require(resources.distinct.size == resources.size, "Material resource identities must be unambiguous")
        require(input.resourceIds.forall(resources.contains), "Every exact selected resourceId must exist in the selected Material")
      }
      text <- metadata.toJsonStringC
      observed <- Consequence(io.circe.parser.parse(text).fold(throw _, identity))
    } yield {
      val fields = Vector(
        "producerRef" -> Json.fromString(producerRef), "proposalId" -> Json.fromString(input.proposalId),
        "sourceKind" -> Json.fromString(sourceKind), "name" -> Json.fromString(input.name.value),
        "content" -> Json.fromString(input.content), "preparationState" -> Json.fromString("unprepared"),
        "evidenceRefs" -> Json.arr(Json.obj("id" -> Json.fromString(input.materialRef), "kind" -> Json.fromString("material"),
          "retrievalState" -> Json.fromString("unverified"), "limitation" -> Json.fromString(limitation))),
        "preparedMaterialRefs" -> Json.arr(),
        "materialSelection" -> Json.obj("materialRef" -> Json.fromString(input.materialRef),
          "resourceIds" -> Json.fromValues(input.resourceIds.map(Json.fromString)), "metadata" -> observed)
      ) ++ input.context.map(text => "context" -> Json.fromString(text))
      Json.obj(fields*).noSpaces
    }

  def confirmed(response: OperationResponse): Consequence[OperationResponse] =
    for {
      received <- record(response)
      result <- value.RegisterProposalResult.createC(received)
      _ <- if (result.candidate.id.print.trim.nonEmpty) Consequence.unit
           else Consequence.stateConflict("Candidate registration did not acknowledge a native identity")
    } yield OperationResponse(result.toRecord())
}
