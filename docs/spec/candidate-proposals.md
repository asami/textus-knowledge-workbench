# Workbench candidate proposals

Spec: `workbench-candidate-proposals`\
Rules: `PROP-01` through `PROP-09`\
Owner: PHASE-1 / STEP-2 / S2-A\
Status: STEP-2 / S2-A accepted under local binding `tkw-step2-connected-acceptance-commit-20261019-01@1`

This specification implements the parent-decided minimal proposal contract
`tkw-phase1-step2-proposal-contract-20261003@1`. It consumes the accepted
[component foundation](component-foundation.md), commit
`70620d1aeaae4a3859df93d6ff6948bdcff5742c`, and its native fixed-user Subsystem,
Entity/UnitOfWork authorization and persistent SQLite application datastore.
Source fixtures establish a local mapping contract; live TKL transport
compatibility is not an acceptance claim.

## Public operations

| Candidate operation | Kind | Input value | Output value |
| --- | --- | --- | --- |
| `registerProposal` | COMMAND | `RegisterProposal`: required string `proposalJson` | `RegisterProposalResult`: required `InformationCandidate` `candidate`, required Boolean `created` |
| `findCandidateByProposal` | QUERY | `FindCandidateByProposal`: required strings `producerRef`, `proposalId` | `FindCandidateByProposalResult`: optional `InformationCandidate` `candidate` |

`proposalJson` is one ordinary JSON object decoded by the existing
`org.goldenport.record.io.RecordDecoder`. Its exact first admitted string is
retained as `originalProposal`, including whitespace and unmodeled provenance.
There is no payload canonicalization, comparison or conflict response.

Required nonblank strings are `producerRef`, `proposalId`, `sourceKind`, `name`,
`content` and `preparationState`. The native Name constraint is length 1..256.
`sourceKind` accepts any nonblank source label. `preparationState` is `prepared`
or `unprepared`. `evidenceRefs` is a required, nonempty array of reference objects.
Optional `context`, when supplied, is a string and may be empty.
Optional `preparedMaterialRefs` is an array; prepared input requires at least
one material reference. Optional `sourceRunRef` and `sourceContextRef` are single
references. Unprepared input requires neither material nor run; prepared input
also permits an absent run.

Every `ProposalReference` has required nonblank string `id` and `kind` and
optional nonblank strings `revision`, `snapshotRef`, `retrievalState`,
`retentionUntil` and `limitation`. Revision is an opaque supplied version
reference. Missing version/snapshot/retrieval facts remain unknown. Supplied
unavailable, expired or unverified evidence is retained with its limitations;
it does not become available or verified through registration.
If present, `captureConfirmed` must be Boolean and remains a transfer observation
in the original JSON. Other original fields remain retained without becoming
human decisions. Capture confirmation does not approve or admit Information.

## Native source correspondence

InformationCandidate retains required `id`/`name` and optional `description`.
The added scalar fields `producerRef`, `proposalId`, `sourceKind`,
`originalProposal`, `content`, `context` and `preparationState` are optional for
legacy rows. `evidenceRefs` and `preparedMaterialRefs` are zero-many typed
ProposalReference values; `sourceRunRef` and `sourceContextRef` are optional
typed ProposalReference values.

CandidateSource responsibilities are represented by producer/proposal/source
fields, first original JSON and optional run/context references in the native
candidate. RawDataIndex responsibilities are represented by evidence/material
associations and their supplied version, snapshot, retention and retrieval
limits. There is no independent index table, Aggregate, receipt or raw-body copy.

Registration validates the complete selected proposal before any create, even
on resend. It uses `Query.And(Eq producerRef, Eq proposalId)` and
`EntityQueryFieldResolver` on the current user's native candidate collection.
Keys are exact and case sensitive; whitespace is not normalized. A limit of
two observes ambiguous visible correspondence.

| Visible matches | Registration | Lookup |
| --- | --- | --- |
| Zero | Native create, then native load of persisted candidate; `created=true` only after success | Absent optional candidate |
| One | Current stored candidate, `created=false`; no field write | Current stored candidate |
| More than one | Explicit native ambiguity failure, no reconciliation or selection | Same explicit native ambiguity failure |

Lookup validates both nonblank key strings and never writes. Invalid JSON,
missing fields/evidence, malformed references or preparation states fail through
native Consequence and create no candidate. Native storage failure cannot yield
a successful registration result.

Sequential resend ignores all incoming-field differences after validation:
name, content, context, source label, original JSON and references remain those
currently stored. An actual generated `entity.updateInformationCandidate`
content-only write is the human edit fixture. Reopening the ordinary Subsystem
after shutdown with the same fixed subject and storage root retains the ID,
edited current content, first original proposal and references. This Step does
not claim global metadata immutability against arbitrary low-level patches.

## Fixtures and executable specifications

The [prepared TKL fixture](../../src/test/resources/proposals/tkl-prepared.json)
contains evidence revision/snapshot/retrieval metadata, a PreparedMaterial and
ProcessingRun. Its deterministic adapter maps TKL `producerId` to `producerRef`,
`title` to `name`, `summary` to `content`, `materialRef` to
`preparedMaterialRefs`, and `producingRunId` to `sourceRunRef`. TKL receipt
expectations are outside this contract.

The [Editing Studio fixture](../../src/test/resources/proposals/editing-studio-unprepared.json)
is an unprepared confirmed book capture with a source-context reference, an
unverified capture evidence reference and an explicit unavailable-scan/version
limitation. It has no fabricated PreparedMaterial or ProcessingRun.

