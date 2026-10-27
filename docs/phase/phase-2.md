# Phase 2 — Material-backed Candidate Work

status=planned
planned_at=2026-10-28
owner=textus-knowledge-workbench
execution_started=false
predecessor=phase-1.1.md
checklist=phase-2-checklist.md
validation_ownership=phase-owner
full_validation_method=sbt-full-suite

## Purpose

Make Workbench usable with an existing TKL Material: search/select a source,
inspect its original, explicitly create a Candidate, then reopen, edit and
record a human decision on that same Candidate with retained source/evidence
correspondence. Workbench's browser is the initial consumer. A second
domain-neutral fixture proves the shared work contract.

This is ordinary TKW Phase 2 following completed Phase 1 and Phase 1.1. Its
execution ledger is [Phase 2 Checklist](phase-2-checklist.md). The earlier
unnumbered delivery draft is superseded; it is not a parallel execution ledger.
Phase planning does not start a Goal, adopt a native Plan or claim acceptance.

## Inputs and preserved acceptance

| Input | Accepted boundary |
| --- | --- |
| TKW Phase 1 | Local closure `f96057bd85f17b5647c520e2f00e690e14c63363`: proposal registration/lookup, persistence, explicit human work and views. |
| TKW Phase 1.1 | Native Session `dc5aec0f04843c377ad539f04783861f` completed; local release `43205e2db92a3fb70ea618971b5e7a6e2b82a856`. Its earlier release-candidate source snapshot does not supersede that terminal result. |
| KnowledgeHub | Accepted local dependency commit `4fad532f798925c797736a2e7c1bb8cd7ecdeb7f`, with retained compile, 19 real-PostgreSQL tests and independent review. |
| Material route | Existing MaterialWork/TKL public search/metadata/original Operations; closed Studio Phase 3 supplies reference-route evidence at its recorded scope. |

Preserve original proposals, human edits/history, native IDs/revisions,
approval-target invalidation and admitted Information behavior. Reuse accepted
evidence only where relevant to the unchanged consumed source and boundary.

## Workstream ownership

| Existing item | Phase 2 responsibility | Separate responsibility retained |
| --- | --- | --- |
| TKW-K01/K02 | Consume and bind the accepted reference route. | No replay of registration/materialization or reopening of accepted reference work. |
| TKW-K03 | Deliver selected-Material -> Candidate correspondence and human work. | Candidate admission behavior remains the accepted Phase 1.1 contract. |
| TKW-K04 | Neutral consumer, Workbench browser integration and failure outcomes. | Actual Studio Candidate integration remains separately owned with Studio Phase 1.2 on hold. |

Phase 2 is not a child of Studio Phase 1.2. That hold does not prevent direct
Workbench use. Full K04 is not claimed complete by accepting this scoped
Workbench delivery. Design questions and tentative mappings remain in
[notes](../notes/material-candidate-work-design.md); settled behavior belongs
in the owning specs and executable specifications during implementation.

## Step sequence and acceptance ledger

| Step | Observable delivery | Prerequisite | Checklist IDs |
| --- | --- | --- | --- |
| P2-01 | Exact public contract, proposal-key ownership, source mapping and truthful evidence-readiness boundary. | Accepted public TKL/TKW contracts. | P2-01-A through P2-01-D |
| P2-02 | Explicit native Candidate creation, source-key lookup, persistent correspondence and unchanged human edits. | P2-01 | P2-02-A through P2-02-D |
| P2-03 | Real Material search/preview/create/edit/decision in the served Workbench browser. | P2-02 | P2-03-A through P2-03-E |
| P2-04 | Neutral consumer and real-source connected acceptance, focused review and handoff. | P2-03 | P2-04-A through P2-04-E |

Each Step is accepted from its checklist, affected behavioral evidence and
independent review. Accepted coherent implementation Steps receive local Step
commits through the owning workflow. A checklist mark alone is not a native
receipt. Keep preparation, adopted runtime Plan, issued work, validation,
independent review, commit and Phase closure distinct.

After all four accepted Step commits, the separate Phase closure stage owns
P2-C01 through P2-C04: final full validation, independent whole-Phase review,
closure ledger and release commit. No Step acceptance depends on a later
Phase release or substitutes for whole-Phase closure.

## Implementation scope and constraints

Product changes are limited to TKW CML, handwritten actions/factories, generated
contract declarations/ABI, browser assets, specs and documentation. Start
execution on a dedicated branch preserving the accepted baseline and unrelated
work. TKL/CNCF/Hub are public-contract inputs; report a missing owning capability
before proposing upstream source scope.

Use the native Candidate store, existing proposal correspondence and ordinary
same-caller Operations. Search/preview does not create a Candidate. Explicit
creation and decisions retain identity/revision and unknown-outcome handling;
there is no new receipt engine, provider adapter or approval bypass.

Reuse an existing live Material read-only. Local repeatable integration uses
disposable native PostgreSQL and controlled TKL; Candidate writes belong to the
isolated test/work store. Preserve original bytes and private response policy.
No Drive writes, materialize replay, credential/ACL change, main integration,
push or external publication is included.

## Validation and closure criteria

- Focused specs cover source authorization, exact logical correspondence,
  explicit creation, same-intent lookup/resend, intentional new work, restart,
  human-edit preservation, native revision conflict and unknown acknowledgement.
- Actual served-browser evidence covers selection, source inspection, create,
  reopening, edits, attributed decisions, unsaved work and failure outcomes.
- One existing real Material and a separate neutral consumer demonstrate the
  connected route. Genuine versioned evidence alone enables Review/Approve;
  insufficient real evidence remains inspectable/editable with an honest Hold.
- Phase closure P2-C01 owns one repository-complete TKW SBT validation on the
  final frozen product tree after accepted Steps. No full suite is required
  per helper or intermediate Step.
- Complete independent whole-Phase review, admitted repairs, exact checklist
  disposition, closure ledger and distinct local release commit. Then mark
  Phase 2 closed through the owning workflow. Retain real/controlled evidence
  distinctions and separately held Studio obligations.

## Entry questions and estimate

P2-01 must settle the minimal Material/resource reference mapping and creation
intent before implementation. Existing public metadata has no generic
revision/snapshot field; a read or registration ID cannot certify that version.
Report any producer gap and its affected criterion without weakening the
existing approval gate or silently expanding this Phase's repository scope.

Estimate: expected eight hours, range six to twelve, low confidence without
comparable calibration. Includes contract binding, implementation, focused
checks/reviews, local acceptance and final validation/review. External capability
development/provider waiting is excluded; reassess from actual entry findings.
No automatic split or execution start follows from this estimate.

## References

- [Phase index](README.md)
- [Cross-project integration plan](knowledge-editing-integration-plan.md)
- [Shared editing boundary](../spec/knowledge-editing-component-boundary.md)
- [Material work contract](../spec/material-work-reference-v1.md)
- [Proposal contract](../spec/candidate-proposals.md)
- [Human work contract](../spec/candidate-formation-and-review.md)
- [Workbench view contract](../spec/candidate-workbench-views.md)
