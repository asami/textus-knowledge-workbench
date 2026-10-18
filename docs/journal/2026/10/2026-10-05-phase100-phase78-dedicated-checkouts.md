# Workbench upstream development: CNCF Phase 100 and Cozy Phase 78

Date: 2026-10-05
Disposition: four dedicated checkouts admitted; Phase 1 Goal active; Core prerequisite focused validated (94/94), independently reviewed (PASS), locally committed as `8c0d31e768f600045f16c4dbdb3a93bdf63b8f3c` and locally published. CNCF codec passed 26/26 focused tests and independent focused closure (PASS). SimpleModeler producer passed 17/17 tests in four suites and its local dependency refresh succeeded. Cozy generation ran 32 tests: 31 passed, one operation-schema regression remains. Downstream/final acceptance is pending.

## User direction

Develop CNCF Phase 100 and Cozy Phase 78 in dedicated checkouts for workbench.
These upstream Phases own the logical datatype, storage mapping and generated
metadata prerequisites needed by the workbench persistence scenario.

The user subsequently explicitly added goldenport-core and SimpleModeler:
`goldenport-coreとSimpleModelerも専用チェックアウトを使って。`
That direction resolves the additional source-owner scope question.

## Checkout allocation

All checkouts live under this workbench's ignored `.codex-workflow/checkouts/`.
CNCF, Cozy and Core start from their selected GitHub `main` revisions.
SimpleModeler starts from GitHub `scala12`, whose Scala 2.12 generator matches
the Cozy dependency; the older default `main` is not the generator baseline.

| Repository / Phase | Checkout | Branch | Starting revision |
| --- | --- | --- | --- |
| `asami/goldenport-cncf` / Phase 100 | `.codex-workflow/checkouts/goldenport-cncf-phase100` | `codex/workbench-cncf-phase100` | `21c1c25960bbf9db3d92c1d12c1ea043ce8af6cb` |
| `asami/cozy` / Phase 78 | `.codex-workflow/checkouts/cozy-phase78` | `codex/workbench-cozy-phase78` | `be780917fffb2b71cf10d452a40baad416cd482b` |
| `asami/goldenport-core` / canonical datatype prerequisite | `.codex-workflow/checkouts/goldenport-core-types` | `codex/workbench-core-types` | `3c8ac18c528807b88a6bf3f92dc79a8ba775d7dd` |
| `asami/simple-modeler` / semantic model and generator prerequisite | `.codex-workflow/checkouts/simplemodeler-types` | `codex/workbench-simplemodeler-types` | `494c72a5617ae3c683bd7a88bfda9bc7ae41b076` |

The declared `ai/directive` submodule is initialized at each repository's
pinned revision. Existing main checkouts and their local implementation work
remain separate. This setup imports no uncommitted implementation into the
dedicated checkouts.

## Development and acceptance order

1. CNCF DSM-100-01 inventories the affected declarations, codecs and consumers.
2. CNCF DSM-100-02 freezes the logical datatype, storage mapping and
   producer-facing ABI with the expected generated fixture.
3. Cozy Phase 78 consumes that contract and implements the CML -> semantic
   datatype -> generated Scala/schema/storage metadata route.
4. CNCF DSM-100-03/04 consumes the generated fixture and connects OR mapping
   and consumer transition. DSM-100-05 owns existing-data recovery rehearsal.
5. CNCF DSM-100-06 accepts the generated fixture through provider/restart
   roundtrips, including the workbench scenario. Cozy Phase 78 retains its
   downstream acceptance requirement.

The workbench proposal-original-text regression remains an upstream-dependent
acceptance case. After the upstream handoff, select its declared dependency
version and checkout explicitly and rerun the affected workbench validation.
These checkout locations do not implicitly select runtime versions.

## Setup result

The dedicated checkouts contain the latest Phase 100 and Phase 78 plans and
their checklists. No upstream implementation, SBT execution, dependency-version
change, Step/Phase acceptance, commit or push was performed by this setup.
The existing workbench Phase 1 acceptance requirements remain in force.

