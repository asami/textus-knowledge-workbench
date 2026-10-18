# Phase 1 Deep Review — Business Flow and Proportionate Guarantees

- Date: 2026-10-02
- Status: Design review and implementation guidance
- Scope: TKW Phase 1 and related journals, with emphasis on avoiding speculative defensive machinery
- Authority: Phase 1 remains the implementation-scope authority; this journal records review rationale and implementation guidance

## Review conclusion

The current Phase 1 is suitable as the implementation baseline.

The important historical sequence is:

1. TKW was established as the common Information Candidate formation/admission layer.
2. CandidateSource / RawDataIndex and TKL preparation paths were introduced.
3. The TKL-side requirements proposal described a broad set of receipt, concurrency, recovery and provenance guarantees.
4. The Workbench development plan deliberately reduced the first slice to the business flow and deferred guarantees not yet justified by the selected execution/storage model.
5. Phase 1 now reflects that reduction while keeping real KnowledgeHub Admission as the final completion gate.

This correction should be preserved.

The main implementation risk is no longer that Phase 1 explicitly requires excessive machinery. The risk is that an implementation agent reads the incoming TKL requirements in isolation and expands the implementation to satisfy every hypothetical failure mode before the Workbench business flow exists.

## Scope authority rule

For implementation, use the following precedence:

```text
Phase 1
  -> current architecture/contracts
  -> adopted Workbench development-plan decisions
  -> incoming TKL requirements as requirements input
  -> older journals as design history
```

The TKL requirements journal is intentionally broader than the first TKW development slice. Its TKL-TKW-01..07 and AC-01..11 lists are not automatically first-slice acceptance criteria.

Do not turn an incoming requirement proposal into implementation scope merely because it describes a theoretically desirable guarantee.

## What the first slice should actually build

The first useful TKW system should primarily consist of:

```text
RawCandidateProposal / EditingStudioSource
        |
        v
registerCandidate
        |
        v
InformationCandidate
        |
        +--> CandidateSource
        +--> Evidence/PreparedMaterial references
        |
        v
edit / ground / map
        |
        v
review
        |
        +--> approve
        +--> hold
        +--> reject
```

with standard CNCF persistence and focused View/Display exposure.

It is not initially a general reliable-message delivery subsystem.

## Suggested minimum domain objects

### InformationCandidate

Candidate should be the main business object.

Conceptual minimum:

```text
InformationCandidate
  candidateId
  candidateKind
  content
  context
  lifecycleState
  revision
  sourceRefs
  evidenceRefs
  review
  approval
  admissionTrace?
```

Use the actual current CNCF/Cozy type and persistence conventions rather than introducing parallel infrastructure.

Avoid adding fields solely because a future integration might someday need them.

### CandidateSource

CandidateSource records origin/correlation.

Conceptually:

```text
CandidateSource
  sourceKind
  producerRef
  sourceObjectRef
  proposalId?
  receivedAt
```

For TKL input:

```text
sourceKind = TKL
producerRef + proposalId -> candidateId
```

For Editing Studio:

```text
sourceKind = EditingStudio
sourceObjectRef = confirmed capture/source identity
```

This is sufficient to prove that two acquisition paths converge on one Candidate lifecycle.

### Evidence references

Do not initially reproduce the full TKL model in TKW.

A small reference value is sufficient:

```text
EvidenceReference
  evidenceRef
  resourceRef?
  preparedMaterialRef?
  processingRunRef?
  versionRef?
  availability
```

where `availability` should initially be deliberately small, for example:

```text
Available
Unavailable
Unknown
```

Only introduce richer states when an actual workflow needs to distinguish them.

Do not build an Evidence retrieval state machine merely to represent uncertainty.

## Candidate registration implementation

The first registration invariant can be simple:

```text
(producerRef, proposalId) -> candidateId
```

Pseudo-behavior:

```text
registerCandidate(proposal):
  existing = lookup(proposal.producerRef, proposal.proposalId)

  if existing:
    return existing candidate

  candidate = createCandidate(proposal)
  persist candidate + source correspondence
  return candidate
```

The key property is that sequential resend does not apply the original proposal over subsequent human edits.

The first slice does not need to compare payloads for equality.

It does not need:

- content hashing;
- payload canonicalization;
- a generic deduplication engine;
- an independent receipt aggregate;
- a receipt state machine;
- concurrent-delivery arbitration machinery;
- retained-key tombstone infrastructure.

If a real transport later requires changed-payload conflict detection or concurrent exactly-once-like behavior, add the minimum mechanism required by that concrete contract.

## Persistence and uniqueness

Prefer existing CNCF persistence/repository uniqueness facilities.

