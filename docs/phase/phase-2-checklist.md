# Phase 2 Checklist — Material-backed Candidate Work

status=in-progress
planned_at=2026-10-28
owner=textus-knowledge-workbench
execution_started=true
phase=phase-2.md

This is the execution ledger for [TKW Phase 2](phase-2.md). Native Session
`8e7f63e6e34ec5b0aefe9aff93509af9` issued ImplementStep P2-01; its contract
is authored, with validation and independent acceptance pending. Update an
item only from actual accepted evidence, and retain failed attempts and limits.
The [Phase index](README.md) owns navigation; the
[implementation considerations](../notes/material-candidate-work-design.md)
remain non-normative design input.

## P2-01 — Selected-Material creation contract

Stage Status:
- Current status: IN_PROGRESS
- Owner: TKW development parent
- Update rule: Close from the reviewed contract and dependency findings below.

Authored contract: [Material-backed Candidate work v1](../spec/material-candidate-work-v1.md).
All items below remain unchecked until accepted by the owning runtime.

- [ ] P2-01-A: Bind the actual consumed MaterialWork/TKL Operations and logical result
  fields, including authorization and missing revision/snapshot facts.
- [ ] P2-01-B: Specify the minimal public command/input and proposal-key ownership,
  using existing registration/lookup and Candidate storage wherever sufficient.
  Define exact Material/resource correspondence and the first-proposal record.
- [ ] P2-01-C: Specify explicit creation intent, same-intent lookup/resend, deliberate
  new intent, unknown acknowledgement and preservation of later human edits.
- [ ] P2-01-D: Fix the minimum decision outcome: real source inspection/edit/Hold must
  work; Review/Approve retain their existing evidence prerequisites. An actual
  version/snapshot dependency gap is reported, never replaced by invented facts.

## P2-02 — Native Candidate creation and source lookup

Stage Status:
- Current status: OPEN
- Owner: TKW implementation and independent review owners
- Update rule: Close from focused executable specifications and accepted review.

- [ ] P2-02-A: Selected caller-authorized Material/resources produce one confirmed
  native Candidate through the bound contract, with logical correspondence
  retained across native store restart and same-intent lookup.
- [ ] P2-02-B: Reading/searching/selecting alone creates no Candidate. Invalid,
  unauthorized or unresolved selections create no Candidate; an uncertain
  mutation outcome is reconciled by the existing exact source-key lookup.
- [ ] P2-02-C: Sequential same-intent registration preserves the existing Candidate,
  original proposal and human edits; native conditional revisions continue to
  protect edits/decisions. No concurrent-registration guarantee is added.
- [ ] P2-02-D: Run affected proposal/formation/MaterialWork/factory specifications,
  including native PostgreSQL persistence for new stored correspondence;
  complete independent focused review of the connected change.

## P2-03 — Workbench browser interaction

Stage Status:
- Current status: OPEN
- Owner: TKW browser and independent review owners
- Update rule: Close from actual served-browser observations and focused checks.

- [ ] P2-03-A: Search -> select -> inspect -> safe original preview works through
  MaterialWork/TKL using the admitted caller and private binary-response policy.
- [ ] P2-03-B: An explicit create action opens the confirmed Candidate; subsequent
  editing/source navigation returns to the same ID and observed revision.
- [ ] P2-03-C: Unsaved editing, empty results, missing/denied originals, conflicts and
  uncertain commands have truthful UI outcomes; no automatic mutation replay
  or stale successful selection is displayed.
- [ ] P2-03-D: Inspect real-source evidence and record Edit/Hold. Review/Approve remain
  unavailable where evidence readiness is insufficient; verify unchanged-target
  decisions and edit-driven approval invalidation with valid controlled evidence.
  No Information admission action is needed to complete this delivery.
- [ ] P2-03-E: Complete focused browser/HTTP specifications and independent review;
  retain the existing native Display and Business Operation boundaries.

## P2-04 — Connected acceptance and handoff

Stage Status:
- Current status: OPEN
- Owner: TKW connected acceptance owner
- Update rule: Close only from all connected evidence and final checks below.

- [ ] P2-04-A: A second domain-neutral consumer fixture creates/loads the same work
  through the public contract without mandatory Book/ISBN/Studio/provider fields.
- [ ] P2-04-B: One existing real Material is inspected and referenced from a persisted
  Candidate in the actual Workbench browser, with restart/readback, truthful
  evidence limitations and a recorded human decision. Original bytes remain
  unchanged; record complete equality at the binary boundary when available.
- [ ] P2-04-C: Keep real-source observations separate from controlled ready-evidence
  Review/Approve tests; no fixture outcome claims real versioned-source approval.
- [ ] P2-04-D: Complete focused connected checks and independent Step review
  against the actual combined source, neutral-consumer and failure evidence;
  keep final repository-full validation with the separate Phase closure gate.
- [ ] P2-04-E: Record accepted source/contracts, executable/browser evidence and limits;
  update only the demonstrated K03/K04 checklist scope and complete the local
  P2-04 acceptance commit through the normal execution owner.

## Phase closure

Stage Status:
- Current status: OPEN
- Owner: TKW Phase acceptance owner
- Update rule: Close after all accepted Step commits and the actual Phase gates below.

- [ ] P2-C01: After accepted P2-01 through P2-04, run the repository-complete
  TKW SBT suite on the final frozen product tree and retain actual PostgreSQL
  results. Intermediate Steps require focused checks, not repeated full suites.
- [ ] P2-C02: Complete independent whole-Phase review, qualifying any admitted
  repairs through the owning workflow while retaining prior evidence and history.
- [ ] P2-C03: Reconcile the Phase/checklist and K03/K04 handoff to actual accepted
  results, preserve limits and separate Studio obligations, and record closure.
- [ ] P2-C04: Complete the distinct local Phase release commit through the
  owning workflow and confirm Phase completion. Main integration, push and
  external publication remain excluded.

## Retained external obligations

Editing Studio Phase 1.2 remains on hold. Its consumer orchestration and full
Studio acceptance in TKW-K04 are outside this Phase's checklist; they remain
explicitly unresolved in the [cross-project plan](knowledge-editing-integration-plan.md).
No inherited acceptance item is removed or marked complete by this separation.
