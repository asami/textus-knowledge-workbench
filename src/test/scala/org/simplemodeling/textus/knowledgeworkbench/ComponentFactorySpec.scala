package org.simplemodeling.textus.knowledgeworkbench

import java.nio.file.{Files, Path}
import org.goldenport.configuration.{Configuration, ConfigurationTrace, ConfigurationValue, ResolvedConfiguration}
import org.goldenport.cncf.component.{ComponentCreate, ComponentId, ComponentOrigin}
import org.goldenport.cncf.config.RuntimeConfig
import org.goldenport.cncf.subsystem.{
  DefaultSubsystemFactory,
  GenericSubsystemAssemblyDescriptorSource,
  GenericSubsystemAuthenticationBinding,
  GenericSubsystemDescriptor,
  GenericSubsystemFactory,
  GenericSubsystemLocalSubjectBinding,
  GenericSubsystemSecurityBinding,
  Subsystem,
  SubsystemExecutionProfile
}
import org.goldenport.record.Record
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
 * @since   Oct.  3, 2026
 *  version Oct. 18, 2026
 * @version Oct. 28, 2026
 */
final class ComponentFactorySpec extends AnyWordSpec with Matchers with GivenWhenThen {
  private val _foundation = afterWord(
    "in spec:workbench-component-foundation; rule:FND-02/FND-03; phase:1; slice:S1-A"
  )

  "ComponentFactory" should _foundation {
    "create an initialized generated bundle with Candidate operations" in {
      Given("a production default subsystem in server mode and the Workbench repository origin")
      val server = "server"
      val subsystem = DefaultSubsystemFactory.default(Some(server))
      val params = ComponentCreate(
        subsystem,
        ComponentOrigin.Repository("workbench-foundation-spec")
      )

      try {
        When("the Workbench bundle factory creates the component bundle")
        val bundle = new impl.ComponentFactory().create(params)

        Then("the initialized primary has the generated identity and both Candidate operations, with no other participants")
        bundle.primary.core.componentId shouldBe KnowledgeWorkbenchComponent.componentId
        bundle.primary.isPrimaryParticipant shouldBe true
        bundle.primary.operationDefinitions.map(_.name) should contain allOf (
          "registerCandidate",
          "listCandidates"
        )
        bundle.participants shouldBe Vector(bundle.primary)
      } finally {
        subsystem.shutdown()
      }
    }

    "keep starter Candidate operations outside MCP publication" in {
      Given("a Workbench component initialized through its production bundle factory")
      val server = "server"
      val subsystem = DefaultSubsystemFactory.default(Some(server))
      try {
        val bundle = new impl.ComponentFactory().create(
          ComponentCreate(
            subsystem,
            ComponentOrigin.Repository("workbench-foundation-spec")
          )
        )

        When("the initialized component evaluates MCP readiness for both starter Candidate operations")
        val registerready = bundle.primary.isMcpReady("Candidate", "registerCandidate")
        val listready = bundle.primary.isMcpReady("Candidate", "listCandidates")

        Then("neither starter operation is published through MCP")
        registerready shouldBe false
        listready shouldBe false
      } finally {
        subsystem.shutdown()
      }
    }

    "assembly startup" when {
      "given the complete authored assembly" should {
        "admit the canonical Workbench and its Candidate operations in fixed-user command mode" in {
          Given("the authored Workbench, Lake and published Hub assembly with a test-owned local subject")
          val descriptor = _instance_descriptor()
          val configuration = _runtime_configuration()

          When("the production GenericSubsystemFactory constructs the ordinary command subsystem")
          val subsystem = GenericSubsystemFactory.default(descriptor, Some("command"), configuration)
          try {
            val component = subsystem.findComponent(KnowledgeWorkbenchComponent.componentId)
              .getOrElse(fail("the canonical Workbench component was not constructed"))

            Then("the canonical Workbench exposes both Candidate operations, the canonical Lake and Hub are present, and the fixed-user execution profile is admitted")
            component.componentId shouldBe KnowledgeWorkbenchComponent.componentId
            subsystem.findComponent(ComponentId("org.simplemodeling.textus.KnowledgeLake")).isDefined shouldBe true
            subsystem.findComponent(ComponentId("org.simplemodeling.textus.KnowledgeHub")).isDefined shouldBe true
            component.operationDefinitions.map(_.name) should contain allOf (
              "registerCandidate",
              "listCandidates"
            )
            subsystem.executionProfileC.toOption shouldBe Some(SubsystemExecutionProfile.Fixed)
          } finally {
            subsystem.shutdown()
          }
        }
      }

      "given the same assembly without its capability providers" should {
        "reject production admission with the required-capability diagnostic" in {
          Given("the same authored Workbench, Lake and published Hub assembly with an explicitly empty native capability providers override")
          val descriptor = _without_capability_providers(_instance_descriptor())
          val configuration = _runtime_configuration()
          var constructed: Option[Subsystem] = None

          try {
            When("the production GenericSubsystemFactory attempts ordinary command subsystem construction")
            val failure = intercept[Exception] {
              constructed = Some(GenericSubsystemFactory.default(descriptor, Some("command"), configuration))
            }

            Then("admission fails because no declared provider supplies the required optimistic concurrency capability")
            failure.getMessage should include ("datastore.optimistic-concurrency@1")
            failure.getMessage should include ("no declared provider supplies it")
            constructed shouldBe None
          } finally {
            constructed.foreach(_.shutdown())
          }
        }
      }
    }
  }

