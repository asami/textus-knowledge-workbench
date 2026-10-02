# Phase 1 — Information Candidate Admission Vertical Slice

Status: planned; implementation and acceptance pending
Updated: 2026-10-02

## Goal

Establish Textus Knowledge Workbench as the common human formation/admission layer between acquisition applications and canonical KnowledgeHub Information.

## Primary use case

Form, review and admit Information from a raw/prepared Information Candidate.

## Reference flow

Raw/Prepared Candidate -> Register Candidate -> Resolve Evidence/Context -> Edit/Correct -> Semantic Grounding/Mapping -> Human Review -> Approve/Hold/Reject -> Information Admission -> Admission Trace.

## Inputs

Support deterministic fixtures for at least:
- a TKL-originated candidate with PreparedMaterial/Evidence references;
- an Editing-Studio-originated candidate derived from confirmed book-capture raw data.

Full Google Workspace, Slack and smartphone integration is not required for closure.

## Scope

- initial InformationCandidate Entity/lifecycle;
- candidate identity and Evidence/Provenance references;
- candidate content/context editing;
- minimal grounding/mapping using existing CNCF/KnowledgeHub contracts where applicable;
- explicit human review decision and approval;
- KnowledgeHub Information Admission boundary;
- trace link from admitted Information back to Candidate/Evidence;
- CML application use case/workflow model;
- executable specification for the vertical slice.

## Development policy

Prioritize the business flow: register a Candidate, inspect its evidence,
edit/ground/map, review, and explicitly approve it. Add integration guarantees
in proportion to the selected execution/storage model and demonstrated needs.
Reuse current Cozy/CML and CNCF persistence, revision and concurrency contracts;
do not introduce application-owned substitutes for framework facilities.

The [TKL requirements](../journal/2026/10/2026-10-02-tkl-workbench-requirements.md)
are an incoming proposal. Their designation of TKL-TKW-01 through 07 as initial
acceptance does not automatically make every guarantee a prerequisite for the
first TKW development slice. The following disposition is the TKW development
plan; the shared TKL/TKW acceptance contract remains to be agreed.
See the [planning decision](../journal/2026/10/2026-10-02-workbench-development-plan.md).

## Development order

| Order | Work item | Initial outcome |
| --- | --- | --- |
| 1 | Minimum component foundation | Inspect current CML examples, model the first Candidate operations, and establish generation, execution, focused tests and standard persistence |
| 2 | Candidate registration and lookup | Receive a provider-neutral proposal fixture; persist source/proposal identity, original proposal and Candidate correspondence; find that Candidate on sequential resend without applying the incoming content again |
| 3 | Candidate formation and human decisions | Edit content/context, retain versioned evidence references, provide minimal grounding/mapping, and record Review / Approve / Hold / Reject with actor, time, reason and approval target |
| 4 | Workbench views and both inputs | Expose List/Detail/edit/review and evidence limitations through the existing View Model / Display Model direction; run TKL and Editing Studio fixtures through the same lifecycle |
| 5 | KnowledgeHub Admission | Agree the real Information Admission boundary, submit the approved target, confirm the outcome and retain Candidate-to-Information/Evidence trace |
| 6 | Follow-on integration | Add TKL result lookup/feedback and existing-Candidate preparation when their owning contracts and concrete usage are available |

Items 1 through 4 form the first development slice. They can progress without
Drive/provider integration or completion of the app client prototype. They do
not complete Phase 1; item 5 and the completion criteria below remain required.

### Decisions within each work item

Resolve the following as part of their owning work items. They clarify the
existing first slice and do not add prerequisites for starting item 1.

| Work item | Decision to record |
| --- | --- |
| 2 — Candidate registration and lookup | Define the minimal TKL and Editing Studio fixtures, separating common required fields from source-specific optional fields. Unprepared book-capture input must not require TKL PreparedMaterial or ProcessingRun references that do not yet exist; retain its available source/evidence references and represent preparation limitations explicitly. |
| 3 — Candidate formation and human decisions | Record a small lifecycle transition table covering resumption from Hold, whether and how a Rejected Candidate may be edited again, review eligibility when required evidence is unavailable, and renewed review/approval when approved content or evidence changes. These are explicit human-workflow decisions within the existing lifecycle. |
| 4 — Workbench views and both inputs | Identify the View Model / Display Model contracts and capabilities actually available from their owning dependencies. Record any missing upstream capability as a dependency for the affected work; do not infer runtime availability from architectural direction or build a TKW-owned substitute. |

