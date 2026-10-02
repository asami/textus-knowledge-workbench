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