## Source inventory and required owners

The dedicated branches are now recorded as participants of the retained
Workbench development context. Original Workbench authority/base, accepted
Step 1, PLAN epoch and the three existing reviews remain unchanged; prior
reviews do not cover the added repositories.

Read-only source inspection located the missing canonical types in
goldenport-core's sealed `org.goldenport.schema.DataType` hierarchy. The active
`Named("record")` emission and EntityStoreAttribute generation are in
SimpleModeler, which Cozy calls as a dependency. Thus the canonical route needs
dedicated goldenport-core and SimpleModeler development checkouts as well.
The initial setup made no source changes in those repositories. Core datatype
implementation started after the retained Phase Goal became active.

The CNCF branch now records its initial inventory in
`docs/journal/2026/10/2026-10-05-phase100-source-inventory.md`; the Cozy branch
records producer ownership in
`docs/journal/2026/10/2026-10-05-phase78-producer-inventory.md`.
Both inventories keep their Phase checklist items OPEN. The source scope is now
explicitly authorized and both prerequisite checkouts were created and
verified clean at setup, with `ai/directive` at the declared revision
`a727716b856349c0ae5da4cc7f844c3d5842fe12`.
That setup result does not describe Core's current working-tree cleanliness.

## Resumed development

Native `get_goal` now reports the retained Phase 1 Goal as `active`. The parent
reassessed the expanded planning boundary and admitted Core and SimpleModeler
through RepositoryAddition in the existing review ledger. That admission was
recorded at revision 8; the accepted independent Core review advances the ledger
to revision 9. The
original Goal, Workbench authority/base, existing CNCF and Cozy additions, PLAN
epoch, accepted Step 1, three prior reviews and repair counters are retained.

Core's initial `XJson` and `XSemiStructuredData` slice with explicit JSON conversion
was authored and passed 62/62 focused tests across four suites, including
existing datatype and Record I/O compatibility. Exact decimal values, null
members, empty objects and arrays are preserved. The out-of-range exponent
fixture uses `1e2147483649`; the previous `1e2147483648` is representable at
BigDecimal's minimum scale and was an incorrect failure fixture.

The subsequent explicit JSON/YAML/HOCON intake and typed-reader slices are
authored, independently of SQL storage selection. The bounded compile/numeric
corrections and initial 62/62 result are prior history. The combined Core
prerequisite passed 94/94 focused tests across six suites, and the complete
Core protected focused independent conformance review returned PASS with no
blockers. The exact ten-path Core prerequisite was committed on
`codex/workbench-core-types` as `8c0d31e768f600045f16c4dbdb3a93bdf63b8f3c`.
The parent verified the actual branch, original single parent, exact message,
committed paths and clean Core working tree. Local dependency publication,
generator/provider integration, final full-suite validation and Phase closure
remain pending. No Workbench Step 2, CNCF Phase 100 or Cozy Phase 78 acceptance
is claimed by these focused Core results.

The next actions are local Core dependency publication and focused validation
of the authored CNCF value-codec ABI, followed by field-domain ABI freeze and SimpleModeler
semantic/schema/storage generation -> Cozy projection and generated fixture
-> CNCF mapped persistence -> Workbench validation. The Phase 100
`docs/spec/datastore-logical-type-mapping.md` proposal is the selected behavioral
input. Field-domain producer ABI, downstream implementation, generation and test
acceptance remain pending. Runtime dependency versions and checkouts must still
be selected explicitly; focused Core acceptance does not close the independent
Phase boundaries.

## Local dependency handoff

The selected producer is the dedicated Core checkout above, at commit
`8c0d31e768f600045f16c4dbdb3a93bdf63b8f3c`. CNCF's dedicated `build.sbt`
explicitly consumes `org.goldenport:goldenport-core_3:0.4.3-SNAPSHOT`.
The planned command is exactly `sbt --batch publishLocal`, with the standard
Ivy output `/Users/asami/.ivy2/local/org.goldenport/goldenport-core_3/0.4.3-SNAPSHOT`.
This is a local dependency refresh at the unchanged declared version.

