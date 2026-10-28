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

## P2-02 acceptance and P2-03 handoff

Native generation/compile and the five selected suites passed (48 tests),
including PostgreSQL restart, authorization and conditional revision behavior.
Earlier failed attempts remain in the same Session. Test setup corrections
retained the public contract and assertions; independent review additionally
required the local helper name and target/ work-directory corrections. Fresh
checks passed and the independent affected re-review accepted the candidate.

Ordinary native closure created local Step commit
`b40de62c19a34d50a45276445e57f42576355e40` and issued ImplementStep P2-03.
P2-03 owns the served Material-to-Candidate browser flow. Real-source and
neutral-consumer acceptance, final full validation and whole-Phase review remain
open. Studio Phase 1.2 remains held; no main integration, push or publication
was performed. Detailed runtime outputs remain in private target/ evidence.

## P2-03 HTTP collection failure and owning dependency decision

The served browser searched and inspected the controlled Material, displayed its
original, retained creation intent and draft after one create response, and
performed an explicit lookup after reload without resubmission. Lookup returned
no Candidate. Native focused-browser evidence retained two failures in 18 tests:
the actual HTTP create returned 500, and the browser collection remained empty.
The fixture had replaced any first response with 503; its authored correction
now masks only successful native responses and preserves actual failures. It
remains unvalidated. The failed run remains failed.

Source tracing found that common Ingress.recordToParameters expands the array
into individual Properties, while Request.toRecord preserves one Property as a
scalar. Workbench retains strict collection admission rather than coercing a
scalar or reading the raw HTTP body as a workaround. Additional public diagnostic
delivery returned unavailable because Common Evidence was disabled; it supplies
no new diagnostic Evidence or successful validation.

The same Goal/Session accepted a dependency-scope proposal for simplemodeling-lib
and issued a genuine Human decision (ApproveDependencyScope, RejectDependencyScope,
Defer or Cancel). No library source or dependency artifact was changed. P2-01/02
commits and all prior checks/reviews/counters remain retained; P2-03, P2-04 and
Phase full/review/release stay open. The bounded test server finished under the
native test; its browser tab was closed. Detailed outputs stay in private target/.

## Approved common-protocol dependency repair

The user explicitly selected ApproveDependencyScope. Normal public Human
preparation and Submit/Advance admitted that answer without replacing the
Session. A same-purpose inventory update added the existing semantic resolver
and specification. Its first Advance failed before inventory registration
because the caller had not configured the concrete dependency Binding; adding
the dedicated root/branch/base/check binding allowed ordinary Advance to admit
receipt `8e7f63e6e34ec5b0aefe9aff93509af9:ordered-dependency-inventory:315@1`.
No accepted Submit was replayed and the repair count remains 1/10.

The dedicated simplemodeling-lib branch `codex/tkw-phase2-http-collections` starts
at `bd552c6f725dfb7dd5da1059b7797b1e671b6e30`. Six source/spec/journal files
are authored to preserve declared collection carriers and normal element
validation. Native dependency capture, prerequisite local publication, fresh
Workbench/browser and dependency focused checks, and independent review remain
pending. The existing input constraints, original-byte policy and final Phase
full/review/release obligations are unchanged.

## P2-03 acceptance and P2-04 continuation

Source revision `484` passed primary compile and 18 tests across two suites,
including the coordinated served browser; dependency compile and 59 tests across
five suites also passed. Independent reviewer `/root/tkw_p2_p202_review` accepted
both exact trees, resolving issue-3/4. Native local commit
`f0949753f5b39d4a59ba6b3da905e9ce2b840cde` closed P2-03; ordinary Advance
issued ImplementStep P2-04. All earlier failures and accepted history remain.

The browser proved one native create with a masked successful response, explicit
retained-intent reconciliation, same-ID edit/Hold, source-denial clearing,
controlled ready-evidence Review/Approve, unchanged-target approval retention and
edit-driven invalidation. Source navigation preserved unsaved edits and first
Material association. Creation text was deliberately cleared before actual reload
because the in-app browser canceled beforeunload; no discard-dialog acceptance
is claimed. These fixture results do not establish P2-04 real-cloud acceptance.

Two dependency Test compilation failures were corrected without relaxing data or
expectations: JSON quote syntax and the existing `RecordDecoder().json` API.
A primary-only diagnostic Manifest rejected dependency paths; dependency diagnosis
was preserved separately. The first review Submit omitted prepared execution
evidence and failed before acceptance; supplying the actual reviewer identity
corrected caller input without replaying successful checks or review.

The existing real Material package and comparison originals were located for
P2-04. Private connection/path selections remain task-local. Live source access
will be read-only, without new Drive objects or materialize replay. Final full
validation, independent whole-Phase review and local release remain required.
Studio Phase1.2 remains held; no main integration, push or external publication.

## P2-04 first connected check and authenticated fixture repair

Source revision `527` passed compile. The selected connected check ran 19 tests
in three suites: 18 passed, including neutral-consumer PostgreSQL restart; the
actual-TKL case failed at its first metadata inspection because the fixture's
standalone local subject was not provider-authenticated. No original was fetched
and the real browser was not started. The original failure remains issue-5;
the native repair counter is 1 of 10.

