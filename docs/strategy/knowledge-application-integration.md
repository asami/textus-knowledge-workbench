# Knowledge Application Integration Strategy

status=active
owner=textus-knowledge-workbench
updated_at=2026-09-29

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
| Display Model / Projection / Protocol | goldenport-cncf | Runtime presentation projection and transport contract |
| Display client/runtime | textus-flutter-core | Decode, runtime representation, source switching and common client state |
| Visual application UI | textus-flutter-application-framework | Reusable List/Detail etc. visual realization |
| Development driver | nict-editing-studio-app | Android-first mock implementation and later end-to-end consumer proof |
| Editing Studio server | nict-editing-studio | Application operations and server integration using CNCF contracts |
| KnowledgeHub | nict-knowledgehub | Canonical Information/knowledge boundary; no ownership of generic UI protocol |

NICT repositories should contain only delivery-relevant contracts and implementation work. Internal Textus cross-project roadmap and coordination remain here.

## Development tracks

### Track A — Android reference application

Continue nict-editing-studio-app development using mock data. Use concrete List/Detail behavior to discover the minimum abstract UI vocabulary. Do not block Android UI development on the server protocol.

### Track B — Abstract UI and Display Model

Develop in parallel:

1. Cozy minimum target-neutral Abstract/Logical UI Model.
2. CNCF Display Model as runtime instances of that model.
3. CNCF Display Projection from semantic View Models.
4. Versioned Display Model Protocol.

Initial vocabulary should be driven by the reference application rather than attempting a complete UI metamodel up front.

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
| M6 Action Round-trip | Display Action -> CNCF Operation/Command -> result/view refresh completes a round trip |

## Primary acceptance criterion

The first vertical slice is:

~~~text
Knowledge Candidate List
        -> Knowledge Candidate Detail
        -> one Display Action
        -> CNCF Operation/Command
        -> refreshed View/Display Model
~~~

The application framework must not require Knowledge Candidate semantics to render this standard path. Replacing mock data with CNCF-backed Display Models must not require rewriting the standard List/Detail UI.

## Evolution principle

A server-side metadata/projection change should be able to adapt an existing compatible client to a changed service without application redevelopment when the change remains within the supported Abstract UI and Display Model Protocol.

New visual capabilities such as Gallery, Timeline, Graph, or Conversation extend the Abstract UI vocabulary only when concrete application requirements justify them.

## Status update rule

Keep this file current as the single cross-project status view. Record decision history and rationale in TKW journal entries. Keep implementation detail, checklists, and closure evidence in each owning repository's Phase documents.
