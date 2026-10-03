# Textus Knowledge Workbench reference

Component: `org.simplemodeling.textus.KnowledgeWorkbench`\
Version: `0.1.0-SNAPSHOT`\
Scala package: `org.simplemodeling.textus.knowledgeworkbench`

Textus Knowledge Workbench forms Information Candidates for human review and
approval. This starter foundation provides native Candidate registration and
search. Follow the [user guide](user-guide.md) for build, instance setup and
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

## Failures and scope

Prepare the classpath and select a readable regular assembly file before
startup. The server scripts reject missing/empty classpath and missing,
unreadable or non-file instance paths. Direct CLI startup also depends on valid
assembly/profile configuration. A missing stable identity, conflicting profile
identity or missing required facility provider can prevent native admission.
Invalid or missing operation inputs fail through the existing input contracts;
native datastore access remains subject to authorization. Consult diagnostics
from the actual command; this starter defines no additional error codes.

The supplied foundation evidence covers clean generation/runtime preparation,
four focused factory/assembly scenarios, CAR packaging, canonical CLI help,
registration and separate-process list/search/paging with a persistent SQLite
row, plus controlled script routing. Documentation projection and packaging
changes still require fresh validation and independent Step acceptance.
Native component Help uses a framework-fixed generic summary; authored
DESCRIPTION prose projects into Cozy metadata. Web/manual/OpenAPI/MCP routes
have not been verified by these CLI checks. Starter operations remain outside
MCP publication; the factory creates only the primary participant.

Phase 1 remains in progress: provider-neutral proposal receipt, source/proposal
lookup and sequential resend, evidence-aware editing/grounding/review,
approve/hold/reject, approval-target invalidation and List/Detail/edit/review
views remain later Phase 1 work. Real KnowledgeHub Admission belongs to Phase
1.1. TKL preparation, feedback and live provider adapters are follow-on work.
