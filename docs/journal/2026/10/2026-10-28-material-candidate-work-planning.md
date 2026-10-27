# Material-to-Candidate work planning

Date: 2026-10-28

The user placed Editing Studio server Phase 1.2 on hold, requested alternative
development candidates, then requested a development plan. The recommended
next delivery is existing TKW K03 plus the Workbench-owned portion of K04:
use an existing Material to create and edit a Candidate with source inspection
and explicit human decisions in the Workbench browser.

The [delivery plan](../../../phase/material-candidate-work-plan.md) records four
open stages, focused checks/independent review, and final repository-full
validation. The [considerations](../../../notes/material-candidate-work-design.md)
record unresolved source-key/reference mapping and evidence readiness. They
are planning material, not an issued public API or executed acceptance result.

Accepted Phase 1 and Phase 1.1 are preserved. Native Phase 1.1 completed at
local release `43205e2db92a3fb70ea618971b5e7a6e2b82a856`; KnowledgeHub's accepted
14-file change is locally committed at
`4fad532f798925c797736a2e7c1bb8cd7ecdeb7f`. Earlier release-candidate document
snapshots and failures retain their historical meaning.

Inspection found separate Material reference forwarding and Candidate lifecycle
surfaces, with no browser bridge. TKL public metadata does not supply generic
resource revision/snapshot facts. The plan therefore retains the existing
approval gate and distinguishes real inspection/Edit/Hold from controlled
ready-evidence Review/Approve. It does not claim actual source approval.

Only planning documents were authored. No Goal or product execution was started;
no test result, new live operation or additional acceptance is recorded here.
Studio integration, main integration, push and external publication remain out
of this delivery's execution scope.

## Phase operation correction — 2026-10-28

The user pointed out that the unnumbered workstream draft did not follow Phase
operation. It had mixed the scope and execution checklist, and the subsequent
chat proposed a number without updating canonical Phase documents.

The current work is now [ordinary TKW Phase 2](../../../phase/phase-2.md),
with a separate [checklist](../../../phase/phase-2-checklist.md), stable P2-01
through P2-04 Step/item IDs, predecessor, completion gates and a
[Phase index](../../../phase/README.md). The original plan path is a link
pointer, not a second ledger. Strategy and the K03/K04 ownership table point
to this Phase. The four-stage scope, evidence-readiness limits and separate
held Studio obligations are preserved. Earlier text above records the first
draft's historical state; it is not the current Phase organization.

Only documentation was reorganized. No native Start/adopted Plan, implementation,
test result, review acceptance or commit was fabricated by assigning Phase 2.

## Native Phase 2 entry and P2-01 authoring

The user invoked sm-goal-phase 2. The Codex Goal was registered; the dedicated
branch `codex/tkw-phase2-material-work` starts at committed plan `0be13b99`.
The existing installed caller prepared the selected four-Step Plan and started
native Session `8e7f63e6e34ec5b0aefe9aff93509af9`. Ordinary Advance issued
ImplementStep P2-01. Earlier host preparation failed before a Session was
issued because unused old capability grants were retained; correcting that
configuration produced the successful normal preparation. No Session history
or storage was edited to obtain entry.

P2-01 authored [Material-backed Candidate work v1](../../../spec/material-candidate-work-v1.md):
one explicit creation command composes public Material metadata and existing
Candidate lookup/registration; source correspondence stays in first proposal.
No resource version or prepared-source facts are invented. Real inspection,
Edit/Hold and separately ready evidence retain distinct meanings. Generated
implementation, checks, independent review and Step acceptance remain pending.

## P2-01 acceptance and P2-02 handoff

Native selected checks passed and independent ReviewStep P2-01 returned accept.
The normal evidence and closure-authority preparation/StartupImport route
registered their actual current targets under the existing Phase direction.
Ordinary Advance created local Step commit
`98adb364573893ab87e3f35309b75118d35b63e9` and issued ImplementStep P2-02.
No extra Human decision, Session restart or validation replay was needed.

P2-02 implements the accepted source/intent bridge and its native persistence
specifications. Its implementation, selected checks, independent review and
Step acceptance remain separate unfinished obligations. The held Studio line
and no-main/no-push/no-external-publication constraints are unchanged.
