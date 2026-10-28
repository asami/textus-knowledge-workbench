# Textus Knowledge Workbench user guide

This guide runs starter and proposal Candidate operations for
`org.simplemodeling.textus.KnowledgeWorkbench` version `0.1.0-SNAPSHOT`.
The [reference manual](index.md) explains results, security and scope.

## Prepare the repository

Use Java compatible with the Scala/SBT toolchain, SBT `1.9.7`, Cozy
`0.3.3-SNAPSHOT` on `PATH`, sbt-cozy `0.1.18-SNAPSHOT`, CNCF
`0.5.3-SNAPSHOT`, Scala `3.3.8` and ScalaTest `3.2.19`. The build resolves
through local Ivy, local Maven and SimpleModeling repositories. The sbt-cozy
version may be overridden with `-Dsbt.cozy.version=...` or `SBT_COZY_VERSION`.

Run every command below from the repository root. The ordinary focused workflow
is:

```sh
sbt --batch clean cozyGenerate cozyPrepareRuntime 'testOnly org.simplemodeling.textus.knowledgeworkbench.CandidateProposalSpec org.simplemodeling.textus.knowledgeworkbench.ComponentFactorySpec' cozyBuildCAR
```

For agent execution, the parent routes this invocation through
`cncf_command_runner` and the machine's serial SBT wrapper. Runtime preparation
must precede the assembly scenarios on a fresh build. It writes
`target/cncf.d/runtime-classpath.txt`; rerun preparation after dependency changes.
Ordinary clean also refreshes source-bound Scaladoc staging.

`project.yaml` owns the CNCF version under `build.dependencies.compile`, currently
`org.goldenport::goldenport-cncf:0.5.3-SNAPSHOT`. Missing, blank or duplicate CNCF
declarations fail build configuration before dependency resolution. Legacy
`CNCF_VERSION`, `CNCF_VERSION_FILE`, `CNCF_SAMPLES_ROOT` and `CNCF_BIN` do not
select a version or launcher.

## Select an operator instance

Choose an absolute path in an existing operator-owned directory and copy the
complete authored assembly descriptor:

```sh
export WORKBENCH_ASSEMBLY_DESCRIPTOR="/absolute/path/to/workbench-instance.yaml"
cp src/main/car/assembly-descriptor.yaml "$WORKBENCH_ASSEMBLY_DESCRIPTOR"
```

Edit that copy's existing security mapping to set your explicitly selected
stable standalone identity. Replace the placeholder before running:

```yaml
security:
  authentication:
    convention: disabled
    fallbackPrivilege: disabled
    localSubject:
      id: <selected-stable-user-id>
```

Use the identity from an admitted HOME standalone profile when present. Keep
all components, providers and existing configuration from the complete copy.
The base descriptor has no production user ID, and no interpolation or `extends`
syntax is assumed. A conflicting fixed-user ID requires explicit migration or
datastore isolation under CNCF's existing rule.

Optionally add an absolute isolated storage root to the copy's existing `config`
mapping, retaining both existing entries:

```yaml
config:
  textus.subsystem.user-mode: standalone
  textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.policy: local-default
  textus.local-data.root: /absolute/path/to/workbench-data
```

This remains the ordinary framework-managed persistent application datastore.
No extra application database or privilege override is required. Use the same
instance, user and storage root for subsequent commands. Development projection
excludes the adjacent assembly descriptor, so runtime preparation alone does not
select this instance. Only `WORKBENCH_ASSEMBLY_DESCRIPTOR` needs exporting for
the examples below.

## Run CLI operations

The project launcher consumes the prepared classpath, invokes
`org.goldenport.cncf.CncfMain` and passes discovery and assembly arguments after
`--`. Show canonical component help:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command meta.help org.simplemodeling.textus.KnowledgeWorkbench
```

Help should contain all four Candidate operations without assembly/profile failure
diagnostics; exit zero alone does not establish successful activation. Native
component Help retains the framework's generic summary. The authored operation
descriptions also supply Cozy metadata.

Register a new starter candidate with optional description:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.Candidate.registerCandidate --name "Research note" --description "Evidence for review"
```

