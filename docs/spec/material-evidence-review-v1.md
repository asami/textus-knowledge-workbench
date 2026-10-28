# Material evidence and human review requirements v1

Owner: TKW Phase3 / P3-01–P3-03. Contract acceptance is retained at
`ffed0eba623e66ad8c853436158e494e75cf4c0c`; native consumer acceptance is retained
at `b10952b25c89c8e23624e289c2ac79dfde94d03f`. P3-03 browser implementation and
executable specifications require native qualification and independent review.
This document does not publish a new TKL Operation.

## Public ownership

TKL owns authoritative logical Material/resource identity, source version or
snapshot, private storage mapping and the correspondence of its observation to
an original read. Workbench consumes the owning public result with the same
caller. KnowledgeHub remains the canonical Information owner. Reuse existing
public Operations where sufficient; an additive input/result or new Operation
must be justified and specified by its owning producer before consumer use.

Existing metadata-only search/get and readOriginal semantics remain compatible.
Ordinary metadata access must not silently acquire provider credentials or read
original bytes merely to populate a new optional field. Explicit evidence work
selects exact logical resource IDs already belonging to the selected Material.
No client-supplied provider reference, version claim or snapshot label can
substitute for the owning observation on this Material-backed path.

## Selected producer wire contract

The owning TKL `Material.getMaterial` accepts the existing `materialRef` plus
optional ordered `evidenceResourceIds`. Omitted or empty selection retains the
existing metadata-only response and performs no new original access. An explicit
selection contains distinct logical resource IDs belonging to that Material.
The producer bounds selection and authorizes all selected resources before
original access; a partially successful selection is not returned as complete.

The explicit result adds `observedEvidence`, in selected order. Each typed
observation contains `evidenceRef`, `materialRef`, `resourceId`, optional
`revision`, `retrievalState` and `limitation`. Workbench treats the producer-issued
identity and revision as opaque. The evidence identity distinguishes the exact
backing-resource incarnation: rebinding the same logical pair to another original
must change the evidence identity even if its file-scoped provider version happens
to match. A producer-internal digest may qualify identity; it is never a source
revision, snapshot or claim that bytes were read. Only actual source observations
and the corresponding full read supply those facts. Matching known provider versions around a full
validated original read yield `available` with that observed revision. Missing
version yields `unverified` without a revision. Changed or contradictory resource
observations fail; neither branch fabricates `snapshotRef` or `retentionUntil`.
Physical resource/connection identities remain private.

This additive contract is selected under the actual KnowledgeLake dependency
approval. Producer implementation, native checks and independent acceptance must
qualify it before consumer use; this specification alone proves no delivery.

## Required producer observation

For each explicitly selected resource, the accepted producer contract must bind
the exact materialRef/resourceId, TKL-issued opaque evidence identity, actual
source revision or snapshot when known, truthful retrieval state and applicable
limitations. An observation must not be ambiguously reassigned to another
resource or Material. Multiple resources retain exact correspondence and order.
Only observed facts are supplied; unknown version/retention remain unknown.

Available bytes plus a provider version observed consistently around the same
read may establish that scoped observation. A changed source cannot be reported
as the unchanged revision. Observation is not a promise of version-pinned future
retrieval, immutable archival storage or permanent retention. A producer must
disclose any such limit instead of manufacturing snapshotRef/retentionUntil.
Native registration/Candidate revisions and local timestamps or hashes cannot
replace source version facts. Existing originals are read only and preserved.

Caller/Material/resource authorization precedes credentials and provider access.
Return safe structured Conclusions for invalid, denied, missing, unavailable,
changed or uncertain observations. Credentials, storage keys, folder structure
and reusable access tokens remain outside public metadata and diagnostics.
Use existing managed CNCF storage capabilities, with no client/provider fallback.

## Native Candidate application and compatibility

Unversioned Material remains usable for inspect, create, edit and Hold under the
accepted Phase2 contract. Missing evidence does not become available merely
because a preview or native registration succeeded. Existing evidenceRefs are
all subject to the current Candidate readiness rule; this delivery introduces
no optional-reference override.

