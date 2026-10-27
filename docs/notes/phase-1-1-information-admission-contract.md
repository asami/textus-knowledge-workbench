# Phase 1.1 — Information Admission contract preparation

Status: original proposal retained; owning delivery recorded below. Updated: 2026-10-28.
Owner: TKW Phase 1.1, with KnowledgeHub owning canonical Information.

## Confirmed starting point

Phase 1 closed at `f96057bd85f17b5647c520e2f00e690e14c63363`.
Its two producer fixtures, durable Candidate correspondence, exact approval
target, attributed human decisions and native revision checks are accepted.
Phase 1.1 must consume those contracts without reopening that work.

The inspected KnowledgeHub checkout is
`/Users/asami/src/Project2026/nict-knowledgehub`, clean at
`91fd0fa4a3290b0a98c8a16aa353c93cad770712`. The same latest commit and
unchanged public CML/operation specification were also confirmed through the
connected GitHub source on 2026-10-28. Its public CML and
`docs/spec/knowledgehub-operations.md` declare a `Knowledge` service with
`ingestKnowledge`, `searchKnowledge` and `evaluateKnowledge` over `KnowledgeItem`.
Ingestion uses the standard entity-create implementation. This is an existing
public surface, not evidence that every runtime behavior is absent.

However, that contract does not identify `KnowledgeItem` as canonical
Information in the Material -> Information Candidate -> Information -> Knowledge
lifecycle. It does not define submission of an exact approved Candidate target,
an Admission outcome/identity lookup, or the Candidate/Evidence trace needed by
Phase 1.1. Existing factory specifications verify the generated interface and
input handling; they do not prove the required canonical Admission route.
The source ABI has the same three Knowledge operations and no Information
Admission operation or types. Treating their names as interchangeable would
silently choose an unresolved domain contract.

## Required owning contract

Before TKW implements the affected boundary, KnowledgeHub must determine and
deliver its smallest public Information Admission capability:

- identify the actual canonical Information representation and its stable native
  identity; explicitly settle coexistence with the existing KnowledgeItem API;
- accept the selected approved content/context and source Candidate identity,
  reviewed Evidence and optional available preparation/Run provenance;
- return an observed confirmed canonical outcome and permit lookup of that
  same outcome, without guessing success from a request being sent;
- define rejection, failure and uncertain-outcome handling, including whether
  and how the same submission may be looked up or retried;
- retain trace from the real Information to its Candidate and exact reviewed
  target, without duplicating raw Material or provider storage;
- use CNCF's native managed persistence/revision/authorization contracts and
  PostgreSQL-first RDB policy, without an application shadow store or general
  receipt/recovery framework.

Potential operation names such as `admitInformation` and `getAdmissionOutcome`
describe needed behavior only. They are not a frozen API selection. The
KnowledgeHub owner must decide whether a documented compatible extension or a
separate Information surface is appropriate, preserve the current three named
operations, and provide executable behavior evidence.

## TKW integration after owner delivery

TKW remains responsible for eligibility and its Candidate lifecycle. It submits
only the exact current human-approved target, uses ordinary CNCF component
Operations with the caller context, and records `AdmissionRequested`/`Admitted`
according to the owning confirmed outcome contract. An Entity revision advanced
by a decision does not redefine the approved target. Changed targets require
renewed human review; unprepared input must not invent PreparedMaterial or Run.

Both accepted fixtures must reach the actual KnowledgeHub component, persist
canonical Information, and expose the resulting identity/trace through existing
Workbench views. A simulated provider can assist development but cannot close
the Phase. Current source/proposal lookup, normal restart and human edit
preservation remain regression obligations.

The dependency proposal concerns KnowledgeHub's Component and this Admission
purpose. Its file list is an implementation inventory, not an independent grant.
The runtime must admit any scope and concrete execution binding before upstream
edits, validation or necessary local publication. Main integration, push and
external publication remain excluded. This paragraph records the initial request,
before the separately admitted delivery below.

## Current delivery state

The actual dependency Human answer `ApproveDependencyScope` was admitted by
native Session `dc5aec0f04843c377ad539f04783861f`; `P11-01` was reissued with
its retained decision context. The authorized KnowledgeHub branch is
`codex/tkw-p11-information-admission`. Its owning public contract revision 1
chooses a separate Information Entity and `Information.admitInformation` /
`getInformation`, retaining the existing KnowledgeItem service. TKW's consuming
contract is [Candidate Information Admission](../spec/candidate-information-admission.md).

Producer repair closed generated mutation-route bypass CB-KH-01. Fresh focused
verification passed 19 tests, including real PostgreSQL, and independent re-review
passed. Native LocalPublicationService request002 published the CAR to the
task-private warehouse. The earlier request001 catalog failure remains failed.
TKW consumption and integration checks, native acceptance and final full validation
remain outstanding; publication is not Phase acceptance. No main integration,
push or external publication is authorized or performed.