If the selected persistence layer supports a unique key/index, the conceptual uniqueness requirement is:

```text
unique(producerRef, proposalId)
```

Do not implement application-level hash locking or ad-hoc in-memory synchronization as a substitute for the framework/storage contract.

For the initial sequential fixture, lookup plus standard persistence is enough to start development. Concurrency guarantees should be claimed only after the selected persistence mechanism and test demonstrate them.

## Restart validation

Phase 1 asks for persisted lookup across a normal restart.

Interpret this narrowly:

1. register a proposal;
2. persist the Candidate/source correspondence through the normal CNCF mechanism;
3. stop and normally restart the application/component;
4. resend or look up the same proposal identity;
5. resolve to the same Candidate without overwriting its human edits.

This does not imply:

- crash-consistency research;
- process-kill matrices;
- WAL verification;
- filesystem corruption simulation;
- comprehensive fault injection.

Those are separate requirements if ever needed.

## Candidate operations

Prefer explicit business operations over generic state mutation.

Conceptually:

```text
registerCandidate
editCandidate
updateContext
applyGrounding
requestReview
approveCandidate
holdCandidate
rejectCandidate
requestAdmission
```

The actual operation names should follow current CNCF/Cozy conventions.

A generic `setState` operation should not be the public business API.

## Lifecycle

Keep the first lifecycle deterministic and small.

Conceptual happy path:

```text
Proposed
 -> Draft
 -> Editing
 -> Review
 -> Approved
 -> AdmissionRequested
 -> Admitted
```

Hold and Reject are human decision outcomes. Their exact representation should be selected after inspecting the current CML/state-machine conventions.

Before implementation, write a small transition table for only the decisions Phase 1 actually needs:

| Current | Operation | Result |
| --- | --- | --- |
| Proposed/Draft | edit | Editing |
| Editing | requestReview | Review |
| Review | approve | Approved |
| Review | hold | Held |
| Review | reject | Rejected |
| Held | resume/edit | Editing or Review according to the selected rule |
| Approved | relevant content/evidence change | Editing or review-required state |
| Approved | requestAdmission | AdmissionRequested |
| AdmissionRequested | confirmed admission | Admitted |

Do not create speculative transitions for cases not used by Phase 1.

## Approval invalidation without hashes

The requirement that approval becomes invalid when reviewed content/evidence changes is correct. It must not lead to a content-hash subsystem.

Prefer deterministic invalidation through business operations.

For example:

```text
editCandidate(...)
  update candidate content
  clear/invalidate current approval if the approved target is affected
  advance standard entity revision
```

Likewise, an operation that changes review-relevant Evidence explicitly invalidates the approval.

Conceptually:

```text
Approval
  approvedCandidateRevision
  approvedEvidenceRefs
  actor
  approvedAt
```

The important fact is that the application knows which operation changed an approved target. It does not need to rediscover that fact later by hashing serialized payloads.

If CNCF/CML provides a better native representation of revision/approval target, use it.

## Review readiness

Do not over-model review readiness in the first slice.

The initial rule can be a business predicate over known information:

```text
reviewReadiness(candidate)
  -> Ready
  -> Limited(reason)
```

or an equivalent existing Consequence/validation representation.

Evidence that is unavailable or unknown should be visible to the reviewer. Whether it blocks review is a domain decision recorded in the Phase 1 transition/rule table.

Do not create a second lifecycle state machine solely for evidence readiness.

## Failure handling

Use the normal application/framework failure model.

The first slice needs to avoid false success and preserve already-confirmed persisted correspondence. That does not require a recovery engine.

Conceptually:

```text
operation succeeds -> return confirmed result
operation fails    -> return Consequence/failure
uncertain external result -> represent uncertainty only when such an external boundary actually exists
```

For local Candidate registration using one normal persistence boundary, do not simulate distributed partial failure before the implementation actually has distributed steps.

When KnowledgeHub Admission is added, its external-boundary uncertainty can be designed against the real Admission contract.

## Receipt concept

The incoming TKL requirements describe a durable receipt with its own states.

Do not create it initially unless CandidateSource/correspondence metadata proves insufficient.

Start with:

```text
CandidateSource
  producerRef
  proposalId
  candidateId
  receivedAt
```

If later requirements need independent receipt lifecycle, transport acknowledgement, conflict state or retention beyond Candidate lifetime, then introduce a Receipt concept based on those demonstrated needs.

This is an example of progressive formalization: begin with the business fact, extract a new entity only when it acquires an independent lifecycle.

## RawDataIndex

RawDataIndex should also grow from demonstrated Candidate operations.

