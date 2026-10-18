# Phase 1 — Information Candidate Formation Vertical Slice

status=planned
split_full_test_policy=final-only
split_full_validation_method=sbt-full-suite
split_validation_bootstrap=bootstrap-structural
validation_ownership=aggregate-deferred
aggregate_validation_owner=PHASE-1.1
aggregate_validation_sequence=["PHASE-1","PHASE-1.1"]
Updated: 2026-10-02
Successor: [Phase 1.1 — KnowledgeHub Admission](phase-1.1.md)

## Goal

Establish a runnable Textus Knowledge Workbench component that registers producer
proposals, supports evidence-aware Candidate formation and views, and records
explicit human review/approval. Produce the accepted Candidate and approval-target
contract consumed by [Phase 1.1](phase-1.1.md).

## Primary use case

Register a raw/prepared producer proposal as a TKW InformationCandidate, then
form, review and explicitly approve the Candidate for later Information Admission.

## Reference flow

Raw/Prepared Proposal -> TKW registration -> InformationCandidate -> Resolve Evidence/Context -> Edit/Correct -> Semantic Grounding/Mapping -> Human Review -> Approve/Hold/Reject.

The approved target passes to Phase 1.1 for real Information Admission and its
Candidate/Evidence trace. That successor's outcome is not a Phase 1 closure claim.

## Inputs

Support deterministic fixtures for at least:
- a TKL RawCandidateProposal with PreparedMaterial/Evidence and available Run/provenance references;
- an Editing Studio submission derived from confirmed book-capture raw data, including unprepared input without PreparedMaterial/Run references.

Full Google Workspace, Slack and smartphone integration is not required for closure.

## Scope

- initial InformationCandidate Entity/lifecycle;
- candidate identity and Evidence/Provenance references;
- minimum CandidateSource origin/correspondence and RawDataIndex supporting-material associations;
- candidate content/context editing;
- minimal grounding/mapping using existing CNCF/KnowledgeHub contracts where applicable;
- explicit human review decision and approval;
- source/Evidence linkage and approved content/evidence target for the successor;
- CML application use case/workflow model;
- executable specification for the vertical slice.

Work item 1 creates the repository's root `build.sbt`, using current Cozy/CML
and CNCF contracts, and establishes executable focused and complete validation.
This new bootstrap owns the missing validation entry; it must exist and pass
the complete SBT suite before Phase 1 closure.

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
| 1 | Minimum component foundation | Inspect current CML examples, model the first Candidate operations, and establish generation, execution, root build.sbt, focused/full tests and standard persistence |
| 2 | Candidate registration and lookup | Receive a provider-neutral proposal fixture; persist source/proposal identity, original proposal and Candidate correspondence; find that Candidate on sequential resend without applying the incoming content again |
| 3 | Candidate formation and human decisions | Edit content/context, retain versioned evidence references, provide minimal grounding/mapping, and record Review / Approve / Hold / Reject with actor, time, reason and approval target |
| 4 | Workbench views and both inputs | Expose List/Detail/edit/review and evidence limitations through the existing View Model / Display Model direction; run TKL and Editing Studio fixtures through the same lifecycle |