The parent saved the pre-edit authenticated-fixture diagnosis, bound to the
retained stdout lines 27–40. The limited repair uses an ordinary test-only
authentication provider and ingress resolution for direct, HTTP and browser
calls. TKL's provider-authentication guard and package subject allowlist stay
required. A disposable loopback token is separate from the existing service
account credential. Native revalidation and independent review remain pending.

Source revision `560` confirmed the auth repair and retained 18 passing tests,
but actual TKL inspection then reported an unavailable Material catalog. The
parent's initial configuration example used qualified global keys where the
component requires its declared initialization parameters. The limited fixture
repair puts unchanged values in the KnowledgeLake component binding and resolves
the actual declaration object for a package-mode assertion. Source repair only;
revalidation and independent review remain required. The original failed result
and counter 2 of 10 remain preserved.

Source revision `593` resolved package configuration, then the existing readonly
connection rejected the new fixture caller. Sanitized connection metadata
confirmed only `editing-studio-mobile-development` is admitted with Drive readonly
scope. The opt-in real case now selects that existing caller instead of expanding
ACLs; ordinary controlled-only runs retain their neutral fixture caller. Native
verification and review remain pending, with counter 3 of 10 and all failures
preserved. Connection, credentials, scope and remote data remain unchanged.

Source revision `626` confirmed caller authorization and reached outbound HTTP,
which correctly rejected the unconfigured fixture host. The real opt-in Cwd now
supplies the accepted bounded HTTP plan through its normal private config file:
KnowledgeLake only, two exact Google origins, GET/POST, 10s connect/20s read and
8MiB response. Methods are Cartesian, not origin-specific; the unchanged Drive
readonly scope is still required. No PUT or default-test outbound grant is added.
The original policy rejection remains; repair count 4 of 10 is retained pending
native validation and independent review.


## P2-04 connected acceptance and final full validation

After the four retained fixture-precondition failures above, connected receipt
`8e7f63e6e34ec5b0aefe9aff93509af9:ordered-validation:659:validation@1`
passed compile and all 19 tests across three suites. The neutral technical
observation consumer demonstrated the public contract without mandatory
Book/ISBN/Studio/provider fields and recovered current human work through
PostgreSQL restart and exact public intent lookup without a producer.

The separate actual TKL/browser case used existing Material `437e5b…` and its
first selected resource `5a8453…`; these abbreviated identities describe the
retained private selection and are not replacement selectors. The browser
previewed the actual 3000 × 4000 JPEG and issued exactly one explicit Candidate
creation. The confirmed same Candidate advanced from revision 1 through Edit 2
to Hold 3 with present-empty context; reload/source preview and PostgreSQL
restart retained complete native work, revision, first source association and
exact-intent lookup. Complete 1,535,406-byte equality with the captured local
original was observed through native Bag and HTTP before and after work; the
public original was read again after restart.

The real fixture used the already admitted `editing-studio-mobile-development`
provider-authenticated service caller with a disposable loopback token. This
attributes the observed decisions to the fixture caller, not real end-user OAuth.
The neutral consumer retains a separate fixture caller. Real evidence remained
unverified, without revision/snapshot; Review/Approve were unavailable. Controlled
ready-evidence approval/invalidation and uncertain-write exact lookup with one
create/no replay remain separate P2-03 receipt 484 evidence.

Independent Step review accepted the connected result. Native local commit
`166f10c5b95a800a09f457003891ac77a5f72211` closed P2-04, following P2-01
`98adb364573893ab87e3f35309b75118d35b63e9`, P2-02
`b40de62c19a34d50a45276445e57f42576355e40` and P2-03
`f0949753f5b39d4a59ba6b3da905e9ce2b840cde`. The admitted core dependency's
six-path correction at `f4ad94ab…`, its compile/59 tests in five suites at 484
and independent review remain retained on its unchanged dedicated branch.
Earlier HTTP collection, two dependency test-compilation and four connected
fixture failures, all accepted history and repair counters remain preserved.

Final full receipt
`8e7f63e6e34ec5b0aefe9aff93509af9:ordered-validation:672:validation@1`
passed all 71 tests in nine suites, including actual PostgreSQL 17.11, against
the accepted P2-04 product candidate. The normal full run excludes opt-in
cloud/browser execution; actual connected receipt 659 and controlled browser
receipt 484 remain separate. No Drive writes, materialize replay, main
integration, push or external publication occurred.

## First whole-Phase review and current-document repair

The first independent whole-Phase review confirmed the integrated behavior,
all four accepted Steps and matching final full validation, then returned REPAIR
for stale current ledger/handoff text. Native issue-6 is the REVIEW finding;
the phase-level release repair contract retains counter 1 of 10. This issued
documentation repair reconciles the twelve owned current-status paths with
P2-04 acceptance and full receipt 672, marking the demonstrated P2-04 items and
P2-C01. The original K03/K04 OPEN table remains a historical planning snapshot;
current disposition records K03 delivered through P2-01–03 and Workbench-owned
neutral-consumer/failure K04 delivered through P2-04. Actual Studio Candidate
integration/full K04 remain held with Studio Phase 1.2.

This is source documentation repair only. Independent closing review, the
native closure record and distinct local FinalRelease remain pending through
the owning workflow. No Phase completion, unexecuted release commit or release
hash is claimed. Earlier failed-attempt paragraphs remain historical observations.
