# Textus Knowledge Workbench

Shared editing direction (2026-10-21): Workbench supplies common knowledge-work
and Candidate operations to Editing Studio and other domain editors. It composes
TKL's public Material APIs; TKL shares its ordinary access model with OpenClaw
continuation processing. See the
[component specification](docs/spec/knowledge-editing-component-boundary.md) and
[integration plan](docs/phase/knowledge-editing-integration-plan.md).
This planned connection does not imply live TKL acceptance.

Textus Knowledge Workbench (TKW) forms Information Candidates for human review
and approval. The current development foundation is a standard Cozy-generated
CAR with canonical component identity `org.simplemodeling.textus.KnowledgeWorkbench`,
Scala package `org.simplemodeling.textus.knowledgeworkbench`, and version
`0.1.0-SNAPSHOT`.

The CML `STYLE` declaration is the sole style authority and selects
`full-fledged-with-standalone` without parameters or mode keys. In the selected
CNCF catalog this is `full-fledged-with-standalone@1`, providing `domain.full@1`,
`user.fixed-context-compatible@1` and `user.multi-user@1`. It requires the CNCF
Subsystem to supply `datastore.optimistic-concurrency@1`,
`datastore.persistent@1`, `datastore.transactional@1` and
`user-context.current@1`. The Subsystem owns datastore/provider placement and
current-user context; this declaration alone does not prove activation or
persistence.

The canonical authored ABI source is `src/main/car/abi-manifest.json`, in
`cozy.car.abi-manifest.v2` format with `abi.version=1`. It declares the component
coordinate, Candidate and MaterialWork services, their operations and value
types, seven nominal DATATYPEs, the
`ProposalPreparationState` and `CandidateDecisionKind` POWERTYPEs and the
InformationCandidate entity with their complete fields. Its KnowledgeLake
dependency requires ABI range `[0.1.0,0.2.0)`. Later CML surface or component-coordinate changes must update
the canonical ABI exports/version coherently and revalidate ordinary CAR
packaging and runtime preparation.

Preparation state uses explicit stable codes: `prepared=1`, `unprepared=2` in
the native datastore, while JSON and CLI retain `prepared` / `unprepared`.
Definition order does not assign codes. The remaining semantic scalar fields
keep their text representation. Core Phase 3 collection semantics adopted
through CNCF Phase 115 preserve empty list output as `{"items":[]}`.

The starter `Candidate/registerCandidate` operation accepts a name and optional
description; `Candidate/listCandidates` searches InformationCandidate entities.
`Candidate/registerProposal` accepts one JSON proposal, creates a native
candidate if its exact producer/proposal key is absent, and returns the current
candidate on sequential resend without overwriting human edits.
`Candidate/findCandidateByProposal` looks up that persisted key without writes.
`MaterialWork/createCandidateFromMaterial` explicitly connects an exact authorized
Material/resource selection to native Candidate registration. Retain a caller-owned
`proposalId` before submitting; the fixed producer is `workbench-material`. A valid
same-intent resend returns current work without source reads or overwriting human
edits. Initial evidence stays unverified and unversioned. See the
[Material-backed work contract](docs/spec/material-candidate-work-v1.md).
The source and executable specifications are authored; generation, native validation
and Step acceptance remain workflow-owned. The PostgreSQL specification uses the
same Testcontainers PostgreSQL 17.11 fixture approach as KnowledgeHub and requires
that image to be available locally; it never falls back to SQLite.
All Candidate operations remain outside MCP publication. STEP-2 is accepted
under the local acceptance binding in the [execution checklist](docs/phase/phase-1-checklist.md).

See the [proposal specification](docs/spec/candidate-proposals.md),
[component foundation specification](docs/spec/component-foundation.md),
[packaged reference manual](src/main/car/manual/index.md),
[user guide](src/main/car/manual/user-guide.md),
[Phase 1 plan](docs/phase/phase-1.md), [Phase 1 checklist](docs/phase/phase-1-checklist.md)
and [architecture](docs/notes/architecture.md) for scope and acceptance boundaries.

Prerequisites are Java compatible with the Scala/SBT toolchain, SBT `1.9.7`,
Cozy `0.3.3-SNAPSHOT` on `PATH`, sbt-cozy `0.1.18-SNAPSHOT`, CNCF
`0.5.3-SNAPSHOT`, Scala `3.3.8` and ScalaTest `3.2.19`. The build uses local
Ivy, local Maven and SimpleModeling repositories. Override the sbt-cozy version
with `-Dsbt.cozy.version=...` or `SBT_COZY_VERSION` when required.

