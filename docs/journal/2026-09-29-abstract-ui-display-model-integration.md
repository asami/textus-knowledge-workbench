# Abstract UI and Display Model Integration Direction

Date: 2026-09-29
Status: architectural decision record

## Context

The Android-oriented nict-editing-studio-app is progressing with mock data while Textus needs a reusable path from CNCF server views to Flutter UI. The discussion clarified that semantic integration consumers and visual clients have different needs.

## Decisions

1. CNCF View Model remains a meaning-preserving semantic read model.
2. Display Model is separate from View Model.
3. Display Model is a runtime instance of the target-neutral Abstract/Logical UI Model owned by Cozy.
4. Display Projection may rename semantic attributes into presentation roles, for example product_name -> title.
5. Server-native or opaque structures such as JSON are converted into displayable values before crossing the Display Model boundary.
6. A visual client needs to know what to display and what actions are available; it need not understand the domain meaning behind those presentation roles.
7. Display Model Protocol is the CNCF/client runtime contract. It carries abstract presentation semantics, not Flutter Widget trees.
8. textus-flutter-core owns the protocol client/runtime boundary; textus-flutter-application-framework owns reusable visual realization.
9. nict-editing-studio-app remains the Android-first development driver using mock data while Abstract UI + Display Model work proceeds in parallel.
10. The integration proof replaces mock data with CNCF-backed Display Models without rewriting standard List/Detail UI.

## Coordination

TKW acts as the control tower for knowledge-processing application integration across smartphone and server. The canonical current plan is docs/strategy/knowledge-application-integration.md. Repository Phase documents own implementation; journal records preserve rationale and history.

NICT repositories may be delivery artifacts, so internal Textus cross-project strategy is not duplicated into them. They receive only delivery-relevant contracts, dependencies, and implementation requirements.
