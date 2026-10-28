# Workbench Phase index

Updated: 2026-10-28

This directory records engineering scope, checklist progress and handoffs.
Specs own public behavior, notes hold provisional design, and journals retain
chronological decisions. Phase planning does not start execution.

| Phase | Scope | Current disposition | Ledger |
| --- | --- | --- | --- |
| [Phase 1](phase-1.md) | Candidate formation, human decisions and views. | Closed at local release `f96057bd85f17b5647c520e2f00e690e14c63363`. | [Checklist](phase-1-checklist.md) |
| [Phase 1.1](phase-1.1.md) | Real KnowledgeHub Information admission and trace. | Native Session `dc5aec0f04843c377ad539f04783861f` completed at local release `43205e2db92a3fb70ea618971b5e7a6e2b82a856`; earlier source snapshot retains its historical release-candidate status. | [Checklist snapshot](phase-1.1-checklist.md) |
| [Phase 2](phase-2.md) | Existing Material to persisted Candidate work in Workbench. | Native Phase completion at release `7c320293`; same-Session completion records committed at `a530dd82`. Earlier authored ledgers remain historical snapshots; current completion is retained in the native result and appended record. | [Checklist and completion record](phase-2-checklist.md) |
| [Phase 3](phase-3.md) | Versioned real Material evidence through explicit Review/Approve to canonical Information. | Native Session `963ef583fb3d4acdc16123c1d3032d13` started; P3-01 contract/dependency-scope work is in progress and unaccepted. No Phase 2.1 is defined. | [Checklist](phase-3-checklist.md) |

Phase 2 follows accepted TKW Phase 1/1.1 and owns the TKW-K03/neutral-consumer
portion of K04 described in the [integration plan](knowledge-editing-integration-plan.md).
Editing Studio server Phase 1.2 is a separate, held consumer line. Its hold
does not reopen completed TKW Phases or close K04's Studio integration criterion.

[Strategy](../strategy/knowledge-application-integration.md) owns cross-project
coordination. This index is navigation, not a substitute for native execution
results or the applicable acceptance checklist.

<!-- sm-workflow completion 8e7f63e6e34ec5b0aefe9aff93509af9 phase phase-2 -->
Accepted phase phase-2.
Evidence: 8e7f63e6e34ec5b0aefe9aff93509af9:ordered-local-completion@1
Evidence: file:///Users/asami/src/Project2026/nict-editing-studio/.codex-workflow/checkouts/tkw-step3/target/sm-workflow/tasks/tkw-phase2-20261028/phase-review2-result.json
Local record/commit closure is runtime-owned; later publication, return and synchronization outcomes are recorded separately.
