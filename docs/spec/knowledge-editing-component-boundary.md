# Shared knowledge editing component boundary

Status: agreed direction; connected implementation pending
Decision date: 2026-10-21

## Purpose

Workbench is the reusable knowledge-formation component for multiple editing
applications. Editing Studio (編集工房) is a bibliographic knowledge editing
application; other applications may provide different bibliographic views or
edit other domains. They share Workbench work/Candidate state rather than
implementing independent formation and admission lifecycles.

## Public work surface

Domain applications use Workbench for work-oriented operations: find/select
supporting Material, inspect its metadata/originals in a work context, create
and update a Candidate, inspect evidence, request review and record human
decisions. Workbench owns Candidate identity, associations to evidence,
work/review state and requests for KnowledgeHub admission. Editor-local drafts
and presentation state are allowed; they are not another canonical Candidate
or approval store. Applications retain domain-specific semantics and UI.

Existing Information editing must use the owning KnowledgeHub update contract;
do not invent a second Information store or assume Candidate admission also
updates an existing Information. Bind that capability when its use case is
implemented, without blocking the first Material reference route.

## Knowledge Lake access

Workbench links/composes TKL and invokes its public Material Operations through
CNCF, preserving caller execution context and authorization. Initially these
components may run in the same host; REST or independent servers are optional
deployment choices, not required logical boundaries.

Workbench owns the work-specific selection/index and Candidate-to-evidence
relationship. TKL owns global Material identity, URNs, metadata, raw/prepared
placement and provider correspondence. Workbench must not parse URNs into Drive
paths, maintain a shadow Lake registry or invoke TKL private provider classes.

```text
Domain editor -> Workbench work operation -> TKL Material operation -> storage
                         |
                         +-> Candidate/review -> KnowledgeHub admission
```

The normal editor route is through Workbench. This is not a generic proxy that
mirrors every TKL operation: it exposes the Material operations needed for the
user's knowledge-editing work. Other independent TKL clients are not required
to pass through Workbench.

TKL may supply physical transfer/access information. Workbench may pass it to
an editor for large-byte transfer without making Workbench a mandatory byte
proxy. Material identity and registration remain TKL-owned. Reuse the public
contract in [TKL](../../../textus-knowledge-lake/docs/spec/material-access-and-processing.md).

## Processing distinction

TKL also supports OpenClaw registration through the continuation protocol.
Ordinary Workbench Material queries do not start continuation or require a
running OpenClaw worker. When work explicitly requests preparation, delegate
through TKL's processing contract and retain the returned correlation/result
reference. Processing completion does not automatically approve a Candidate.

## Acceptance

- A bibliographic editor searches/selects a Material through Workbench, views
  the original, and retains the logical evidence association without Drive
  folders or physical naming in its work contract.
- A second domain-neutral consumer fixture uses the same work operations and
  state model; no second application implementation is required for this proof.
- Accepted Candidate registration and human-edit preservation remain intact;
  edits/review/admission continue under the existing owning Phase contracts.
- TKL unavailable/unauthorized/unfinished results remain visible; Workbench
  does not substitute a direct Drive implementation.

Exact operation selectors and DTOs must be bound from public component contracts
during implementation. These are requirements, not a claim that live APIs exist.
