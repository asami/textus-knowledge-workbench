# DisplayService Development-Driver Phase Relationship

Date: 2026-09-29
Scope: approved planning/ownership update only

## Decision

Keep nict-editing-studio-app as the overall development driver. Add app Phase 4
as the integration parent because completed app Phase 2 is the fake Resource
List/Detail baseline and planned Phase 3 is the separate Common Settings line.

Use nict-editing-studio Phase 2 as its server-side development child. The server
must originate concrete application DisplayModel/projection contracts and
reference fixtures so the connected app has a contract to consume. CNCF Phase
96 and Cozy Phase 74 are generic dependencies developed from that concrete
proof, not completed-infrastructure prerequisites for starting the fixture
work. Flutter Core owns client/runtime; TFAF owns visual realization.

## Execution and acceptance boundary

- Exchange versioned contracts, fixture provenance and focused evidence in both
  directions between parent, server child and generic dependency owners.
- Use focused verification in children; no child full-test handoff gate.
- App Phase 4 owns the final aggregate/E2E acceptance for this integration line.
- Keep generic repository Phase acceptance independent.
- Do not reopen completed app Phase 2 or server Phase 1/1.1.
- Keep server Phase 1.2's real admission gate intact and separate; use an
  existing admitted resource or explicitly development-only fixture for the
  bounded DisplayService proof.
- Keep standard mutation on DisplayService and Business Operations on ordinary
  REST Operations, followed by explicit Display reload.

## Cozy identity check

GitHub main's docs/phase/phase-74.md was read and is titled Abstract UI Runtime
Contract. The local Cozy workspace has an unrelated untracked Phase 74 draft.
Only the verified GitHub Phase identity is referenced by this planning update;
the local draft was not overwritten or renumbered. Local Cozy execution needs
separate identity reconciliation.

## Outcome

The strategy and owning app/server/CNCF planning documents were aligned.
Implementation, test execution, Phase closure and commit/push are not claimed
by this documentation update.

## Canonical coordination

[Knowledge Application Integration Strategy](../../../strategy/knowledge-application-integration.md)