Request `tkw-core-local-refresh-20261005-a1` was prepared as typed V5 and
assigned to `/root/sbt_receipt_92bdac738a934a97b04e`. That runner stopped before
rendering or submitting SBT: it reported the originating instruction
unobservable, then rejected the one same-runner continuation because its
loaded recovery instructions cover converted V4 rather than this V5 request.
No SBT execution or platform denial occurred. The original permission and
unconsumed request remain recorded; publication is not accepted. Aligning that
runner handoff is an execution prerequisite, not a request to repeat user
authorization. CNCF ABI planning can continue from the accepted Core source.

## CNCF value-codec authoring

The dedicated CNCF checkout now contains `EntityStoreValueCodec.scala`, its
public-behavior specification and the value-codec signatures in
`docs/spec/datastore-logical-type-mapping.md`. String, Json, Record, explicitly
bound application classes and recursively declared arrays have authored codecs.
These changes have not yet been compiled, tested or independently reviewed.

Parent authoring inspection clarified the ambiguous original null-root wording:
an explicit application codec may represent a nonnull class value as Json.Null
and reconstruct it, including an array element. Native application null, SQL
NULL and present JSON null remain distinct. Finding
`CNCF-CLASS-JSON-NULL-001` was corrected in exactly the codec, its specification
and the normative document by `/root/tkw_cncf_class_json_null_fix`
(`cncf_fix_worker_terra`, configured `gpt-6.1-sol` / `high`). E17 retains invalid
native-null and incompatible-class cases; E18 adds the selected JSON-null
representation roundtrips and invalid-shape cases. Parent source inspection and
whitespace checks found no outstanding issue in this correction. This is
authoring evidence, not executed regression coverage or independent acceptance.

Core and SimpleModeler remain on their dedicated branches above. Core's current
HEAD is `8c0d31e768f600045f16c4dbdb3a93bdf63b8f3c`; SimpleModeler's current HEAD
remains its starting revision. Both working trees were verified clean. The
field declarations, provider wiring, SimpleModeler/Cozy generation and Workbench
acceptance are still required.

## Structured producer bridge and current execution prerequisite

Core and SimpleModeler continue to use the dedicated checkouts and branches in
the allocation table. Core remains at its accepted prerequisite commit; the
initial SimpleModeler producer manifest encountered an existing
`MStructuredDataType` case class for user-defined complex DATATYPEs before any
edit. The parent revised the unaccepted manifest to introduce the distinct
`MBuiltinStructuredDataType` name, preserving that existing class and its
consumers. Revision 2 owns ten paths across the dedicated SimpleModeler and
Cozy checkouts and is assigned to `/root/tkw_structured_producer_builtin`
(`cncf_implementation_worker_terra`, configured `gpt-6.1-sol` / `high`).
The typed semantic node retains Json versus Record through multiplicity,
runtime class and canonical schema generation at the authored-source level.
All ten owned paths have now been authored and inspected by the parent. The
SimpleModeler specification contains six Given/When/Then behavior blocks and
Cozy contains two, covering semantic projection and actual generation. Tracked
and new-file whitespace checks reported no errors. No generator test has yet
executed; compilation, independent review and downstream persistence
acceptance remain pending. This is progress in implementation, not a test pass
or Step/Phase acceptance.

The subsequent current Core refresh request
`tkw-core-local-refresh-20261005-a2` was assigned to
`/root/tkw_core_refresh_current` (`cncf_command_runner`, configured
`gpt-6-luna` / `medium`). It also stopped before rendering or submitting SBT.
The runner reported only an environment block in its native user context but
treated the selected origin as present-but-unobservable rather than absent.
Its one continuation rejected the V5 request under its V4 retention-recovery
wording. No history helper rejection, SBT start, platform denial or publication
result occurred. This retains the execution prerequisite, not a test failure.

