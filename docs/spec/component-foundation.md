# Workbench component foundation

2026-10-21 clarification: the
[shared editing contract](knowledge-editing-component-boundary.md) defines
Workbench's public role for multiple editors and its TKL dependency. This
preserves accepted foundation/proposal behavior; it does not claim the new
live Material APIs are implemented.

Spec: `workbench-component-foundation`\
Rules: `FND-01` through `FND-05`\
Owner: PHASE-1 / STEP-1 / S1-A\
Status: STEP-1 / S1-A accepted at commit 70620d1aeaae4a3859df93d6ff6948bdcff5742c

Acceptance basis: parent record
`tkw-phase1-step1-post-hardbreak-commit-acceptance-20261003@1`, retained Step
review `tkw-phase1-step1-acceptance-review-20261003@1`, focused closure review
`tkw-phase1-step1-hardbreak-focused-review-20261003@1`, and review ledger
`tkw-phase1-review-ledger-20261002@4`. The commit receipt confirms all 23 admitted
foundation paths, original parent `c3425d2968e2b95ad94474a1d83c2cc005373c3e`,
and preservation of the seven planning changes. It closes STEP-1/S1-A only.
The historical limits below remain applicable; the successful full SBT suite,
comprehensive Phase review, closure ledger and final release remain pending.

This foundation adopts the actual standard Cozy CAR scaffold for
KnowledgeWorkbench, Candidate and InformationCandidate. `project.yaml` owns
canonical identity `org.simplemodeling.textus.KnowledgeWorkbench`, component
class `KnowledgeWorkbenchComponent`, package
`org.simplemodeling.textus.knowledgeworkbench`, version `0.1.0-SNAPSHOT`,
bounded context `knowledge-workbench` and domain `knowledge`.

The CML `STYLE` declaration is the sole style authority. It selects
`full-fledged-with-standalone` before `PACKAGE`, with no parameters or mode keys.
The selected CNCF catalog identifies this as `full-fledged-with-standalone@1`
and provides `domain.full@1`, `user.fixed-context-compatible@1` and
`user.multi-user@1`. Its required Subsystem capabilities are
`datastore.optimistic-concurrency@1`, `datastore.persistent@1`,
`datastore.transactional@1` and `user-context.current@1`. CNCF Subsystem owns
datastore/provider placement and user context. The subject-10 predecessor evidence
passed production factory/assembly scenarios and actual CLI help in isolated
standalone storage; style metadata alone does not establish Subsystem activation
or persistence. STEP-1 acceptance is recorded at the commit above.

The native authored `src/main/car/assembly-descriptor.yaml` declares subsystem
`textus-knowledge-workbench` at `0.1.0-SNAPSHOT`, binds
`org.simplemodeling.textus.KnowledgeWorkbench` at that version and names
`workbench-runtime-facilities` as its provider for all four required facilities.
Its configuration selects `textus.subsystem.user-mode=standalone` and
`textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.policy=local-default`.
This consumes CNCF-managed `ComponentDataStore`/`SqlDataStore` and their existing
persistent, transactional and optimistic mutation contracts. It adds no local
store or provider implementation and removes no style requirement.

The base descriptor sets authentication convention and fallback privilege to
`disabled` and deliberately carries no production user ID. An operator copies
the complete descriptor to an instance file and adds
`security.authentication.localSubject.id` for the selected stable standalone
identity, using an admitted HOME profile's identity where applicable. Conflicting
fixed-user IDs require explicit migration or datastore isolation under CNCF's
existing rule. The framework uses canonical namespace/component application
storage; an instance can explicitly select an isolated `textus.local-data.root`.
The instance uses native descriptor syntax without assumed interpolation or
`extends` semantics. The [README](../../README.md) supplies setup instructions.

