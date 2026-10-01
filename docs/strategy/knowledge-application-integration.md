# Knowledge Application Integration Strategy

status=active
owner=textus-knowledge-workbench
updated_at=2026-10-02

## Purpose

This document is the canonical cross-project strategy and status view for Textus knowledge-processing applications spanning server and smartphone clients.

Detailed implementation remains owned by each repository's Phase documents. This strategy owns coordination, dependencies, integration milestones, and the current end-to-end target.

## Architectural direction

The server and client expose two deliberately different contracts:

- **View Model** is a semantic read model. Integration clients, AI/MCP consumers, and other applications may need its domain/application meaning.
- **Display Model** is a presentation contract. It is an instance of the target-neutral Abstract/Logical UI Model and exposes what to display and what actions are available without requiring the client to understand domain meaning.

A Display Projection may rename and reshape semantic data for presentation. For example, product_name may become title, and JSON or server-specific values must be projected into displayable values before crossing the Display Model boundary.

~~~text
Semantic/Application Model
        |
        v
CNCF View Model -----------------> semantic consumers
        |
        | Display Projection
        v
CNCF Display Model
(instance of Cozy Abstract/Logical UI Model)
        |
        | Display Model Protocol
        v
textus-flutter-core
        |
        v
textus-flutter-application-framework
        |
        | visual realization
        v
Flutter application
~~~

The server determines **what is displayed and what can be acted on**. The client framework determines **how that abstract UI is visually realized**. The protocol must not transmit a Flutter Widget tree.

## Ownership

| Workstream | Owner | Responsibility |
| --- | --- | --- |
| Integration strategy | textus-knowledge-workbench | Overall architecture, dependencies, milestones, status |
| Abstract/Logical UI Model | cozy | Target-neutral List/Detail/Section/Field/Value/Action semantics |
| Semantic View Model | goldenport-cncf | Meaning-preserving read model |
| Display Model / Projection / Protocol | goldenport-cncf | Runtime presentation projection, ExecutionContext projection/validated continuation and transport contract |
| Display client/runtime | textus-flutter-core | Decode/transport, runtime representation, source switching, Presentation Context lifetime/propagation and common client state |
| Visual application UI | textus-flutter-application-framework | Supplied arm/variant-to-definition resolution and reusable List/Detail/Editor etc. visual realization |
| Development driver | nict-editing-studio-app | UI-driven protocol/server-model proposals, declarative configuration/bindings and end-to-end consumer proof |
| Editing Studio server | nict-editing-studio | Concrete application DisplayModel/projection/fixtures, application operations and server integration using CNCF contracts |
| KnowledgeHub | nict-knowledgehub | Canonical Information/knowledge boundary; no ownership of generic UI protocol |

NICT repositories should contain only delivery-relevant contracts and implementation work. Internal Textus cross-project roadmap and coordination remain here.

## Development-driver Phase relationship

