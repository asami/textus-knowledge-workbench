# Material-backed Candidate work contract v1

Owner: TKW Phase 2 / P2-01
Contract date: 2026-10-28
Status: contract accepted in P2-01; P2-02 source and executable specifications authored; generated bindings, validation and acceptance pending

## Owning contracts

The component remains `org.simplemodeling.textus.KnowledgeWorkbench`.
Consume [MaterialWork](material-work-reference-v1.md),
[proposal registration](candidate-proposals.md),
[human work](candidate-formation-and-review.md) and
[Workbench views](candidate-workbench-views.md). TKL owns Material/resource
identity, global metadata and physical storage. Workbench owns creation intent,
Candidate correspondence and human work. KnowledgeHub remains the canonical
Information owner. Editing Studio Phase 1.2 remains a separately held consumer.

## Public work operation

Add one product operation to service `MaterialWork`:
`createCandidateFromMaterial`, COMMAND, `authenticated_only`.
It connects a caller-authorized logical source to existing Candidate registration;
it is not a test interface, arbitrary gateway or alternative lifecycle/store.

| Input attribute | Logical type | Requirement |
| --- | --- | --- |
| `proposalId` | string | Nonblank exact caller-owned creation-intent key. |
| `materialRef` | string | Nonblank exact TKL-issued opaque reference. |
| `resourceIds` | collection of strings | Nonempty ordered distinct nonblank logical resource IDs within that Material. |
| `name` | string | Explicit operator-supplied Candidate name; existing native Name constraint applies. |
| `content` | string | Explicit nonblank Candidate statement. Material title does not silently supply content. |
| `context` | optional string | Absence and present empty text remain distinct. |

Validate the complete local input before any create, including on an existing
intent. Preserve admitted text and ordering without trimming, case folding,
URN/path construction or provider lookup. There are no owner, actor, source
revision, snapshot, grant, connection, Drive file/folder or operation-target
inputs. CML/generated readers provide structural types; handwritten admission
provides the nonempty/distinct/exact-content conditions. Generation must bind
the actual collection DTO before its implementation is claimed.

Return the existing `RegisterProposalResult`: confirmed native `candidate` and
Boolean `created`. Preserve its standard Record/JSON projection. The bridge
does not allocate Candidate IDs, manufacture revisions or report a successful
create before native registration/load acknowledgement.

## Creation intent and dispatch

The fixed proposal producer is `workbench-material`; source kind is
`tkl-material`. These identify the Workbench-origin proposal, not a TKL-issued
proposal or a physical resource. A creation intent is the exact pair
`producerRef=workbench-material` and caller-supplied `proposalId` in the native
caller's accessible Candidate collection. Neither Material URN nor resource ID
is a creation key. Different deliberate work on the same Material uses an
explicit new intent; selecting/reloading alone creates none.

The caller retains the intent before submitting a create command and keeps it
across response loss/reload. Browser persistence contains only the opaque intent
key and fixed producer, no token or original/content payload. Candidate is the
durable work store; this small caller continuation is not another Candidate
store. If the key is lost, do not claim to reconcile that unknown request or
silently generate/replay its replacement. An operator may locate existing work
or explicitly choose a new creation intent with the uncertainty visible.

After local validation, use the existing public Candidate proposal lookup with
the same caller. One accessible match returns the current Candidate with
`created=false`, without TKL reads or field writes. Existing edits, first source
observation, decisions and admission history win over changed incoming content
or selection. More than one match retains the existing ambiguity failure.

For no match, query public TKL Material metadata through the existing same-caller
MaterialWork route. Require exact returned `materialRef`, a well-formed logical
resource collection and one unambiguous matching resource for each selected ID.
Missing, unauthorized, malformed or unresolved selection creates no Candidate.
Create the proposal below and invoke existing public `Candidate/registerProposal`
with the same execution/security/observability context. Native registration
owns its final lookup/create and persistent acknowledgement.

No parallel uniqueness, cross-call transaction, exactly-once guarantee or new
reservation protocol is added. The contract preserves the existing sequential
resend behavior. Each browser submission executes once; it never automatically
retries a failed or uncertain command. Explicit reconciliation uses public
`Candidate/findCandidateByProposal` with the retained exact pair. An absent or
failed lookup is not proof that an unacknowledged write cannot exist; retain
uncertainty and require a deliberate operator choice before another create.