`src/main/car/abi-manifest.json` is the canonical authored ABI source, using
`cozy.car.abi-manifest.v2` and `abi.version=1` for the canonical component
coordinate above. At accepted STEP-1 its complete exports were KnowledgeWorkbench, service
Candidate, COMMAND `registerCandidate` and QUERY `listCandidates`, value types
RegisterCandidate, ListCandidates, InformationCandidateDescription,
InformationCandidateSearchText, RegisterCandidateResult and ListCandidatesResult,
and entity InformationCandidate. It declares all CML-defined fields with their
types, multiplicities and required flags and an empty dependency list. Every
later Phase 1 CML surface or component-coordinate change must update the
canonical ABI exports/version coherently and revalidate ordinary packaging and
runtime preparation.

The build uses the generated `ProjectYamlBuild` helper, CozyPlugin and existing
CNCF contracts. Versions are SBT `1.9.7`, sbt-cozy `0.1.18-SNAPSHOT` (with
property/environment override), Cozy `0.3.3-SNAPSHOT`, Scala `3.3.8`, CNCF
`0.5.3-SNAPSHOT` and ScalaTest `3.2.19`. Runtime metadata retains minimum/tested
CNCF `0.5.3-SNAPSHOT`. Resolver settings use `useCoursier := false`, default
local, local Ivy, local Maven and SimpleModeling repositories. The delegate
uses `cozy --runtime` with `build.cozyVersion` read by the generated helper.

The sole CNCF version selection is the explicit
`org.goldenport::goldenport-cncf:0.5.3-SNAPSHOT` declaration in
`project.yaml` at `build.dependencies.compile`. The existing dependency helper
requires exactly one `org.goldenport` CNCF compile dependency named
`goldenport-cncf` or `goldenport-cncf_3`, with a nonblank version. Missing, blank
or duplicate declarations produce a configuration error while loading build
settings, before dependency resolution. Neither sibling checkout presence nor
`CNCF_VERSION`, `CNCF_VERSION_FILE`, `CNCF_SAMPLES_ROOT` or `CNCF_BIN` supplies a
version.

Normal `cozyGenerate` and `cozyBuildCAR` project the CML style and canonical
component identity into generated model/component and CAR descriptors and
package the complete ABI exports and the authored assembly input. The development
component projection uses `includeAssemblyDescriptor=false`, so packaging alone
does not wire development startup. `cozyPrepareRuntime` prepares
`target/cncf.d/runtime-classpath.txt` from project dependencies and a development
runtime manifest containing the prepared classpath, generated component
descriptor and canonical authored ABI source. Run preparation again through
ordinary SBT routing after declaration changes. Both `scripts/run-server.sh`
and `scripts/run-server-debug.sh` invoke the project's prepared-classpath launcher
with the same main class, Java port/base URL options, discovery arguments and
`server`. After rejecting a missing or empty prepared classpath, they require a
nonempty `WORKBENCH_ASSEMBLY_DESCRIPTOR` naming a readable regular operator
instance file and pass its exact path as one `--textus.assembly.descriptor=<path>`
argument after the wrapper delimiter. Debug adds only the JDWP Java option using
`DEBUG_PORT` (default `5005`). Runtime preparation retains its SBT route and
the same dependency admission. Neither script selects a sibling or default
instance; legacy CNCF version/location variables cannot change this routing.

The canonical [reference manual](../../src/main/car/manual/index.md) and
[user guide](../../src/main/car/manual/user-guide.md) are authored under
`src/main/car/manual` for CAR packaging as `manual/index.md` and
`manual/user-guide.md`. Their internal links stay within the packaged manual
tree. CML DESCRIPTION edits supply purpose and operation prose to Cozy metadata
without changing ABI, schemas, actions or publication. Native component Help
uses a framework-fixed generic summary without manual navigation fields;
the documentation does not require a framework display override. Fresh
generation, packaged manual bytes, description projection, canonical CLI help
and CAR lint were included in the accepted STEP-1 documentation evidence.

