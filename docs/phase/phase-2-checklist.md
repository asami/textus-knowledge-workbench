# Phase 2 Checklist — Material-backed Candidate Work

status=in-progress
planned_at=2026-10-28
owner=textus-knowledge-workbench
execution_started=true
phase=phase-2.md

This is the execution ledger for [TKW Phase 2](phase-2.md). Native Session
`8e7f63e6e34ec5b0aefe9aff93509af9` closed P2-01 at `98adb364` after selected
checks and independent review, then closed P2-02 at `b40de62c` after compile,
48 tests across five suites including PostgreSQL, and independent review.
P2-03 closed at `f0949753` after successful checks and independent review.
P2-04 closed at `166f10c5b95a800a09f457003891ac77a5f72211` after connected
checks and independent review. Final full validation passed at receipt
`8e7f63e6e34ec5b0aefe9aff93509af9:ordered-validation:672:validation@1`.
The first whole-Phase review returned REPAIR for stale documentation (issue-6).
This documentation repair is authored; closing review, native closure recording
and the distinct local release remain pending. Retain failed attempts and limits.
The [Phase index](README.md) owns navigation; the
[implementation considerations](../notes/material-candidate-work-design.md)
remain non-normative design input.

## P2-01 — Selected-Material creation contract

Stage Status:
- Current status: CLOSED
- Owner: TKW development parent
- Update rule: Close from the reviewed contract and dependency findings below.

Accepted contract: [Material-backed Candidate work v1](../spec/material-candidate-work-v1.md).
Native local Step commit: `98adb364573893ab87e3f35309b75118d35b63e9`.

- [x] P2-01-A: Bind the actual consumed MaterialWork/TKL Operations and logical result
  fields, including authorization and missing revision/snapshot facts.
- [x] P2-01-B: Specify the minimal public command/input and proposal-key ownership,
  using existing registration/lookup and Candidate storage wherever sufficient.
  Define exact Material/resource correspondence and the first-proposal record.
- [x] P2-01-C: Specify explicit creation intent, same-intent lookup/resend, deliberate
  new intent, unknown acknowledgement and preservation of later human edits.
- [x] P2-01-D: Fix the minimum decision outcome: real source inspection/edit/Hold must
  work; Review/Approve retain their existing evidence prerequisites. An actual
  version/snapshot dependency gap is reported, never replaced by invented facts.

## P2-02 — Native Candidate creation and source lookup

Stage Status:
- Current status: CLOSED
- Owner: TKW implementation and independent review owners
- Update rule: Close from focused executable specifications and accepted review.

- [x] P2-02-A: Selected caller-authorized Material/resources produce one confirmed
  native Candidate through the bound contract, with logical correspondence
  retained across native store restart and same-intent lookup.
- [x] P2-02-B: Reading/searching/selecting alone creates no Candidate. Invalid,
  unauthorized or unresolved selections create no Candidate; an uncertain
  mutation outcome is reconciled by the existing exact source-key lookup.
- [x] P2-02-C: Sequential same-intent registration preserves the existing Candidate,
  original proposal and human edits; native conditional revisions continue to
  protect edits/decisions. No concurrent-registration guarantee is added.
- [x] P2-02-D: Run affected proposal/formation/MaterialWork/factory specifications,
  including native PostgreSQL persistence for new stored correspondence;
  complete independent focused review of the connected change.

Native local Step commit: `b40de62c19a34d50a45276445e57f42576355e40`.

## P2-03 — Workbench browser interaction

Stage Status:
- Current status: CLOSED
- Owner: TKW browser and independent review owners
- Update rule: Close from actual served-browser observations and focused checks.

Accepted source revision `484`: primary compile and 18 tests across two suites
passed; the admitted dependency passed compile and 59 tests across five suites.
Independent review accepted both candidates without blockers, resolving issue-3/4.
Native Step commit: `f0949753f5b39d4a59ba6b3da905e9ce2b840cde`.

Earlier HTTP/collection and dependency test-compilation failures remain in Session
history. The browser used a controlled logical Material producer and separate
controlled ready evidence. Actual cloud original inspection and its persisted
Candidate were subsequently accepted in P2-04 below; no real-source approval is claimed.

- [x] P2-03-A: Search -> select -> inspect -> safe original preview works through
  MaterialWork/TKL using the admitted caller and private binary-response policy.
- [x] P2-03-B: An explicit create action opens the confirmed Candidate; subsequent
  editing/source navigation returns to the same ID and observed revision.
- [x] P2-03-C: Unsaved editing, empty results, missing/denied originals, conflicts and
  uncertain commands have truthful UI outcomes; no automatic mutation replay
  or stale successful selection is displayed.
- [x] P2-03-D: Inspect real-source evidence and record Edit/Hold. Review/Approve remain
  unavailable where evidence readiness is insufficient; verify unchanged-target
  decisions and edit-driven approval invalidation with valid controlled evidence.
  No Information admission action is needed to complete this delivery.
