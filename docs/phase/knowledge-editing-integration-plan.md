# Shared editing applications and TKL integration plan

status=planned
planned_at=2026-10-21
spec=../spec/knowledge-editing-component-boundary.md

## Current handoff — 2026-10-28

The active delivery is [TKW Phase 2 — Material-backed Candidate Work](phase-2.md):
K03 and the Workbench-owned neutral-consumer portion of K04. Editing Studio
Phase 1.2 is on hold; Phase 2's [checklist](phase-2-checklist.md) is the execution
ledger. Native Session `8e7f63e6e34ec5b0aefe9aff93509af9` accepted P2-01 at
`98adb364`, then P2-02 at `b40de62c` after native checks and independent
review. P2-03 closed at `f0949753`; P2-04 closed at
`166f10c5b95a800a09f457003891ac77a5f72211` after connected receipt 659 and
independent Step review. Final full receipt 672 passed 71 tests in nine suites,
including PostgreSQL 17.11. The first whole-Phase review returned REPAIR for
stale current documentation; this repair awaits closing review and native
closure/local release. This coordination plan starts no execution and does not
close K04's actual Studio integration criterion.

Accepted Material reference/registration work remains input from closed Studio
Phase 3. TKW Phase 1/1.1 native closures and KnowledgeHub's accepted local
commit are retained by the new plan. The original OPEN table/checklist below
is the retained 2026-10-21 planning snapshot, not a claim that the accepted
reference route must be implemented again. Reconcile individual items against
their exact evidence using the current disposition below; the historical
checkboxes are preserved as the entry snapshot.

| Item | Current execution owner | Residual boundary |
| --- | --- | --- |
| TKW-K03 | Delivered by accepted Phase 2 P2-01 through P2-03. | Selected-Material/Candidate correspondence and Workbench human work are delivered; Phase release remains pending. |
| TKW-K04 | Workbench-owned neutral-consumer/failure portion delivered by accepted P2-04, connected receipt 659. | Actual Studio Candidate integration remains separately owned with Studio Phase 1.2 on hold; full K04 remains unresolved. |

## Original reference and registration planning — 2026-10-21

K01/K02 reference contract: [Material work reference v1](../spec/material-work-reference-v1.md).
The first slice defines read-only search/inspection/original access before
Candidate creation. Generated producer/consumer bindings and connected
acceptance remain pending; the OPEN entries below are unchanged.

Preserve accepted Phase 1 STEP-1/2. STEP-3/4 continue delivering common Candidate
editing/review and reusable views. This named integration workstream owns live
Material reference access, previously deferred from the fixture-based Phase 1;
it does not silently add live-provider acceptance to those accepted Steps.
Phase 1.1 still owns real KnowledgeHub admission and its aggregate validation.

| ID | Connected delivery | Status |
| --- | --- | --- |
| TKW-K01 | Bind work-oriented Material search/select/inspect/access inputs/results to the public TKL M01 contract; define work/evidence association ownership. | OPEN |
| TKW-K02 | Compose TKL through CNCF and expose one Material search -> metadata -> original route to Studio. | OPEN |
| TKW-K03 | Connect selected Material to existing Candidate registration and the owning Phase 1 editing/review operations; retain one shared identity and work state. | OPEN |
| TKW-K04 | Verify a second domain-neutral consumer fixture, actual Studio integration and failure outcomes; deliver the reusable component contract. | OPEN |

## Checklist

- [ ] TKL public capabilities and consumed versions are bound; no direct Drive
  adapter, folder mapping, package schema implementation or URN construction
  enters Workbench.
- [ ] Studio displays a real original through Workbench/TKL; ordinary reads
  require neither OpenClaw nor Candidate admission completion.
- [ ] Material selection/evidence associations are work-owned; Candidate
  identity, edits and human decisions remain Workbench-owned across clients.
- [ ] A second consumer fixture demonstrates the contract has no mandatory
  Book/ISBN-specific or Studio-only parameters.