Applying newly observed evidence is an explicit native expected-revision work
command. Keep first original proposal, retained selection, human edits/history
and unrelated current references. Do not silently overwrite a human-modified
reference or resurrect a removed one. Effective target changes invalidate
approval through the existing lifecycle. Observation or application never
performs implicit Review, Approve or Information submission.

Review and Approve remain separate explicit decisions on the same eligible
unchanged target. Their advancing Entity revisions do not become source versions.
The existing confirmed/unknown Information Admission contract remains unchanged:
retain the approved target, canonical ID/revision and provenance, and reconcile
known identity without automatic write replay. Generic declared reference
readiness remains its existing contract, not cryptographic provider attestation.

## Explicit application command

`MaterialWork.refreshCandidateMaterialEvidence(id: EntityId,
expectedRevision: Long, reason: CandidateDecisionReason) -> CandidateWorkResult`
is an authenticated normal business command. Its responsibility is source-owned
acquisition and application; `Candidate.editCandidateWork` remains a complete
human-declared target edit. Clients provide no alternate selection, provider
address, evidence identity, revision, snapshot, actor or timestamp. The existing
`Candidate.getCandidateWork` is the read and uncertain-acknowledgement reconciliation
operation; no additional query or test-only API is added.

The command authenticates and loads the caller-authorized native Candidate,
checks expected revision and nonblank reason, validates the original
`workbench-material` producer/proposal identity and `tkl-material` source kind,
and derives the exact ordered selection from immutable first
`originalProposal.materialSelection`. This validation and binding-ownership
checking precede TKL access. The same caller invokes TKL `Material.getMaterial`
with precisely `materialRef` and `evidenceResourceIds`. Whole-selection failure,
unknown or malformed acknowledgement, missing/duplicate/wrongly ordered resources,
wrong logical identity or inconsistent revision/readiness fails without a Candidate
write. Structured source Conclusions are retained; no acquisition or mutation is
replayed automatically.

Each producer observation becomes a `ProposalReference` with exact opaque
`id=evidenceRef`, `kind=material-resource`, actual optional revision, retrieval
state and limitation. `snapshotRef` and `retentionUntil` remain absent. Available
requires its actual nonblank revision; unversioned observations stay unverified.
All current evidence references still pass the existing EVERY-reference gate.

## Native managed correspondence

`InformationCandidate.materialEvidence` is optional native provenance:

- `MaterialEvidenceBinding(materialRef, resourceIds, status, applications)`.
  `resourceIds` is the exact original ordered selection; status is `active` or
  permanently `detached` for this contract.
- Each `MaterialEvidenceApplication(resources)` retains ordered
  `MaterialResourceEvidence(resourceId, appliedReference)` entries.
- Applications are appended only for effective changes. Earlier exact logical
  resource/reference correspondence remains available beside ordinary attributed
  edit targets/history, in the same atomic native Candidate update.

Generic proposal registration constructs known fields and does not accept a
client-supplied managed binding. Absence is compatible with existing stored and
new Phase2 Candidates. A first application is permitted only when the exact
original singleton unverified Material reference remains unchanged and appears
once in current evidence. It replaces that source slot with ordered resource
references and preserves unrelated references. An existing active binding must
match the original selection, have well-formed historical applications and retain
its latest exact unambiguous current reference projection. A producer identity
colliding with unrelated human evidence conflicts rather than overwriting it.

A generic human edit that modifies, deletes, duplicates or reorders managed
references records `detached` persistently with that edit. Restoring the old text
or references does not reattach it. Refresh of detached/ambiguous ownership
conflicts before source IO. Unrelated human content/context/references remain
editable without detachment; no explicit rebind operation is supplied here.

