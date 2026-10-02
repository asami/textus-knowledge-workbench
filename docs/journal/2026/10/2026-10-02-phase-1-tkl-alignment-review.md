# Phase 1 Review after TKL Processing-Context Design

- Date: 2026-10-02
- Status: Design review and implementation proposal
- Scope: TKW Phase 1, related architecture/journal decisions, and alignment with the 2026-10-02 TKL design

## Review conclusion

The current Phase 1 structure should remain the baseline. It correctly separates the first development slice from Phase 1 completion and keeps real KnowledgeHub Information Admission as the completion gate.

The development order is appropriate:

1. component foundation;
2. Candidate registration and lookup;
3. Candidate formation and human decisions;
4. Workbench views and both input families;
5. real KnowledgeHub Admission;
6. follow-on TKL feedback and Candidate preparation.

Items 1 through 4 are sufficient to establish the Workbench business flow but must not be reported as Phase 1 completion. Real Admission remains required.

The main follow-up is not a redesign of Phase 1. It is a clarification of the TKL/TKW handoff and source-index model using the TKL decisions made on 2026-10-02.

## Alignment with the current TKL model

The TKL design now distinguishes:

```text
Resource / Evidence
 -> KnowledgeProcessingContext
 -> ProcessingRun
 -> PreparedMaterial
 -> RawCandidateProposal
 -> TKW
```

TKW should consume this boundary without reproducing TKL's processing model.

TKW owns:

- InformationCandidate identity and lifecycle;
- candidate content/context formation;
- CandidateSource and candidate-centric source indexing;
- human Review / Approve / Hold / Reject;
- the approved target;
- KnowledgeHub Admission request/outcome;
- Candidate -> admitted Information trace.

TKL owns:

- provider-neutral Resource/Evidence management;
- preparation;
- KnowledgeProcessingContext;
- ProcessingRun;
- PreparedMaterial and its provenance;
- proposal of new raw candidates;
- preparation of material requested for an existing TKW Candidate.

KnowledgeHub owns canonical admitted Information.

## Clarify Proposal versus Candidate

Phase 1 currently uses phrases such as "raw/prepared Information Candidate". This is understandable but can blur ownership.

The preferred boundary is:

```text
TKL PreparedMaterial
  -> RawCandidateProposal
  -> TKW registration
  -> InformationCandidate
```

A producer proposal is not yet a TKW InformationCandidate. TKW creates and owns the Candidate after accepting/registering the proposal.

This also fits the existing Phase 1 resend policy using `producerRef + proposalId`.

### Proposed minimum proposal contract

Conceptually:

```text
RawCandidateProposal
  producerRef
  proposalId
  candidateKind
  suggestedContent
  evidenceRefs
  preparedMaterialRefs
  processingRunRefs
  provenanceRefs
  createdAt
  sourceMetadata?
```

The exact DTO should be kept minimal for the first fixture. TKL-specific optional fields must not become mandatory for Editing Studio raw capture.

The common contract should therefore separate:

- common proposal identity and source references;
- optional prepared-material/run provenance;
- source-specific metadata.

## Registration implementation proposal

The first registration operation can be conceptually:

```text
registerCandidate(proposal)
  lookup(proposal.producerRef, proposal.proposalId)
    existing -> return existing Candidate identity
    absent   -> create Candidate + source correspondence
```

Sequential resend must not overwrite human edits.

Do not add content hashing or general payload canonicalization merely to implement this lookup. If changed-payload conflict detection later becomes a real joint requirement, define that contract explicitly rather than making a hash an implicit application identity.

The proposal-to-Candidate correspondence should be durable. Phase 1 is correct not to require a separate Receipt aggregate before demonstrated need. Candidate metadata or a small correspondence record is sufficient if it can enforce the lookup invariant cleanly.

## CandidateSource and RawDataIndex

The 2026-09-25 journal identifies both `CandidateSource` and `RawDataIndex`, but their responsibilities should now be made explicit.

### CandidateSource

Represents the logical origin/correlation through which the Candidate entered or is maintained by TKW.

Examples:

- TKL RawCandidateProposal;
- Editing Studio confirmed capture;
- future domain application submission.

Suggested fields:

```text
CandidateSource
  candidateSourceId
  candidateId
  sourceKind
  producerRef
  proposalId / sourceObjectRef
  receivedAt
  sourceMetadata
```

### RawDataIndex

