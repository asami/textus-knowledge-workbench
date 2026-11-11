package org.simplemodeling.textus.knowledgeworkbench

import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.simplemodeling.textus.knowledgeworkbench.impl.CandidateFormation
import org.simplemodeling.textus.knowledgeworkbench.{value as workbenchvalue}

object StructuredCandidateTargetSpec {
  def sample(text: String, material: String = "material:1", resource: String = "resource:1"): workbenchvalue.InformationStructuredTarget =
    workbenchvalue.InformationStructuredTarget(
      Vector(workbenchvalue.InformationContent("content-1", text, Some("ja")),
        workbenchvalue.InformationContent("content-2", " second ", None)),
      Vector(workbenchvalue.InformationTranslation("content-1", " translation ", "en")),
      Vector(workbenchvalue.InformationAnnotation("annotation-1", Some(" note "), Vector(
        workbenchvalue.InformationAnnotationMedia("image-1", "image", "image/png", None),
        workbenchvalue.InformationAnnotationMedia("audio-1", "audio", "audio/wav", Some("audio-v1")))),
        workbenchvalue.InformationAnnotation("annotation-2", None, Vector(
          workbenchvalue.InformationAnnotationMedia("video-1", "video", "video/mp4", None)))),
      Vector(workbenchvalue.InformationContext("purpose", " context ")),
      Vector(workbenchvalue.InformationSource(material, resource, Some("source-v1"), Some("observed"),
        Vector(workbenchvalue.InformationWorkbookCell(" Sheet 1 ", "2", "C"),
          workbenchvalue.InformationWorkbookCell(" Sheet 1 ", "2", "D")),
        Vector(workbenchvalue.InformationOriginalBook(" book ", Some(" 12 "))),
        Some(workbenchvalue.InformationDerivation("adapter", "adapter-v2", "profile", "profile-v3")))))
}

final class StructuredCandidateTargetSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Structured Candidate target" should {
    "preserve exact ordered values through the generated Record codec" in {
      Given("independent annotations, multiple media, locators and distinct observed and adapter versions")
      val property = Prop.forAll(Gen.nonEmptyListOf(Gen.alphaChar)) { chars =>
        val target = StructuredCandidateTargetSpec.sample(" " + chars.mkString + " \n")
        workbenchvalue.InformationStructuredTarget.createC(target.toRecord()).toOption.contains(target) &&
          CandidateFormation.validateStructured(target).toOption.nonEmpty
      }
      When("arbitrary text is encoded and decoded")
      val result = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), property)
      Then("the full typed target is identical and valid")
      result.passed shouldBe true
      result.succeeded should be >= 100
    }

    "reject malformed semantic relationships without manufacturing versions" in {
      Given("a complete structured target")
      val target = StructuredCandidateTargetSpec.sample("text")
      val invalid = Vector(
        target.copy(contents = Vector.empty),
        target.copy(sources = Vector.empty),
        target.copy(contents = target.contents :+ target.contents.head),
        target.copy(translations = target.translations.map(_.copy(contentId = "missing"))),
        target.copy(annotations = target.annotations :+ target.annotations.head),
        target.copy(annotations = Vector(target.annotations.head.copy(text = None, media = Vector.empty))),
        target.copy(annotations = Vector(target.annotations.head.copy(media = Vector.fill(2)(target.annotations.head.media.head)))))
      When("relationships and optional source versions are checked")
      val unknown = target.copy(sources = target.sources.map(_.copy(sourceVersion = None, observation = None)))
      Then("invalid structures fail and unknown source evidence remains valid and absent")
      invalid.foreach(x => CandidateFormation.validateStructured(x).toOption shouldBe None)
      CandidateFormation.validateStructured(unknown).toOption shouldBe Some(())
      unknown.sources.head.sourceVersion shouldBe None
      unknown.sources.head.derivation.get.adapterVersion shouldBe "adapter-v2"
    }

    "decode a scalar target from the original wire shape" in {
      Given("a scalar JSON target saved before the structured field existed")
      val legacytext = """{"content":" legacy ","context":" context ","evidenceRefs":[],"preparedMaterialRefs":[],"sourceRunRef":null,"sourceContextRef":null,"groundingRefs":[],"mappingRefs":[]}"""
      val json = io.circe.parser.parse(legacytext).toOption.get
      When("the current typed decoder reads it")
      val decoded = new org.goldenport.record.io.RecordDecoder().json(legacytext)
        .flatMap(workbenchvalue.CandidateApprovalTarget.createC)
      Then("the absent field stays absent and scalar text is unchanged")
      json.hcursor.downField("structured").succeeded shouldBe false
      decoded.toOption.map(_.structured) shouldBe Some(None)
      decoded.toOption.map(_.content.value) shouldBe Some(" legacy ")
    }
  }
}