The accepted STEP-1 InformationCandidate schema had required `id` and `name`
and optional `description`. The proposal specification owns its additive STEP-2 fields.
The CML declares `entity-create` implementation metadata for
`Candidate/registerCandidate`, with required name and optional description, and
`entity-search` metadata for `Candidate/listCandidates`, with optional text,
offset and limit. The generated Candidate ActionCall instances remain
`uowmNotImplemented` stubs; this metadata alone does not implement persistence.
The handwritten Candidate service factory selects private ActionCalls extending
the generated contracts and connects them to native CNCF Entity/UnitOfWork helpers.
Registration decodes the generated InformationCandidate create value, retains
optional description, and calls `entity_create`. Its declared
`RegisterCandidateResult` returns the native create result's ID and the validated
input name. Listing decodes `ListCandidates`, builds a native query matching
case-insensitive substrings in name or description, and resolves logical fields
through `EntityQueryFieldResolver` before `entity_search`. Absent text matches all
accessible rows; offset and limit apply inside the native query. The declared
`ListCandidatesResult` maps the returned entities to `items`. Both helpers use
existing application datastore, authorization and user-context behavior.
Generator value types remain unchanged. The subject-10 predecessor CLI checks
registered a native entity and returned the same ID/name through a separate
process, including description text search and offset/limit, with one persistent
SQLite row inspected in isolated storage. Registration returns `id` and `name`;
listing returns optional description as `description.value`. At the historical
STEP-1 boundary, generated `Record.dataAuto` omitted empty `items`, yielding `{}`
for an empty search or page. These historical observations form part of the
accepted foundation evidence. The current STEP-2 continuation consumes CNCF
Phase 115 and the Core Phase 3 Record convention: empty collection fields are
preserved, so an empty search or page now returns `{"items":[]}`.
The accepted STEP-1 starter schema did not provide proposal registration;
[STEP-2](candidate-proposals.md) adds that surface under its own acceptance gate.

The handwritten BundleFactory binds Candidate to the native ActionCalls above
and delegates aggregate, view and entity services to the generated contracts.
It creates only the primary participant
and preserves the generated empty MCP-ready service policy. The executable
`ComponentFactorySpec` uses production `ComponentCreate`, repository origin and
`DefaultSubsystemFactory` in server mode. Its two Given/When/Then examples assert
initialized component identity, primary role, both operation definitions and
single-participant membership, then false MCP readiness for both operations.
Two adjacent assembly-startup scenarios load the authored descriptor through
public `GenericSubsystemDescriptor.load`, copy native security bindings to add
a test-owned local subject, and use public `GenericSubsystemFactory.default`
in ordinary command mode with an explicit absolute `component-dev-dir` repository
and temporary `textus.local-data.root`. The complete descriptor must construct
the canonical component with both Candidate operations and admit
`SubsystemExecutionProfile.Fixed` through public `executionProfileC`. The negative
fixture applies a native `GenericSubsystemAssemblyDescriptorSource` record with
explicitly empty `subsystemCapabilities.providers` through public
`GenericSubsystemDescriptor.applyAssemblyOverrideC` to the same complete descriptor.
Clearing only the decoded provider vector inherits primary component defaults
and retains the original assembly source, which reapplies its providers. The
explicit native override must instead fail production admission with the required
optimistic-concurrency diagnostic and leave no constructed subsystem. Every scenario has explicit
Given/When/Then and should matchers; constructed subsystems shut down in
`finally`, without global JVM property or environment mutation. Its trace is
`spec:workbench-component-foundation; rule:FND-02/FND-03; phase:1; slice:S1-A`.

The [README](../../README.md) records the focused workflow in this order:

```sh
sbt --batch clean cozyGenerate cozyPrepareRuntime 'testOnly org.simplemodeling.textus.knowledgeworkbench.ComponentFactorySpec' cozyBuildCAR
```

The production `component-dev-dir` assembly scenarios consume complete native
`target/cncf.d` evidence, so runtime preparation must precede the focused tests
on a fresh build. Ordinary `clean` refreshes source-bound public Scaladoc staging
after handwritten Scala changes before CAR packaging.

