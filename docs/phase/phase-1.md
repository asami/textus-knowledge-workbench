# Phase 1 — Knowledge Candidate Formation Vertical Slice

## Goal

Establish Textus Knowledge Workbench as the common formation/approval layer between acquisition applications and KnowledgeHub.

## Primary use case

**Form and approve knowledge from a raw knowledge candidate.**

## Reference flow

```text
Raw Knowledge Candidate
      |
      v
Register Candidate
      |
      v
Resolve Evidence / Context
      |
      v
Edit / Correct
      |
      v
Semantic Grounding / Mapping
      |
      v
KnowledgeFormationProposal
      |
      v
Human Review
      |
      +--> Hold / Reject
      |
      v
Approve
      |
      v
KnowledgeHub Knowledge Formation
      |
      v
Formation Trace
```

## Inputs

Phase 1 should support deterministic fixtures for at least:

- a TKL-originated raw candidate with PreparedMaterial/Evidence references;
- an Editing-Studio-originated candidate derived from captured book information.

Full Google Workspace, Slack and smartphone integration is not required for closure.

## Scope

- initial KnowledgeCandidate Entity;
- candidate identity and lifecycle;
- Evidence/Provenance references;
- candidate content/context editing;
- minimal grounding/mapping representation using existing CNCF/KnowledgeHub contracts where available;
- KnowledgeFormationProposal;
- explicit human review decision;
- approval and KnowledgeHub formation boundary;
- trace link from formed Knowledge back to Candidate/Evidence;
- CML application use case/workflow model;
- executable specification for the vertical slice.

## Non-goals

- TKL evidence federation;
- Google Workspace/Slack adapters;
- smartphone capture implementation;
- publishing-specific Editing Studio UI;
- agriculture-specific UI;
- advanced KnowledgeHub distillation/federation;
- automated final approval.

## Completion criteria

Phase 1 is complete when both a TKL-like raw candidate fixture and an Editing-Studio-like capture candidate fixture can enter the same Workbench candidate lifecycle, be reviewed and approved, cross the KnowledgeHub formation boundary, and retain traceability to their original evidence/context.