## Persisted proposal and source association

| Existing proposal field | Bridge value |
| --- | --- |
| `producerRef` / `proposalId` | Fixed producer and retained creation intent. |
| `sourceKind` | `tkl-material`. |
| `name`, `content`, optional `context` | Exact admitted operator values. |
| `preparationState` | `unprepared`; registration/preview does not imply preparation. |
| `evidenceRefs` | One reference with `id=materialRef`, `kind=material`, `retrievalState=unverified`, and the explicit source limitation below. |
| `preparedMaterialRefs` | Empty; raw Material is not relabeled PreparedMaterial. |
| `sourceRunRef`, `sourceContextRef` | Absent; no Run or semantic context is invented. |
| `materialSelection` in original JSON | Object containing exact `materialRef`, ordered `resourceIds` and `metadata` from the actual public logical metadata response. |

The evidence limitation is: `Material metadata does not attest original retrieval or a source revision/snapshot.`
No evidence `revision`, `snapshotRef` or `retentionUntil` is supplied by this
bridge. A native registration ID/revision or Candidate revision is not a source
resource version. A local timestamp, hash or invented label cannot replace it.

The exact first serialized proposal is retained by existing `originalProposal`.
The additive `materialSelection` object uses that contract's preserved unmodeled
provenance; no new Entity field, index table or database migration is needed.
`metadata` is an observation at creation, not a certified immutable source
snapshot. It contains only TKL's public logical projection, never provider
paths, storage configuration or credentials. Later source viewing uses the
retained exact pair of Material/resource selectors and current authorized TKL
reads; it does not treat old observed availability as a fresh successful read.

The source association remains available from first proposal after native
restart and same-intent resend, including after human content/evidence edits.
Current editable evidence and retained original association have different
roles; keeping the source history does not silently reinsert removed evidence
into the current approval target.

## Human work and failure semantics

Search, metadata, selection and original preview remain read-only. Creation
does not perform Review, Approve or Information admission. This initially
unversioned source may be inspected/edited, held or rejected through existing
work operations. Review/Approve retain the existing required-reference
availability plus revision/snapshot gate. Only actual separately supplied
ready evidence can satisfy it. Preview success does not alter those facts or
the persisted target. Effective edits still invalidate old approval.

All hops preserve the actual caller; operation admission and native Material/
Candidate authorization remain independent of component composition or cloud
credentials. Propagate the existing structured Conclusion and safe HTTP policy.
No privileged fallback, remote write, materialize replay or source-version
capability is implemented in Workbench. Existing originals and their bytes
remain TKL-owned and unchanged.

| Condition | Observable outcome |
| --- | --- |
| Valid new intent and accessible exact source | Native confirmed Candidate plus `created=true`; source association retained. |
| Valid sequential same intent | Current accessible Candidate plus `created=false`; no incoming-field overwrite or TKL access. |
| Invalid local input | Structured invalid input; no Candidate creation. |
| Missing/denied/unavailable source or invalid resource correspondence | Owning structured failure; no new Candidate and no provider fallback. |
| Native create/load or acknowledgement failure | Actual failure/uncertainty; no fabricated successful Candidate or replay. |
| Source later becomes inaccessible | Retained association/history remains; preview reports current failure without replacing displayed selection with stale success. |

P2-02 supplies generated/action/native persistence specifications; P2-03
supplies browser behavior and safe binary preview; P2-04 supplies neutral-consumer
and separate real-source evidence. Their successful execution, independent
review and Phase closure remain pending, not implied by authoring this contract.

## P2-02 implementation mapping

The CML `CreateCandidateFromMaterial` collection DTO and authored ABI bind the
MaterialWork command to `MaterialCandidateWorkActions` through the normal service
factory. `MaterialCandidateWork` admits local logical conditions and exact public
metadata/resource correspondence, then preserves the first serialized proposal.
All persistence and final identity acknowledgement stay with existing public
Candidate operations. `MaterialCandidateWorkSpec` covers public dispatch, invalid
input/source failures, intent preservation, ordered-selector properties, unknown
acknowledgement and actual PostgreSQL restart/native revision/authorization.
The test-only PostgreSQL container follows KnowledgeHub's existing fixture approach.
These are authored specifications, not execution evidence; runtime generation,
compile, focused validation and independent review remain pending.