A reviewable clarification proposal is prepared in ignored local Phase data:
`runner-origin-clarification-proposal.patch`. It targets the canonical shared
command-runner definition and native-history input reference. It distinguishes
actual native-message absence from a present message whose exact text cannot
be observed, and permits one fresh contract replacement only for the former
pre-helper stop after the clarified role is deployed. Parent source admission,
standing authorization, history-helper acceptance, one-use permission,
scoped platform review, shared SBT serialization and terminal evidence remain
required. The proposal has not been applied or deployed; its shared source
repository is outside the five admitted product roots.

The proposal passed read-only `git apply --check` against the canonical shared
repository, and its candidate TOML parsed with the same registered role,
model, effort and sandbox profile. These checks do not establish behavioral
acceptance. The user subsequently answered `承認する`, approving the two
shared-source changes and copying the selected runner definition to its
deployment. Decision `tkw-phase1-shared-runner-origin-clarification-20261005`
is resolved at decision revision 2 and Phase control revision 28. Native
`get_goal` reports the same complete Phase 1 Goal as active again. The approval
and consumed resolution are retained in local Phase decision data.

At approval consumption, the authorized next state was parent capability
assessment and PLAN for the bounded shared repair. No further approval for
this same repair is required. All product
checkouts, original bases, accepted work, PLAN epoch, review history, repair
counters and full Phase acceptance obligations are retained.

## Approved runner repair and resumed validation

The bounded shared repair is now applied to exactly
`common/codex/agents/cncf_command_runner.toml` and
`common/codex/skills/cncf-command-execution/references/sbt-inherited-history-input.md`
in development-workstation. The registered fix worker
`/root/tkw_shared_runner_origin_fix` (`cncf_fix_worker_terra`, configured
`gpt-6.1-sol` / `high`) applied the approved patch. Parent diff inspection and
`git diff --check` passed; the selected regular-file runner definition was
copied to `/Users/asami/.codex/agents/cncf_command_runner.toml` successfully.
Shared changes remain uncommitted and are an explicitly approved execution
prerequisite, outside the five-repository product Phase acceptance ledger.

Fresh request `tkw-core-local-refresh-approved-20261005-a3` was separately
admitted under the current human approval and assigned to registered
`cncf_command_runner` `/root/tkw_core_refresh_approved` (configured
`gpt-6-luna` / `medium`). The unchanged producer, subject revision 2, exact
`--batch publishLocal`, local SNAPSHOT coordinate and output scope were retained.
The original request, source instruction, authority and old permission
selection remain preserved in predecessor a2; its stopped child is retired
do-not-resume. A fresh permission was selected without copying or rebinding
the old permission.

The fresh child's actual-origin-absence preflight accepted the existing
`asami-local-sbt-v1` standing branch with `origin_present: false`. The scoped
submission completed, and parent typed local-publication admission verified
SBT exit 0, wrapper exit 0, terminal completion and shared-lock release.
The actual local `goldenport-core_3` `0.4.3-SNAPSHOT` JAR contains `XJson`,
`XSemiStructuredData` and `StructuredDataFormat`. Actual log:
`/private/tmp/skill.cncf.d/a-8ce60732-7c96-495a-9961-f11bf1e97c16/6304-20261005T015204Z.log`.
This accepts dependency availability only, not consumer behavior or Phase closure.

