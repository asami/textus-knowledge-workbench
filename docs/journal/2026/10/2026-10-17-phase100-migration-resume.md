# Workbench migration: Phase 100 development resume

Date: 2026-10-17; updated 2026-10-18 (Asia/Tokyo)
Status: implementation and acceptance in progress
Current ledger: [asamiair migration](../../../phase/workbench-asamiair-migration.md)

## User-selected outcome

The user reports Phase 96 completed and pushed to GitHub main and requests
CNCF Phase 100 completion within the Workbench source migration. The selected
scope contains no existing Workbench DB. Retained fixtures and test-owned
stores provide the data for restart and recovery validation.

## Source ownership

The canonical checkouts are Workbench main, Core main, SimpleModeler scala12,
Cozy main and CNCF main. Their mac mini source merges remain uncommitted.
Textual conflicts were resolved while retaining current-main behavior and the
incoming Phase 100 declarations/codecs. CNCF retains main's generation build
contract; ordinary shared SNAPSHOT artifacts connect producer and consumers.
Generated source is regenerated through that contract, never hand-patched.

Core owns canonical Json/Record logical types. SimpleModeler owns declaration
projection and generated builders; Cozy Phase 78 owns the CML producer route.
CNCF owns declared EntityStore stages and physical provider transport.
Workbench supplies the actual proposal preservation and persistence consumer.
The consumer boundary inventory is [recorded separately](2026-10-17-phase100-integrated-consumer-inventory.md).

## Acceptance still required

The owning CNCF checklist remains authoritative for all DSM-100 items.
Current runtime/full/downstream validation, independent review and release
records must be completed before Phase closure. Workbench Phase 1 history and
its unresolved private-control gap do not become new Phase 100 counters.
The current comparison review explicitly retains the incomplete historical
review evidence and known producer obligations rather than importing an old PASS.

## Retained investigation

The exact pre-consolidation journal, including original failed and successful
execution observations, is preserved on asamiair at
`.codex-workflow/migration-document-originals/20261018-before-consolidation/docs/journal/2026/10/2026-10-17-phase100-migration-resume.md`.
Current typed command receipts/logs remain at their bound ignored locations.
Neither this management summary nor a local dependency refresh constitutes
new test coverage or Phase acceptance.