Represents candidate-centric references to supporting Resource/Evidence, without taking ownership of those resources.

Suggested fields:

```text
RawDataIndex
  candidateId
  sourceRef
  resourceRef?
  evidenceRef?
  preparedMaterialRef?
  processingRunRef?
  providerVersionRef?
  snapshotRef?
  provenanceRef?
  preparationState?
  retrievalStatus?
```

The index should primarily use stable TKL Resource/Evidence identities and explicit version/snapshot/provenance references.

The older "checksum where useful" wording should not be a normal application-level design recommendation. A checksum may exist where an owning storage/protocol contract specifically requires it, but TKW should not introduce hashing as a default identity, concurrency, deduplication or provenance mechanism.

## Authority boundary

The source-of-truth split should be explicit:

```text
TKL
  Resource / Evidence
  KnowledgeProcessingContext / ProcessingRun
  PreparedMaterial
        |
        | references
        v
TKW
  InformationCandidate
  CandidateSource
  RawDataIndex
  Review / Approval / Admission state
        |
        v
KnowledgeHub
  admitted canonical Information
```

TKW DB is authoritative for candidate-centric human work and admission state.

The initial TKL Drive JSON backend is authoritative for TKL metadata and PreparedMaterial according to the TKL design.

TKW must not copy raw file bodies or reconstruct TKL processing history as its own state. It retains references needed to explain and operate the Candidate.

## ProcessingRun handling

TKL `ProcessingRun` should remain a referenced external processing fact from the TKW point of view.

TKW does not need a duplicate ProcessingRun entity for TKL preparation.

A Candidate may retain:

```text
Candidate
  -> CandidateSource / RawDataIndex
      -> Evidence snapshot
      -> TKL ProcessingRun
      -> PreparedMaterial version
```

TKW's own review and admission events are separate Workbench lifecycle facts.

This distinction prevents preparation execution state from being mixed with human review/admission state.

## Candidate lifecycle implementation proposal

The Phase 1 lifecycle should remain small and explicit.

A useful conceptual state set is:

```text
Proposed
 -> Draft
 -> Editing
 -> Review
 -> Approved
 -> AdmissionRequested
 -> Admitted
```

with `Held` and `Rejected` as explicit review outcomes/states according to the selected CML/state-machine representation.

The implementation should record a transition table before coding ambiguous recovery behavior.

At minimum decide:

- Hold -> Editing/Review resumption;
- whether Rejected can be reopened and by whom;
- whether Review is allowed with unavailable evidence;
- what changes invalidate an approval;
- whether Admission failure returns to Approved or remains AdmissionRequested with an outcome/failure state.

Do not infer these rules from UI behavior.

## Approval target

Approval must refer to the content/evidence target actually reviewed, not merely to the current Entity revision.

Conceptually:

```text
Approval
  candidateId
  approvedCandidateRevision
  approvedEvidenceSet/version refs
  actor
  approvedAt
  reason?
```

If the approved content or relevant evidence changes, renewed review/approval is required.

Use CNCF revision/concurrency facilities for entity update control. Do not create a hash-based approval-version subsystem.

## Candidate Preparation

Candidate Preparation should remain follow-on implementation work rather than a prerequisite for the first development slice.

However, reserve the contract shape now because the 2026-09-25 journal and the current TKL design agree on this path:

```text
existing TKW Candidate
 -> CandidatePreparationRequest
 -> TKL preparation
 -> ProcessingRun
 -> PreparedMaterial
 -> correlated result
 -> same TKW Candidate
```

A conceptual request:

```text
CandidatePreparationRequest
  requestId
  candidateRef
  resource/evidenceRefs
  purpose
  requestedProcessing
  requestedAt
```

The returned result should correlate to `requestId` and supply PreparedMaterial/ProcessingRun/provenance references.

TKL does not gain ownership of the Candidate. TKW does not gain ownership of the preparation execution.

## Editing Studio input

Editing Studio remains an important second fixture because it proves that TKW is not merely a TKL-specific UI.

Confirmed book capture may enter TKW before any TKL PreparedMaterial or ProcessingRun exists.

Therefore the common registration contract must allow:

```text
Editing Studio source
 -> CandidateSource
 -> available raw Resource/Evidence references
 -> InformationCandidate
```

with preparation limitations explicitly represented.

Later Candidate Preparation can send those resources through TKL and attach PreparedMaterial to the same Candidate.

This validates the intended convergence of the two acquisition paths.