Items 1 through 4 are owned only by Phase 1. They can progress without
Drive/provider integration or completion of the app client prototype.
[Phase 1.1](phase-1.1.md#development-order) owns former item 5, real KnowledgeHub
Admission. Former item 6, TKL feedback and existing-Candidate preparation,
remains follow-on work outside both Phases; see the
[development-plan split note](../journal/2026/10/2026-10-02-workbench-development-plan.md#phase-split--2026-10-02).

### STEP-2 dependency follow-up: fixed POWERTYPE codes (2026-10-19)

Execution handoff: [POWERTYPE persistence-code handoff](../journal/2026/10/2026-10-19-powertype-persistence-code-handoff.md).

Apply the [CML persistence-code specification](../../../../dev2025/cozy/docs/spec/cml-powertype-persistence-code.md)
and [connected producer plan](../../../../dev2025/cozy/docs/phase/powertype-persistence-code-plan.md).
ProposalPreparationState keeps its explicit `prepared=1` and `unprepared=2`
mapping; JSON/CLI retain the symbolic strings. Core owns the generic typed
bidirectional contract, each generated POWERTYPE owns its fixed mapping, and
the declared persistence boundary selects it before external string projection.
The database mapping is `XShort`, using the shared Short attribute path and
retaining its signed 16-bit width through SQL type selection. The existing
generic Int-valued correspondence API does not select the physical SQL type.

First complete the actual generated save/read/provider-reopen route. Combine
the producer rejection scenarios (missing/duplicate/invalid codes and persistent
reuse of an uncoded transient type), stable mapping after reorder/label edits,
and unknown stored-code failure into the same integration work. Display-only
uncoded types remain valid. Preserve applicable existing test/review evidence
and complete independent integrated review before recording acceptance.

No ordinal assignment, textual-storage fallback, application-owned replacement
codec or live database rewrite is introduced. This is STEP-2 dependency work;
closed Core Phase 3, Cozy Phase 78 and CNCF Phase 100/115 remain closed. Its
implementation and connected verification are accepted by the separate
[STEP-2 execution ledger](phase-1-checklist.md). This plan retains its original
scope; STEP-3, STEP-4 and the Phase release gate remain pending.

### Decisions within each work item

Resolve the following as part of their owning work items. They clarify the
existing first slice and do not add prerequisites for starting item 1.

| Work item | Decision to record |
| --- | --- |
| 2 — Candidate registration and lookup | Define the minimal TKL and Editing Studio fixtures, separating common required fields from source-specific optional fields. Unprepared book-capture input must not require TKL PreparedMaterial or ProcessingRun references that do not yet exist; retain its available source/evidence references and represent preparation limitations explicitly. Distinguish CandidateSource origin/correspondence from RawDataIndex supporting-material associations using the smallest suitable representation. |
| 3 — Candidate formation and human decisions | Record a small lifecycle transition table covering resumption from Hold, whether and how a Rejected Candidate may be edited again, review eligibility when required evidence is unavailable, and renewed review/approval when approved content or evidence changes. These are explicit human-workflow decisions within the existing lifecycle. |
| 4 — Workbench views and both inputs | Identify the View Model / Display Model contracts and capabilities actually available from their owning dependencies. Record any missing upstream capability as a dependency for the affected work; do not infer runtime availability from architectural direction or build a TKW-owned substitute. |

The [architecture](../notes/architecture.md#candidate-sources-and-supporting-references)
defines source/index responsibilities. Separate responsibilities do not mandate
separate Aggregates or tables; the review's DTO examples are not mandatory
schemas. Keep TKL processing
facts as references; full CandidatePreparationRequest/result schemas belong to
follow-on implementation. These refinements adopt the
[TKL alignment review](../journal/2026/10/2026-10-02-phase-1-tkl-alignment-review.md)
without adding first-slice guarantee requirements.

## Initial scope and deferred guarantees

| Requirement | Initial treatment | Later work / boundary |
| --- | --- | --- |
| TKL-TKW-01: proposal receipt | Validate the selected fixture schema and required source/evidence references; create one Candidate per new proposal | Agree shared schema compatibility and transport before live TKL integration |
| TKL-TKW-02: retries and conflicts | Use producerRef + proposalId for lookup; an existing key returns its Candidate and never overwrites human edits | Concurrent receipt guarantees, normalized payload comparison, changed-payload conflict detection and retired-key retention remain joint-contract work |
| TKL-TKW-03: durable correspondence | Persist the proposal-to-Candidate correspondence and expose lookup | First assess whether Candidate metadata suffices; an independent receipt Entity, receipt ID and receipt state machine are not initial prerequisites |
| TKL-TKW-04: evidence/provenance | Keep original proposal and available versioned PreparedMaterial / Evidence / Run references; show known retrieval or retention limitations | Extend reference resolution as real sources become available; TKW does not duplicate raw material, TKL processing lifecycle or provider adapters |
| TKL-TKW-05: human formation | Implement the common edit / ground / review / approve / hold / reject flow | Extend comparisons and mapping as admitted domain contracts become available |
| TKL-TKW-06: revision and approval | Reuse CNCF revision/concurrency facilities; record approved content/evidence and require renewed review when that target changes | Select the actual framework/provider policy during implementation; do not claim update-conflict guarantees from a revision field alone |
| TKL-TKW-07: failure and restart | Report observed failure or uncertainty without claiming success; preserve confirmed correspondence and look it up on retry | A dedicated recovery engine, multi-stage resume protocol and comprehensive fault-injection coverage are deferred |
| TKL-TKW-08: Admission | Owned by [Phase 1.1](phase-1.1.md); not implemented or accepted in Phase 1 | Agree target, outcome confirmation and retry semantics with KnowledgeHub in that successor; fixture results do not prove real Admission |
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

Real KnowledgeHub Admission and its outcome/trace implementation (Phase 1.1);
TKL feedback/existing-Candidate preparation; TKL evidence federation;
Google Workspace/Slack adapters; smartphone capture implementation;
publishing-specific Editing Studio UI; agriculture-specific UI;
RDF/Open Knowledge publication; automated final approval.

## Completion criteria

Phase 1 is complete when its standard component runs, both fixture types enter
the same persisted Candidate lifecycle, sequential resend preserves human edits,
Candidates can be edited/grounded/reviewed and explicitly approved by a human,
Hold/Reject and approval-invalidation behavior is specified and validated,
and List/Detail/edit/review expose available evidence and its limitations.
Retain source/context/Evidence correspondence and the reviewed approval target
in the accepted handoff. Validate normal-restart lookup and the selected CNCF
update policy without claiming deferred guarantees.

Every Step requires focused validation and acceptance; this Phase requires its
independent full Phase review, closure ledger and local release commit. The
root `build.sbt` must exist and `sbt --batch test` must succeed at bootstrap
closure. Although aggregate validation ownership is deferred, that first-child
bootstrap proof is mandatory. Phase 1.1 later owns complete validation for
the whole serial sequence, including both Phases' accepted ancestry.

Phase 1 closure proves Candidate formation. The original end-to-end criterion,
including real canonical KnowledgeHub Information and its Admission trace,
is retained as [Phase 1.1 completion](phase-1.1.md#validation-and-completion-criteria).
This plan records scope and order, not implementation or acceptance evidence.

## Applied split — 2026-10-02

The user explicitly invoked `cncf-split-phase Phase 1` in apply mode after the
pre-entry Phase gate. The first delivery unit retains identity **Phase 1**;
the second is **Phase 1.1**. The earlier proposal's provisional 1.1/1.2 labels
are superseded by this numbering. No later Phase is renumbered. There were no
completed executable Steps/Slices, test outcomes or releases to move; the source
was planned. Existing planning decisions and review journals remain as history.

**Dated pre-split gate evidence:** `Phase Plan Gate: SPLIT_REQUIRED`,
`split_reasons: [time-bound]`; expected whole-Phase duration 12h, conservative
upper 16h, excluding unbounded external waiting. The repository had only a CML
scaffold and no build/runtime/tests; real KnowledgeHub contract availability
remained unverified. No comparable planned/observed Phase timing was available;
the bottom-up estimate is low confidence, with no calibration multiplier.
The original goal was formation and real canonical Admission; the original
completion statement was:

> Phase 1 is complete when both fixture types enter the same Workbench candidate lifecycle, can be edited/reviewed, explicitly approved by a human, admitted as canonical KnowledgeHub Information, and retain traceability to original evidence/context.

The applied sequence partitions unfinished items exactly once: 1-4 here, 5 in
Phase 1.1, 6 outside the sequence. Each child expects 6h (range 5-8h), including
planning, implementation/specs, independent review, likely bounded repairs,
Step commits and release validation. No child is below 5h; sub-5h rebalance
and sub-4h adjacent-merge exceptions do not apply. Combining the two would
exceed the 8h expected ceiling. Added Phase review/release and contract-handoff
overhead is included; the benefit is two independently closable units.
No model-cost saving or genuine semantic boundary is claimed.

Incoming semantic handoffs by child, in order:
`{"PHASE-1":[],"PHASE-1.1":[]}`. This is an ordinary serial dependency.
Both child parents are recommended `gpt-6.1-sol / high`, standard mode.
There is no profile transition: Phase 1 produces the accepted component,
fixture/spec set, approval-target contract and closure/release evidence;
Phase 1.1 consumes those exact records. Schema/lifecycle/approval and available
View/Display decisions belong here; real Admission/outcome decisions belong
only to Phase 1.1. Settled execution stays with its decision container because
its specs and acceptance depend on that contract; no micro-Phase is created
solely to change profiles. Split planning itself was bounded-settled with
`PROCEED_UNVERIFIED`; current-task profile resolution was `NOT_FOUND`.

Validation is `final-only / sbt-full-suite`, with Phase 1.1 as final owner and
Phase 1 creating and proving the missing build entry under `bootstrap-structural`.
Focused validation, independent review, Step acceptance, closure ledgers and
release commits remain required in both Phases. Implementation touches only
this repository unless further source roots are explicitly admitted.

## Structural Phase Plan Gate

Phase Plan Gate: PROCEED

- target: 6h expected; allowed ceiling 8h
- estimate_calibration: no comparable evidence; low-confidence bottom-up estimate
- planning_demand: protected-decision
- recommended_parent_profile: gpt-6.1-sol / high
- profile_cost_role: expensive reasoning kernel with coherent execution
- expensive_reasoning_kernel: fixture/schema, lifecycle/approval-target and available View/Display contracts
- frozen_profile_transition_handoff: none; produce accepted component, approval target and fixture/spec contracts for Phase 1.1
- parent_reasoning_mode_policy: standard
- estimated_at_recommended_profile: expected 6h, range 5-8h; at target, expected <=8h
- incoming_semantic_handoffs: []
- merge_attempts_for_every_sub_4h_child: none
- rebalance_attempts_for_every_sub_5h_child: none
- adjacent_merge_structural_rejection_evidence: whole sequence expected 12h >8h
- profile_cost_only_rejection_forbidden: true
- short_child_basis: none
- overhead_tradeoff: extra review/release/handoff included; independent closure within ceiling, no claimed profile-cost saving
- agent_reasoning_mode_policy: default standard; pro only at an eligible launch with interface support and frozen quality evidence
- runtime_suitability: re-evaluate in the Phase execution task
- source: applied split from Phase 1