The CNCF version is explicitly declared in `project.yaml` under
`build.dependencies.compile`, currently as
`org.goldenport::goldenport-cncf:0.5.3-SNAPSHOT`. This declaration is the sole
version selection. Build settings reject a missing CNCF dependency, a blank
version or multiple CNCF compile declarations as a configuration error before
dependency resolution. `CNCF_VERSION`, `CNCF_VERSION_FILE`, `CNCF_SAMPLES_ROOT`,
`CNCF_BIN` and sibling checkout locations do not select or supply a version.

The focused development workflow is:

```sh
sbt --batch clean cozyGenerate cozyPrepareRuntime 'testOnly org.simplemodeling.textus.knowledgeworkbench.CandidateProposalSpec org.simplemodeling.textus.knowledgeworkbench.ComponentFactorySpec' cozyBuildCAR
```

Ordinary `clean` refreshes source-bound public Scaladoc staging after handwritten
Scala changes. Prepare runtime before the `component-dev-dir` assembly tests so
a fresh checkout uses complete native `target/cncf.d` evidence without relying
on previous generated outputs.

For agent execution, route this SBT invocation through `cncf_command_runner`
and the machine's serial SBT wrapper. Generated sources are under
`target/scala-3.3.8/src_managed/main/scala`; edit the CML and handwritten
factory/spec sources. Normal generation and CAR packaging project the CML style
and canonical component identity into their generated descriptors and package
the complete ABI exports. `cozyPrepareRuntime` writes
`target/cncf.d/runtime-classpath.txt` and the development runtime manifest using
the generated component descriptor and canonical authored ABI source. Keep
these source declarations current and regenerate through the ordinary workflow.

The authored `src/main/car/assembly-descriptor.yaml` binds the canonical
Workbench component and declares all four required runtime facilities. It
selects standalone user mode and the framework's canonical application datastore
`local-default` policy. CNCF manages `ComponentDataStore`/`SqlDataStore`; the
declaration passed isolated foundation runtime activation and persistence checks
in the subject-10 predecessor evidence; production instance configuration must
still supply its own valid identity and datastore context.
Cozy packages this assembly input normally. Development projection deliberately
excludes the adjacent assembly descriptor, so startup needs the explicit
operator instance below even after packaging or runtime preparation.

Copy the complete authored descriptor to an operator-owned instance file:

```sh
export WORKBENCH_ASSEMBLY_DESCRIPTOR="/absolute/path/to/workbench-instance.yaml"
cp src/main/car/assembly-descriptor.yaml "$WORKBENCH_ASSEMBLY_DESCRIPTOR"
```

Edit that copy's existing `security.authentication` mapping to add the explicitly
selected stable standalone user ID:

```yaml
security:
  authentication:
    convention: disabled
    fallbackPrivilege: disabled
    localSubject:
      id: <selected-stable-user-id>
```

Use the selected identity from an admitted HOME standalone profile when present.
The base descriptor supplies no production user ID, and the instance is a full
descriptor with no presumed interpolation or `extends` syntax. Conflicting
fixed-user IDs require datastore isolation or explicit migration under CNCF's
existing rule. The application datastore uses the canonical namespace/component
path; an operator can explicitly select an isolated `textus.local-data.root` in
the instance's `config` mapping. This preserves ordinary CNCF persistence and
current-user ownership without a privilege override.

After changing dependency declarations, run `cozyPrepareRuntime` again through
the ordinary SBT route before startup. With that instance selected, normal and
debug server startup are:

```sh
scripts/run-server.sh
scripts/run-server-debug.sh
```

This invokes the project launcher with the prepared classpath file and existing
main class, port and base URL options. Both scripts first reject a missing or
empty prepared classpath, then require `WORKBENCH_ASSEMBLY_DESCRIPTOR` to name
a readable regular file. They pass its exact path as one
`--textus.assembly.descriptor=<path>` argument after the launcher delimiter.
Debug uses the same launcher and classpath plus JDWP with `DEBUG_PORT` (default
`5005`). `scripts/update-runtime-classpath.sh` retains SBT routing and consumes
the same project dependency declarations and configuration checks.