The independent M2 focused review of the shared repair is assigned read-only
to `/root/tkw_shared_runner_origin_review` (configured `gpt-6.1-sol` / `high`).
Its result is PASS with explicit empty blocker, Hygiene and Development
Candidate lists. The parent accepted this approved execution prerequisite
using that independent result and the actual successful fresh-runner behavior;
no product Step or Phase acceptance is inferred. CNCF codec validation request
`tkw-cncf-codec-validation-20261005-a1` is assigned to registered
`cncf_command_runner` `/root/tkw_cncf_codec_sbt_a1` (configured
`gpt-6-luna` / `medium`) for `EntityStoreValueCodecSpec` and existing
`EntityStoreRecordProjectionSpec`. That first execution ran 23 tests: 22 passed
and one E16 diagnostic expectation failed. The unchanged canonical diagnostic
identity is `application(<codecIdentity>)`; two expected substrings had omitted
its wrapper. Frozen M1 repair `CNCF-CODEC-DIAGNOSTIC-EXPECTATION-001` changed
exactly those two expected substrings through `/root/tkw_cncf_codec_diagnostic_fix`
(`cncf_fix_worker_luna`, configured `gpt-6.1-sol` / `medium`). Fixtures,
production behavior and assertion strength were retained.

Fresh request `tkw-cncf-codec-validation-20261005-a2`, registered runner
`/root/tkw_cncf_codec_sbt_a2` (configured `gpt-6-luna` / `medium`), passed all
23 tests in two suites. Parent typed validation admission accepted the complete
subject revision 2, consumed permission, readable log/summary, SBT/wrapper exit 0
and released lock. Actual log:
`/private/tmp/skill.cncf.d/a-7a14690a-4cfe-4d41-a799-60df3732cd36/9020-20261005T020652Z.log`.
Parent whole-file/specification/whitespace preflight passed. Independent
protected value-codec review is assigned read-only to
`/root/tkw_cncf_codec_protected_review` (configured `gpt-6.1-sol` / `high`);
its result is pending. This remains pure value-codec coverage, not field/provider
integration, original-text readback or upstream/product Phase acceptance.

SimpleModeler planned validation `tkw-simplemodeler-structured-validation-20261005-a1`
passed production compilation but failed Test compilation at new specification
lines 89–90: its `SClassBase` result has public `importNames`, not `core`.
No behavior test ran; actual SBT/wrapper exit 1 and serial lock release were
retained. Frozen M1 repair `MODELER-STRUCTURED-SPEC-IMPORT-001` changed only two
test accessors to `generated.importNames`, preserving actual semantic fixtures
and exact Json/Record import expectations. Registered fix worker
`/root/tkw_modeler_import_spec_fix` (configured `gpt-6.1-sol` / `medium`) authored
the correction, and parent inspected it and checked whitespace. Fresh request
`tkw-simplemodeler-structured-validation-20261005-a2` selects complete producer
subject revision 2 and registered `/root/tkw_simplemodeler_structured_sbt_a2`
(configured `gpt-6-luna` / `medium`) for the same four suites. Its result and all
producer/Cozy integration acceptance remain pending.

## Current prerequisite validation after review repair

The CNCF independent Step-prerequisite review found two bounded blockers:
`CPB-TKW-CNCF-CODEC-20261005-RECURSIVE-BINDING` and
`CPB-TKW-CNCF-CODEC-20261005-GWT-ACTION-PLACEMENT`. Empty arrays could bypass
an invalid child binding, and E4/E9/E10 executed some codec actions after Then.
The two-path repair by `/root/tkw_cncf_codec_recursive_binding_fix`
(`cncf_fix_worker_terra`, configured `gpt-6.1-sol` / `high`) recursively validates
selected bindings before parsing/traversal, moves the existing actions to When,
and adds public regression matrices with valid-empty controls. Fresh a3
validation passed 26/26 tests in two suites, with SBT/wrapper exit 0 and the
shared lock released. The actual log is
`/private/tmp/skill.cncf.d/a-a169f76e-56ce-4387-b037-c16af65172d9/14300-20261005T023026Z.log`.
One consumed Step repair is retained against the original Step review and
ledger revision 11. Independent focused closure is assigned to
`/root/tkw_cncf_codec_focused_review` (`cncf_rereview_worker_luna_xhigh`,
configured `gpt-6.1-sol` / `high`); its result is pending. No full Phase review
has been consumed.

