# Candidate formation and human decisions

Owner: Phase 1 / STEP-3. Additive to [proposal registration](candidate-proposals.md).
Material references remain TKL-owned; KnowledgeHub Admission belongs to Phase 1.1.

## Public operations

The `Candidate` service adds `getCandidateWork`, `editCandidateWork`, and
`decideCandidateWork`. Reads select a native Candidate ID. Both commands require
the native `expectedRevision` observed by the reader. CNCF detached revision carriers and
conditional Entity update own revision checking, authorization and persistence;
Workbench adds no revision allocator, lock or datastore.

The component explicitly selects CNCF's Detached representation for the existing
non-SimpleEntity Candidate collection. CNCF owns the persisted `cncf_revision`;
it is not a Candidate business attribute or an accepted command input. New native
records start under this binding. This Step performs no automatic migration or
revision assignment for records written by an earlier unmanaged deployment.

Candidates created by `registerCandidate` without content have no target and remain Draft until an
explicit edit supplies content and evidence. A decision needs that target.

`editCandidateWork` replaces content, optional context, evidence references,
grounding references and mapping references as one target. Evidence is nonempty.
Source/proposal identity, first original JSON, preparation/material/run/context
provenance remain retained. Grounding and mapping are explicit references to
existing semantic context; this Step performs no inferred KnowledgeHub operation,
remote retrieval or AI approval. Omitted context clears context; reference arrays
are complete replacement values, not patches.

Each effective edit and explicit decision retains the authenticated native actor,
execution-clock instant, required reason, decision kind and complete target.
Clients cannot select actor/time through these operations. History is retained
in the native Candidate. Generic Entity administration is not a separate
Workbench decision contract or an attestation mechanism.

## Approval target

The target contains exact content/context, ordered evidence, material, Run/source
context references, and grounding/mapping references. Supplied revisions,
snapshots, retrieval states and limitations remain unchanged. Missing facts are
unknown. No hashes, normalization or separate versioning subsystem is introduced.
The Entity revision is separate: recording Review and Approve advances native
revision without changing the reviewed target.

## Lifecycle

| Current state | Action | Result |
| --- | --- | --- |
| Draft or Held | Review, when eligible | Reviewing; retain exact reviewed target |
| Reviewing | Approve, when target is still eligible and unchanged | Approved |
| Any state | Hold / Reject | Held / Rejected with reason and target |
| Any state | Edit changed target | Draft; retain old decisions, invalidate approval |
| Held or Rejected | Explicit Edit, including same target | Draft; renewed Review required |
| Draft / Reviewing / Approved | Edit identical target | No write; retain state and revision |
| Rejected | Review or Approve directly | State conflict; first explicitly reopen by Edit |
| Any other transition | Review / Approve | State conflict; no write |

Review eligibility requires content and at least one evidence reference, every
evidence reference declaring `retrievalState=available` and a revision
or snapshot reference. This is declared reference readiness, not proof of actual
provider retrieval. Unknown, unavailable, expired or unverified evidence can be
edited, held or rejected, but cannot be approved. Optional preparation/Run facts
are not invented or required. Approval cannot come from capture confirmation.

A target mismatch with the latest decision exposes Draft and invalid approval.
An effective edit records a new event even when it restores older approved text;
reverting through these operations never resurrects an old approval. Metadata
changes outside the target do not redefine the reviewed target.

## Acceptance

- Both accepted proposal fixtures enter the same native workflow.
- Native persistent restart retains content, references and attributed history.
- Review/Approve record the same target across distinct Entity revisions.
- Changed content, context, evidence, grounding or mapping require new Review.
- Hold resumes; Reject requires explicit reopening; missing evidence blocks
  Review/Approve while allowing Hold/Reject.
- Stale native revisions, invalid inputs and denied access write nothing.
- Sequential proposal resend preserves formation/history and first proposal.
- Focused executable specifications include property-based target invalidation.
  STEP-4 views and Phase-wide full validation remain separate obligations.

## Source-managed Material evidence (Phase3 additive contract)

The explicit MaterialWork refresh command supplies TKL-observed resource evidence
under the [Material evidence contract](material-evidence-review-v1.md). It records
ordinary attributed `edit` history only for an effective evidence change, preserving
content/context and unrelated references. Its identical-observation no-op is
separate from generic Edit semantics: Held/Rejected are not reopened by a no-op
source refresh, and a fresh native revision check follows source observation.

Generic human Edit remains complete-target replacement and can reopen Held/Rejected.
If it changes/removes/reorders/duplicates the current managed evidence projection,
it atomically marks native `materialEvidence.status=detached`. Restoring earlier
references does not reattach. Unrelated edits preserve active ownership. Approval
and EVERY-evidence-reference eligibility remain unchanged; source acquisition itself
performs none of Review, Approve or Admission.