- [ ] Candidate edit/review acceptance references Phase 1's actual delivery;
  KnowledgeHub admission remains Phase 1.1, not simulated success in this route.
- [ ] Focused executable specifications and integrated independent review
  cover the connected boundary. Reuse existing accepted evidence where relevant;
  preserve applicable release checks without repeating full suites per helper.

First deliver K01/K02 with TKL M01/M02 and Studio ESS3-02. Then add K03/K04 with
available Phase 1 STEP-3/4 contracts. No requirement to finish all TKL processing
or Phase 1.1 before the first original is displayed. Bind the actual shared
APIs once; report missing TKL capability to its owner rather than implementing
an editor-local/provider-local workaround.

## Connected first-Material registration work

ESS3-02 / TKL-M03 requires work-assisted initial Material registration within
the same TKW-K01/K02 route and original whole Phase 3 Goal. This extends the
[Material work contract](../spec/material-work-reference-v1.md#explicit-initial-material-registration-work)
alongside its three read-only reference queries; it is not TKW-K03 Candidate
acceptance or a new Phase/Goal. All existing OPEN entries, checklist states and
accepted Phase 1 history above retain their current scope and status.

| Required MaterialWork capability | Fixed public TKL Material capability | Ownership |
| --- | --- | --- |
| `createMaterialRegistration(contribution)` | `createMaterialRegistration` | Studio authorizes its source and constructs complete neutral `textus.material-contribution/1` JSON; Workbench forwards it with the same caller; TKL owns native registration creation. |
| `getMaterialRegistration(registrationId)` | `getMaterialRegistration` | TKL owns the exact native ID, committed revision, actual state and planned Material reference; Workbench preserves the response without status synthesis or retry authority. |
| `materializeMaterial(registrationId)` | `materializeMaterial` | TKL owns native authorization, managed OAuth/source/destination, acknowledged progress and physical effects; Workbench forwards the fixed request without replay or duplicate state. |

The owning
[TKL public registration contract](../../../textus-knowledge-lake/docs/spec/material-registration-operations-v1.md)
defines producer behavior. Existing reference reads remain available
independently of registration commands, continuation and Candidate work.
Material registration is not human review, approval or a persisted Candidate
selection. Workbench adds no Material database, provider adapter, credential
surface or general TKL gateway.

Delivery continues in the existing connected route:

1. Bind owning TKL public contract/runtime and retain its producer evidence
   separately from Workbench/Studio availability.
2. Run normal Workbench generation, inspect actual generated DTO/action/factory
   APIs, then implement the fixed same-caller forwards and focused executable
   specifications.
3. Add Studio's authorized immutable-source neutral conversion/action and compose
   registration/status/materialization with its original display through
   Workbench/TKL, including actual native PostgreSQL outcomes and no replay.
4. Complete integrated independent protected Step review, private CAR and real
   Google/App evidence, final full validation/review and original whole Phase
   gates. Retain the existing Candidate acceptance boundary and second neutral
   consumer requirement.

This contract/CML authoring does not establish generation, runtime execution,
successful integration or acceptance. The connected work remains pending under
the existing checklist; no per-helper acceptance review or commit replaces the
integrated gates.

Producer: [TKL plan](../../../textus-knowledge-lake/docs/phase/material-access-and-processing-plan.md).
Consumer: [Studio Phase 3](../../../../Project2026/nict-editing-studio/docs/phase/phase-3.md).
No implementation, acceptance, Goal restart or publication is performed by this plan.

<!-- sm-workflow completion 8e7f63e6e34ec5b0aefe9aff93509af9 phase phase-2 -->
Accepted phase phase-2.
Evidence: 8e7f63e6e34ec5b0aefe9aff93509af9:ordered-local-completion@1
Evidence: file:///Users/asami/src/Project2026/nict-editing-studio/.codex-workflow/checkouts/tkw-step3/target/sm-workflow/tasks/tkw-phase2-20261028/phase-review2-result.json
Local record/commit closure is runtime-owned; later publication, return and synchronization outcomes are recorded separately.