Omit `--description` when it is not needed. Successful registration returns
`id` and `name`. Retain the native ID to recognize the stored entity. Launch a
separate process using the same instance to list accessible candidates:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.Candidate.listCandidates
```

Find the stored ID/name in `items`. Its optional description appears as
`description.value`; registration itself returns only ID/name. Search by a
case-insensitive substring of the description or name:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.Candidate.listCandidates --text "evidence"
```

Use native paging controls:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.Candidate.listCandidates --offset 1 --limit 1
```

With a single accessible row, that offset produces an empty page. Empty search
or page results render as `{"items":[]}` under the Core Phase 3 Record convention.
This is also the expected shape of a list with no accessible rows.
The [reference examples](index.md#listcandidates) show nonempty YAML/JSON output.

## Register and look up proposals

Use the same complete operator instance, fixed subject and native storage root
throughout this sequence. These source fixtures are in the repository's
`src/test/resources/proposals`; they are fixture mappings, with no live provider
transport claim. Shell command substitution supplies the fixture JSON as one
`--proposalJson` argument. The operation retains the exact string it receives;
the shell removes the trailing file newline in these examples.

Register the prepared TKL fixture:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.Candidate.registerProposal --proposalJson "$(cat src/test/resources/proposals/tkl-prepared.json)"
```

Successful first registration returns `created: true` and `candidate` containing
its native ID, current content/context, exact first `originalProposal`, evidence,
PreparedMaterial and ProcessingRun references. Keep the observed native ID.
Look up that exact source key in a separate process:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.Candidate.findCandidateByProposal --producerRef tkl-fixture --proposalId tkl-proposal-1
```

The result has the same persisted `candidate`, with no `created` field. Exact
case and whitespace matter. An absent key returns `{}` through the optional
candidate codec. Lookup never writes. More than one accessible matching row
fails explicitly as ambiguous correspondence.

Set the following variable to the native ID just observed, replacing the
placeholder. Exercise a content-only edit through the already generated native
entity operation:

```sh
WORKBENCH_CANDIDATE_ID="<observed-native-id>"
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.entity.updateInformationCandidate --id "$WORKBENCH_CANDIDATE_ID" --content "Human corrected content"
```

Observe native success, then look up the candidate to confirm its content is
`Human corrected content`. This is a persistence fixture; Step 3 owns the
human-facing edit/review lifecycle and native revision policy. Resend the same
valid source proposal from another ordinary process:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.Candidate.registerProposal --proposalJson "$(cat src/test/resources/proposals/tkl-prepared.json)"
```

The result has `created: false`, the same ID and the edited current content.
The exact first original proposal and source/evidence/material/run metadata
remain stored. A different valid payload with the same producer/proposal key
also returns that current candidate without incoming-field writes or payload
conflict. Validation still applies before any registration result. Sequential
resend promises no concurrent uniqueness guarantee or independent receipt state.

Register and separately look up the unprepared Editing Studio fixture:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.Candidate.registerProposal --proposalJson "$(cat src/test/resources/proposals/editing-studio-unprepared.json)"
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.Candidate.findCandidateByProposal --producerRef editing-studio-fixture --proposalId book-capture-1
```

It retains source context, unverified capture evidence, unknown revision/snapshot
and the stated scan/version limitation. It requires no PreparedMaterial or Run.
`captureConfirmed` stays in the exact original JSON as transfer confirmation;
human review/approval remains pending. Query absence without adding a candidate:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.Candidate.findCandidateByProposal --producerRef editing-studio-fixture --proposalId absent
```

