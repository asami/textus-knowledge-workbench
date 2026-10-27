# Phase 1 STEP-4 — Candidate Workbench views

Status: accepted
Local acceptance binding: `tkw-step4-acceptance-20261027-01@1`

The [view contract](../../../spec/candidate-workbench-views.md) binds generated
logical UI to the native contextual View Browser and standard Display service.
The Workbench entry is `/web/knowledge-workbench/textus-knowledge-workbench`.
Its List and Detail expose both accepted source inputs, human state, exact
decision targets and supplied evidence limitations.

The Browser reads the existing Candidate collection through the current execution
context's native Entity registration and public authorized Detached programs.
No additional Entity, datastore, revision allocator or provider implementation
was introduced. Static logical UI permits one noncontent status role per field
scope; preparation is a content field while human state is the status.

Human actions invoke the existing STEP-3 edit/decision Operations. The client
explicitly selects the native JSON representation and adapts only the standard
Candidate Record's snake_case fields; the separate Display wire protocol remains
unchanged. Context presence distinguishes absent from present-empty text.
Reference fields and provenance are preserved rather than guessed or inferred.

After one acknowledged mutation the client explicitly reloads both selected
Detail and List. A failure preserves acknowledgement and blocks further writes
until reconciliation; no mutation is replayed automatically. Display availability
does not replace server authorization or native revision preconditions. Tokens
remain in page memory, and supplied content is rendered as text.

STEP-1/2/3 acceptance is retained. STEP-4 is accepted at Step scope; comprehensive
Phase review, bootstrap full validation and release remain pending. Actual
KnowledgeHub Admission remains Phase 1.1 work. No Drive/provider resolution or
historical unmanaged database migration is claimed.

Detailed failure, validation, browser and independent-review evidence remains
in ignored local storage at
`/Users/asami/src/Project2026/nict-editing-studio/.codex-workflow/tkw-step4-20261027/`.