SimpleModeler a2 compiled and executed 14 tests in three suites: 11 passed and
three failed. The new specification incorrectly expected the legacy parser to
accept `custom-proposal` and expected `scala.Vector` instead of the existing
`scala.collection.immutable.Vector` type tree. Also, the parent had changed
the scalar-suite selector based on its filesystem path, omitting that suite;
its actual package is `generator.scala.model`. The original four-suite plan
was correct. Frozen spec-only repair by `/root/tkw_modeler_behavior_spec_fix`
(`cncf_fix_worker_luna`, configured `gpt-6.1-sol` / `medium`) retains concrete
unknown-input rejection and exact structured leaves/containers. The existing
GWT gate was completed within that same manifest before validation.

Fresh a3 restored the actual scalar-suite package and passed all 17 tests in
four suites, with SBT/wrapper exit 0 and the shared lock released. Actual log:
`/private/tmp/skill.cncf.d/a-36be9974-ee3e-480d-b130-91f2e6b93b3c/15241-20261005T023758Z.log`.
The dedicated SimpleModeler unchanged `1.1.26-SNAPSHOT` local refresh is now
assigned to `/root/tkw_simplemodeler_local_refresh` (`cncf_command_runner`,
configured `gpt-6-luna` / `medium`) for the explicitly declared Cozy dependency.
No producer acceptance, generated runtime compilation, provider persistence,
Workbench original-text readback or separate Phase acceptance follows from
these prerequisite tests.

## Recorded focused closure and consumer failure

The independent CNCF focused reviewer returned PASS, resolving both frozen
blockers with no new blocker, finding or evidence gap. The parent admitted
`tkw-cncf-codec-focused-closure-20261005@1` into the retained review ledger,
now revision 12, and closed the same consumed Step repair as accepted.
The earlier nonblocking SqlDataStore source-size Hygiene remains OPEN.
This accepts the pure value-codec prerequisite only. Native Phase 1 Goal
remains active; Phase control is PLAN at revision 34; no full Phase review or
release acceptance is claimed.

The SimpleModeler local refresh completed successfully at unchanged
`org.simplemodeling:simplemodeler_2.12:1.1.26-SNAPSHOT`. Parent typed admission
verified terminal success and lock release, and inspected the actual local
JAR for `MBuiltinStructuredDataType` and `TypeName.Structured`. The output
remains the exact bound Ivy directory. It supplies dependency availability,
not additional tests or producer acceptance.

Cozy a1, `/root/tkw_cozy_structured_sbt_a1` (`cncf_command_runner`, configured
`gpt-6-luna` / `medium`), compiled and ran both selected suites: 32 tests,
31 passed, one failed. SBT/wrapper exit 1 and lock release are verified.
Actual log:
`/private/tmp/skill.cncf.d/a-2bbe16b8-1542-4dd9-abde-e74b95d40024/17743-20261005T024355Z.log`.
`ModelerStructuredDataGenerationSpec.scala:85` detects
`DataType.Named("json")` in selected actual generated products. Semantic CML
projection and entity schema canonicalization pass; generated update-operation
request schemas still contain `Named("json")` and `Named("semistructureddata")`.
The read-only parent diagnosis locates the remaining producer edge in
SimpleModeler `ComponentPart._operation_field_parameter_definition_expr`,
which maps `OperationField.datatype` through a separate String-based schema
generator lacking canonical structured cases.

This is current producer integration failure
`MODELER-STRUCTURED-OPERATION-SCHEMA-001`, not a test waiver, platform denial or
Development Candidate deferral. No repair of that new source path has started
in this focused-closure turn. Its next bounded preparation must retain the
authorized SimpleModeler/Cozy ownership and complete producer review obligation.
Generated runtime compilation, field/provider mapping, process-restart and
Workbench original-text behavior, remaining Workbench Steps and all complete
Phase closure requirements remain pending.
