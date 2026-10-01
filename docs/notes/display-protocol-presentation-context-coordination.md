# Display Protocol and Presentation Context coordination — proposal

Date: 2026-10-02
Status: non-normative proposal and ownership handoff
Owner: TKW cross-project integration strategy
History: [proposal recording](../journal/2026/10/2026-10-02-presentation-context-coordination-proposal.md)
Strategy: [Knowledge Application Integration](../strategy/knowledge-application-integration.md)

## Why TKW is involved

TKW owns the integration strategy and coordination across Editing Studio,
CNCF/Cozy and Flutter dependencies. As the [Workbench architecture](architecture.md)
defines, TKW also owns Information Candidate lifecycle, editing/review and
explicit admission to KnowledgeHub. It is therefore a future participant in
context-bearing business flows, alongside its current coordination role.

CNCF owns generic protocol/runtime machinery. TKW contributes cross-project
requirements and, when admitted, concrete Workbench Operation contracts;
Experiment owners retain experiment lifecycle/allocation/evaluation. No TKW
experiment allocator or generic transport subsystem is proposed here.

## Agreed planning direction

The app parent proposes the protocol and server-model evolution from UI
scenarios, using the proposed development-only Editing Studio sample. Concrete
server semantics and fixtures refine the proposal with CNCF/Cozy. Completed
server implementation is not an entry requirement for proposal preparation.

Presentation Context is the bounded presentation-facing projection of CNCF
ExecutionContext. Versioned, typed context families cover admitted arm/variant,
locale/presentation and permitted correlation. Define producer, permitted use,
lifetime, propagation, compatibility and update rules. It is not the full
runtime object and does not grant authorization or admission authority.

Core receives/holds/transports admitted context over Display read/mutation,
ordinary REST Business Operation, reload and retry. TFAF resolves supplied
arm/variant identity to a registered UI definition and realizes it. The app
registers declarative configuration/domain bindings and composes public APIs;
app screen code has no experiment logic or manual context forwarding.

A/B and multi-arm are one context family. The framework's supplied-arm
presentation responsibility is independent of experiment allocation and
result aggregation. Deterministic arm fixtures verify UI behavior; they do
not imply a UI exposure-measurement subsystem.

## Workbench-specific boundary

TKW domain Context, Facets and Relations are meaning-bearing Candidate content.
ExecutionContext/Presentation Context is runtime call/presentation information.
Keep their types, lifecycle and authority distinct. Context propagation does
not mutate Candidate content or approve Information Admission.

When real Editing Studio -> TKW Operations are admitted, specify which context
families can cross that component boundary, how CNCF validates/rebinds them,
how correlation continues and how a client reloads affected Display. Existing
CNCF single-logical-execution assignment and explicit nested-operation policy
remain in force; cross-operation UI continuity is a handoff question.

The first DisplayService sample is explicitly development-only. It does not
require completion of TKW production admission or invent a Candidate lifecycle
API. Server Phase 1.2's real contracts remain a separate gate.

## Coordination handoffs

| Owner | Required proposal/handoff |
| --- | --- |
| [App Phase 4](../../../../Project2026/nict-editing-studio-app/docs/notes/display-protocol-presentation-context-proposal.md) | Client prototype, UI request/response/state/context/arm examples and feasibility findings |
| [App Phase 5](../../../../Project2026/nict-editing-studio-app/docs/phase/phase-5.md) | Real-server integration and final aggregate/E2E acceptance |
| [Editing Studio Phase 2](../../../../Project2026/nict-editing-studio/docs/notes/display-protocol-server-model-evolution.md) | Semantic source, projection/input/operation bindings, revision/freshness and deterministic server fixtures |
| [CNCF Phase 96](../../../../dev2025/cloud-native-component-framework/docs/notes/display-protocol-presentation-context-proposal.md) | ExecutionContext projection map, physical codec/compatibility, validated context continuation/admission |
| Cozy | Shared Logical UI vocabulary and version correspondence |
| [Core](../../../textus-flutter-core/docs/notes/display-protocol-presentation-context.md) | Runtime codec/transport, context flow/lifetime/compatibility and fixture correspondence |
| [TFAF](../../../textus-flutter-application-framework/docs/notes/display-protocol-context-realization.md) | Arm/definition resolution, reusable screens/transitions and deterministic visual fixtures |
| TKW | Dependency/status coordination; future admitted Workbench Operation/context-boundary contracts |
| Experiment | Definitions, arms, runs, assignment policy and evaluation through CNCF provider-neutral contracts |

Keep scope/status in the strategy; runtime/implementation decisions, fixtures
and acceptance remain with their owning repositories. App Phase 4 owns client
prototype feasibility; Phase 5 owns real-server aggregate/E2E verification.
Dependency handoffs use focused evidence;
no generic or Workbench Phase is closed by parent acceptance. Physical context
binding, cross-operation continuity and arm-definition versioning remain open.

## Client prototype scope update — 2026-10-02

The user clarified that app Phase 4 ends at provisional client implementation
and feasibility exploration. It uses development-only sample models and
controllable fake transport/Operations to prove the Core/TFAF/app boundary,
context continuation and supplied-arm UI. Findings revise protocol/server-model
proposals. Completed server implementation and frozen wire contracts are not
prototype closure prerequisites; simulation does not prove real CNCF admission.
App Phase 5 consumes this prototype and owns real-server integration and final
aggregate/E2E acceptance. Dependency contracts/Phases retain separate acceptance.
