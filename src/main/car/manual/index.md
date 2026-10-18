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
return that same ID/name in a separate process. The starter exposes no separate
lookup-by-ID operation.

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

An absent optional description is omitted. Native generated `Record.dataAuto`
also omits an empty `items` field: a search or page with no matching rows returns
`{}`, rather than an explicit empty array.

### registerProposal

The `RegisterProposal` input contains required string `proposalJson`. It carries
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
{"candidate":{"id":"<native-id>","name":"TKL prepared research note","producerRef":"tkl-fixture","proposalId":"tkl-proposal-1","sourceKind":"tkl","content":"A prepared research note for human formation.","preparationState":"prepared","originalProposal":"<exact first JSON string>","evidenceRefs":[{"id":"evidence:tkl:1","kind":"evidence","revision":"1","snapshotRef":"snapshot:tkl:1","retrievalState":"available"}]},"created":true}
```

The actual TKL fixture also returns `context`, `preparedMaterialRefs` and
`sourceRunRef`. On sequential resend, `created` is false and the candidate
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
Proposal candidates add optional string `producerRef`, `proposalId`, `sourceKind`,
`originalProposal`, `content`, `context` and `preparationState`; zero-many typed
`evidenceRefs` and `preparedMaterialRefs`; and optional typed `sourceRunRef` and
`sourceContextRef`. All reference fields use the shape above. Legacy starter
rows have no fabricated proposal fields. List results may expose these additive
fields; absent optionals and empty reference vectors are omitted.

CandidateSource is represented by source/key/original/run/context metadata.
RawDataIndex is represented by typed evidence/material associations with stated
version, snapshot and retrieval limits. Both live in the ordinary native
candidate; no separate table or raw-body copy is introduced. Global metadata
immutability against arbitrary generated low-level patches is not claimed.
Use the [proposal CLI guide](user-guide.md#register-and-look-up-proposals) for both
fixtures, lookup, content-only native edit and resend.

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
The STEP-2 proposal changes still require fresh validation, independent review
and local Step commit.
Native component Help uses a framework-fixed generic summary; authored
DESCRIPTION prose projects into Cozy metadata. Web/manual/OpenAPI/MCP routes
have not been verified by these CLI checks. Starter operations remain outside
MCP publication; the factory creates only the primary participant.

Phase 1 remains in progress. STEP-2 proposal registration/lookup/resend and its
executable specifications are authored, with acceptance pending. Evidence-aware editing/grounding/review,
approve/hold/reject, approval-target invalidation and List/Detail/edit/review
views remain later Phase 1 work. Real KnowledgeHub Admission belongs to Phase
1.1. TKL preparation, feedback and live provider adapters are follow-on work.
