# EAI-ingested Material consumer acceptance

status=planned
planned_at=2026-11-02
owner=textus-knowledge-workbench
producer=TKL Phase 2

## Goal and boundary

Consume mail Excel and mobile-media Materials admitted by TKL through the
existing Workbench Material reference and Candidate-work contracts. This is a
bounded consumer acceptance workstream, not a new transport subsystem or a
reopening of accepted Phases 1/1.1/2/3. Existing Studio knowledge-formation work
and its pending changes retain their scope and priority.

[TKL Phase 2](https://github.com/asami/textus-knowledge-lake/blob/main/docs/phase/phase-2.md) owns Material
identity, admission, journal and logical provenance. EAI owns mail/storage and
transfer mechanics; CNCF owns isolated execution. Workbench neither calls Drive
directly nor operates EAI upload sessions, credentials, scanners or cleanup.
KnowledgeHub owns canonical Information admission; human review stays explicit.

## Delivery order

1. Align the producer's existing Material/Resource/provenance/source-version
   projection. Use genuine source evidence and mark missing evidence unknown;
   do not manufacture a revision from an EAI completion timestamp.
2. Open the first admitted mail workbook through ordinary TKL APIs and trace its
   message/attachment origin. Reuse the existing selection/Candidate path.
3. Repeat with directly uploaded mobile media and verify authorization/recovery
   outcomes. Source acquisition must not change downstream editing semantics.

## Stage: Consumer integration

Stage Status:
- Current status: OPEN
- Owner: Workbench
- Update rule: Check WB-EAI-01..03 only against actual TKL producer and joined-route evidence.

- [ ] WB-EAI-01: Actual TKL contract and delivered version are bound; any missing
  original/provenance/source-version capability is returned to TKL, with no
  Workbench provider workaround or duplicate Material store.
- [ ] WB-EAI-02: A real mail Excel and a mobile Material can be searched/selected,
  inspected and opened with available provenance through TKL, without OpenClaw.
  Unauthorized/unavailable/pending resources have truthful outcomes; no provider
  secrets or resumable-session URI are exposed through Workbench views.
- [ ] WB-EAI-03: Existing Candidate creation and explicit Review/Approve still
  apply. EAI success neither creates an approved Candidate nor admits Information.
  Focused executable regression and one integrated independent review cover this
  consumer change, reusing existing accepted evidence at its actual scope.

Given an EAI-admitted Material, when selected in Workbench, then the same TKL
identity and original are retained. Given pending/rejected ingestion or an
unauthorized caller, when attempting original access, then no successful access
is invented. Given valid ingestion, when no human approval occurs, then no
approved Candidate/Information is inferred.

Reuse [existing integration](knowledge-editing-integration-plan.md) and
[Phase 3](phase-3.md) source-evidence contracts. If all public shapes remain
compatible, deliver this through integration evidence and minimal necessary
fixtures; do not add production features merely to create Workbench work.