Initially it can be a candidate-centric set of references.

Do not implement:

- raw-body storage;
- provider synchronization;
- content checksum deduplication;
- TKL Resource lifecycle;
- TKL ProcessingRun lifecycle.

TKW should know enough to answer:

- what source/evidence supports this Candidate?
- which PreparedMaterial/Run produced the supplied preparation when available?
- can the reviewer currently access/inspect it?
- what source should later Candidate Preparation refer to?

The 2026-09-25 `checksum where useful` wording should not be treated as a default implementation instruction. A checksum is appropriate only when required by an owning storage/protocol/security contract.

## TKL requirements that should remain deferred

The following are valid possible integration requirements but are not first-slice implementation obligations:

- simultaneous receipt proof;
- same-key changed-payload comparison;
- payload normalization rules;
- independent receipt lifecycle;
- retention/tombstone rules for old proposal keys;
- multi-stage recovery/resume;
- comprehensive fault injection;
- notification replay/order guarantees;
- CandidatePreparationRequest deduplication;
- complete source retrieval-state taxonomy.

Keep them visible as open contract questions. Do not silently mark them satisfied.

## Real Admission remains different

The reduction of defensive machinery must not weaken the actual Phase 1 goal.

Phase 1 still requires:

```text
human-approved Candidate
 -> real KnowledgeHub Information Admission
 -> confirmed canonical Information identity/version
 -> Candidate/Evidence trace
```

Admission is a real external boundary and may legitimately require retry/result-confirmation semantics. Design those semantics from the actual KnowledgeHub contract rather than pre-building a generic recovery framework.

A simulated Admission is not Phase 1 completion.

## Implementation-oriented executable specification

A useful first focused specification is approximately:

### Registration

- valid TKL proposal fixture creates one Candidate;
- invalid required common fields fail with a normal validation Consequence;
- resend by the same `producerRef + proposalId` returns the same Candidate;
- human edits survive that resend;
- normal restart preserves the lookup.

### Second source

- Editing Studio fixture creates a Candidate through the same Candidate lifecycle;
- it does not require PreparedMaterial or ProcessingRun;
- missing preparation is represented explicitly rather than synthesized.

### Formation

- Candidate content can be edited;
- minimal grounding/mapping can be attached using admitted existing contracts;
- evidence references remain traceable.

### Human decision

- Candidate enters Review;
- reviewer can Approve, Hold or Reject;
- actor/time/reason are recorded as required by the selected domain model;
- review-relevant edits invalidate approval deterministically.

### Admission

Later in Phase 1, using the real contract:

- Approved Candidate can request Information Admission;
- confirmed result records Information identity/version and trace;
- simulated result is distinguishable from real acceptance evidence.

This set tests the Workbench. It does not attempt to prove a general messaging system.

## Journal reconciliation findings

### TKL requirements proposal

Keep it as the requesting application's requirements proposal. It is useful because it records future integration concerns.

Its broad initial acceptance wording must not override TKW Phase 1's adopted development scope.

### Workbench development plan

This is the key corrective decision and should remain active. Its business-flow-first and proportionate-guarantee principles should guide implementation reviews.

### 2026-09-25 RawDataIndex journal

Retain the ownership decision, but interpret Google Workspace as the initial TKL backend rather than the semantic definition of the Knowledge Lake.

Do not treat checksum as a default mechanism.

### 2026-10-02 TKL alignment review

Retain its Proposal/Candidate boundary, CandidateSource/RawDataIndex distinction, external TKL ProcessingRun references and Candidate Preparation correlation proposal.

These are domain-boundary refinements, not justification for implementing all future integration machinery in the first slice.

## Review checklist for implementation agents

Before adding a defensive mechanism, answer:

1. Which current Phase 1 behavior requires it?
2. Is the guarantee already supplied by CNCF/Cozy/persistence?
3. Is there a real external boundary that creates this failure mode now?
4. Is the mechanism needed for the current executable specification?
5. Can the requirement remain explicit and deferred instead?

If the only justification is that a failure is theoretically possible, defer it.

This rule does not permit ignoring known correctness requirements. It prevents speculative infrastructure from replacing the business implementation.

## Final assessment

Phase 1 has successfully corrected the over-specified direction visible in the incoming TKL requirements.

The implementation should now stay centered on:

```text
Candidate
 + source/evidence references
 + formation
 + human review/approval
 + real Admission
```

and use existing CNCF/Cozy facilities for persistence, revision, concurrency and failure representation.

The best defense against generative-AI overengineering is not another defensive framework. It is a small executable business specification, explicit ownership boundaries, and a rule that additional guarantees require a concrete current requirement or owning framework contract.
