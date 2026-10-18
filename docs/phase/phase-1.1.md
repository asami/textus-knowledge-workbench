# Phase 1.1 — KnowledgeHub Information Admission and Trace

status=planned
split_full_test_policy=final-only
split_full_validation_method=sbt-full-suite
split_validation_bootstrap=none
validation_ownership=aggregate-final-owner
aggregate_validation_owner=PHASE-1.1
aggregate_validation_sequence=["PHASE-1","PHASE-1.1"]
depends_on=phase-1.md
Updated: 2026-10-02
Split from: [Phase 1](phase-1.md#applied-split--2026-10-02)
Predecessor: [Phase 1 — Candidate Formation](phase-1.md)
Successor: none selected; feedback/preparation remain future work

## Goal

Admit both fixture-derived, human-approved TKW Candidates as real canonical
KnowledgeHub Information, confirm the outcome, and retain Information-to-Candidate/
Evidence trace. This owns the original Phase 1 end-to-end completion gate.

## Input and dependency

Consume Phase 1's accepted runnable component, TKL and Editing Studio fixtures,
persisted source correspondence, lifecycle/approval-target contract, executable
specifications and committed closure/release evidence. Its root `build.sbt` and
successful bootstrap full-suite evidence must already exist.

Both fixtures enter the same TKW lifecycle: a prepared TKL proposal and an
unprepared capture-derived Editing Studio submission whose available references
need not include PreparedMaterial/ProcessingRun. Approval identifies the content
and relevant evidence actually reviewed; later Entity revision changes do not
redefine that target. Target changes require renewed review/approval.

Obtain the actual `nict-knowledgehub` Information Admission contract and an
executable integration dependency before implementing the affected boundary.
An unavailable checkout is a verification limitation. This plan does not admit
source changes in KnowledgeHub, CNCF or other repositories. If an upstream
capability is missing, record the owning dependency and request scope/contract
resolution; do not introduce a TKW canonical shadow store or framework substitute.

## Development order

| Original item | Owned work | Required outcome |
| --- | --- | --- |
| 5a | Resolve the real Information Admission contract | Record target representation, approved-target submission, outcome confirmation, retry and trace semantics with the owning contract |
| 5b | Integrate TKW Admission | Submit an eligible human-approved target, retain the observed request/outcome, and link canonical Information back to Candidate/Evidence |
| 5c | Validate and close the sequence | Exercise both fixture paths against actual KnowledgeHub and run the repository-complete SBT suite for Phase 1 and Phase 1.1 |

These are parts of former work item 5, owned only here. Phase 1 owns items 1-4;
former item 6 (TKL result feedback/existing-Candidate preparation) is outside both.

## Decisions and scope

Determine failure/uncertainty handling from the real KnowledgeHub contract,
including whether a failed request returns the Candidate to Approved or retains
AdmissionRequested with an observed outcome. Record allowed retry/lookup behavior;
never claim success before canonical outcome confirmation.

Reuse existing CNCF/KnowledgeHub Information, semantic grounding and standard
revision/concurrency contracts where applicable. Preserve Phase 1's human
workflow and source/evidence contract; this child adds AdmissionRequested/Admitted
behavior, actual boundary integration and trace, without redesigning registration.
Update the applicable CML workflow, operations, views and executable specs for
those Admission edges. Use existing View/Display contracts for outcome presentation.

Preserve original proposal, producerRef + proposalId correspondence and available
Evidence/PreparedMaterial/Run provenance. TKW owns Candidate/admission lifecycle;
KnowledgeHub owns canonical Information; TKL owns preparation/raw material;
Editing Studio owns domain interaction. Mobile Capture Confirm authorizes transfer,
and AI/provisional editing cannot perform final Information Admission.

## Validation and completion criteria

Focused behavior/specification evidence must demonstrate:

- both Phase 1 fixture-derived Candidates retain their common human lifecycle
  and approved targets through real KnowledgeHub submission;
- unapproved or changed targets cannot obtain Admission through the TKW path;
- actual canonical Information creation/acceptance is confirmed, with its identity
  and trace back to the Candidate, reviewed Evidence and context;
- observed failure or uncertainty is recorded without reporting success, with
  retry/lookup following the selected owning contract.

Do not require general fault-injection/recovery machinery or concurrent receipt
proof deferred by the [development policy](phase-1.md#initial-scope-and-deferred-guarantees).
Fixture/simulated Admission is useful during implementation but cannot close
this Phase. Known evidence limitations remain visible.

Each Step still requires focused validation and acceptance. This Phase requires
independent full Phase review, a closure ledger and local release commit. As
`aggregate-final-owner`, verify Phase 1's accepted ancestry and closure handoff,
then run `sbt --batch test` once on the frozen final tree for the complete
`PHASE-1 -> PHASE-1.1` sequence. The final suite does not retroactively rewrite
Phase 1's recorded bootstrap/deferred disposition. No closed Phase policy changes.

The sequence is complete only when both fixture types can be edited/reviewed,
explicitly approved by a human, admitted as canonical KnowledgeHub Information,
and retain traceability to original evidence/context, with all review and
validation obligations satisfied. A planning split or Phase 1 closure alone
does not meet this end-to-end criterion.

## Non-goals

Reimplementing Phase 1 registration/formation; TKL feedback/notification and
existing-Candidate preparation; concurrent receipt/payload canonicalization;
dedicated recovery engines; raw bodies/provider adapters; smartphone capture;
publishing/agriculture-specific UI; RDF/Open Knowledge publication; automatic
approval; upstream source mutations without explicit scope authority.

## Split provenance and packing

Created by the user's explicit `cncf-split-phase Phase 1` apply invocation on
2026-10-02. The original file/identity and prior planning/review history remain
in Phase 1; this new child receives only unfinished former item 5. No product
goal existed at split (`none / no-goal-pre-entry`), and the split starts no child.
The prior proposal's provisional labels are superseded by **Phase 1 -> Phase 1.1**.

The dated pre-split estimate was expected 12h, upper 16h, low confidence with
no comparable planned/observed calibration. Split reason: `[time-bound]` only.
This child expects 6h, range 5-8h, including Admission contract discovery,
implementation/specs, independent review, likely bounded repair, acceptance and
aggregate validation/release. Dependency availability is an assumption; unbounded
external waiting is excluded. Its remaining expensive kernel is the actual
Admission/outcome/retry/trace contract. Keep coherent integration and assurance
with that decision; a micro-Phase for a model switch would duplicate context.

Both children use recommended `gpt-6.1-sol / high` with standard parent mode.
No profile switch or profile-cost saving is claimed. Consume the exact accepted
component, approval-target contract and specs from Phase 1. Incoming semantic
handoffs: `[]`; this serial dependency is not asserted as a genuine semantic
boundary. No child is below 5h; short-child rebalance/merge exceptions are not
needed. A combined child would expect 12h, exceeding 8h. Extra review/release/
handoff overhead is included in the estimates and buys independent closure
within the ceiling. See the [original split note](phase-1.md#applied-split--2026-10-02).

## Structural Phase Plan Gate

Phase Plan Gate: PROCEED

- target: 6h expected; allowed ceiling 8h
- estimate_calibration: no comparable evidence; low-confidence bottom-up estimate
- planning_demand: protected-decision
- recommended_parent_profile: gpt-6.1-sol / high
- profile_cost_role: expensive reasoning kernel with coherent execution
- expensive_reasoning_kernel: actual KnowledgeHub Admission/outcome/retry/trace contract
- frozen_profile_transition_handoff: no profile switch; consume Phase 1's accepted component, approval target, fixtures/specs and closure evidence
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