## KnowledgeHub Admission

Real KnowledgeHub Admission should remain Phase 1's completion dependency.

The contract still needs to establish:

- Admission target identity/version;
- request semantics;
- confirmed success/outcome;
- duplicate/retry semantics;
- Candidate -> admitted Information identity/version trace;
- behavior for uncertain/partial failure.

A fixture or simulated Admission is useful for development but must not close Phase 1.

This is also the main dependency for server Phase 1.2 and later production Display/Operation contracts that require real Workbench semantics.

## Feedback to TKL

Admission feedback can remain after the initial slice, but the trace produced by Phase 1 should make it straightforward.

Conceptually:

```text
Candidate
 -> admitted Information identity/version
 -> TKL admission/result lookup
 -> KnowledgeHub KnowledgeProjection refresh
 -> later TKL Existing Knowledge Context
```

TKW should expose the admission result. TKL/KnowledgeHub remain responsible for KnowledgeProjection storage and refresh.

## Journal reconciliation

### 2026-09-23 Knowledge Workbench Establishment

The architectural decision remains valid.

Recommended terminology refinement:

```text
TKL PreparedMaterial/raw candidate proposal
 -> TKW InformationCandidate
```

rather than wording that suggests both systems own a Candidate lifecycle.

### 2026-09-25 Workbench DB and Raw-Data Index

The core decision remains valid: TKW owns a persistent candidate-centric DB and does not store large raw bodies.

Refine it as follows:

- Google Workspace/Drive is the initial TKL backend, not the semantic definition of Knowledge Lake.
- RawDataIndex references provider-neutral TKL Resource/Evidence identities.
- distinguish CandidateSource origin/correlation from RawDataIndex supporting-resource references;
- replace default checksum-oriented wording with version/snapshot/provenance references;
- add optional PreparedMaterial and ProcessingRun references;
- retain Candidate Preparation as a correlated TKW -> TKL -> TKW flow.

### Current CML scaffold

The scaffold remains consistent at the high level.

When replacing it with valid CML, add explicit concepts for:

- proposal registration versus Candidate ownership;
- CandidateSource;
- Evidence/PreparedMaterial references;
- human review decision;
- approval target;
- Admission request/trace.

Do not model TKL KnowledgeProcessingContext/ProcessingRun as TKW-owned lifecycle objects.

## Proposed implementation order refinement

The existing Phase 1 order remains authoritative. Within it, the following implementation sequence is recommended:

### Work item 1

- inspect actual current Cozy/CML and CNCF component patterns;
- define InformationCandidate minimum aggregate/entity;
- define lifecycle operations rather than only CRUD;
- establish standard persistence and focused executable tests.

### Work item 2

- define a provider-neutral proposal DTO;
- implement `producerRef + proposalId` durable lookup;
- create CandidateSource;
- create minimum RawDataIndex references;
- test normal registration, invalid proposal, restart lookup and sequential resend after human edit.

### Work item 3

- implement editing/grounding;
- define lifecycle transition table;
- implement Review/Hold/Reject/Approve;
- record approval target and invalidate approval when reviewed target changes.

### Work item 4

- run both TKL and Editing Studio fixtures;
- expose List/Detail/edit/review through admitted View/Display capabilities;
- explicitly show evidence/preparation limitations rather than inventing unavailable data.

### Work item 5

- freeze the real KnowledgeHub Admission contract;
- execute Admission;
- confirm result;
- retain Candidate -> Information -> Evidence trace;
- test retry/uncertain failure semantics required by the real boundary.

### Follow-on

- TKL admission-result lookup/feedback;
- CandidatePreparationRequest/result correlation;
- richer RawDataIndex;
- live TKL/Drive integration;
- live Editing Studio integration.

## Review result

No Phase 1 restructuring is required.

The most valuable immediate changes are:

1. make `RawCandidateProposal -> InformationCandidate` explicit;
2. separate `CandidateSource` from `RawDataIndex`;
3. treat TKL `ProcessingRun` and PreparedMaterial as referenced provenance, not TKW-owned execution state;
4. preserve Candidate Preparation as follow-on implementation while reserving a correlated request/result contract;
5. update the 2026-09-25 journal terminology to the provider-neutral TKL model;
6. keep real KnowledgeHub Admission as the non-negotiable Phase 1 closure gate.

These refinements preserve the current business-first development plan while making the TKL/TKW authority and implementation boundaries concrete enough for implementation.