- [x] P2-03-E: Complete focused browser/HTTP specifications and independent review;
  retain the existing native Display and Business Operation boundaries.

## P2-04 — Connected acceptance and handoff

Stage Status:
- Current status: CLOSED
- Owner: TKW connected acceptance owner
- Update rule: Closed from connected receipt 659, independent Step review and the native Step commit.

Native Step commit: `166f10c5b95a800a09f457003891ac77a5f72211`.
Connected receipt `8e7f63e6e34ec5b0aefe9aff93509af9:ordered-validation:659:validation@1`
records compile and 19 passing tests across three suites. The actual TKL/browser
case inspected one existing real Material and a 3000 × 4000 JPEG, created exactly
one Candidate, edited revision 1 to 2, then Held revision 3 with present-empty
context. Same-ID reload/source preview and PostgreSQL restart retained complete
work, revision, first association and exact-intent lookup. All 1,535,406 bytes
matched the captured original at native Bag and HTTP boundaries before/after
work; the public original was read again after restart.

The real fixture caller was the already admitted
`editing-studio-mobile-development`, a provider-authenticated service subject
using a disposable loopback bearer, not real end-user OAuth. The neutral consumer
uses a separate fixture caller and requires no Book/ISBN/Studio/provider fields.
Real evidence stays unverified without revision/snapshot; Review/Approve remain
unavailable. Controlled ready-evidence approval is separate P2-03 receipt 484.
Four earlier failed fixture preconditions remain in history.

- [x] P2-04-A: A second domain-neutral consumer fixture creates/loads the same work
  through the public contract without mandatory Book/ISBN/Studio/provider fields.
- [x] P2-04-B: One existing real Material is inspected and referenced from a persisted
  Candidate in the actual Workbench browser, with restart/readback, truthful
  evidence limitations and a recorded human decision. Original bytes remain
  unchanged; record complete equality at the binary boundary when available.
- [x] P2-04-C: Keep real-source observations separate from controlled ready-evidence
  Review/Approve tests; no fixture outcome claims real versioned-source approval.
- [x] P2-04-D: Complete focused connected checks and independent Step review
  against the actual combined source, neutral-consumer and failure evidence;
  keep final repository-full validation with the separate Phase closure gate.
- [x] P2-04-E: Record accepted source/contracts, executable/browser evidence and limits;
  update only the demonstrated K03/K04 checklist scope and complete the local
  P2-04 acceptance commit through the normal execution owner.

## Phase closure

Stage Status:
- Current status: OPEN
- Owner: TKW Phase acceptance owner
- Update rule: Close after all accepted Step commits and the actual Phase gates below.

- [x] P2-C01: After accepted P2-01 through P2-04, run the repository-complete
  TKW SBT suite on the final frozen product tree and retain actual PostgreSQL
  results. Intermediate Steps require focused checks, not repeated full suites.
- [ ] P2-C02: Complete independent whole-Phase review, qualifying any admitted
  repairs through the owning workflow while retaining prior evidence and history.
- [ ] P2-C03: Reconcile the Phase/checklist and K03/K04 handoff to actual accepted
  results, preserve limits and separate Studio obligations, and record closure.
- [ ] P2-C04: Complete the distinct local Phase release commit through the
  owning workflow and confirm Phase completion. Main integration, push and
  external publication remain excluded.

Full receipt `8e7f63e6e34ec5b0aefe9aff93509af9:ordered-validation:672:validation@1`
passed 71 tests in nine suites, including actual PostgreSQL 17.11, on the accepted
P2-04 product candidate. The normal full run excludes opt-in cloud/browser cases;
connected receipt 659 and controlled browser receipt 484 remain separate evidence.

The initial whole-Phase review confirmed integrated behavior and matching full
validation, but returned REPAIR for the stale ledger/current handoff (issue-6).
This source repair reconciles those documents. P2-C02 awaits independent closing
review; P2-C03 awaits its native closure record; P2-C04 awaits the owning workflow's
distinct local release and terminal result. No release commit or Phase completion
is claimed by these documentation edits.

## Retained external obligations

Editing Studio Phase 1.2 remains on hold. Its consumer orchestration and full
Studio acceptance in TKW-K04 are outside this Phase's checklist; they remain
explicitly unresolved in the [cross-project plan](knowledge-editing-integration-plan.md).
No inherited acceptance item is removed or marked complete by this separation.

<!-- sm-workflow completion 8e7f63e6e34ec5b0aefe9aff93509af9 phase phase-2 -->
Accepted phase phase-2.
Evidence: 8e7f63e6e34ec5b0aefe9aff93509af9:ordered-local-completion@1
Evidence: file:///Users/asami/src/Project2026/nict-editing-studio/.codex-workflow/checkouts/tkw-step3/target/sm-workflow/tasks/tkw-phase2-20261028/phase-review2-result.json
Local record/commit closure is runtime-owned; later publication, return and synchronization outcomes are recorded separately.
