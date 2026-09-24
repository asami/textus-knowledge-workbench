# Knowledge Workbench Establishment

- Date: 2026-09-23
- Updated: 2026-09-25
- Status: Active architecture decision

## Background

KnowledgeHub has multiple acquisition paths. Domain applications such as NICT Editing Studio receive raw real-world material from smartphones. Textus Knowledge Lake discovers and prepares evidence from Drive, Gmail, Slack, Web and other providers.

Both paths need a common human-governed step before canonical KnowledgeHub Information: candidates must be edited, semantically mapped, reviewed and explicitly approved.

Initially this responsibility was effectively placed in Editing Studio. That is too domain-specific because Editing Studio is a publishing/editorial application.

## Decision

Textus Knowledge Workbench (TKW) is the reusable intermediate application/component for Information Candidate formation and admission.

Flow: Mobile/Domain Capture or TKL PreparedMaterial -> TKW -> edit/ground/map/review/approve -> KnowledgeHub Information -> KnowledgeProjection -> RDF/Open Knowledge.

## Roles

### TKL — Discover & Prepare
Federated Evidence, Preparation, PreparedMaterial and raw/prepared candidate proposal.

### Editing Studio — Domain Interaction
Publishing/editorial capture, raw-source registration, domain validation/views and Book-specific interaction. It presents editorial views over TKW-managed candidates.

### TKW — Form & Admit Information
Owns Information Candidate lifecycle and interactive formation/review/approval. Human approval is the Information Admission boundary.

### KnowledgeHub — Canonical Information
Stores, links and processes canonical Information. KnowledgeHub knowledge is managed as Information.

### KnowledgeProjection / Open Knowledge
Projects canonical Information to external representations such as RDF. RDF is not KnowledgeHub's canonical internal model.

## Candidate ownership

Candidate state is centralized in TKW. Domain applications and TKL do not create parallel admission lifecycle stores. They may retain source/application linkage and view state.

## Human boundaries

Mobile Capture Confirm means permission to transfer a capture entry to the server. It is not Information Admission.

AI may provisionally edit/structure candidates, but final Information Admission requires explicit human approval in the Workbench workflow.

## Initial implementation direction

1. Define TKW application use cases.
2. Define InformationCandidate Entity/lifecycle.
3. Define TKL -> TKW candidate submission contract.
4. Define Editing Studio -> TKW raw-capture/candidate interaction contract.
5. Define TKW -> KnowledgeHub Information Admission contract.
6. Define Evidence/Provenance resolution without copying TKL resources.
7. Model use cases/workflow in CML.
8. Build one vertical slice through human approval to canonical Information.

## Reference vertical slices

TKL: Drive/Gmail/Slack -> TKL Preparation -> PreparedMaterial/raw candidate -> TKW review/admission -> KnowledgeHub Information.

Editing Studio: Smartphone Book Capture -> Capture Confirm -> nict-editing-studio raw source -> TKW candidate -> editorial view + TKW operations -> human approval -> KnowledgeHub Information.

Both paths converge on the same Workbench Information Candidate/admission model.
