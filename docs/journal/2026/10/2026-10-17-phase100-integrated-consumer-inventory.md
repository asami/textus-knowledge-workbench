# Phase 100 integrated consumer inventory

Date: 2026-10-17; updated 2026-10-18 (Asia/Tokyo)
Status: current source boundary inventory; integration acceptance pending
Owner: CNCF DSM-100-01; Workbench migration driver

## Selected source and declarations

The canonical CNCF checkout is
`/Users/asami/src/dev2025/cloud-native-component-framework`, main comparison
base `e455716c` plus the uncommitted migration integration. This base selects
the current comparison; it does not reconstruct the historical Phase start.
Core, SimpleModeler, Cozy and Workbench retain the source ownership described
in the [migration ledger](../../../phase/workbench-asamiair-migration.md).

Workbench declares originalProposal and proposal/source keys as String,
repeated evidence/material references as ProposalReference products and run/
context references as optional products. Exact original text is intentional;
JSON-looking String never supplies a structured datatype declaration.
The actual generated CNCF fixture separately covers String, Json, Record,
application products, arrays, native scalars, optional values and updates.

## Current conversion ownership

Paths below are relative to CNCF's src/main/scala/org/goldenport/cncf/.

| Boundary | Owned conversion |
| --- | --- |
| datastore/sql/SqlDataStoreRecordCodec.scala | Physical TEXT reads stay String; native scalar/binary transport and exact big numeric text are explicit. No payload inference or implicit structured writer is admitted. |
| entity/EntityPersistent.scala and EntityPersistentCreate/Update | Declared logical write, physical row admission and logical decode are separate stages. None retains the explicit legacy physical hook; invalid selected declarations fail. |
| entity/StandardEntityCreatePart.scala | Declared conversion precedes plain/upsert/managed create; existing identity, management and provider owners remain. |
| entity/StandardEntityReadSavePart.scala | Ordinary and carrier load/save route through declared admission/decode, with ContentBody hydration and revision ownership retained. |
| entity/StandardEntityRecordPart.scala | Record writes normalize native Update once before partial declared encoding and retain managed-field policy. |
| entity/StandardEntityUpdatePart.scala | Direct, CAS and versioned mutations use declared writes and authoritative mapped readback. |
| entity/StandardEntityLifecycleQueryPart.scala | Typed search admits logical rows before applying logical query/order/page; provider mutation guards remain physical. |
| entity/StandardEntityConditionalPart.scala | Root and successor use their own declarations without duplicate application decode; atomic provider operations retain ownership. |
| entity/StandardEntityRevisionPart.scala | Raw revision/identity evidence stays physical; typed carrier construction crosses the declared admission boundary. |
| component/ComponentFactoryReflectionPart.scala | Generated mapping/logical/view/security hooks and explicit storage names survive the runtime bridge; input aliases do not replace storage identity. |
| information/InformationEntityRepository.scala | Current writes consume generated declarations. Retained V0 reads use the explicit nonwriting migration decoder and existing lifecycle/alias refusals. |
| job/DurableJobStoreMapping.scala | Closed String declarations retain jobId/canonicalJson for Job and all three quota families. Their canonical parsers remain explicit application owners; direct inventories receive String. |
| job/JobEntity.scala | Built-in Job and JobDefinition management projections use fixed declarations for their known Record fields and ordered continuation arrays. Their logical records and JCL-source interpretation retain their existing owners; these projections are distinct from durable canonical-text records. |
| blob/BlobRepository.scala and blob/MediaModel.scala | Each existing attributes owner explicitly encodes its known String map as JSON text and explicitly reads it back. This does not restore generic SQL Record/Map inference. |
| entity/ContentBodyStoragePolicy.scala and SimpleEntityStorageShapePolicy.scala | Overflow rows, managed fields, security identity and canonical permission String remain explicit framework physical contracts. |

## Direct physical readers and writers found

A source search for direct `ds`/`datastore`/`dataStoreSpace`
load/search/create/save/update/delete calls identified these additional paths:

- `association/AssociationBindingWorkflow.scala`: physical load for binding;
- `repository/RepositorySupport.scala` and
  `action/ActionCallRepositoryPart.scala`: raw repository search/load;