## Initial scope and deferred guarantees

| Requirement | Initial treatment | Later work / boundary |
| --- | --- | --- |
| TKL-TKW-01: proposal receipt | Validate the selected fixture schema and required source/evidence references; create one Candidate per new proposal | Agree shared schema compatibility and transport before live TKL integration |
| TKL-TKW-02: retries and conflicts | Use producerRef + proposalId for lookup; an existing key returns its Candidate and never overwrites human edits | Concurrent receipt guarantees, normalized payload comparison, changed-payload conflict detection and retired-key retention remain joint-contract work |
| TKL-TKW-03: durable correspondence | Persist the proposal-to-Candidate correspondence and expose lookup | First assess whether Candidate metadata suffices; an independent receipt Entity, receipt ID and receipt state machine are not initial prerequisites |
| TKL-TKW-04: evidence/provenance | Keep original proposal and versioned PreparedMaterial / Evidence / Run references; show known retrieval or retention limitations | Extend reference resolution as real sources become available; TKW does not duplicate raw material or provider adapters |
| TKL-TKW-05: human formation | Implement the common edit / ground / review / approve / hold / reject flow | Extend comparisons and mapping as admitted domain contracts become available |
| TKL-TKW-06: revision and approval | Reuse CNCF revision/concurrency facilities; record approved content/evidence and require renewed review when that target changes | Select the actual framework/provider policy during implementation; do not claim update-conflict guarantees from a revision field alone |
| TKL-TKW-07: failure and restart | Report observed failure or uncertainty without claiming success; preserve confirmed correspondence and look it up on retry | A dedicated recovery engine, multi-stage resume protocol and comprehensive fault-injection coverage are deferred |
| TKL-TKW-08: Admission | Retain as the real Phase 1 completion dependency | Agree target, outcome confirmation and retry semantics with KnowledgeHub; fixture results do not prove real Admission |
| TKL-TKW-09 through 10: feedback | Preserve source linkage and supplied Information/version references when present | Start with result lookup after Admission; notification/replay machinery and extended knowledge comparison follow demonstrated need |
| TKL-TKW-11 through 12: Candidate preparation | Keep only the source/evidence associations needed by the initial fixtures | Expand CandidateSource / RawDataIndex and implement correlated preparation requests/results later |

Content hashing and general payload canonicalization are not required for the
initial slice. Introduce them only for a specific agreed requirement. The
lookup key remains source/proposal identity. An initial key lookup without
payload comparison does not fulfill the proposal's changed-content conflict
requirement.

Keep receipt observations, Candidate lifecycle, evidence readiness and Admission
outcomes conceptually distinct. This does not require a separate persisted
state machine for every concept. The recorded approval target must distinguish
approved content/evidence from the Entity revision advanced by later operations;
use the minimum representation supported by existing contracts, without a new
hash or versioning subsystem.

## Proportionate validation

The first slice verifies valid/invalid registration, persisted lookup across a
normal restart, sequential resend after human edits, both source fixtures,
evidence limitations, human review outcomes and approval invalidation on target
change. Select focused update-conflict checks for the CNCF policy actually used.
Do not require the full incoming AC-01 through AC-11 matrix before this slice.

Concurrent receipt, payload-conflict detection and intermediate-failure recovery
need separate evidence when implemented. Deferred or unverified guarantees stay
explicitly open; the first slice must not be reported as satisfying all
TKL-TKW-01 through 07 or all incoming acceptance examples.

## Architectural invariants

- mobile Capture Confirm is transfer authorization, not Information Admission;
- AI/provisional editing cannot perform final admission;
- canonical KnowledgeHub knowledge is Information;
- RDF/Open Knowledge is downstream through KnowledgeProjection, not a Phase 1 canonical model;
- TKW owns candidate/admission lifecycle; Editing Studio/TKL provide domain/source interaction.

## Non-goals

TKL evidence federation; Google Workspace/Slack adapters; smartphone capture implementation; publishing-specific Editing Studio UI; agriculture-specific UI; RDF/Open Knowledge publication; automated final approval.

## Completion criteria

Phase 1 is complete when both fixture types enter the same Workbench candidate lifecycle, can be edited/reviewed, explicitly approved by a human, admitted as canonical KnowledgeHub Information, and retain traceability to original evidence/context.

Completion of the first development slice or simulated Admission alone does not
meet these criteria. This plan records scope and order, not implementation or
acceptance evidence.