| Role | Owning Phase | Contract / acceptance responsibility |
| --- | --- | --- |
| UI/prototype driver | [nict-editing-studio-app Phase 4](../../../../Project2026/nict-editing-studio-app/docs/phase/phase-4.md) | Client provisional implementation, UI-driven protocol/model proposals and feasibility |
| Actual integration parent | [nict-editing-studio-app Phase 5](../../../../Project2026/nict-editing-studio-app/docs/phase/phase-5.md) | Real-server integration and final aggregate/E2E acceptance |
| Server-side development child | [nict-editing-studio Phase 2](../../../../Project2026/nict-editing-studio/docs/phase/phase-2.md) | Concrete application DisplayModel, semantic projection, admitted mutations and reference fixtures |
| Generic server dependency | [goldenport-cncf Phase 96](../../../../dev2025/cloud-native-component-framework/docs/phase/phase-96.md) | DisplayService, projection SPI, standard mutation and versioned protocol |
| Abstract UI dependency | [cozy Phase 74 - Abstract UI Runtime Contract](https://github.com/asami/cozy/blob/main/docs/phase/phase-74.md) | Shared target-neutral runtime vocabulary |
| Client/runtime dependency | textus-flutter-core | Protocol decoding/runtime and common source switching; Phase binding selected after minimum contract freeze |
| Visual framework dependency | textus-flutter-application-framework | Generic visual realization; Phase binding selected for the admitted capability scope |

The overall development driver remains the app. The server child is the
starting point for the concrete server contract needed to build the connected
app: it supplies domain-to-presentation mapping and reproducible fixtures to
CNCF/Cozy, then binds their minimum generic contract. It is not merely a
consumer waiting for all infrastructure to complete.

Handoff preparation is app UI requirements and protocol/server-model proposals
using the server sample -> joint concrete server contract/fixtures ->
Cozy/CNCF minimum generic contract -> server bindings and Core runtime -> TFAF
realization and app E2E acceptance. Each handoff records versions, provenance,
ownership and focused evidence. This integration line uses final-only aggregate
validation in app Phase 5; Phase 4 ends at client prototype feasibility.
Child handoffs do not require full-test runs.
Generic repository Phase acceptance remains independent.

Completed app Phase 2/3 and server Phase 1/1.1 remain closed. App Phase 3 Common
Settings was accepted on 2026-10-02 and retains its separate acceptance owner.
Server Phase 1.2 remains separately blocked on real Workbench/KnowledgeHub
admission contracts. The bounded DisplayService proof uses an existing admitted
resource or an explicitly development-only fixture; it must not invent
production Candidate/admission APIs or claim fixture success as production
acceptance.

The same-numbered local Cozy draft concern was recorded during the 2026-09-29
planning update. On 2026-10-02 the [current local Phase 74](../../../../dev2025/cozy/docs/phase/phase-74.md)
is Abstract UI Runtime Contract, closed for local provisional acceptance.
Actual Android integration and CNCF wire protocol remain separate handoffs.

## UI-driven protocol and generic Presentation Context proposal

The [coordination note](../notes/display-protocol-presentation-context-coordination.md)
and [2026-10-02 journal](../journal/2026/10/2026-10-02-presentation-context-coordination-proposal.md)
record the requested proposal across app, Editing Studio, CNCF, Core and TFAF.
Presentation Context is a bounded projection of CNCF ExecutionContext. Typed,
versioned families support admitted experiment/arm, locale/presentation and
correlation with declared lifetime/propagation/compatibility rules.

Core holds/transports context through read, mutation, direct REST Business
Operation and reload. TFAF uses supplied arm/variant identity to select and
realize registered UI. App screen code remains unaware of experiment logic
and context propagation. Experiment owners retain allocation and evaluation.
Cross-operation continuation is an explicit CNCF admission handoff question.

TKW owns coordination and future real Candidate/review/admission Operation
contracts. Its domain Context/Facets/Relations are distinct from runtime
Presentation Context. The bounded fixture introduces no production admission
API and does not require TKW lifecycle implementation. All new protocol/model
material remains in notes/journal until its owning contracts are reviewed;
no Phase or runtime acceptance is claimed by this strategy recording.

### Client prototype scope revision — 2026-10-02

The user narrowed app Phase 4 to provisional Core/TFAF/app implementation and
fixture-backed feasibility, clarifying that their "Phase 3" wording meant app
Phase 4. Fake transport and Operations explore context/arm presentation and
return protocol/server-model findings before completed server infrastructure.
Phase 5 consumes that prototype and owns real-server connection and final
aggregate/E2E acceptance. Neither prototype success nor this strategy update
closes CNCF/server/dependency Phases or establishes production TKW admission.

## Development tracks

### Track A — Android reference application

Continue nict-editing-studio-app development using mock data. Use concrete List/Detail behavior to discover the minimum abstract UI vocabulary. Do not block Android UI development on the server protocol.

### Track B — Abstract UI and Display Model

Develop in parallel:

1. Cozy minimum target-neutral Abstract/Logical UI Model.
2. CNCF Display Model as runtime instances of that model.
3. CNCF Display Projection from semantic View Models.
4. Versioned Display Model Protocol.

Initial vocabulary should be driven by the reference application's UI requirements
and server Phase 2's concrete projection/fixtures rather than attempting a
complete UI metamodel up front.

### Track C — Flutter integration

After the minimum protocol is stable, connect it through textus-flutter-core. Replace mock sources with CNCF-backed Display Model sources while retaining the same standard application-framework UI.

## Integration milestones

| Milestone | Outcome |
| --- | --- |
| M1 Android Mock UI | List/Detail runs on Android from mock data |
| M2 Abstract UI Minimum | List/Detail/Field/Value/Action minimum semantics are accepted |
| M3 Display Model Protocol | CNCF can project and transport a versioned Display Model |
| M4 Flutter Core Integration | Flutter Core consumes the protocol and exposes runtime Display Models |
| M5 List/Detail End-to-End | Editing Studio List -> Detail works from CNCF to Android without screen rewrite |
| M6 Mutation and Business Round-trip | Standard Display Mutation -> Entity/Aggregate -> refreshed Display; Business Operation -> ordinary REST Operation -> explicit Display reload |

## Primary acceptance criterion

The initial reference scenario is Knowledge Candidate List/Detail, but its
production binding is gated by the real Workbench/KnowledgeHub contracts.
The first executable DisplayService slice therefore selects an existing
admitted server resource or a clearly marked development-only resource fixture:

~~~text
Resource List -> Resource Detail
        -> standard DisplayService update
        -> Entity/Aggregate validation
        -> refreshed Display Model

Resource Detail -> admitted Business Operation via ordinary REST
        -> on success: explicit DisplayService reload
~~~

The application framework must not require Knowledge Candidate semantics to render this standard path. Replacing mock data with CNCF-backed Display Models must not require rewriting the standard List/Detail UI.

Standard create/delete are offered only where admitted by the resource's
capability/constraint metadata. Business Operations are not tunneled through
DisplayService. This bounded acceptance does not close the production
admission path.

## Evolution principle

A server-side metadata/projection change should be able to adapt an existing compatible client to a changed service without application redevelopment when the change remains within the supported Abstract UI and Display Model Protocol.

New visual capabilities such as Gallery, Timeline, Graph, or Conversation extend the Abstract UI vocabulary only when concrete application requirements justify them.

## Status update rule

Keep this file current as the single cross-project status view. Record decision history and rationale in TKW journal entries. Keep implementation detail, checklists, and closure evidence in each owning repository's Phase documents.