After classpath preparation, the exact one-shot component help invocation from
the repository root is:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command meta.help org.simplemodeling.textus.KnowledgeWorkbench
```

Packaged CLI use also requires the operator instance when fixed-user settings
are needed. STEP-1 acceptance required component help containing both starter Candidate
operations with no assembly/profile failure diagnostics; exit zero alone is
insufficient. It also requires actual `KnowledgeWorkbench.Candidate.registerCandidate`
and `KnowledgeWorkbench.Candidate.listCandidates` in separate ordinary command
processes to return the same stored ID/name, with the SQLite row/file inspected
under an isolated test datastore root. The subject-10 predecessor evidence passed
the ordinary clean/generation/runtime-preparation workflow, all four focused
factory/assembly tests and CAR packaging. Actual CLI help, registration and
separate-process list, description text filtering and offset/limit checks passed
with one persistent SQLite row inspected under isolated storage. Controlled
normal/debug routing passed all 16 cases. Metadata and factory initialization
alone do not establish persistence acceptance. STEP-1 documentation generation,
packaging/help/lint and independent review were admitted, including focused
hardbreak closure review. STEP-1/S1-A is accepted at local commit
`70620d1aeaae4a3859df93d6ff6948bdcff5742c`, recorded by
`tkw-phase1-step1-post-hardbreak-commit-acceptance-20261003@1` and review ledger
`tkw-phase1-review-ledger-20261002@4`. STEP-2 has completed its focused
proposal validation, representative CLI evidence, development CAR lint and independent
review for its local acceptance binding. These CLI checks do not
verify Web/manual/OpenAPI/MCP routes or complete Phase 1 acceptance.
Runtime startup uses the Workbench-prepared classpath; scripts that invoke SBT
use the serial command runner. The Phase 1 bootstrap release gate requires
`sbt --batch test` on the accepted final tree.

The prepared [TKL fixture](src/test/resources/proposals/tkl-prepared.json)
and unprepared [Editing Studio fixture](src/test/resources/proposals/editing-studio-unprepared.json)
use the same `--proposalJson` input. Both retain the exact first proposal and
supplied source/evidence associations in InformationCandidate. Prepared input
requires a material reference; unprepared capture may lack material/run and
keeps its evidence limitations. Capture confirmation is transfer only. Use the
[user guide](src/main/car/manual/user-guide.md#register-and-look-up-proposals)
for registration, lookup, generated content-only edit and same-key resend examples.

Phase 1 is closed at `f96057bd85f17b5647c520e2f00e690e14c63363`, retaining
its proposal registration, human formation/approval and List/Detail/edit/review
contracts and full-suite/review history. Real KnowledgeHub Admission belongs to
[Phase 1.1](docs/phase/phase-1.1.md), currently in progress; TKL preparation,
feedback and live provider adapters remain follow-on work.

Accepted STEP-3 adds revision-aware `Candidate.getCandidateWork`,
`editCandidateWork` and `decideCandidateWork` operations. Explicit human decisions
retain their exact evidence target and native actor/time; target changes require
renewed review. See the [formation contract](docs/spec/candidate-formation-and-review.md).
Its local acceptance binding is `tkw-step3-acceptance-20261027-01@1`.

Accepted STEP-4 adds the same-origin Candidate Workbench entry at
`/web/knowledge-workbench/textus-knowledge-workbench`, backed by generated native
Display List/Detail and the existing conditional human edit/decision operations.
See the [view contract](docs/spec/candidate-workbench-views.md) and packaged
user guide for operation and failure reconciliation. Step acceptance and Phase
closure remain governed by the execution checklist. STEP-4's local binding is
`tkw-step4-acceptance-20261027-01@1`; the Phase closure is recorded in its plan/checklist.

Phase 1.1 adds explicit `Candidate.admitCandidateWork` and
`confirmCandidateAdmission`, using KnowledgeHub's public Information Operations.
The authored assembly now includes KnowledgeHub `0.1.0-SNAPSHOT`; install its
matching CAR through the normal component repository. No Maven implementation
JAR is required. A reserved native Information identity is saved before submission;
uncertain outcomes are resolved only by lookup of that identity, without resend.
See the [Admission contract](docs/spec/candidate-information-admission.md) and
[user guide](src/main/car/manual/user-guide.md). Integration tests select the actual
published CAR with JVM property `textus.test.knowledge-hub.car`; the default is its
standard `~/.cncf/local/repository/car` coordinate. Native validation and whole-Phase
acceptance are still pending in the [execution checklist](docs/phase/phase-1.1-checklist.md).
