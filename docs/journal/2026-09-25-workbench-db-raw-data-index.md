# Workbench DB and Raw-Data Index

Date: 2026-09-25
Status: active

Textus Knowledge Workbench owns a persistent DB for Information Candidate lifecycle and candidate-centric source indexing.

TKW DB stores management metadata, not large raw file bodies. Core records include InformationCandidate, CandidateSource, RawDataIndex, Review/Approval/Admission state, and provenance/trace links.

RawDataIndex identifies which raw Knowledge Lake resources support a candidate. It should carry stable references such as rawDataId, candidateId, source kind/media type, TKL/Knowledge Lake resource ID, checksum where useful, origin/capture/submission metadata, preparation state and provenance references.

Physical photographs, audio, documents and other raw/derived files live in Google Workspace Knowledge Lake. TKL owns Knowledge Lake Resource/Evidence/Preparation/PreparedMaterial management. TKW owns the candidate-centric index and human work/admission state.

Two TKL paths converge on TKW:
- Discovery Preparation: Native Raw Evidence -> TKL -> PreparedMaterial -> Information Candidate/TKW.
- Candidate Preparation: TKW Information Candidate + RawDataIndex -> Google Workspace/TKL preparation -> PreparedMaterial -> same TKW candidate.

For mobile book capture, Capture Confirm creates/updates candidate source metadata and authorizes transfer. Raw material is stored in Google Workspace, indexed by TKW, prepared by TKL, then returned to TKW for human review and Information Admission.

This makes TKW the operational Workbench: it can show source-storage state, preparation state, review readiness and admission state without storing the raw media itself.
