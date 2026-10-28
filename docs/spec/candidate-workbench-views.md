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

Phase 2 adds same-origin Material search, explicit resource selection,
inspection and private original preview before creation. These queries never
create work. The create form requires explicit operator name/content and a
new intent retained before dispatch. Only opaque producer/key pairs are kept in
sessionStorage; earlier intents remain selectable after deliberately starting
new work. Reload and caller changes require explicit same-caller public proposal
lookup. Missing/failed lookup leaves the outcome unknown and never enables
automatic resend. Creation and Candidate drafts survive failed/uncertain
operations in memory; only the small intent continuation survives reload.

The confirmed create response must identify its native Candidate and exact
proposal pair before opening the existing Display workflow. Source navigation
follows the first proposal's `materialSelection`, read from native
`original_proposal`; edits to current evidence do not replace that association.
Current inspection failure clears old selection and preview. Review/Approve
options are disabled when native `review_eligible` is false and retain native
state prerequisites. Unversioned Material supports inspection, Edit and Hold;
a preview never supplies a revision or snapshot.

Original preview fetches only the exact same-origin public `MaterialWork/readOriginal`
route with the current admitted bearer token, `credentials: omit`, `cache:
no-store` and `redirect: error`. It admits PNG/JPEG/GIF/WebP or MPEG/WAV/OGG
audio with private/no-store, nosniff, attachment and sandboxed default-src-none
response headers. Bytes use revocable blob URLs in image/audio controls and a
download link; no HTML, SVG, embed, iframe or provider URL is rendered. The URL
is revoked on replacement, failed/new inspection, selection/caller change or
page exit. Unsupported media and missing/denied reads remain explicit failures.

P2-03 specifications add actual native HTTP create/lookup/source-denial/
invalid-input behavior and retain existing Display/edit/stale-revision checks.
`textus.test.browser-probe=true` enables one bounded loopback Ember case,
writing `target/candidate-material-browser-ready.json` with its URL, selectors,
procedure and completion path. The first real native create response is
deliberately replaced by HTTP 503, so browser coordination must reload and
reconcile rather than resend; the test asserts one command dispatch. The browser
then inspects/edits/Holds unversioned work, handles controlled source denial,
adds explicitly controlled ready evidence, Reviews/Approves unchanged work and
invalidates approval with an edit. Completion is explicit at
`/__fixture_complete`; the test checks actual persisted identity/history/source.
Normal full-suite execution needs no external browser. The historical Phase 1
probe has its own `textus.test.legacy-browser-probe=true` opt-in.
P2-03 was accepted at `f0949753f5b39d4a59ba6b3da905e9ce2b840cde`.
P2-04 owns existing real source evidence; the controlled PNG producer does not
establish that acceptance.

`MaterialCandidateIntegrationSpec` adds a separate opt-in actual TKL browser
scenario, enabled by `textus.test.real-material-browser=true` and explicit private
selection/read-only connection locators. Its owner-only ready file
`target/material-candidate-real-browser-ready.json` contains public selectors,
coordination instructions and a random disposable `testToken`. Enter that token
in `#token` before the first browser request. The fixture installs a test-only
AuthenticationProvider through the ordinary component/subsystem descriptor
binding, preserving the existing component bindings, configuration and security.
Native clients use IngressSecurityResolver and child-context execution; HTTP and
browser clients send the same bearer. The real opt-in case uses the existing
read-only connection's admitted service caller `editing-studio-mobile-development`;
ordinary controlled-only runs use `material-candidate-integration-user`. Both use
the standalone Fixed ingress profile. The connection ACL and Drive scope remain
unchanged. Decisions retain the actual selected fixture caller ID;
this is not evidence of real end-user OAuth authentication. The actual TKL
provider-authentication guard and package subject allowlist remain authoritative.
Anonymous and incorrect-token inspection/original/create attempts must fail and
leave the Candidate store empty before the admitted real-source reads begin.
No original response is returned to those rejected callers. The ready file never
contains a Google token, service-account key or connection contents.
The runtime owns a loopback native Workbench server and disposable PostgreSQL
Candidate store. The browser searches/inspects/previews the existing real JPEG,
explicitly creates Candidate work, edits/Holds it, reloads and follows the first
source association. Completion is explicit at `/__real_material_complete` within
twelve minutes. The fixture then verifies native identity, human actor/history,
actual revision and association across restart, plus complete original byte
equality at native Bag and native HTTP boundaries. Real evidence stays unverified
and cannot enable Review/Approve. No remote mutations or source versions are
introduced. This authored case still requires parent-owned execution and review.

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