[CandidateProposalSpec](../../src/test/scala/org/simplemodeling/textus/knowledgeworkbench/CandidateProposalSpec.scala)
authors behavior-oriented AnyWordSpec scenarios with Given/When/Then adjacent
to setup, native operation execution and should assertions. They initialize through the public
`CncfBootstrap.initialize(BootstrapConfig)` production embedding boundary,
which admits ordinary runtime configuration and operation-security bindings.
The fixture copies the complete authored assembly with a test-owned fixed local
subject, uses explicit `component-dev-dir` and temporary `textus.local-data.root`,
and calls `Subsystem.executeOperationResponse` with ordinary Request/Property
inputs. Each native `CncfHandle` closes its owned runtime before reopening the
same root. No fake store, SQL write,
ControlledTest profile or private framework UnitOfWork fabrication is used.

| Rule | Authored behavior |
| --- | --- |
| PROP-01 | Prepared TKL registration, exact original, native ID and typed evidence/material/run retention; opaque versions and supplied limits |
| PROP-02 | Editing Studio unprepared registration, transfer observation, unknown version/snapshot, source context and limitations |
| PROP-03 | Existing/absent/exact-case/exact-whitespace lookup without writes; ambiguous correspondence refuses lookup and resend |
| PROP-04 | Identical sequential resend returns current candidate and one visible row |
| PROP-05 | Generated native content-only edit survives changed valid same-key payload with first metadata retained |
| PROP-06 | Shutdown/reopen of the same production datastore retains both fixtures and edited resend behavior, with read-only SQL confirming physical numeric preparation codes 1/2 and textual external state round trips |
| PROP-07 | Different producer or proposal isolates correspondence, including significant whitespace |
| PROP-08 | Table of invalid JSON, missing/blank/wrong-type scalars, evidence/reference/preparation errors and blank lookup keys leaves rows unchanged |
| PROP-09 | Starter name/description creation, description/name search and native paging retain behavior; legacy rows have no proposal key |

The [execution checklist](../phase/phase-1-checklist.md) records STEP-2 acceptance
under the local binding above, including the generated consumer and fixed-code
XShort persistence contract. Detailed execution and review material remains in
private local evidence. The successful full SBT suite remains a Phase release
gate after all Step commits and comprehensive Phase review.

## Preserved behavior and later owners

`registerCandidate` and `listCandidates` retain inputs/results, description
filtering and paging. Lists may expose the additive proposal fields. Native
Record.dataAuto omits absent optional values and preserves empty arrays. An absent
lookup renders as `{}`; an empty list renders as `{"items":[]}`. Canonical component coordinates,
`abi.version=1` initial additive surface, explicit CNCF version selection,
Subsystem/user/store ownership and empty MCP publication policy remain intact.

Step 3 owns human-facing evidence-aware edit/ground/review/approve/hold/reject,
native revision policy and approval-target invalidation. Step 4 owns
List/Detail/edit/review views and provider retrieval. [Phase 1.1](../phase/phase-1.1.md)
owns actual KnowledgeHub Admission. Live provider integrations, TKL feedback
and existing-candidate preparation retain their follow-on owners. Concurrent
receipt uniqueness, payload conflict/canonicalization/hashes, independent
receipt state, key retirement and a general recovery engine are excluded.

## Semantic scalar adoption — 2026-10-19

The developer approved the STEP-2 semantic model addition after current CAR lint
rejected raw Entity string attributes. This supersedes the primitive Scala type
selection in the 2026-10-03 minimal contract while preserving scalar JSON/CLI
text, exact identity strings, raw first-proposal text and existing validation.
The six scalar DATATYPEs retain SQL text; preparation state uses the fixed
numeric persistence codes defined below.
The semantic scalar amendment is included in STEP-2's accepted local binding.

| Field | Declared type | Contract |
| --- | --- | --- |
| producerRef | ProposalProducerRef | Nonblank exact external source key |
| proposalId | SourceProposalId | Nonblank exact producer-owned key |
| sourceKind | ProposalSourceKind | Nonblank open source vocabulary |
| originalProposal / proposalJson | ProposalJsonText | Raw JSON text of at least two characters; registration owns full object/schema validation |
| content | CandidateContent | Nonblank exact text; no imposed maximum or locale transformation |
| context | CandidateContext | Optional text, including an explicitly supplied empty string |
| preparationState | ProposalPreparationState | Closed POWERTYPE accepting exactly lowercase prepared/unprepared in JSON/CLI, with fixed database codes 1/2 respectively, without lifecycle transitions |

The six nominal DATATYPEs use the library scalar conversion contract.
ProposalPreparationState is a closed POWERTYPE preserving the exact lowercase
`prepared` and `unprepared` values in scalar JSON and CLI output. Its CML table
declares explicit database codes: `prepared = 1` and `unprepared = 2`. Table order
must not determine these codes, and codes must never be reassigned or reused.
The declared database mapping is `XShort`. It shares the ordinary Short attribute
path: exact signed 16-bit values become Scala `Short`, PostgreSQL/MySQL
`SMALLINT`, or SQLite `INTEGER`. The common persistence boundary rejects
fractional and out-of-range values before restoring a domain value; the type's
fixed correspondence rejects unknown codes. Zero is a value, not optional absence.
Native persistence and reads, including shutdown/reopen, preserve the mapping
between numeric database codes and textual JSON/CLI values. No existing database
migration is introduced. FindCandidateByProposal uses the same identity types.
The nonblank pattern excludes exactly strings removed by
the established String.trim check, without trimming the admitted string.
Missing legacy fields remain optional. ProposalReference remains a composite
Value carrying the supplied opaque version/retrieval facts; advisory reference
field modeling debt does not authorize new validation constraints.
