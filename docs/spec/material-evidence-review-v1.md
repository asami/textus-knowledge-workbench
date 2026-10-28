# Material evidence and human review requirements v1

Owner: TKW Phase3 / P3-01. Status: consumer requirements authored; producer
contract selection, dependency admission, implementation and independent
acceptance remain pending. This document does not publish a new TKL Operation.

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
