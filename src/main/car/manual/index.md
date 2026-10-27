# Textus Knowledge Workbench reference

Component: `org.simplemodeling.textus.KnowledgeWorkbench`\
Version: `0.1.0-SNAPSHOT`\
Scala package: `org.simplemodeling.textus.knowledgeworkbench`

Textus Knowledge Workbench forms Information Candidates for human review and
approval. The implemented surface provides starter Candidate registration/search
and provider-neutral proposal registration with durable source-key lookup and
sequential resend preserving current edits. Follow the [user guide](user-guide.md) for build, instance setup and
complete CLI examples.

## Runtime requirements

The repository's `project.yaml` selects CNCF explicitly through
`build.dependencies.compile`: `org.goldenport::goldenport-cncf:0.5.3-SNAPSHOT`.
Runtime preparation supplies `target/cncf.d/runtime-classpath.txt`. A readable,
complete operator-owned assembly descriptor is required for standalone startup;
set its `security.authentication.localSubject.id` to a selected stable user ID.
Use the admitted standalone profile identity when one exists. Authentication
convention and fallback privilege remain disabled; native identity and
authorization rules still apply.

The declared `full-fledged-with-standalone` style requires persistent,
transactional and optimistic-concurrency datastore facilities and current-user
context from the CNCF Subsystem. The authored assembly binds those facilities
and selects the canonical application datastore's `local-default` policy.
CNCF manages `ComponentDataStore`/`SqlDataStore`, including persistent storage
and current-user ownership. An instance may set `textus.local-data.root` for
isolated storage. Keep the same identity and datastore root across processes
to find previously registered rows. Conflicting fixed-user IDs require datastore
isolation or explicit migration under the framework's existing rule.

## Candidate operations

| Operation | Kind | Inputs | Result |
| --- | --- | --- | --- |
| `KnowledgeWorkbench.Candidate.registerCandidate` | COMMAND | Required `name`; optional `description` | `id`, `name` |
| `KnowledgeWorkbench.Candidate.listCandidates` | QUERY | Optional `text`, `offset`, `limit` | `items` |
| `KnowledgeWorkbench.Candidate.registerProposal` | COMMAND | Required `proposalJson` string containing one JSON object | `candidate`, `created` |
| `KnowledgeWorkbench.Candidate.findCandidateByProposal` | QUERY | Required `producerRef`, `proposalId` strings | Optional `candidate` |

### registerCandidate

Creates a new native InformationCandidate through the existing Entity/UnitOfWork
contract. The result contains its native generated ID and validated input name;
it does not return the description. CLI input accepts `--name` and optional
`--description`. For example, registration with name `Research note` and
description `Evidence for review` has this illustrative YAML result:

```yaml
id: "<native-id>"
name: Research note
```

The ID identifies the stored entity. A subsequent `listCandidates` call can
return that same ID/name in a separate process. Use `Candidate.getCandidateWork` for revision-aware work lookup by ID.

### listCandidates

Searches accessible InformationCandidate rows. `--text` matches a
case-insensitive substring in either name or description. Without text, all
accessible rows are eligible. `--offset` and `--limit` are integer controls
applied in the native query. Listing does not create an entity.

The optional description is a value wrapper, so a list item with a description
has `description.value`. Illustrative JSON and equivalent YAML are:

```json
{"items":[{"id":"<native-id>","name":"Research note","description":{"value":"Evidence for review"}}]}
```

```yaml
items:
  - id: "<native-id>"
    name: Research note
    description:
      value: Evidence for review
```

An absent optional description is omitted. Under the Core Phase 3 Record
convention adopted through CNCF Phase 115, a search or page with no matching
rows returns `{"items":[]}`.

### registerProposal

The `RegisterProposal` input contains required `ProposalJsonText` `proposalJson`,
represented as a JSON/CLI string. It carries
one JSON object with these fields:

