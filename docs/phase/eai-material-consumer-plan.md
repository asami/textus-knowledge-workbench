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

## BMFT production-feature extension — 2026-11-08

The user's explicit [BMFT connected plan](https://github.com/KnowledgeHubProject/bok-knowledgehub/blob/main/docs/phase/bmft-excel-material-information-plan.md) authorizes planning the
concrete workbook interpretation and Web Candidate path in addition to the
bounded compatibility acceptance above. The earlier "minimal necessary fixtures"
guidance does not mean this newly selected functionality is already implemented.
WB-EAI-01..03 retain their OPEN state and accepted Phases are not reopened.

The 2026-11-08 user clarification replaces the earlier Workbench parsing allocation:
Studio server owns BMFT-specific sheet/header/bibliography interpretation and maps
selected excerpts into generic content/translation/annotation/context/source items.
Workbench owns BMFT-01/04's generic public/save contract support and Candidate
creation from those application-supplied items. Textus must not require BMFT column
names, profiles or vendor-specific branches. Retain source references, translations,
contextual annotations and both locators through the generic contract. Reuse generic
spreadsheet read capabilities only for structural access, not BMFT interpretation.
Name necessary generic model/API extensions before implementation; no arbitrary
string packing, inferred authorship or replacement of raw TKL Material.

BMFT-05/06 reuse real TKL versioned source evidence, Candidate editing and explicit
Review/Approve, then existing Hub admission/getInformation. Preserve original
selection/source after reopen, invalidate decisions after edits under existing
rules, distinguish retry from deliberate new interpretation, and do not blindly
resend unknown admissions. TKL identity/version and Hub canonical ownership remain.

BMFT-07 verifies representative Japanese and translated English rows with Studio
Web, persisted Information and both source locators. Mail ingestion and row parsing
never themselves approve a Candidate. The central BMFT ledger tracks the added
delivery; it does not replace WB-EAI or other owners' completion ledgers.

## Immediate existing-Material demo contribution — 2026-11-08

The user's follow-up assigns the existing-TKL-Material Web route to the next demo
development: [App Phase 7.1](https://github.com/KnowledgeHubProject/nict-editing-studio-app/blob/main/docs/phase/phase-7.1.md) and [Server Phase 5.1](https://github.com/KnowledgeHubProject/nict-editing-studio/blob/main/docs/phase/phase-5.1.md),
immediately after their active Phases. Prioritize BMFT-01/04/06 generic contract
capability delivery for those successors, before broader non-demo expansion.
BMFT-03 parsing/mapping is owned by the Studio application adapter.
Begin from an authorized existing Material with real identity/version/original
reads; no new phone capture or Studio receipt is required. Workbench discovery,
generic Candidate formation from application-normalized inputs and explicit
review/admission stay on the owning contracts. Missing generic capabilities
receive explicit owning spec/design
and implementation work, rather than being assumed present or postponed to EAI.

The registered-Material demo path does not wait for EAI2-02/TKL2-02 live mail or
all producer Phases to close. Their actual ingestion acceptance and WB-EAI-01..03
remain separately open. Join actual browser correction, Information persistence/
reopen and source trace; fixture-only approval cannot close the demo route.
Acceptance also checks that BMFT profile changes are handled by the app adapter
without a BMFT branch in Textus, and that the raw Material bytes remain unchanged
after normalization, Candidate correction and Information admission.
