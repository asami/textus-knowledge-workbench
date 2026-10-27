# Phase 1 STEP-3 — Candidate formation and human decisions

Status: accepted
Local acceptance binding: `tkw-step3-acceptance-20261027-01@1`

The [formation contract](../../../spec/candidate-formation-and-review.md) adds
Candidate work retrieval, complete target editing and explicit Review, Approve,
Hold and Reject. Grounding and mapping remain explicit references; they do not
invent KnowledgeHub operations or provider retrieval results.

An approval selects exact content, context and evidence/provenance associations.
Native Entity revision advances for each decision independently of that target.
Edits retain previous decisions and require renewed Review, including when old
approved content is restored. Sequential proposal resend preserves current work
and the original proposal.

The existing non-SimpleEntity collection explicitly uses CNCF's Detached revision
binding and its public conditional access/mutation helpers. CNCF owns revision
assignment, authorization and persistence. Workbench does not allocate revisions,
introduce locks or keep a shadow datastore. Authenticated execution context supplies
decision actor and time; clients cannot choose them through these operations.

No automatic migration or revision assignment for historical unmanaged databases
is included. A deployment with those records needs an explicit migration decision.
Declared evidence availability is not a verified provider result or an approval
attestation against generic Entity administration.

STEP-1/2 history is retained. STEP-4 owns human-facing List/Detail/edit/review
views; Phase 1.1 owns actual KnowledgeHub Information Admission. Comprehensive
Phase review, final full validation and release remain separate pending gates.

Detailed validation, failure and independent-review evidence stays in ignored local
storage at `/Users/asami/src/Project2026/nict-editing-studio/.codex-workflow/tkw-step3-20261027/`.
Retain it while downstream acceptance consumes it; this journal records the
accepted design and scope rather than execution payloads.