- `importer/StartupImport.scala`: explicit seed Record save/create;
- `unitofwork/UnitOfWorkInterpreter.scala` and
  `action/ActionCallEntityAccessPart.scala`: physical reads around entity access;
- `component/builtin/admin/AdminComponent.scala`: raw admin data operations;
- `security/OperationAccessPolicy.scala`: raw security-attribute load;
- `job/DurableJobStorageQuotaInventory.scala`,
  `job/DurableJobOwnedBlobQuotaInventory.scala`, and
  `job/DurableJobLifetimeQuotaProvider.scala`: direct physical search alongside
  EntityStore mutation paths;
- `entity/EntityStoreSpace.scala`: collection/entry routing and physical access.

`action/ActionCallDataStorePart.scala` also matched commented examples; those
are not active call sites. The search is an initial call-site catalog, not an
exhaustive alias/dataflow proof. Each active reader must be classified against
its consuming decoder and its actual collection before provider cutover. Raw
admin/seed/repository access must not silently acquire application inference.

### Current direct-call classifications

The following owner classifications were checked against the current source
after integration. They do not introduce a new codec or change an interface.

| Direct path | Current consuming owner |
| --- | --- |
| AssociationBindingWorkflow | The direct load checks presence and normal lifecycle/tenant visibility of an existing target. It does not reconstruct application products from SQL text. |
| RepositorySupport | searchRecords and searchForView return provider records; mapRecords applies the caller-supplied projection separately. This physical interface does not select an EntityPersistent mapping. |
| ActionCallRepositoryPart and ActionCallEntityAccessPart | Their direct loads supply physical evidence to the persistent owner's authorizationRecord. Typed entity loads and searches retain their separate EntityStore/collection routes. |
| StartupImport | DataStore seed entries are passed directly to physical save/create. Entity seed entries separately use the registered persistent fromRecord and EntityStore importSeed route. This does not establish support for arbitrary nested logical values in physical DataStore seeds. |
| UnitOfWorkInterpreter | Direct records supply authorization and physical mutation/capture evidence; typed entity operations delegate to EntityStore. Capture's physical-versus-logical correspondence is recorded in the entity-mutation-capture spec/design. |
| AdminComponent | Raw data read/list/create/apply uses the DataStore interface. Typed entity routes remain separate. Raw data records do not gain automatic application datatype selection. |
| OperationAccessPolicy | A raw fallback supplies security evidence. SimpleEntityStorageShapePolicy explicitly reads the known security/permission representation; it does not infer arbitrary application JSON fields. |
| DurableJobStorageQuotaInventory | Physical rows provide bounded opaque jobId, provider revision and canonicalJson to the existing entity decoder and quota metadata calculation. Canonical text remains String. |
| DurableJobOwnedBlobQuotaInventory | Physical Blob rows cross the existing Blob metadata decoder before ownership/quota inspection. Its attributes conversion is owned by BlobRepository, as recorded above. |
| DurableJobLifetimeQuotaProvider | The direct search proves emptiness with an explicitly bounded page. Actual quota loading uses loadDetached and the existing durable String declaration and record decoder. |
| EntityStoreSpace | Collection/entry routing and physical provider access retain their existing owners; declared logical conversion occurs in the selected persistent/StandardEntity stages rather than payload inspection by the router. |

These classifications preserve the distinction between a physical interface
and an application mapping. The current full-suite and selected consumer
results are retained separately; this source inventory is not evidence that
every possible raw admin, seed or caller-defined repository payload is accepted.

## Data and recovery boundary

The user selected source migration only, with no existing Workbench DB.
Retained proposal fixtures and isolated test-owned SQLite source/backup/working
copies are the selected recovery targets. Trusted producer/mapping identity is
required before adoption; unknown provenance and corrupt payloads are refused.
Read-only diagnosis, interruption/retry and backup restoration are separate
from any production cutover authority. No original-data recovery is claimed.

The exact earlier inventory and its original reproduction observations are
retained on asamiair under
`.codex-workflow/migration-document-originals/20261018-before-consolidation/docs/journal/2026/10/2026-10-17-phase100-integrated-consumer-inventory.md`.
All DSM closure decisions still require the owning checklist, current validation
and independent review; this inventory alone supplies no acceptance.
