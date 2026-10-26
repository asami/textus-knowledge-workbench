# Shared editing applications and TKL integration plan

status=planned
planned_at=2026-10-21
spec=../spec/knowledge-editing-component-boundary.md

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

Producer: [TKL plan](../../../textus-knowledge-lake/docs/phase/material-access-and-processing-plan.md).
Consumer: [Studio Phase 3](../../../../Project2026/nict-editing-studio/docs/phase/phase-3.md).
No implementation, acceptance, Goal restart or publication is performed by this plan.
