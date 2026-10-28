# Material evidence to human review — design notes

Date: 2026-10-28. Non-normative P3-01 exploration; no producer delivery or
acceptance is established here.

## Observed input

TKL MaterialMetadata currently contains materialRef, title, resources and
provenance. A resource contains resourceId, role, mediaType and optional
byteLength. The public projection does not carry revision, snapshot or retrieval
attestation. Workbench Phase2 therefore creates unverified Material evidence.
The current Candidate gate requires every evidence reference to declare available
retrieval and a nonblank revision or snapshot; there is no optional-reference
flag in ProposalReference. KnowledgeHub already admits an explicitly approved
exact target and confirms its persisted canonical identity.

CNCF FileStoreMetadata has optional version. GoogleDriveFileStore requests and
decodes the provider version, but FileStore explicitly describes it as an
observation and promises no version-pinned read. This is reusable producer
input, not a complete public Knowledge Lake evidence contract. Consumed artifact
availability still needs ordinary dependency compilation/validation.

## Owning delivery

TKL should resolve the actual caller-authorized logical Material/resource into
its private admitted storage mapping, observe the provider version and the
actual original read, and return an honest resource-scoped result. Observing
matching versions around a read can qualify that observation; it does not
create an immutable historical snapshot or a retention promise. Changed,
missing or unsupported versions remain explicit. Reuse CNCF's managed FileStore
stat/read, OAuth/service-account and response policy; add no TKL raw HTTP or
Workbench provider implementation.

## Public-shape alternatives to settle with the producer

1. An explicit optional evidence-resource selection on existing getMaterial,
   with an optional typed evidence result. Omitted selection must preserve the
   existing metadata-only shape, authorization and provider-independent behavior.
2. Existing readOriginal may carry sufficient logical version/read information
   if its ordinary native response contract supports a typed safe observation.
3. A distinct domain query is justified only if the owning contract cannot
   express the required observation clearly through existing Operations.

No alternative is implemented or accepted by this note. Freeze actual generated
inputs/results and correspondence in the owning TKL contract after scope
admission, then bind the Workbench consumer to that delivered version.

## Candidate work

Source evidence acquisition does not itself edit, Review or Approve a Candidate.
Any applying command uses the native expected revision and explicit operator
intent. Preserve first proposal and selected Material/resource association,
existing human content/context and unrelated references/history. Do not replace
a human-modified reference merely because it resembles an original source
reference. An effective evidence change invalidates old approval normally.

Use TKL-issued opaque evidence identity and exact source revision/snapshot;
Candidate or registration revisions and local timestamps/hashes are unsuitable.
All current evidenceRefs remain subject to the existing readiness rule. Merely
descriptive optional observations may remain provenance without a fabricated
optional-evidence bypass. Generic manually declared reference semantics remain
their existing contract; this path does not claim cryptographic attestation.

## Verification boundary

One existing real Material must reach Review, Approve and confirmed canonical
Information while preserving complete original bytes and native PostgreSQL
restart/history. Separate controlled producer and browser fixtures remain
necessary for changed/unknown/denied cases and a neutral consumer. A fixture
service caller is not real end-user OAuth. No real Drive creation/materialize
replay, provider fallback, main integration or external publication is implied.

## Selected producer direction after native scope approval

The owner selected alternative 1: optional `evidenceResourceIds` on the existing
`getMaterial`, with optional typed `observedEvidence` on Material metadata. No new
Operation is needed. The initial producer inventory was extended by native
same-purpose continuation to reuse the existing registered-mode PostgreSQL
fixture, retaining the actual Human approval. This is implementation direction;
producer compile, affected behavioral tests and independent review are pending.

The exact logical selection is authorized before credentials or original reads.
Each selected resource is read fully using the managed FileStore, with bounded
memory and matching resource/type/parent/size/version observations before and
after. Missing version remains unverified; contradictory or changed observations
fail without certifying a previous revision. The response reports limitations
without promising immutable storage, historical pinned reads or retention.

### Identity across physical rebinding

Provider revisions are file-scoped. A logical Material/resource pair alone would
conflate a newly bound physical original with the former one when both report
the same provider version. The selected producer therefore qualifies its opaque
evidence identity with the exact admitted backing incarnation, using a canonical
private-binding identity digest. This digest has identity semantics only; it is
not a content hash, source revision, snapshot or retrieval proof. No secret or
raw/base64 physical resource key is disclosed. The provider revision stays its
actual observed value, and the native evidence change invalidates approval in
the ordinary way. No mapping registry or additional persistence is introduced.
