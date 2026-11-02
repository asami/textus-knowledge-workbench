# Workbench contribution to Studio Knowledge formation

status=planned
planned_at=2026-10-29
consumer=nict-editing-studio Phase 4
semantic_producer=nict-knowledgehub Phase 6

This is a bounded shared-work/review contribution to the
[Studio workflow](https://github.com/KnowledgeHubProject/nict-editing-studio/blob/codex/ess3-server-editing/docs/spec/information-to-knowledge-workflow.md),
not a redefinition or reopening of accepted Workbench Phases 1/1.1/2/3.
It does not allocate the proposed Workbench Phase 4 AI Candidate-drafting work
or make that proposal a prerequisite for Studio's connected route.

| ID | Delivery | Studio / Hub mapping | Status |
| --- | --- | --- | --- |
| TKW-KF01 | Inventory existing work/decision APIs; bind the semantic target and decision/result relationship with Hub, reusing native state where meaningful. | ESS4-01 / KPF-01/02 | OPEN |
| TKW-KF02 | Provide common saved work, correction, review and explicit approval actions consumed by Studio; delegate semantic processing and formation to Hub. | ESS4-02 / KPF-05/06 | OPEN |
| TKW-KF03 | Verify persisted work/decisions and source/result trace in the composed saved-data route; publish the bounded consumer handoff. | ESS4-03 / KPF-07–09 | OPEN |

- [ ] Common review is owned here; neither Studio nor Hub creates a parallel
  general-purpose review state machine.
- [ ] Already admitted Information is the input. Its prior approval does not
  authorize formation for a new semantic target; correction requires new approval.
- [ ] Semantic candidates and Knowledge belong to Hub; no shadow canonical store.
- [ ] Existing TKL original navigation and accepted source correspondence are reused.
- [ ] Restart and uncertain result handling preserve the current work/decision
  references using native infrastructure, without copied-state/hash/TTL machinery.
- [ ] Focused tests and the integrated review qualify this contribution; owning
  release checks and remaining limitations are recorded at their actual scope.

Entry records the exact owned paths/API extension, if needed. Do not silently
reinterpret existing Candidate admission operations to mean Knowledge formation.
Build this contribution alongside the connected route, not as a general work
management framework. [Hub checklist](https://github.com/KnowledgeHubProject/nict-knowledgehub/blob/main/docs/phase/phase-6-checklist.md)
and [Studio checklist](https://github.com/KnowledgeHubProject/nict-editing-studio/blob/codex/ess3-server-editing/docs/phase/phase-4-checklist.md)
own their separate acceptance.