| Proposal field | Required | Shape and constraint |
| --- | --- | --- |
| `producerRef`, `proposalId` | yes | Nonblank exact strings forming the correspondence key |
| `sourceKind` | yes | Any nonblank source label |
| `name` | yes | Nonblank native Name, length 1..256 |
| `content` | yes | Nonblank string |
| `preparationState` | yes | `prepared` or `unprepared` |
| `evidenceRefs` | yes | Nonempty array of reference objects |
| `context` | no | String; an empty string is allowed |
| `preparedMaterialRefs` | for prepared input | Reference array with at least one material for prepared input |
| `sourceRunRef`, `sourceContextRef` | no | One reference object each |
| `captureConfirmed` | no | Boolean transfer observation, retained in original JSON |

`ProposalReference` has required nonblank string `id` and `kind`. Optional
`revision`, `snapshotRef`, `retrievalState`, `retentionUntil` and `limitation`
are nonblank strings when supplied. Revision is an opaque source version.
Unavailable, expired or unverified references retain those limits; omitted
version/snapshot/retrieval facts remain unknown. Prepared input may omit a Run.
Unprepared capture requires neither PreparedMaterial nor Run. Other fields,
including provenance, remain in the exact original JSON without becoming decisions.

Registration validates before creating or returning an existing candidate. It
searches accessible native entities by exact, case-sensitive producer/proposal
strings without trimming or normalization. No match creates once and then loads
the persisted candidate. One match returns the current candidate without writing
any incoming field. More than one visible match returns an explicit ambiguous
correspondence native failure without selecting or reconciling rows.

`RegisterProposalResult` contains required `candidate: InformationCandidate`
and `created: boolean`. Illustrative abbreviated output:

```json
{"candidate":{"id":"<native-id>","name":"TKL prepared research note","producer_ref":"tkl-fixture","proposal_id":"tkl-proposal-1","source_kind":"tkl","content":"A prepared research note for human formation.","preparation_state":"prepared","original_proposal":"<exact first JSON string>","evidence_refs":[{"id":"evidence:tkl:1","kind":"evidence","revision":"1","snapshot_ref":"snapshot:tkl:1","retrieval_state":"available"}]},"created":true}
```

The proposal input JSON schema and model attribute names use camelCase. Native
CLI output uses the framework's default snake_case property names, including
nested reference fields; the model attribute descriptions below retain their
semantic camelCase names.

The actual TKL fixture also returns `context`, `prepared_material_refs` and
`source_run_ref`. On sequential resend, `created` is false and the candidate
contains current stored content. A different valid payload with the same key
has the same behavior: incoming name/content/context/original/references never
replace stored fields. A generated content-only entity edit and restart retain
the first original/source/evidence metadata. No payload-conflict result or
independent receipt is created. Native storage failure cannot report success.

### findCandidateByProposal

`FindCandidateByProposal` requires nonblank string `producerRef` and `proposalId`.
`FindCandidateByProposalResult` returns optional current `candidate` without any
write, using the same exact lookup and ambiguity refusal. Found output contains
`candidate` as in registration, with no `created` field. Absence renders as `{}`
because the native codec omits the absent optional candidate.

### InformationCandidate additions

The required `id`/`name` and optional description wrapper remain compatible.
Proposal candidates add optional semantic string types for `producerRef`
(`ProposalProducerRef`), `proposalId` (`SourceProposalId`), `sourceKind`
(`ProposalSourceKind`), `originalProposal` (`ProposalJsonText`), `content`
(`CandidateContent`) and `context` (`CandidateContext`). `preparationState` uses
`ProposalPreparationState`, a closed POWERTYPE accepting exactly lowercase
`prepared` and `unprepared` in scalar JSON and CLI output. The six nominal
DATATYPEs retain scalar string JSON, CLI and SQL storage representations.
Preparation state instead stores the explicit CML database codes `prepared = 1`
and `unprepared = 2`; table order must not determine these codes, and codes must
never be reassigned or reused. Native storage/read round trips, including
shutdown/reopen, retain numeric database codes and textual JSON/CLI values.
No existing database migration is introduced. There are also zero-many typed
`evidenceRefs` and `preparedMaterialRefs`; and optional typed `sourceRunRef` and
`sourceContextRef`. All reference fields use the shape above. Legacy starter
rows have no fabricated proposal fields. List results may expose these additive
fields; absent optionals are omitted and empty reference vectors are explicit arrays.

