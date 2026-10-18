# Workbench development plan — business flow first

- Date: 2026-10-02
- Status: Adopted planning direction; implementation pending
- Owner: Textus Knowledge Workbench
- Plan: [Phase 1](../../../phase/phase-1.md)
- Input: [TKL requirements proposal](2026-10-02-tkl-workbench-requirements.md)

The user identified a risk of excessive initial quality requirements, including
hash-based mechanisms, in the proposed receipt/concurrency/recovery work. The
user approved a smaller initial scope and requested reflection in the plan.

Prioritize Candidate registration, evidence-aware formation, human review and
approval. Retain durable source/proposal-to-Candidate correspondence and never
apply a resend over human edits. Reuse CNCF revision/concurrency facilities and
record the approved content/evidence without introducing a separate framework.

Assess whether Candidate metadata can represent the initial receipt mapping.
An independent receipt Entity/state machine, content hashing/canonicalization,
concurrent receipt proof, changed-payload conflict detection and comprehensive
failure/recovery machinery are not first-slice prerequisites. Select guarantees
and focused checks for the execution/storage model actually adopted; deferred
requirements remain open and are not reported as satisfied.

The incoming TKL journal remains unchanged as the requesting application's
proposal. Its full initial acceptance scope is not automatically adopted by
TKW. Shared schema, transport and guarantee commitments remain joint-contract
work. Real KnowledgeHub Admission is still required for Phase 1 completion;
feedback and existing-Candidate preparation follow later.

The Phase document owns detailed development order and requirement disposition;
the integration strategy links this independent Workbench track. This update
records planning only, with no product implementation or acceptance claim.

## Review follow-up — 2026-10-02

The Phase plan now assigns three clarifications to their existing work items:
minimal fixtures with common required and source-specific optional fields in
item 2; Hold/Reject, evidence readiness and approval-invalidation transitions
in item 3; and available View/Display contracts and upstream dependencies in
item 4. These clarify implementation decisions without expanding the first
slice. Real Admission remains item 5, and the deferred guarantees above remain
deferred.

Cross-repository checkout directories vary by operating environment. The
review observation that treated `dev2025` references as obsolete solely because
this checkout uses `dev2026` was withdrawn. Workspace instructions now require
repository/document lookup before diagnosing a stale link; no shared links
were rewritten to fit this machine.

## Adopted TKL alignment refinements — 2026-10-02

The user requested adoption of the necessary parts of the
[TKL alignment review](2026-10-02-phase-1-tkl-alignment-review.md). Architecture,
Phase 1 and CML scaffold guidance now distinguish producer proposals from
TKW-owned Candidates, CandidateSource origin/correspondence from RawDataIndex
supporting references, and external TKL processing facts from TKW human work.
The 2026-09-25 source-index journal retains its original text with a current
clarification appended.

The review's DTOs are conceptual examples, not mandatory schemas. Separate
source/index responsibilities do not require separate Aggregates/tables. Full
preparation request/result schemas remain follow-on; source-specific prepared
material/Run references are optional for unprepared capture. Existing work
items retain fixture, transition and available View/Display decisions, with
Admission failure handling assigned to item 5. Content hashing and advanced
receipt/recovery guarantees remain deferred. This is documentation/scaffold
guidance only, with no executable CML, runtime or acceptance claim.

## Phase split — 2026-10-02

The user explicitly invoked `cncf-split-phase Phase 1` after a time-bound
pre-entry `SPLIT_REQUIRED` result. Apply mode retains [Phase 1](../../../phase/phase-1.md)
for former items 1-4 and creates [Phase 1.1](../../../phase/phase-1.1.md) for
former item 5. The original end-to-end criterion, real canonical KnowledgeHub
Information with Candidate/Evidence trace, remains required for Phase 1.1 and
the sequence. Earlier statements above and in the alignment review describing
that criterion as unsplit Phase 1 completion are historical pre-split wording.

The prior proposal's provisional 1.1/1.2 labels are replaced by the skill's
numbering rule, **1 -> 1.1**. No completed implementation/test/release history
is moved; both remain planned. Former item 6, feedback and correlated existing-
Candidate preparation, remains future work outside this sequence. Existing
Hygiene/Development Candidate records keep their canonical locations/IDs.

Each child expects 6h (range 5-8h, low confidence, no comparable timing data),
with `gpt-6.1-sol / high`, standard parent mode. The only split reason is
`time-bound`; there is no asserted semantic boundary or model-profile switch.
Details, cost-role/ownership and pre-split evidence are retained in the
[source split note](../../../phase/phase-1.md#applied-split--2026-10-02).

Validation policy is `final-only / sbt-full-suite`, final owner Phase 1.1.
Because no validation entry existed, Phase 1 explicitly creates root `build.sbt`
and must pass its complete suite at bootstrap closure. Both keep focused
validation, independent review, Step acceptance, closure ledger and release
commit. The split is planning only: no product goal, child execution, runtime
test or commit is created. The validated uncommitted split can be adopted by
the next explicit Phase 1 execution task without a separate planning commit.

Reference classification: the original TKL request and the alignment review
describe the pre-split end-to-end plan and remain input/history. The unchanged
CML explanatory scaffold also describes that whole flow; its old Phase 1 label
is pre-split guidance, not current acceptance authority. Executable CML work
must use the current owning Phase documents. Unrelated Editing Studio/CNCF/Cozy
Phase references and environment-dependent checkout links keep their owners.