An effective application preserves content/context, first proposal, preparation,
prepared-material/source/grounding/mapping fields and all earlier decisions. It
appends the ordinary attributed `edit` target with the application's correspondence
and uses the original native revision for one conditional atomic update. Concurrent
native changes conflict without producer/mutation replay. This source refresh
invalidates approval through the normal lifecycle, without Review, Approve or
Admission. Identical exact managed references produce no write, no new application,
no decision and no revision/state change, including Approved, Held and Rejected.
After source observation the no-op path reloads the native Candidate and checks
its original revision before returning, so it cannot report stale success during
a concurrent edit. It performs no write solely to guard a no-op.

## Canonical trace boundary

Existing Hub Admission schema remains unchanged. It receives `candidateId`, the
approved `candidateRevision` and the exact approved target/evidence references.
The extra Entity `materialEvidence` binding and full per-resource application
history are **not copied into KnowledgeHub**. Full resource correspondence is
jointly traced by the native Candidate identity, its retained history and the Hub
record's approved target/ID/revision; a self-contained Hub resource mapping is not
claimed. P3-04 must verify that joint native/Hub trace after restart.

Executable specifications: `MaterialEvidenceWorkSpec`, affected
`MaterialCandidateWorkSpec`, `ComponentFactorySpec` and `CandidateAdmissionSpec`.
Their authored coverage is not a claim of successful native execution or Phase
acceptance. Browser delivery remains P3-03 and connected acceptance P3-04.

## Evidence required for acceptance

- Owning producer compile, affected behavioral checks and independent review
  establish exact authorization, resource/version/read correspondence,
  absent/changed-version behavior and compatible existing metadata/original reads.
- Consumer native and actual served-browser evidence preserves explicit intent,
  observed revision, human target edits, safe source handling and unknown outcomes.
- One existing real Material reaches confirmed real KnowledgeHub Information
  through explicit Review/Approve, retaining complete original equality and
  real PostgreSQL restart/source/history correspondence. Controlled fixtures,
  neutral consumer and admitted service-caller facts remain separately identified.
- TKW final full validation and integrated independent Phase review remain
  required. Studio Phase1.2 remains held; no upstream change is accepted merely
  because this requirement or a dependency proposal was authored.

References: [Phase3 checklist](../phase/phase-3-checklist.md),
[Material-backed work](material-candidate-work-v1.md),
[human lifecycle](candidate-formation-and-review.md),
[Information admission](candidate-information-admission.md),
[design notes](../notes/material-evidence-review-design.md).

## Served Workbench evidence work (P3-03)

The existing Candidate Detail projects the native `materialEvidence` binding and
its ordered applications beside the exact current evidence references. Display
availability for `refresh-evidence` is a pure application of the existing owning
preconditions; reading the screen acquires no source bytes. Its business binding
is the existing `MaterialWork.refreshCandidateMaterialEvidence` command. Display
metadata is inert and the command independently enforces authorization and state.

The browser submits only the known Candidate ID, acknowledged native revision and
a nonblank operator reason. It blocks evidence refresh, Review, Approve and
Admission while human edits are unsaved. It never silently saves/discards those
edits. Detached or invalid original associations disable refresh. Source versions,
evidence identity, retrieval state and limitations are rendered as exact text;
missing version remains unknown. Generic manually declared references remain
editable under the existing policy and cannot become source-attested merely by UI.

A refresh is distinct from Review, Approve and Admission. An identical observation
keeps the current state/revision; an effective change returns to ordinary Draft.
Before any command, the browser marks the current read as unconfirmed. An error,
malformed acknowledgement or lost response leaves mutations blocked until an
explicit read reconciles the same known Candidate. No refresh or Admission is
replayed. Safe native HTTP error message, detail code and disclosed Conclusion
are rendered using text nodes; debug diagnostics are not copied into the UI.
Bearer values remain in memory/input only and are not included in error text,
storage, URLs or handoff records. A caller change clears previews and invalidates
current action availability until an explicit fresh read.

`MaterialEvidenceViewsSpec` supplies normal native HTTP/view coverage and an
explicitly opt-in loopback browser scenario. Its producer/fault controls are
owned test endpoints and establish controlled observations, not real Google Drive
or end-user OAuth. Native validation, actual browser observations and independent
review retain their separate owners; source authoring does not establish PASS.