See the [proposal reference](index.md#registerproposal) for complete validation
and candidate/reference fields. These examples describe the implemented
operation sequence. STEP-2 acceptance checks this sequence in isolated native
storage through separate CLI processes and a read-only SQL inspection.

## Material work sequence

Use `KnowledgeWorkbench.MaterialWork` with a host that includes the owning
KnowledgeLake component and admits the actual caller to both components.
Workbench's authenticated-only gate is independent of TKL native authorization,
producer configuration and managed provider custody. The standalone Candidate
instance setup above alone does not establish that composed route; Workbench
adds no connection setting or credential bootstrap for these operations.

For an already available Material, call `searchMaterials` with optional `text`,
`offset` and `limit`, inspect its exact logical `materialRef` with
`inspectMaterial`, then select an original using `readOriginal` with that
`materialRef` and the returned `resourceId`. Metadata, provenance and original
binary response come from TKL. Preserve the producer MIME type and safety
headers. These reads can precede Candidate work and require no new registration,
OpenClaw processing or dummy Candidate.

For an explicitly authorized first registration:

1. Construct one complete neutral `textus.material-contribution/1` JSON value
   from the application's authorized source. Supply it as the typed
   `contribution` input to `createMaterialRegistration`. Preserve source
   references, opaque documents, provenance, histories, nulls, array order and
   Unicode. Workbench forwards the entire JSON to the fixed public TKL owner.
2. Retain the returned native `registration_id` exactly. Observe `revision`,
   `state` and the planned `material_ref`; a prepared/materializing reference
   is not an available original. Status and materialization requests use the
   camelCase input name `registrationId`, without trimming or normalization.
3. Explicitly call `getMaterialRegistration` for that ID when status is needed,
   or request `materializeMaterial` according to the owning TKL admission and
   lifecycle contract. Both return the same four logical fields from the
   producer's actual saved snapshot. No physical receipt or grant is exposed.
4. After observing `registered`, use the existing `inspectMaterial` and
   `readOriginal` operations with the observed logical Material/resource
   references. Candidate evidence selection and human approval are separate work.

All six operations target only `org.simplemodeling.textus.KnowledgeLake` /
`Material`. Do not supply a Book/ISBN/App key, owner, provider selector, token,
connection or alternate target as a work input. Opaque nested contribution
content remains data and confers no authority. A domain-neutral application
uses the same contribution and selector inputs.

Preserve any structured failure and uncertainty returned by the producer.
An unknown creation outcome is unresolved; do not automatically create again.
Materialization failure may retain Materializing and remote bytes, or an
uncertain final acknowledgement may leave Registered. Explicit status reads
report saved state without authorizing replay, reset or deletion. Workbench
does not retry or substitute a direct provider call. Its controlled producer
specifications describe forwarding/context behavior only; parent focused
validation, actual composed PostgreSQL outcomes, Studio original display,
private CAR and real Google/App evidence remain required for connected acceptance.

## Troubleshooting and validation boundary

If startup fails, confirm the prepared classpath exists and is nonempty, the
selected descriptor is a readable regular file and its complete component and
provider mappings remain intact. Check the selected stable user against the
standalone profile and confirm the required native providers are present. For
input diagnostics, supply the required name and valid optional query inputs.
Use the actual native admission, authorization and input diagnostics; no
additional starter-specific error codes are defined.

Declared server commands are `scripts/run-server.sh` and
`scripts/run-server-debug.sh`; debug uses `DEBUG_PORT` (default `5005`). Both
require the prepared classpath and exported instance path. Controlled routing
checks cover their guards and exact arguments. Actual supplied runtime checks
were CLI help, registration, separate-process list, description filtering and
paging, with one persistent SQLite row in isolated storage. No Web/manual,
OpenAPI or MCP route execution is established by those checks.

STEP-1 clean generation/runtime preparation, four focused factory/assembly scenarios,
CAR packaging/manual/help/lint and independent reviews were admitted before
local commit `70620d1aeaae4a3859df93d6ff6948bdcff5742c`. Parent record
`tkw-phase1-step1-post-hardbreak-commit-acceptance-20261003@1` and review ledger
`tkw-phase1-review-ledger-20261002@4` close STEP-1/S1-A only. STEP-2 proposal
registration/lookup, sequential resend and fixed-code XShort persistence are
accepted under local binding `tkw-step2-connected-acceptance-commit-20261019-01@1`.
STEP-3 human Candidate work is accepted under local binding
`tkw-step3-acceptance-20261027-01@1`. STEP-4 views are accepted under
`tkw-step4-acceptance-20261027-01@1`. Phase 1 retains comprehensive
Phase review and its final successful bootstrap full SBT suite/closure/release.
Actual KnowledgeHub Admission belongs to Phase 1.1.

## Human Candidate work

After proposal registration, call `KnowledgeWorkbench.Candidate.getCandidateWork`
with the returned Candidate `id`. Keep the result's native `revision`.
Use `editCandidateWork` to submit the complete corrected content/context and
evidence/grounding/mapping arrays with that `expectedRevision` and a reason.
No source proposal or capture confirmation is interpreted as human approval.

When required references actually have known available versions or snapshots,
record those explicit associations, then call `decideCandidateWork` with
`decision=review`. Inspect the returned target and revision, and explicitly
call the same operation with `decision=approve` only for that reviewed target.
`hold` records a reason without fabricating missing evidence. `reject` requires
an explicit subsequent edit to reopen. Every target-changing edit requires
renewed Review; uncertain responses should be reconciled by reading current
work rather than blindly resending a decision. STEP-4 supplies the human-facing
views described below; Phase 1.1 owns actual canonical Information Admission.

## Candidate Workbench screen

Use a prepared Workbench runtime and its existing assembly/instance settings,
including a stable current-user identity and the intended persistent datastore.
Start it with the documented `scripts/run-server.sh` procedure, then open
`/web/knowledge-workbench/textus-knowledge-workbench` on that same server.
No Candidate is created automatically: register the selected proposal through
the documented registration operation first. Both prepared TKL and unprepared
Studio submissions use this screen; no Drive connection is required to inspect
their supplied evidence references.

If the server requires bearer authentication, enter an operator-provided token
in the connection field. It stays in page memory; there is no browser credential
storage. The assembly's authentication/authorization policy is not bypassed by
this field. Use the configured server identity in local fixed-user operation.

1. Choose **一覧を読み込む**, then **選択** on a Candidate.
2. Inspect content, preparation, evidence version/snapshot, retrieval state and
   limitations. Unprepared or unknown facts stay explicit; no availability is
   invented and selecting a row never approves it.
3. Edit content and reference entries and provide an edit reason. The context
   presence checkbox preserves the difference between an absent value and an
   intentionally present empty string. **編集内容を保存** sends one conditional
   edit using the observed native revision.
4. After the confirmed edit and explicit reload, inspect the stored target.
   Select Review and provide a reason, then choose **判断を記録**. Inspect the
   reloaded target before a separate explicit Approve. Hold and Reject are
   also explicit decisions; evidence must be ready before Review/Approve.
5. A target-changing edit invalidates approval. Rejected work needs an explicit
   edit to reopen. Switching or reloading an edited draft asks before discarding
   it; a decision never silently incorporates unsaved edits.

On a conflict, denied request or unknown response, do not repeat the command.
Choose **選択中を再読み込み** and reconcile the current stored state. If the
mutation was acknowledged but its subsequent reload failed, the success remains
acknowledged and no retry is performed. Further writes remain disabled until
a successful explicit reconciliation read.

Phase 1.1 owns formal Information Admission; this human Candidate decision does
not perform that operation. Historical unmanaged databases still require their
own migration decision. Full Phase validation and closure remain separate gates.


## Canonical Information Admission

After editing and explicitly reviewing/approving the current Candidate target,
use `Candidate.admitCandidateWork` with its native `id` and observed
`expectedRevision`. The Workbench retains the original approval and reserved
Information identity before calling KnowledgeHub. The same route handles prepared
TKL and unprepared Studio input; optional preparation/Run provenance is not added
when absent. `Admitted` means the persisted owning Information and native revision
were confirmed with exact original target and trace correspondence.

If the response is missing or unsuccessful, reload the Candidate and use
`Candidate.confirmCandidateAdmission` with its saved native identity/revision.
This performs lookup only; never resubmit an uncertain write or choose another ID
to bypass it. Missing lookup alone does not prove the owning write never occurred.
The existing Detail shows Admission history, saved Information ID, the original
approval, requester/time and confirmed Information revision. A changed target
requires renewed human Review/Approve; historical confirmation grants no approval
to changed content. The browser exposes separate Admission and outcome-check
buttons through native Display business bindings and does not automatically resend.

Owning contract: [Candidate Information Admission](../../../../docs/spec/candidate-information-admission.md).

## Create Candidate work from selected Material

Use the existing authenticated instance and public Material search/inspection to
select a TKL-issued `materialRef` and its logical resource IDs. Selection alone is
read-only. Before explicitly invoking `MaterialWork.createCandidateFromMaterial`,
retain a caller-owned creation-intent `proposalId`. A logical request body is:

```json
{
  "proposalId": "<retained-intent-key>",
  "materialRef": "<exact-TKL-reference>",
  "resourceIds": ["<exact-selected-resource-id>"],
  "name": "Operator supplied name",
  "content": "Operator supplied Candidate statement",
  "context": ""
}
```

Submit once through the normal Business Operation boundary. Continue editing only
from the returned native `candidate.id`; `created=false` identifies existing work
for that exact accessible intent. Read the current work revision before each edit
or decision. This initial Material evidence is unverified and has no invented
revision or snapshot, so inspection/Edit/Hold do not imply Review/Approve readiness.

If acknowledgement is lost, keep the intent and use the read-only
`Candidate.findCandidateByProposal` with `producerRef=workbench-material` and the
same `proposalId`. Do not automatically resubmit a command after an uncertain
outcome or infer nonexistence from failed/absent lookup. A deliberate new work
intent remains distinct from continuation of the original one.

## Use the served Material workbench

Open `/web/knowledge-workbench/textus-knowledge-workbench` on the ordinary
Workbench host. Enter the admitted bearer token only if required by that host;
it stays in memory. Search, choose a Material, inspect its logical resources,
select resources explicitly and use the original preview controls. Missing,
denied or unsupported originals clear the previous preview. Only safe raster
images and audio with the private binary headers are displayed/downloaded.
No source version is inferred from a successful preview.

Choose **明示的に新しい作成を開始**, enter name and content, optionally select
context presence (including empty context), and submit **Candidateを作成して開く**.
The browser retains the opaque intent before dispatch; only confirmed matching
native identity opens Candidate work. Failed or unknown operations keep the
in-memory draft and block a second submission. Use **保持した作成意図を照合**
after uncertainty or reload. An absent lookup is still uncertain. Earlier keys
remain in the retained-intent selector even after deliberately starting new work.
Changing the token requires reconciliation with the actual original caller;
the browser does not store or infer intent ownership from a saved credential.
Reload preserves intent keys, not content or source payloads; heed the unsaved
draft warning before leaving the page.

Edit and decide using the existing Candidate forms and actual observed revision.
**最初に選択した元資料を確認** follows the original association even after edits
or reopen. Initial Material evidence has no revision/snapshot, so Review and
Approve remain unavailable; inspection, Edit and Hold remain available. Only
actual separately supplied ready evidence enables the existing Review/Approve
workflow, and a later effective target edit invalidates its approval.

P2-03's opt-in browser specification is coordinated by the runtime owner with
`textus.test.browser-probe=true`. It writes
`target/candidate-material-browser-ready.json`, serves loopback only, and waits
up to twelve minutes for explicit `/__fixture_complete`. It uses a controlled
public logical producer and valid PNG, deliberately loses the first committed
creation acknowledgement, then checks native history after browser reconciliation,
Edit/Hold, controlled readiness, Review/Approve and invalidation. Normal test
execution needs no external browser. P2-03 was accepted at
`f0949753f5b39d4a59ba6b3da905e9ce2b840cde`; its controlled source does not
establish P2-04 real-source acceptance.

For the separately authorized existing-real-Material browser specification,
the runtime owner sets `textus.test.real-material-browser=true`,
`textus.test.real-material-selection=<absolute private metadata JSON>` and
`textus.test.drive-connection=<absolute existing read-only connection JSON>`.
The existing `textus.test.knowledge-lake.repository` selects actual TKL source.
Do not enable the P2-03 browser property for this separately coordinated case.
The consumer never opens the credential JSON; TKL owns connection loading and
read-only package access. No remote Material creation or materialization occurs.

Read `target/material-candidate-real-browser-ready.json` during the active test.
Its owner-only payload contains the loopback URL, exact public Material/resource
selectors, disposable `testToken` and steps. Enter `testToken` in `#token` first.
The admitted real caller is the existing `editing-studio-mobile-development`
fixture service subject under the standalone Fixed ingress profile; recorded
Edit/Hold attribution is not evidence of real end-user OAuth. No Google token or
service-account key is included. Use the ordinary served UI to inspect/preview, explicitly create the specified
local Candidate, edit to `Real source human observation`, record Hold, reload
and revisit its original source. Keep context present-empty. Review/Approve must
remain unavailable; do not supply invented version/snapshot fields. Signal
`/__real_material_complete` when finished. The twelve-minute fixture then checks
native restart/readback, exact first association, attributed Edit/Hold and full
original bytes before/after work, and cleans up local test resources. These
P2-04 connected specifications were accepted at
`166f10c5b95a800a09f457003891ac77a5f72211`, after connected receipt 659
(compile, 19 tests/three suites) and independent review. The actual browser
previewed the 3000 × 4000 JPEG and created exactly one Candidate; same-ID Edit/Hold
advanced revisions 1/2/3 with present-empty context. Reload/source preview and
PostgreSQL restart retained complete work and exact-intent lookup. All 1,535,406
original bytes matched via native Bag/HTTP before/after work; the public original
was read again after restart. Evidence remained unverified and unversioned.

Final full receipt 672 passed 71 tests/nine suites including PostgreSQL 17.11.
Normal full tests use controlled sources and PostgreSQL without real credentials
or an external browser; opt-in real receipt 659 and controlled approval/browser
receipt 484 are separate evidence. The first whole-Phase review requested
current-document reconciliation. This documentation repair awaits closing review
and native closure/local release; Phase completion is not claimed. Full Editing
Studio Candidate integration/K04 remain separately held.

## Explicit Material evidence refresh

Use `MaterialWork.refreshCandidateMaterialEvidence` with the native Candidate `id`,
its latest `expectedRevision` from `Candidate.getCandidateWork`, and a nonblank
`reason`. No source selection, version, provider address or credential is accepted
by this command. It reads the original selected resources through the same caller's
TKL getMaterial contract. First creation and same-intent resend remain metadata-only.

The returned `CandidateWorkResult` exposes optional native `candidate.materialEvidence`:
exact Material/resource selection, active/detached status and ordered historical
resource/reference applications. Effective changes preserve human content and unrelated
references, append ordinary attributed edit history, invalidate approval and require
separate Review/Approve. Identical observations preserve revision and state, including
Held/Rejected. Generic human changes to managed references detach them permanently;
refresh refuses detached work before source access. This contract provides no rebind.

On failed or uncertain acknowledgement, read the same known Candidate ID with
getCandidateWork; do not automatically resubmit refresh. Unknown/unversioned producer
evidence remains unverified and cannot pass Review/Approve. Provider storage mapping
and credentials remain private. KnowledgeHub receives exact approved references plus
Candidate ID/revision; detailed per-resource correspondence stays in native Candidate
history and is not copied into Hub. See the repository
`docs/spec/material-evidence-review-v1.md` for the complete command and provenance
contract. Native qualification/review and Phase3 browser/connected acceptance remain
required; authored behavior is not an acceptance claim.