The README also records the exact one-shot `command meta.help org.simplemodeling.textus.KnowledgeWorkbench`
invocation with the explicit assembly argument. Packaged CLI also receives the
operator instance when fixed-user settings are required. Acceptance requires
both Candidate operations in help with no assembly/profile failure diagnostics,
then successful registration and listing of the same ID/name in separate
ordinary command processes and actual SQLite row/file evidence under an isolated
test root. The subject-10 predecessor passed the ordinary clean/generation/
runtime-preparation workflow, all four focused factory/assembly scenarios and
CAR packaging, actual canonical CLI help and registration/list/filter/paging
with one inspected persistent SQLite row. Controlled normal/debug argument
routing passed all 16 cases. Factory/profile checks and metadata equality alone
do not prove persistence; this evidence includes actual separate-process CLI
operations. It does not verify Web/manual/OpenAPI/MCP routes.
The starter schema has no versioned transition; lifecycle revision-policy
validation belongs to its subsequent Step. Runtime scripts implement
the explicit project dependency, instance and prepared-classpath contract above; their
file modes and web descriptors are preserved. `target/` and launcher
`.cache/` are ignored. SBT-containing scripts are routed through the serial
command runner. No generated source under `target` is edited or committed.
STEP-1 documentation projection passed the selected generation/runtime-preparation,
focused specifications, packaging, canonical help and CAR lint gates. Independent
acceptance review and the focused hardbreak closure review were admitted before
the local Step commit. The parent owns validation and evidence reuse for later changes. The required successful complete
SBT suite remains the Phase 1 bootstrap release gate after Step commits and
comprehensive Phase review. Foundation predecessor evidence does not close
Phase 1 or its later business acceptance gates.

The former conceptual CML guidance is retained here through the owning
[architecture](../notes/architecture.md), [Phase 1 plan](../phase/phase-1.md),
[establishment journal](../journal/2026-09-23-knowledge-workbench-establishment.md)
and historical [TKL alignment review](../journal/2026/10/2026-10-02-phase-1-tkl-alignment-review.md).
The current Phase documents own acceptance after the planning split.

The downstream Phase 1 use case is producer proposal -> TKW registration ->
InformationCandidate -> evidence/context resolution -> editing/correction ->
semantic grounding/mapping -> human review -> approve/hold/reject. Use cases and
application workflow remain distinct modeling concerns. Both TKL prepared
proposals and Editing Studio confirmed capture submissions must converge on
this lifecycle; unprepared capture may lack PreparedMaterial/ProcessingRun.

CandidateSource retains logical input origin and source/proposal correspondence.
RawDataIndex retains supporting Resource/Evidence and available version,
snapshot, provenance, PreparedMaterial and ProcessingRun references. These
responsibilities do not require separate Aggregates or tables. The later lookup
uses `producerRef + proposalId`; sequential resend returns the existing Candidate
without overwriting human edits. Human decision and the reviewed content/evidence
target must be recorded; target changes require renewed review/approval. Reuse
CNCF revision/concurrency and canonical CNCF/KnowledgeHub types where semantics match.

TKW owns Candidate lifecycle and human work. TKL owns evidence federation,
preparation, KnowledgeProcessingContext, ProcessingRun and PreparedMaterial;
TKW retains external references without copying raw bodies or execution history.
Editing Studio owns editorial interaction and domain views. Mobile Capture
Confirm authorizes transfer only. KnowledgeHub owns canonical Information;
KnowledgeProjection maps it to downstream RDF/Open Knowledge. AI cannot perform
final Information Admission.

The accepted foundation closes STEP-1 only. Proposal registration, restart
lookup and sequential resend are accepted by the separate
[STEP-2 contract and binding](candidate-proposals.md). Evidence-aware formation, lifecycle
transitions/approval invalidation and available List/Detail/edit/review contracts
remain subsequent Phase 1 work. [Phase 1.1](../phase/phase-1.1.md) owns real
KnowledgeHub Admission, its outcome/failure semantics and Candidate/Evidence
trace. Existing-Candidate preparation remains a correlated request/result
follow-on flow; full DTOs, execution, TKL feedback and live provider integration
are excluded. No hashes, separate receipt/versioning framework, recovery engine
or application-local storage/process implementation is introduced here.
