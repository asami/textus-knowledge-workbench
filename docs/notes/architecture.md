# Textus Knowledge Workbench Architecture

## Purpose

Textus Knowledge Workbench (TKW) is the reusable application/admission layer for turning raw source data and prepared candidates into reviewed, human-approved **KnowledgeHub Information**.

KnowledgeHub's canonical knowledge is managed as Information. TKW therefore does not create a separate canonical Knowledge entity. It manages Information Candidate lifecycle, semantic editing/grounding, review, and explicit Information Admission.

## Position

Acquisition/Discovery -> TKW -> KnowledgeHub Information -> KnowledgeProjection -> RDF/Open Knowledge.

Sources include Textus Knowledge Lake PreparedMaterial/raw candidates and domain applications such as NICT Editing Studio.

## Responsibilities

- manage Information Candidate lifecycle;
- accept raw/prepared candidates from TKL and domain applications;
- retain Evidence/Provenance references;
- edit/correct candidate content and Context;
- support Semantic Grounding and Concept Mapping;
- edit Context, Facets and Relations;
- compare candidates with existing canonical Information;
- support human review, correction, hold and rejection;
- record explicit human approval;
- request Information Admission into KnowledgeHub;
- retain traceability from admitted Information to Candidate and Evidence;
- expose reusable operations/views to domain applications.

## Non-responsibilities

- raw resource federation/evidence discovery: TKL;
- provider-specific Drive/Gmail/Slack preparation: TKL/provider adapters;
- smartphone capture and Capture Confirm: domain mobile applications;
- publishing/editorial domain semantics: NICT Editing Studio;
- final canonical Information storage/runtime/search/processing: KnowledgeHub;
- RDF/Open Knowledge canonicalization: RDF is a downstream KnowledgeProjection.

## Candidate ownership

Information Candidate state is centralized in TKW. Editing Studio and TKL must not create parallel admission lifecycles. Domain applications may retain linkage/view state and provide domain-specific views over TKW-managed candidates.

Lifecycle: Proposed -> Draft -> Editing -> Review -> Approved -> Admission Requested -> Admitted, with Held/Rejected review outcomes.

## Editing Studio integration

Smartphone Book Capture -> nict-editing-studio raw-source registration -> TKW Information Candidate -> editorial view + TKW operations -> human approval -> KnowledgeHub Information.

Mobile Capture Confirm only authorizes transfer. TKW human approval is the separate Information Admission decision.

## TKL integration

Drive/Gmail/Slack/Web -> TKL -> PreparedMaterial/raw candidate -> TKW.

PreparedMaterial and resource IDs preserve provenance. TKW does not duplicate TKL resource federation.

## KnowledgeHub integration

TKW is the interactive Information Admission layer in front of KnowledgeHub. Existing SemanticContext, SemanticGrounding, GroundingCandidate and related KnowledgeHub/CNCF capabilities should be reused where semantics match, while older KnowledgeFormation terminology should be mapped to the current Information Admission model rather than introducing a second canonical layer.

KnowledgeHub owns canonical Information. Downstream KnowledgeProjection can project Information into RDF/Open Knowledge.

## Primary use case: Form and admit Information

A Knowledge Worker receives a raw/prepared candidate, resolves Evidence/Context, edits and semantically maps it, reviews it, and explicitly approves it for Information Admission.

Flow: Register Candidate -> Resolve Evidence/Context -> Edit/Correct -> Ground/Map Concepts/Facets/Relations -> Human Review -> Approve/Hold/Reject -> Request Information Admission -> retain Admission Trace.

AI proposal is never identical to admitted Information.

## Feedback loop

TKW retains Candidate -> admitted Information identity/version -> Evidence trace. It exposes admission results so TKL can refresh Existing Knowledge Context and update/rebuild KnowledgeProjection context. TKW does not own KnowledgeProjection storage or compaction.
