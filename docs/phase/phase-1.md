# Phase 1 — Information Candidate Admission Vertical Slice

## Goal

Establish Textus Knowledge Workbench as the common human formation/admission layer between acquisition applications and canonical KnowledgeHub Information.

## Primary use case

Form, review and admit Information from a raw/prepared Information Candidate.

## Reference flow

Raw/Prepared Candidate -> Register Candidate -> Resolve Evidence/Context -> Edit/Correct -> Semantic Grounding/Mapping -> Human Review -> Approve/Hold/Reject -> Information Admission -> Admission Trace.

## Inputs

Support deterministic fixtures for at least:
- a TKL-originated candidate with PreparedMaterial/Evidence references;
- an Editing-Studio-originated candidate derived from confirmed book-capture raw data.

Full Google Workspace, Slack and smartphone integration is not required for closure.

## Scope

- initial InformationCandidate Entity/lifecycle;
- candidate identity and Evidence/Provenance references;
- candidate content/context editing;
- minimal grounding/mapping using existing CNCF/KnowledgeHub contracts where applicable;
- explicit human review decision and approval;
- KnowledgeHub Information Admission boundary;
- trace link from admitted Information back to Candidate/Evidence;
- CML application use case/workflow model;
- executable specification for the vertical slice.

## Architectural invariants

- mobile Capture Confirm is transfer authorization, not Information Admission;
- AI/provisional editing cannot perform final admission;
- canonical KnowledgeHub knowledge is Information;
- RDF/Open Knowledge is downstream through KnowledgeProjection, not a Phase 1 canonical model;
- TKW owns candidate/admission lifecycle; Editing Studio/TKL provide domain/source interaction.

## Non-goals

TKL evidence federation; Google Workspace/Slack adapters; smartphone capture implementation; publishing-specific Editing Studio UI; agriculture-specific UI; RDF/Open Knowledge publication; automated final approval.

## Completion criteria

Phase 1 is complete when both fixture types enter the same Workbench candidate lifecycle, can be edited/reviewed, explicitly approved by a human, admitted as canonical KnowledgeHub Information, and retain traceability to original evidence/context.
