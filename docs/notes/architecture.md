# Textus Knowledge Workbench Architecture

## Purpose

Textus Knowledge Workbench (TKW) is the reusable application/admission layer for registering raw/prepared proposals as its own Information Candidates and forming reviewed, human-approved **KnowledgeHub Information**.

KnowledgeHub's canonical knowledge is managed as Information. TKW therefore does not create a separate canonical Knowledge entity. It manages Information Candidate lifecycle, semantic editing/grounding, review, and explicit Information Admission.

## Position

Acquisition/Discovery -> TKW -> KnowledgeHub Information -> KnowledgeProjection -> RDF/Open Knowledge.

Sources include Textus Knowledge Lake RawCandidateProposal with PreparedMaterial/Evidence references and domain application submissions such as NICT Editing Studio confirmed capture.

## Responsibilities

- manage Information Candidate lifecycle;
- register raw/prepared proposals from TKL and domain applications as TKW Candidates;
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

A producer proposal is input to registration, not a TKW InformationCandidate.
TKW creates and owns the Candidate identity. The first registration path keeps
producerRef + proposalId correspondence and returns the existing Candidate on
sequential resend without reapplying content over human edits.

Lifecycle: Proposed -> Draft -> Editing -> Review -> Approved -> Admission Requested -> Admitted, with Held/Rejected review outcomes.

Approval records the content and relevant evidence actually reviewed, actor,
time and reason. Changes to that target require renewed review/approval.
Entity update control uses CNCF revision/concurrency facilities; an operation
advancing the Entity revision does not itself redefine the approved target.
Hold/Reject resumption and evidence eligibility are decisions in Phase 1 work
item 3; Admission failure handling belongs to [Phase 1.1](../phase/phase-1.1.md),
which owns former work item 5 after the 2026-10-02 planning split. Phase 1 owns
Candidate formation and approval; Phase 1.1 owns actual KnowledgeHub Admission
and its trace. The complete lifecycle above spans both Phases.

## Candidate sources and supporting references

CandidateSource identifies the logical origin and correspondence of a Candidate,
such as a TKL proposal or Editing Studio capture submission. RawDataIndex
represents the Candidate's associations with supporting materials. These are
distinct responsibilities; the minimum implementation may use Candidate
metadata or small related records without separate Aggregates or tables.

Supporting references use provider-neutral TKL Resource/Evidence identities and
their available version/snapshot/provenance references. PreparedMaterial and
ProcessingRun references are retained when present. An unprepared capture may
enter with its available source references and explicit preparation limitations;
TKL-prepared material and Run references are not common mandatory input fields.

TKL owns KnowledgeProcessingContext, ProcessingRun and PreparedMaterial.
TKW retains their references to explain a Candidate and does not duplicate
preparation execution history or raw file bodies. TKW domain Context and
runtime Presentation Context remain separate from TKL processing Context.
Google Workspace/Drive is the initial TKL backend, not the definition of these
domain concepts. TKW does not introduce hashes as a default identity,
deduplication, concurrency or approval mechanism.

## Editing Studio integration

Smartphone Book Capture -> nict-editing-studio raw-source registration -> source proposal/submission -> TKW registration -> InformationCandidate -> editorial view + TKW operations -> human approval -> KnowledgeHub Information.

Mobile Capture Confirm only authorizes transfer. TKW human approval is the separate Information Admission decision.

## TKL integration

Drive/Gmail/Slack/Web -> TKL -> PreparedMaterial -> RawCandidateProposal -> TKW registration -> InformationCandidate.

PreparedMaterial and resource IDs preserve provenance. TKW does not duplicate TKL resource federation.

Existing-Candidate preparation remains follow-on work: TKW Candidate/source
references -> correlated preparation request -> TKL Run/PreparedMaterial ->
result linked to the same Candidate. Keep requestId and Candidate reference
correspondence in the design; full request/result DTOs and execution are not
prerequisites for the first slice. Returned material does not automatically
overwrite content or renew approval.

## KnowledgeHub integration

TKW is the interactive Information Admission layer in front of KnowledgeHub. Existing SemanticContext, SemanticGrounding, GroundingCandidate and related KnowledgeHub/CNCF capabilities should be reused where semantics match, while older KnowledgeFormation terminology should be mapped to the current Information Admission model rather than introducing a second canonical layer.

KnowledgeHub owns canonical Information. Downstream KnowledgeProjection can project Information into RDF/Open Knowledge.

## Primary use case: Form and admit Information

A Knowledge Worker receives a registered TKW Candidate derived from a raw/prepared proposal, resolves Evidence/Context, edits and semantically maps it, reviews it, and explicitly approves it for Information Admission.

Flow: Register Proposal -> TKW InformationCandidate -> Resolve Evidence/Context -> Edit/Correct -> Ground/Map Concepts/Facets/Relations -> Human Review -> Approve/Hold/Reject -> Request Information Admission -> retain Admission Trace.

AI proposal is never identical to admitted Information.

## Feedback loop

TKW retains Candidate -> admitted Information identity/version -> Evidence trace. It exposes admission results so TKL can refresh Existing Knowledge Context and update/rebuild KnowledgeProjection context. TKW does not own KnowledgeProjection storage or compaction.
