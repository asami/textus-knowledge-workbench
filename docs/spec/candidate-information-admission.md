# Candidate Information Admission

Owner: TKW Phase 1.1. Public contract revision 1, 2026-10-28.

## Owning boundaries

KnowledgeHub's `Information.admitInformation` and `Information.getInformation`
own canonical Information. Its immutable Information is distinct from the legacy
KnowledgeItem evaluation model. Workbench owns Candidate eligibility and observed
Admission history, never a canonical Information substitute. Both Phase 1 producer
fixtures use this same route. The adapter exchanges the owning Operation's native
Record contract. It does not depend on KnowledgeHub implementation classes or a
second Maven publication. The integration fixture loads the actual published CAR
with CNCF's normal component-file repository. No Drive access or provider/AI
approval is involved.

## Submission and confirmation

`Candidate.admitCandidateWork` accepts `AdmitCandidateWork { id,
expectedRevision }`. It requires the native authenticated actor, the observed
Candidate revision, an eligible exact current target and its latest explicit human
Approve decision. Native Entity conditional update first retains an Admission
record with a framework-generated Information ID, approved Candidate revision,
complete original approval, requester/time and `Requested` status. Only successful
native persistence permits one child invocation of the Hub command. The original
approval actor/time/reason and exact target, including all optional provenance,
are forwarded unchanged. Recording Admission does not redefine the approved target.

Confirmed Admission requires the Hub's returned persisted Information, positive
native revision and exact ID, Candidate ID/revision, approval and complete target
correspondence. Only this observation permits `Confirmed` and `Admitted` display.
An invocation failure retains `Unconfirmed` and returns its original structured
Conclusion through CNCF; an incompatible response is a conflict, never success.
Failure while recording observation cannot erase the prior reserved identity or
authorize another submission. The native persistence behavior is verified through
interruption/failure executable specifications, not inferred from an HTTP response.

`Candidate.confirmCandidateAdmission` accepts `ConfirmCandidateAdmission` with the
same `id` and `expectedRevision` fields, uses only
`Information.getInformation` for the selected saved ID, and conditionally retains
a confirmed matching outcome. It never calls Admission or allocates an ID.
Missing/failed lookup is not proof that the original write never committed.
An existing Admission for the same approval cannot be submitted again, including
after restart or an uncertain acknowledgement. A confirmed same-approval result
may be read without another write. No automatic retry or alternate-ID workaround
is provided. This is Candidate business state in the existing native Entity,
not an application receipt store, lock or general recovery controller.

## Lifecycle and trace

Human approval validity remains determined by Phase 1's exact target and decision
history. Admission adds `AdmissionRequested`, `AdmissionUnconfirmed`, and `Admitted`
presentation states only for the current approval. An edit/hold/reject invalidates
the current eligibility while preserving prior Admission and its original target.
Confirmation of a retained historical Admission may preserve its fact but never
grants approval to a changed target. Current view fields expose every retained ID,
approved revision, attributed decision, outcome and confirmed Information revision.
The browser uses native Display business operations and never infers success or
automatically resends after uncertain transport. Existing source/proposal resend,
native authorization and expected-revision behavior remain unchanged.

## Acceptance

- Both retained prepared/unprepared fixtures reach the real KnowledgeHub component.
- Admission preserves exact approved content/context and Evidence/available provenance.
- Ineligible, stale, denied and changed targets cannot create canonical Information.
- Reservation precedes the owning write; failures/reopen/confirmation retain its ID.
- Duplicate/uncertain submission is not replayed; mismatching lookup is not accepted.
- Existing Candidate views show confirmed canonical identity and historical trace.
- Focused executable behavior, independent reviews and final aggregate Phase 1/1.1
  full validation remain required; implementation alone is not acceptance.

## Material resource correspondence (Phase3)

The Admission target carries the exact approved TKL-issued `material-resource`
evidence references, their observed revisions, retrieval states and limitations.
The existing `candidateId` and approved `candidateRevision` link it to native
Candidate history. The additional Entity `materialEvidence` binding and ordered
resource/reference application history stay in that native Candidate; Admission
does not copy them into Hub and does not promise a self-contained Hub mapping.
No Hub/admission schema change is required. P3-04 verifies the joint native/Hub
trace. Source refresh invalidates approval and requires separate renewed Review
and Approve before a changed target can be submitted. Existing uncertain outcome
reconciliation and no-replay rules remain unchanged.
