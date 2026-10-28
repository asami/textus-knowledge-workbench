package org.simplemodeling.textus.knowledgeworkbench

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import org.goldenport.Consequence
import org.goldenport.cncf.bootstrap.{BootstrapConfig, CncfBootstrap, CncfHandle}
import org.goldenport.cncf.cli.RunMode
import org.goldenport.cncf.subsystem.Subsystem
import org.goldenport.protocol.{Property, Request}
import org.goldenport.protocol.operation.OperationResponse
import org.goldenport.record.Record
import org.simplemodeling.textus.knowledgeworkbench.{value as workbenchvalue}
import org.simplemodeling.textus.knowledgeworkbench.impl.CandidateAdmission
import org.scalacheck.{Gen, Prop, Test}
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct. 28, 2026
 * @version Oct. 28, 2026
 */
final class CandidateAdmissionSpec extends AnyWordSpec with Matchers with GivenWhenThen {
  "Candidate Information Admission" should {
    "actual canonical Information and trace" which {
      "admit both retained producer fixtures through the real Hub and restore confirmation after reopen" in {
        Vector("tkl-prepared.json", "editing-studio-unprepared.json").foreach { fixture =>
          Given(s"a current eligible human-approved $fixture in the normal Workbench and KnowledgeHub assembly")
          val root = _root()
          val writer = _handle(root)
          val before = try {
            val approved = _approve(writer.subsystem, fixture)
            When("the explicit Admission command uses its observed native revision")
            val admitted = _admit(writer.subsystem, approved)
            val saved = admitted.candidate.admissions.last
            val canonical = _information(writer.subsystem, saved.informationId.print)
            Then("the actual persisted Hub identity and complete original reviewed target correspond")
            admitted.state shouldBe "Admitted"
            admitted.approvalValid shouldBe true
            saved.status shouldBe "Confirmed"
            saved.candidateRevision shouldBe approved.revision
            saved.approval shouldBe approved.candidate.decisions.last
            saved.requestedBy shouldBe "workbench-admission-spec-user"
            val actual = _json(canonical)
            actual.hcursor.downField("information").focus shouldBe Some(_json(CandidateAdmission.request(approved.candidate.id, saved)))
            actual.hcursor.downField("information").get[String]("id").toOption shouldBe Some(saved.informationId.print)
            actual.hcursor.downField("information").get[String]("candidateId").toOption shouldBe Some(approved.candidate.id.print)
            actual.hcursor.downField("information").get[Long]("candidateRevision").toOption shouldBe Some(approved.revision)
            saved.informationRevision shouldBe actual.hcursor.get[Long]("revision").toOption
            admitted.candidate.originalProposal shouldBe approved.candidate.originalProposal
            admitted.candidate.preparedMaterialRefs shouldBe approved.candidate.preparedMaterialRefs
            admitted.candidate.sourceRunRef shouldBe approved.candidate.sourceRunRef
            admitted.candidate.sourceContextRef shouldBe approved.candidate.sourceContextRef
            When("the same approved snapshot is submitted again")
            _command(writer.subsystem, "admitCandidateWork", admitted) shouldBe a[Consequence.Failure[?]]
            Then("no second Admission or canonical identity is allocated into persisted work")
            _get(writer.subsystem, admitted.candidate.id.print).toRecord() shouldBe admitted.toRecord()
            admitted
          } finally writer.close()
          When("a fresh normal runtime opens the same persistence and confirms by saved identity")
          val reader = _handle(root)
          try {
            val restored = _get(reader.subsystem, before.candidate.id.print)
            val confirmed = _work(_command(reader.subsystem, "confirmCandidateAdmission", restored))
            Then("lookup preserves the exact canonical outcome and performs no redundant Candidate write")
            confirmed.toRecord() shouldBe before.toRecord()
            _json(_information(reader.subsystem, confirmed.candidate.admissions.last.informationId.print)).hcursor
              .get[Long]("revision").toOption shouldBe confirmed.candidate.admissions.last.informationRevision
          } finally reader.close()
        }
      }
    }

    "Material-bound approved evidence" which {
      "retain updated managed correspondence natively while admitting the exact approved evidence to the actual Hub" in {
        Given("a public Material-created Candidate and controlled TKL observations beside the actual KnowledgeHub CAR")
        val handle = _handle(_root())
        try {
          val producer = MaterialEvidenceWorkSpec.installProducer(handle.subsystem)
          val createdresponse = handle.subsystem.executeOperationResponse(Request.of(component = KnowledgeWorkbenchComponent.name,
            service = "MaterialWork", operation = "createCandidateFromMaterial", properties = List(
              Property("proposalId", "admission:material-evidence", None),
              Property("materialRef", MaterialWorkFixture.materialRef, None),
              Property("resourceIds", Vector(MaterialWorkFixture.resourceId), None),
              Property("name", "Material evidence admission", None), Property("content", "Human candidate statement", None))))
          val created = _success(workbenchvalue.RegisterProposalResult.createC(_record(_success(createdresponse)))).candidate
          val first = _get(handle.subsystem, created.id.print)
          val bound = _work(handle.subsystem.executeOperationResponse(Request.of(component = KnowledgeWorkbenchComponent.name,
            service = "MaterialWork", operation = "refreshCandidateMaterialEvidence", properties = List(
              Property("id", created.id.print, None), Property("expectedRevision", first.revision, None),
              Property("reason", "Observe original evidence", None)))))
          val previous = Vector("review", "approve").foldLeft(bound) { (current, decision) =>
            _work(_call(handle.subsystem, "decideCandidateWork", "id" -> current.candidate.id.print,
              "expectedRevision" -> current.revision, "decision" -> decision, "reason" -> "Human reviewed observed evidence"))
          }
          producer.observations = producer.observations.map(_.upsertSingle("revision", "2"))
          producer.observations.map(_.getString("revision")) shouldBe Vector(Some("2"))
          val updated = _work(handle.subsystem.executeOperationResponse(Request.of(component = KnowledgeWorkbenchComponent.name,
            service = "MaterialWork", operation = "refreshCandidateMaterialEvidence", properties = List(
              Property("id", created.id.print, None), Property("expectedRevision", previous.revision, None),
              Property("reason", "Observe changed original version", None)))))
          updated.approvalValid shouldBe false
          val approved = Vector("review", "approve").foldLeft(updated) { (current, decision) =>
            _work(_call(handle.subsystem, "decideCandidateWork", "id" -> current.candidate.id.print,
              "expectedRevision" -> current.revision, "decision" -> decision, "reason" -> "Review changed evidence again"))
          }
          When("explicit Admission submits the current eligible approved target")
          val admitted = _admit(handle.subsystem, approved)
          val saved = admitted.candidate.admissions.last
          val information = _json(_information(handle.subsystem, saved.informationId.print)).hcursor.downField("information")
          Then("the Hub keeps exact source references and Candidate trace while resource correspondence stays in native Candidate history")
          admitted.state shouldBe "Admitted"
          admitted.candidate.materialEvidence shouldBe approved.candidate.materialEvidence
          information.get[String]("candidateId").toOption shouldBe Some(created.id.print)
          information.get[Long]("candidateRevision").toOption shouldBe Some(approved.revision)
          information.downField("target").downField("evidenceRefs").focus shouldBe
            Some(_json(CandidateAdmission.request(created.id, saved)).hcursor.downField("target").downField("evidenceRefs").focus.get)
          information.downField("materialEvidence").succeeded shouldBe false
          saved.approval.target.evidenceRefs shouldBe approved.candidate.materialEvidence.get.applications.last.resources.map(_.appliedReference)
          producer.calls.count(_._1.properties.exists(_.name == "evidenceResourceIds")) shouldBe 2
          admitted.candidate.materialEvidence.get.applications should have size 2
        } finally handle.close()
      }
    }

    "eligibility and retained uncertainty" which {
      "reject unapproved changed and stale targets without a reservation or canonical call" in {
        Given("one approved native Candidate and a newer explicit human edit")
        val handle = _handle(_root())
        try {
          val approved = _approve(handle.subsystem, "tkl-prepared.json")
          val changed = _edit(handle.subsystem, approved, "Changed after approval")
          When("the stale approved revision or current unapproved target attempts Admission")
          _command(handle.subsystem, "admitCandidateWork", approved) shouldBe a[Consequence.Failure[?]]
          _command(handle.subsystem, "admitCandidateWork", changed) shouldBe a[Consequence.Failure[?]]
          Then("the current target, history and native revision remain exact and no identity is reserved")
          val after = _get(handle.subsystem, changed.candidate.id.print)
          after.toRecord() shouldBe changed.toRecord()
          after.candidate.admissions shouldBe empty
        } finally handle.close()
      }

      "persist the identity before a failed Hub invocation and confirm only the original owning write after reopen" in {
        Given("an approved Candidate whose owning Hub route is temporarily absent")
        val root = _root()
        val writer = _handle(root, includehub = false)
        val retained = try {
          val approved = _approve(writer.subsystem, "editing-studio-unprepared.json")
          When("the Admission invocation cannot reach the owning Operation")
          _command(writer.subsystem, "admitCandidateWork", approved) shouldBe a[Consequence.Failure[?]]
          val pending = _get(writer.subsystem, approved.candidate.id.print)
          Then("native persistence retains one reserved ID and an unconfirmed result despite the failed command")
          pending.state shouldBe "AdmissionUnconfirmed"
          pending.candidate.admissions should have size 1
          pending.candidate.admissions.last.status shouldBe "Unconfirmed"
          pending.candidate.admissions.last.candidateRevision shouldBe approved.revision
          _command(writer.subsystem, "admitCandidateWork", pending) shouldBe a[Consequence.Failure[?]]
          _get(writer.subsystem, pending.candidate.id.print).toRecord() shouldBe pending.toRecord()
          pending
        } finally writer.close()
        When("the same persistence reopens with the actual Hub and its outcome is initially missing")
        val reader = _handle(root)
        try {
          val restored = _get(reader.subsystem, retained.candidate.id.print)
          _command(reader.subsystem, "confirmCandidateAdmission", restored) shouldBe a[Consequence.Failure[?]]
          _get(reader.subsystem, restored.candidate.id.print).toRecord() shouldBe restored.toRecord()
          Given("the original reserved owning write commits, with no acknowledgement recorded by TKW")
          val admission = restored.candidate.admissions.last
          val input = CandidateAdmission.request(restored.candidate.id, admission)
          _success(_hub(reader.subsystem, "admitInformation", input.asMap.toVector*))
          _get(reader.subsystem, restored.candidate.id.print).state shouldBe "AdmissionUnconfirmed"
          When("the Workbench explicitly looks up that same identity")
          val confirmed = _work(_command(reader.subsystem, "confirmCandidateAdmission", restored))
          Then("only exact confirmed correspondence changes the observation; no replacement identity or write replay occurs")
          confirmed.state shouldBe "Admitted"
          confirmed.candidate.admissions should have size 1
          confirmed.candidate.admissions.last.informationId shouldBe admission.informationId
        } finally reader.close()
      }

      "refuse a foreign owning outcome across generated Candidate and target mismatches" in {
        Given("a real canonical Information and its retained approved Admission")
        val handle = _handle(_root())
        try {
          val admitted = _admit(handle.subsystem, _approve(handle.subsystem, "tkl-prepared.json"))
          val admission = admitted.candidate.admissions.last
          val result = _information(handle.subsystem, admission.informationId.print)
          When("generated changes attempt to substitute canonical content or context")
          val property = Prop.forAll(Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString)) { suffix =>
            val original = _json(result)
            val altered = original.hcursor.downField("information").downField("target").downField("content")
              .withFocus(content => io.circe.Json.fromString(content.asString.get + suffix)).top.get
            val foreign = _success(new org.goldenport.record.io.RecordDecoder().json(altered.noSpaces))
            Then("each mismatch is rejected without marking the supplied record confirmed")
            CandidateAdmission.confirm(admitted.candidate, admission, foreign).isSuccess shouldBe false
            true
          }
          val checked = Test.check(Test.Parameters.default.withMinSuccessfulTests(100), property)
          checked.passed shouldBe true
          checked.succeeded should be >= 100
          checked.discarded shouldBe 0
        } finally handle.close()
      }
    }
  }

  private def _success[A](result: Consequence[A]): A =
    result.toOption.getOrElse(fail(s"Expected actual native success: $result"))

  private def _record(response: OperationResponse): Record = response match {
    case OperationResponse.RecordResponse(record) => record
    case other => fail(s"Expected native Record response: $other")
  }

  private def _work(result: Consequence[OperationResponse]): workbenchvalue.CandidateWorkResult =
    _success(workbenchvalue.CandidateWorkResult.createC(_record(_success(result))))

  private def _call(subsystem: Subsystem, operation: String, properties: (String, Any)*): Consequence[OperationResponse] =
    subsystem.executeOperationResponse(Request.of(component = "org.simplemodeling.textus.KnowledgeWorkbench",
      service = "Candidate", operation = operation,
      properties = properties.map((name, input) => Property(name, input, None)).toList))

  private def _hub(subsystem: Subsystem, operation: String, properties: (String, Any)*): Consequence[OperationResponse] =
    subsystem.executeOperationResponse(Request.of(component = "org.simplemodeling.textus.KnowledgeHub",
      service = "Information", operation = operation,
      properties = properties.map((name, input) => Property(name, input, None)).toList))

  private def _information(subsystem: Subsystem, id: String): Record =
    _record(_success(_hub(subsystem, "getInformation", "id" -> id)))

  private def _json(record: Record): io.circe.Json =
    io.circe.parser.parse(_success(record.toJsonStringC)).fold(throw _, identity)

  private def _get(subsystem: Subsystem, id: String): workbenchvalue.CandidateWorkResult =
    _work(_call(subsystem, "getCandidateWork", "id" -> id))

  private def _command(subsystem: Subsystem, operation: String, current: workbenchvalue.CandidateWorkResult): Consequence[OperationResponse] =
    _call(subsystem, operation, "id" -> current.candidate.id.print, "expectedRevision" -> current.revision)

  private def _admit(subsystem: Subsystem, current: workbenchvalue.CandidateWorkResult): workbenchvalue.CandidateWorkResult =
    _work(_command(subsystem, "admitCandidateWork", current))

  private def _edit(subsystem: Subsystem, current: workbenchvalue.CandidateWorkResult, content: String): workbenchvalue.CandidateWorkResult = {
    val properties = Vector("id" -> current.candidate.id.print,
      "expectedRevision" -> current.revision, "content" -> content,
      "evidenceRefs" -> Vector(workbenchvalue.ProposalReference("evidence:reviewed", "evidence", Some("v1"),
        None, Some("available"), None, None).toRecord()),
      "groundingRefs" -> current.candidate.groundingRefs.map(_.toRecord()),
      "mappingRefs" -> current.candidate.mappingRefs.map(_.toRecord()), "reason" -> "Human content correction") ++
      current.candidate.context.map(context => "context" -> context.value).toVector
    _work(_call(subsystem, "editCandidateWork", properties*))
  }

  private def _approve(subsystem: Subsystem, fixture: String): workbenchvalue.CandidateWorkResult = {
    val stream = getClass.getResourceAsStream(s"/proposals/$fixture")
    val json = try new String(stream.readAllBytes(), StandardCharsets.UTF_8) finally stream.close()
    val candidate = _success(workbenchvalue.RegisterProposalResult.createC(_record(_success(
      _call(subsystem, "registerProposal", "proposalJson" -> json))))).candidate
    val ready = _edit(subsystem, _get(subsystem, candidate.id.print), "Exact human approved content")
    Vector("review", "approve").foldLeft(ready) { (current, decision) =>
      _work(_call(subsystem, "decideCandidateWork", "id" -> current.candidate.id.print,
        "expectedRevision" -> current.revision, "decision" -> decision, "reason" -> "Human reviewed target"))
    }
  }

  private def _root(): Path = {
    val parent = Path.of("target/candidate-admission-spec").toAbsolutePath.normalize()
    Files.createDirectories(parent)
    Files.createTempDirectory(parent, "case-")
  }

  private def _handle(root: Path, includehub: Boolean = true): CncfHandle = {
    val repository = Path.of(".").toAbsolutePath.normalize()
    val lake = Path.of(sys.props.getOrElse("textus.test.knowledge-lake.repository",
      repository.resolveSibling("textus-knowledge-lake").toString)).toAbsolutePath.normalize()
    val hubcar = Path.of(sys.props.getOrElse("textus.test.knowledge-hub.car",
      Path.of(System.getProperty("user.home"), ".cncf/local/repository/car/org/simplemodeling/textus/" +
        "textus-knowledge-hub/0.1.0-SNAPSHOT/textus-knowledge-hub-0.1.0-SNAPSHOT.car").toString)).toAbsolutePath.normalize()
    val authored = Files.readString(repository.resolve("src/main/car/assembly-descriptor.yaml"))
    val selected = if (includehub) authored else authored.replace(
      "  - namespace: org.simplemodeling.textus\n    id: KnowledgeHub\n    version: 0.1.0-SNAPSHOT\n", "")
    val isolated = selected.replace("  textus.subsystem.user-mode: standalone\n",
      s"""  textus.subsystem.user-mode: standalone
  textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.kind: sqlite
  textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.path: ${root.resolve("workbench.db")}
  textus.component.org.simplemodeling.textus.knowledge-hub.datastores.application.kind: sqlite
  textus.component.org.simplemodeling.textus.knowledge-hub.datastores.application.path: ${root.resolve("hub.db")}
""")
    val instance = root.resolve("assembly-instance.yaml")
    Files.writeString(instance, isolated.replace("    fallbackPrivilege: disabled\n",
      "    fallbackPrivilege: disabled\n    localSubject:\n      id: workbench-admission-spec-user\n"))
    val dependencies = if (includehub) Vector(s"--component-file=$hubcar") else Vector.empty
    _success(CncfBootstrap.initialize(BootstrapConfig(cwd = root,
      args = (Vector("--discover=classes", s"--component-dev-dir=$repository", s"--component-dev-dir=$lake") ++
        dependencies ++ Vector(s"--textus.assembly.descriptor=$instance", s"--textus.local-data.root=$root", "command")).toArray,
      modeHint = Some(RunMode.Command))))
  }
}
