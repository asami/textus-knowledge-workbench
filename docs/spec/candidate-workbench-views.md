# Candidate Workbench views

Owner: Phase 1 / STEP-4. Extends the accepted formation contract and preserves
both proposal fixtures and STEP-1/2/3 acceptance.

## Available upstream contract

CNCF supplies contextual `Browser` / `ViewCollection`, generated logical UI
definitions, `DisplayProjection`, `DisplayRuntime`, ordinary `display.query`,
`display.read` and `display.describeBusiness`. These are available native
contracts, not inferred future capabilities. TKW binds its existing Candidate
collection through the standard authorized Detached Entity read/search programs.
There is no new Entity, datastore, revision allocator or provider adapter.

The List screen `candidate-list` selects `candidate-detail` using the native
Entity identity and revision. Detail shows content/context, preparation/source,
required evidence and supplied retrieval/version/snapshot/retention limitations,
prepared Material and Run/context references, grounding/mapping, current human
state, decision history and the exact approval target. Missing references remain
missing. Display readiness means declared evidence readiness, not verified access
to TKL or a provider. Browser reads never resolve Drive or mutate Candidate work.

`resourceType=information-candidate` selects these two generated screens. Display
Business metadata maps Edit and Human decision to the existing STEP-3
`Candidate.editCandidateWork` / `Candidate.decideCandidateWork` Operations.
Those Operations retain authentication, native conditional update and history
rules. A generic Display CRUD update must not bypass their lifecycle.

## Human interaction

The same-origin Workbench web application renders the standard Display payload.
It selects the standard `.json` REST representation explicitly for all reads and
business commands rather than assuming a response format from HTTP success.
Selection explicitly loads Detail and its editable Candidate work at matching
identity/revision. An operator edits complete content/context and reference
arrays, supplies a reason and explicitly submits. Review, Approve, Hold and
Reject require a separate explicit submission. Capture confirmation and source
preparation never perform approval. References are shown as structured entries;
opaque IDs and unavailable facts are not guessed or promoted.
The context presence control distinguishes absent context from present empty
text. The narrow Candidate Record response adapter consumes native snake_case
fields; the separate closed Display JSON protocol is not recased.

Each mutation uses the last observed revision, executes once and has no automatic
retry. After a confirmed response the client explicitly queries selected Detail
and the List again. Failure of either reload preserves the acknowledgement and
blocks further writes until explicit reconciliation. Failed or unknown mutation
responses also block further writes until an explicit reconciliation read. A later
read failure cannot erase the acknowledged mutation or trigger its replay.
Switching Candidate or reloading with an edited draft requires confirmation.

The client uses text nodes for supplied content, same-origin routes, no credential
storage or logging, and optional in-memory bearer authentication. Server policy
remains authoritative; display availability never grants permission. Read errors
leave no stale Candidate visible as a newly selected successful result.

## Acceptance and limits

Both TKL prepared and Studio unprepared fixtures follow the same native List,
Detail, edit and explicit decision lifecycle. Verify unavailable evidence remains
visible, target edits invalidate approval, revision conflicts preserve data,
and normal restart preserves the displayed work. Native transport checks cover
the registered Display Operations and static application route. Browser checks
exercise actual served assets and the human workflow without external providers.

STEP-4 focused validation and independent review do not close Phase 1. The final
Phase review, bootstrap full suite and release remain separate gates. Actual
KnowledgeHub Admission belongs to Phase 1.1. Historical unmanaged databases
still require an explicit migration decision; no automatic migration is added.
