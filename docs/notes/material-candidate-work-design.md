# Material selection to Candidate work implementation considerations

Date: 2026-10-28
Status: historical planning considerations with current acceptance disposition; not a public API specification
Phase: [Phase 2](../phase/phase-2.md)
Execution ledger: [Phase 2 Checklist](../phase/phase-2-checklist.md)

## P2-01 contract selection — 2026-10-28

The initial alternatives below are retained as planning history. The authored
[public work contract](../spec/material-candidate-work-v1.md) now selects one
MaterialWork creation command over the existing public Candidate registration
and lookup. Its first proposal retains logical Material/resource correspondence
and the observed public metadata; no new Entity/table or source-version claim
is introduced. Explicit caller creation intent is distinct from Material identity.
Ready evidence and approval remain governed by the existing human-work contract.
The P2-01 contract was accepted at native local commit `98adb364` after selected
checks and independent review. P2-02 generated bindings and implementation
closed at `b40de62c` after compile, 48 tests across five suites including real
PostgreSQL, and independent review. P2-03 browser interaction closed at
`f0949753` after selected browser/native and dependency checks plus independent
review. P2-04 subsequently closed at `166f10c5b95a800a09f457003891ac77a5f72211`
from connected receipt 659 and independent review, including the neutral consumer,
real-Material browser, complete 1,535,406-byte equality and PostgreSQL readback.
Full receipt 672 passed 71 tests/nine suites. K03 and Workbench-owned K04 scope
are delivered; actual Studio integration/full K04 remain held. The first
whole-Phase review requested current-document reconciliation; closing review
and native closure/local release remain pending. This note summarizes accepted
evidence and supplies no additional cloud execution proof.

## Historical starting point before implementation

MaterialWork already forwards `searchMaterials`, `inspectMaterial` and
`readOriginal` to the public KnowledgeLake/Material Operations using the caller's
CNCF context. The same service also supports explicit Material registration;
registration/materialization is independent of Candidate work and is not needed
to reuse an existing Material.

Candidate already supports `registerProposal`, `findCandidateByProposal`,
`getCandidateWork`, `editCandidateWork` and `decideCandidateWork`. Registration
retains exact producer/proposal correspondence and first proposal; sequential
resend returns the current Candidate without replacing human work. Existing
browser assets edit and decide on registered Candidates, but do not offer a
Material search/select/create-Candidate flow.

The existing registration contract requires nonblank name/content and a
nonempty evidence list. Selecting a title-only Material is insufficient to
construct that proposal automatically. The operator supplies the intended
Candidate statement; a title is not silently promoted into authoritative content.

## Proposed interaction

```text
Workbench Material search
  -> metadata and selected original inspection through MaterialWork/TKL
  -> explicit Candidate creation intent plus operator-supplied content/context
  -> existing native Candidate registration and source correspondence
  -> same Candidate detail, original reference, edit and explicit human decision
```

Search and preview are read-only. Creation is a separate explicit command.
The original stays with TKL/storage; Candidate retains logical associations,
the first source observation and limitations. Large bytes are not copied into
Candidate storage. Provider paths, connection IDs and Google authentication
remain outside the work contract. The initial browser runs Workbench directly;
Studio Phase 1.2 remains on hold.

## P2-01 decisions to settle before implementation

| Question | Proposed minimal direction | Completion evidence |
| --- | --- | --- |
| Where does the bridge run? | Inspect actual generated contracts; use one work-oriented orchestration boundary plus existing Candidate registration. Add an operation only if existing public composition cannot enforce the selected-source contract. | Exact selector/input/output, kind, authorization and ABI impact recorded in a reviewed spec. |
| What is selected? | Retain exact TKL `materialRef` and selected logical `resourceId` values with their roles and available provenance. TKL owns their interpretation. | Mapping to native references and first proposal supports reopening the same original without URN parsing/provider keys. |
| Where is correspondence persisted? | Prefer existing Candidate references and originalProposal. Introduce a new typed persisted field only when this cannot represent required lookup/edit behavior. | Generated/persisted round trip, absence semantics and compatibility with existing rows demonstrated. |
| Who owns creation intent? | Bind one stable proposal key to an explicit work intent before issuing registration. Keep it across failure/readback; selecting the same Material alone is not a new intent. | Exact producer/key ownership and a durable or explicitly limited restoration path; same-intent lookup and deliberate second intent distinguished. |
| What happens after an unknown write? | Use exact proposal lookup before any deliberate resend under existing sequential semantics. Preserve the current Candidate and human edits. | Failure/lookup/missing/ambiguous states and pending UI explained; no automatic replay or invented idempotency guarantee. |
| Can a source be approved? | Preserve owner-issued version/snapshot and retrieval limitations only when actually supplied. Current Material metadata does not supply a generic version field. | Real-source inspection/Edit/Hold works; genuine ready evidence alone enables Review/Approve. A missing upstream version capability is reported separately. |

Material, registration, resource, work-intent, proposal and Candidate identities
are distinct. A native registration revision does not become a resource revision;
neither a successful read, timestamp, locally invented snapshot label nor a
content hash added for this delivery certifies a source version. Do not label
an unprocessed original as PreparedMaterial or invent a ProcessingRun.

Approval currently requires content, nonempty evidence and each required
reference to declare available plus revision or snapshot. Preserve that gate.
The proposed minimum real-source outcome is inspection, durable Candidate
editing and an attributed Hold when readiness is insufficient. Actual
Reviewing/Approved remains separately evidenced, never inferred from a preview.
Any stronger real-source approval requirement needs the producer contract and
explicitly scoped dependency work first.

## Behavioral specifications and observations

Extend the existing MaterialWork, CandidateProposal, CandidateFormation,
CandidateViews and factory specifications at their owning boundaries. Use
Given/When/Then and should matchers; property-based cases should cover opaque
identity/Unicode preservation and source/target edit invalidation. Add a focused
bridge specification if the connected behavior does not fit existing ownership.

Cover read-only search/preview, explicit native create, same-key lookup/resend,
distinct new intent, reopening retained work, actual PostgreSQL persistence,
missing/denied Material, no available resource, invalid creation input, revision
conflict and unknown acknowledgement. The browser must retain unsaved editing
and reconcile uncertainty without treating HTTP success alone as native success.

Use the ordinary generated CML/Display/Business Operation and same-origin REST
paths. Preview retains private/no-store/nosniff policy and renders only safely
supported content types; unsupported types offer an admitted access/download
outcome. Do not render arbitrary supplied HTML or promote a token-bearing URL
into a public reference. Credentials and raw private content stay out of logs.

Use a controlled public TKL fixture for repeatable failures and genuinely
versioned evidence. Separately observe one existing real Material through actual
TKL, with no remote write/materialize replay. A second neutral consumer proves
the work contract has no mandatory bibliographic or Studio vocabulary. Actual
Studio integration remains K04's separately held consumer criterion.

## Deferred scope and dependency handling

Retain completed Phase 1/1.1 and Hub acceptance. Do not redesign their approval,
admission or revision machinery. TKL processing/OpenClaw, result feedback,
existing-Candidate preparation, cross-provider federation, production deployment,
parallel registration receipts, arbitrary Information editing and App changes
remain outside this delivery.

If exact source mapping, safe preview transport or sufficient evidence requires
a missing TKL/CNCF capability, record the owning public requirement and affected
acceptance item. Continue independent work while that item is unresolved;
do not implement a TKW provider adapter, privilege substitution or shadow store.