CandidateSource is represented by source/key/original/run/context metadata.
RawDataIndex is represented by typed evidence/material associations with stated
version, snapshot and retrieval limits. Both live in the ordinary native
candidate; no separate table or raw-body copy is introduced. Global metadata
immutability against arbitrary generated low-level patches is not claimed.
Use the [proposal CLI guide](user-guide.md#register-and-look-up-proposals) for both
fixtures, lookup, content-only native edit and resend.

## Material work reference and registration

`KnowledgeWorkbench.MaterialWork` provides the following authenticated-only
work operations. Every producer target is fixed to
`org.simplemodeling.textus.KnowledgeLake`, service `Material`.

| Work operation | Kind | Inputs | Producer operation / result |
| --- | --- | --- | --- |
| `searchMaterials` | QUERY | Optional `text`, `offset`, `limit` | `searchMaterials` / logical Material search result |
| `inspectMaterial` | QUERY | Required `materialRef` string | `getMaterial` / logical metadata, resources and provenance |
| `readOriginal` | QUERY | Required `materialRef`, `resourceId` strings | `readOriginal` / original binary response with producer MIME type and headers |
| `createMaterialRegistration` | COMMAND | Required `contribution` JSON value | `createMaterialRegistration` / logical registration result |
| `getMaterialRegistration` | QUERY | Required `registrationId` string | `getMaterialRegistration` / logical registration result |
| `materializeMaterial` | COMMAND | Required `registrationId` string | `materializeMaterial` / logical registration result |

Create accepts one complete neutral `textus.material-contribution/1` JSON value,
including its schema, title, creation time, classifications, provenance,
originals and documents. The JSON remains typed JSON, including opaque nested
source references, histories, null bodies, array order and Unicode. Workbench
does not parse those fields into provider authority. Domain applications own
authorized source conversion. Selectors are exact opaque logical strings;
neither Book/ISBN/App identity nor a Google key or operation target is an input.

The registration result has exactly `registrationId: string`, `revision: long`,
`state: string` and `materialRef: string`. Native default result JSON names are
`registration_id`, `revision`, `state` and `material_ref`; request names remain
`registrationId` and `contribution`. The producer's complete response is
returned unchanged. Revision and prepared/materializing/registered state are
the observed committed native snapshot. Workbench never synthesizes them.
A planned Material reference is unavailable until registration is `registered`.
Consumers then use the independent metadata/original read route.

The caller's CNCF execution, security and observability context is retained.
Workbench authentication grants no TKL permission: TKL independently owns
producer configuration, allowed subjects, native storage authorization,
source/destination bindings and managed provider custody. Workbench supplies no
privileged grant, connection setting or credential bootstrap. Structured
producer failures and uncertain acknowledgements remain intact, with one
producer call per explicit work request. Unknown create outcomes stay unresolved;
explicit status reads do not authorize automatic replay, reset or deletion.

These operations create no Candidate, evidence association or human approval.
Existing reference reads require no registration, continuation, OpenClaw or
Candidate creation. The authored forwarding specifications use controlled
producer responses through native dispatch; focused validation and actual
composed Studio/TKL PostgreSQL, private CAR and real Google/App acceptance
remain parent-owned gates. See the [Material work sequence](user-guide.md#material-work-sequence).

## Failures and scope

Prepare the classpath and select a readable regular assembly file before
startup. The server scripts reject missing/empty classpath and missing,
unreadable or non-file instance paths. Direct CLI startup also depends on valid
assembly/profile configuration. A missing stable identity, conflicting profile
identity or missing required facility provider can prevent native admission.
Invalid or missing operation inputs fail through the existing input contracts;
native datastore access remains subject to authorization. Consult diagnostics
from the actual command; no application error-code catalog is added. Invalid
proposal JSON/fields/references/preparation fails natively without creation,
and blank lookup keys fail without writes. Ambiguous correspondence is an
explicit refusal, not a concurrent uniqueness guarantee.

The accepted STEP-1 foundation evidence covers clean generation/runtime preparation,
four focused factory/assembly scenarios, CAR packaging, canonical CLI help,
registration and separate-process list/search/paging with a persistent SQLite
row, plus controlled script routing. STEP-1 documentation projection/package/help/lint
and independent reviews were admitted before local commit
`70620d1aeaae4a3859df93d6ff6948bdcff5742c`. Parent receipt
`tkw-phase1-step1-post-hardbreak-commit-acceptance-20261003@1` and ledger
`tkw-phase1-review-ledger-20261002@4` record STEP-1/S1-A acceptance.
STEP-2 acceptance requires focused validation, independent review and a local
Step commit in addition to the foundation evidence.
Native component Help uses a framework-fixed generic summary; authored
DESCRIPTION prose projects into Cozy metadata. Web/manual/OpenAPI/MCP routes
have not been verified by these CLI checks. Starter operations remain outside
MCP publication; the factory creates only the primary participant.

Phase 1 remains in progress. STEP-2 owns proposal registration/lookup/resend;
STEP-3 owns evidence-aware formation and explicit human decisions. STEP-4 adds
the Workbench List/Detail/edit/review application described below. Acceptance
and final Phase validation remain recorded by the execution checklist. Real KnowledgeHub Admission belongs to Phase
1.1. TKL preparation, feedback and live provider adapters are follow-on work.

## Candidate formation and human decisions

`getCandidateWork(id)` returns the accessible native Candidate, its `revision`,
effective `state`, `reviewEligible`, `approvalValid` and optional exact `target`.
`editCandidateWork` takes `id`, `expectedRevision`, nonblank `content`, optional
`context`, complete `evidenceRefs`, `groundingRefs`, `mappingRefs`, and `reason`.
The evidence array must be nonempty. Omitted context clears it; arrays replace
the selected target associations. Source identity and the first proposal stay retained.

`decideCandidateWork(id, expectedRevision, decision, reason)` accepts explicit
`review`, `approve`, `hold` or `reject`. The `edit` history code is produced only
by the edit operation and is rejected as a decision input. Actor/time come from
the native execution context. Both commands use native conditional Entity updates.
Read the returned revision before the next command; stale revisions fail without writes.

Review requires declared available, versioned/snapshotted evidence; approval
requires an unchanged Reviewing target. Hold may resume Review. Reject requires
explicit Edit before renewed Review. Changes invalidate approval; restoring old
text does not restore an old approval. An identical ordinary edit writes nothing.
Complete decision history and exact targets persist across normal runtime restart.
These operations do not retrieve evidence from a provider, grant KnowledgeHub
Admission or claim actual provider availability from supplied reference metadata.

See the repository specification `docs/spec/candidate-formation-and-review.md`
for the complete transition table and representation boundaries.

## Candidate Workbench application

The same-origin entry is `/web/knowledge-workbench/textus-knowledge-workbench`.
Its generated `candidate-list` and `candidate-detail` displays share
`resourceType=information-candidate` through ordinary `display.query` and
`display.read`. `display.describeBusiness` maps the two human actions to
`Candidate.editCandidateWork` and `Candidate.decideCandidateWork`. Native
authorization and conditional revision updates remain authoritative.

Detail retains supplied evidence availability/version/snapshot/retention and
limitations, Material/Run/context references, grounding/mapping and attributed
decision targets. These are declared facts, not a provider access guarantee.
The editor preserves missing versus present-empty context explicitly. Its narrow
native Candidate Record adapter does not alter the separate Display wire format.
See the user guide's Candidate Workbench screen procedure for the first-success
workflow and reconciliation after a failed or unknown response.
