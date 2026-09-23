# Textus Knowledge Workbench Architecture

## Purpose

Textus Knowledge Workbench (TKW) is the reusable application layer for turning raw knowledge candidates and captured information into reviewed, formed and approved knowledge proposals.

It separates generic knowledge formation/curation interaction from domain applications such as NICT Editing Studio and from the KnowledgeHub runtime.

## Position

```text
Acquisition / Discovery
────────────────────────────────────
Textus Knowledge Lake        Domain Applications
(TKL)                        Editing Studio / others
  Discover / Prepare           Capture / Domain View
        |                            |
        +-------------+--------------+
                      v
            Textus Knowledge Workbench
            Form / Edit / Review / Approve
                      |
                      v
                 KnowledgeHub
            Store / Link / Process
```

TKW is not a storage lake and is not the final Knowledge repository.

## Responsibilities

- manage Knowledge Candidate lifecycle;
- accept raw candidates from TKL and domain applications;
- retain Evidence and Provenance references;
- provide candidate editing/correction;
- resolve/use Semantic Context;
- support Semantic Grounding and Concept Mapping;
- edit Context, Facets and Relations;
- compare candidates with existing Knowledge;
- create KnowledgeFormationProposal;
- support human review, correction, hold and rejection;
- record human approval;
- invoke KnowledgeHub Knowledge Formation;
- retain traceability from formed Knowledge back to Candidate and Evidence;
- expose reusable operations/views to domain applications.

## Non-responsibilities

- raw resource federation and evidence discovery: TKL;
- provider-specific Drive/Gmail/Slack preparation: TKL/provider adapters;
- publishing/editorial domain semantics: NICT Editing Studio;
- agriculture-specific semantics: agriculture application;
- canonical Information/Knowledge primitives: CNCF;
- final Knowledge storage/runtime/search/processing: KnowledgeHub.

## Candidate ownership

Knowledge Candidates are managed by TKW, not by Editing Studio or TKL.

```text
TKL
 PreparedMaterial
      |
 Raw Knowledge Candidate
      |
      v
     TKW
 Candidate Entity / lifecycle
      |
 KnowledgeFormationProposal
      |
 Human Approval
      |
      v
 KnowledgeHub
```

Domain applications present their own views of the Workbench-managed candidate and call TKW operations.

## Editing Studio integration

```text
Editing Studio Smartphone App
  -> nict-editing-studio
       - book photos
       - bibliographic data
       - edition/printing
       - audio comments
       - annotations
       - editorial context
  -> TKW
       - candidate management
       - knowledge formation editing
       - review / approval
  -> KnowledgeHub
```

Editing Studio remains a publishing/editorial application. It provides an editorial view and interaction model over TKW candidates.

## TKL integration

TKL performs evidence discovery and preparation.

```text
Drive / Gmail / Slack / Web
       |
      TKL
       |
 PreparedMaterial
       |
 Raw Knowledge Candidate
       |
      TKW
```

PreparedMaterial and TKL resource IDs preserve provenance to canonical Evidence. TKW should not duplicate TKL's resource federation.

## KnowledgeHub integration

TKW is the interactive formation/admission layer in front of KnowledgeHub.

Existing KnowledgeHub concepts such as:

- SemanticContext
- SemanticGrounding
- GroundingCandidate
- KnowledgeFormation
- KnowledgeFormationProposal

should be reused/aligned rather than duplicated.

KnowledgeHub remains responsible for Knowledge processing/runtime and canonical Knowledge formation/storage.

## Initial candidate lifecycle

Provisional lifecycle:

```text
Proposed
   |
   v
Draft
   |
   v
Editing
   |
   v
Review
   +----> Held
   +----> Rejected
   |
   v
Approved
   |
   v
Formation Requested
   |
   v
Admitted
```

Exact state-machine semantics should be defined in CML/CNCF Workflow after reviewing current Workflow support.

## Initial domain concepts

- KnowledgeCandidate
- KnowledgeCandidateId
- CandidateSource
- CandidateContent
- CandidateContext
- EvidenceReference
- ProvenanceReference
- Grounding / Mapping
- CandidateRelation
- CandidateFacet
- KnowledgeFormationProposal
- ReviewDecision
- Approval
- FormationLink

Canonical CNCF/KnowledgeHub types should be reused where their semantics match.

## Primary application use case

### Form and approve knowledge

Goal:

A Knowledge Worker receives a raw candidate from TKL or a domain application, examines its Evidence and Context, edits/grounds/maps it into a Knowledge Formation Proposal, and explicitly approves it for KnowledgeHub formation.

Main scenario:

1. Receive/register raw candidate.
2. Resolve Evidence and existing Knowledge references.
3. Present candidate through a domain or generic view.
4. Edit/correct content and Context.
5. Ground/map Concepts, Facets and Relations.
6. Compare with existing Knowledge where relevant.
7. Compose KnowledgeFormationProposal.
8. Human reviews the proposal.
9. Human accepts, corrects, holds or rejects.
10. Approved proposal is submitted to KnowledgeHub Knowledge Formation.
11. Formation result and trace link are retained.

## Design principle

AI proposal is never identical to accepted Knowledge.

```text
AI / TKL / Capture
       -> Candidate
       -> Human Formation / Review
       -> Explicit Approval
       -> KnowledgeHub Knowledge
```

This boundary is the central purpose of TKW.
