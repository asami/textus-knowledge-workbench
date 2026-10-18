# Workbench STEP-2 handoff: CML POWERTYPE fixed persistence codes

Date: 2026-10-19
Status: original specification/planning handoff; implementation accepted by the
separate STEP-2 local binding `tkw-step2-connected-acceptance-commit-20261019-01@1`
Owner: Workbench Phase 1 STEP-2 development task

## Requested outcome

Continue the existing Candidate registration/lookup work using generated
ProposalPreparationState. Persist `prepared` as integer 1 and `unprepared` as
integer 2, restore the actual typed values on read/reopen, and retain the exact
lowercase symbolic strings in JSON/CLI. Implement the shared producer contract
so that other persistent POWERTYPEs follow the same rules without Workbench-only
branches.

This handoff carries the approved specification direction. It does not activate
a Goal, reset history or counters, certify implementation, or send a command to
the development chat. Continue within its existing admitted scope and retain
earlier approvals; do not ask for general reapproval of these design decisions.
If an actual dependency edit lies outside that scope, identify the specific
component/contract change using the normal boundary process rather than treating
this document as new execution authority.

## Read first

- [Workbench Phase 1](../../../../docs/phase/phase-1.md), STEP-2 dependency follow-up.
- [Execution checklist](../../../../docs/phase/phase-1-checklist.md).
- [Candidate proposal contract](../../../../docs/spec/candidate-proposals.md),
  semantic scalar adoption.
- [CML normative contract](../../../../../../dev2025/cozy/docs/spec/cml-powertype-persistence-code.md).
- [Cross-project implementation plan](../../../../../../dev2025/cozy/docs/phase/powertype-persistence-code-plan.md).
- [SimpleModeler generation contract](../../../../../../dev2025/simple-modeler/docs/spec/entity-store-mapping-generation.md),
  subsequent POWERTYPE persistence-code contract.

## Decisions already made

1. A finite POWERTYPE used in persistence, or explicitly declared as a numeric
   persistence type by its model contract, requires a fixed integer code for
   every kind. Display-only/transient vocabularies may omit codes.
2. Use the existing CML `value` column. No new keyword/flag is required. Codes
   use the signed 16-bit `XShort` persistence contract. The generic model/code
   correspondence may retain its Int-valued API; the declared persistence type
   controls the physical Short representation and range. The Workbench declaration already has
   `prepared=1` and `unprepared=2`; preserve those assignments.
3. Missing required, duplicate, malformed or out-of-range codes fail semantic
   validation before successful generation. Include persisted references through
   optional/repeated wrappers and nested values. Do not convert invalid codes
   into omission or invent codes from ordinal/declaration order.
4. Reorder/label changes preserve codes. Published/persisted codes are not
   reassigned or reused. Unknown stored codes produce the established structured
   failure; no default kind, None or text-storage fallback hides the problem.
5. Optional absence is separate from a code. Zero is not an implicit null code.
   No automatic rewrite or reinterpretation of existing databases is authorized.
6. Fixed-code persistence is separate from external presentation. Do not replace
   ModelValueEncoder's symbolic projection with numeric database encoding, or
   infer a codec after type information has been erased by that projection.

## Responsibility and local checkouts

| Project | Checkout on asamiair | Responsibility |
| --- | --- | --- |
| goldenport-core | `/Users/asami/src/dev2025/simplemodeling-lib` | Minimal typed bidirectional value/code contract, independent of models and SQL |
| Cozy | `/Users/asami/src/dev2025/cozy` | CML declaration preservation and resolved persistence-use diagnostics |
| SimpleModeler | `/Users/asami/src/dev2025/simple-modeler` | Generate each type's explicit mapping/inverse and declared persistence binding |
| simplemodeling-model | `/Users/asami/src/dev2026/simplemodeling-model` | Model-owned protocols/adaptation where needed; no per-type SQL codec subsystem |
| CNCF | `/Users/asami/src/dev2025/cloud-native-component-framework` | Use the selected codec at the existing persistence boundary if integration needs a change |
| Workbench | `/Users/asami/src/dev2026/textus-knowledge-workbench` | CML and actual generated consumer registration/read/reopen acceptance |

Core owns the common contract; each POWERTYPE owns its fixed correspondence.
Generated typed calls replace reflective discovery on the selected route.
Keep Update/Condition, generic external conversion and storage responsibilities
separate. Do not move model-specific cases into Core or hand-edit generated code.

## Execution sequence

1. Reconcile the current owning task and working trees. Preserve existing
   STEP-2 source, tests, CML, documentation and earlier evidence. Read actual
   current APIs before choosing names for the new common codec.
2. Implement the Core contract, producer mapping and one Workbench save/read
   path as a connected batch, with its executable specifications. First prove
   `CML -> generated typed model -> numeric storage -> typed readback`.
3. Complete declaration rejection and transient-omission behavior around that
   route. Correct affected existing declarations deliberately; do not add a
   compatibility branch solely to retain ordinal-derived codes.
4. Validate actual generation/compilation, both values, numeric storage,
   provider reopen, unchanged JSON/CLI strings, reorder/label stability,
   malformed/missing/duplicate codes and unknown stored-code failure. Reuse
   applicable existing evidence and batch related checks; no review cycle per
   helper or fresh full run solely because a handoff document changed.
5. Independently review the connected contract and consumer result, then finish
   the owning task's already-authorized acceptance/commit process. Report real
   outcomes in the existing STEP-2 checklist. SBT execution follows the current
   registered runner and shared wrapper policy; this handoff grants no bypass.

The six nominal scalar attributes retain their existing SQL-text semantics.
Preserve exact proposal/source identities, original proposal text, human edits
on sequential resend and normal lookup behavior. This codec change does not
turn the preparation vocabulary into a lifecycle StateMachine.

## Current state and completion boundary

At handoff authoring, the local STEP-2 CML and proposal specification contain the
explicit 1/2 mapping. The shared CML specification, grammar-note correction,
producer contract and plans are authored but uncommitted. The owning Workbench
working tree already contains other ongoing changes; they are not disposable.

This documentation task ran no build, generation, tests, independent implementation
review or acceptance commit. It does not certify the current implementation or
discard evidence retained by the owning task. Historical Phase 78/100/115 and
Core Phase 3 acceptance does not prove this new contract and remains closed.

Completing the codec route contributes to STEP-2; it does not by itself close
STEP-2, Workbench Phase 1 or Phase 1.1. Preserve their remaining obligations.
Do not add hashes, copied-state comparisons, runtime immutability proofs or a
new history registry to enforce this handoff.