  private def _instance_descriptor(): GenericSubsystemDescriptor = {
    val descriptor = GenericSubsystemDescriptor.load(
      _repository_root.resolve("src/main/car/assembly-descriptor.yaml")
    ).toOption.getOrElse(fail("the authored Workbench assembly descriptor did not load"))
    val security = descriptor.security.getOrElse(GenericSubsystemSecurityBinding())
    val authentication = security.authentication.getOrElse(GenericSubsystemAuthenticationBinding())
    descriptor.copy(security = Some(security.copy(authentication = Some(authentication.copy(
      localSubject = Some(GenericSubsystemLocalSubjectBinding("workbench-assembly-spec-user"))
    )))))
  }

  private def _without_capability_providers(descriptor: GenericSubsystemDescriptor): GenericSubsystemDescriptor = {
    val source = GenericSubsystemAssemblyDescriptorSource(
      Record.data("subsystemCapabilities" -> Record.data("providers" -> Vector.empty[Record])),
      "workbench-assembly-spec"
    )
    GenericSubsystemDescriptor.applyAssemblyOverrideC(descriptor, source).toOption
      .getOrElse(fail("the provider-empty assembly override did not apply"))
  }

  private def _runtime_configuration(): ResolvedConfiguration = {
    val root = Files.createTempDirectory(
      Files.createDirectories(_repository_root.resolve("target/workbench-assembly-spec")), "case-")
    val workbench = "textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application"
    val hub = "textus.component.org.simplemodeling.textus.knowledge-hub.datastores.application"
    ResolvedConfiguration(
      Configuration(Map(
        RuntimeConfig.repositoryDirKey -> ConfigurationValue.StringValue(
          s"component-dev-dir:${_repository_root},component-dev-dir:${_lake_repository_root}"
        ),
        RuntimeConfig.componentFileKey -> ConfigurationValue.StringValue(_hub_car.toString),
        "textus.local-data.root" -> ConfigurationValue.StringValue(root.toString),
        s"$workbench.kind" -> ConfigurationValue.StringValue("sqlite"),
        s"$workbench.path" -> ConfigurationValue.StringValue(root.resolve("workbench.db").toString),
        s"$hub.kind" -> ConfigurationValue.StringValue("sqlite"),
        s"$hub.path" -> ConfigurationValue.StringValue(root.resolve("hub.db").toString)
      )),
      ConfigurationTrace.empty
    )
  }

  private def _repository_root: Path = Path.of(".").toAbsolutePath.normalize()

  private def _hub_car: Path = Path.of(sys.props.getOrElse("textus.test.knowledge-hub.car",
    Path.of(System.getProperty("user.home"), ".cncf/local/repository/car/org/simplemodeling/textus/" +
      "textus-knowledge-hub/0.1.0-SNAPSHOT/textus-knowledge-hub-0.1.0-SNAPSHOT.car").toString)).toAbsolutePath.normalize()

  private def _lake_repository_root: Path = Path.of(
    sys.props.getOrElse(
      "textus.test.knowledge-lake.repository",
      _repository_root.resolveSibling("textus-knowledge-lake").toString
    )
  ).toAbsolutePath.normalize()
}
