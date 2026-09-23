# Knowledge Workbench Establishment

- Date: 2026-09-23
- Status: Initial architecture decision

## Background

NICT KnowledgeHub currently has two important acquisition paths.

1. Domain capture applications such as NICT Editing Studio receive raw real-world information from smartphones: book photographs and bibliographic data, user audio comments, annotations, and in other domains daily observation records such as fruit-tree data.
2. Textus Knowledge Lake (TKL) discovers and prepares evidence already distributed across Drive, Gmail, Slack, Web and other providers and proposes raw knowledge candidates.

Both paths require a common step before KnowledgeHub: raw information/candidates must be formed into knowledge, semantically mapped, reviewed and explicitly approved by a human.

Initially this responsibility was effectively placed in Editing Studio. That is too domain-specific because Editing Studio is expected to become a publishing/editorial application.

## Decision

Create **Textus Knowledge Workbench (TKW)** as the reusable intermediate application/component.

```text
Mobile / Domain Capture ---> Editing Studio ---+
                                               |
TKL ---> PreparedMaterial ---> Raw Candidate --+--> TKW
                                                    |
                                               Form / Edit
                                               Ground / Map
                                               Review
                                               Approve
                                                    |
                                                    v
                                               KnowledgeHub
```

## Roles

### TKL — Discover & Prepare

Federated Evidence, Preparation, PreparedMaterial and raw candidate proposal.

### Editing Studio — Domain Interaction

Publishing/editorial capture, validation, views and domain-specific interaction. It operates on TKW-managed candidates through editorial views.

### TKW — Form & Approve

Owns candidate lifecycle and the interactive knowledge-formation workflow. Provides reusable operations to domain applications.

### KnowledgeHub — Store, Link & Process

Provides Semantic Context/Knowledge Processing and stores/processes formed Knowledge.

## Candidate ownership

Candidate state is centralized in TKW. Domain applications should not create parallel candidate lifecycle stores.

A domain application may retain application linkage and view state, but candidate identity, formation state, review and approval belong to TKW.

## Relationship to existing KnowledgeHub design

KnowledgeHub already defines/proposes SemanticGrounding, GroundingCandidate, KnowledgeFormation and KnowledgeFormationProposal.

TKW should use these capabilities/contracts and place the human-facing candidate workflow above them. It should not fork a competing knowledge model.

The exact boundary may evolve as the KnowledgeHub/CNCF contracts become concrete.

## Initial implementation direction

1. Define TKW application use cases.
2. Define KnowledgeCandidate Entity and lifecycle.
3. Define TKL -> TKW candidate submission contract.
4. Define Editing Studio -> TKW capture/candidate interaction contract.
5. Define TKW -> KnowledgeHub formation contract.
6. Define Evidence/Provenance resolution without copying TKL resources.
7. Model use cases and workflow in CML.
8. Build one vertical slice from raw candidate through human approval to KnowledgeHub formation.

## Reference vertical slices

### TKL path

```text
Drive/Gmail/Slack
 -> TKL Preparation
 -> PreparedMaterial
 -> Raw Knowledge Candidate
 -> TKW review/formation
 -> KnowledgeHub
```

### Editing Studio path

```text
Smartphone Book Capture
 -> nict-editing-studio
 -> TKW candidate
 -> editorial view + TKW operations
 -> human approval
 -> KnowledgeHub
```

These two paths should converge on the same Workbench candidate/formation model.
