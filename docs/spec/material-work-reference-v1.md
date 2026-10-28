# Material work reference contract v1

Status: fixed-forward implementation consumed by accepted Phase 2 Steps through P2-04 `166f10c5`; connected receipt 659 and final full receipt 672 passed; closing review/native closure/local release pending
Contract date: 2026-10-21
Owner: TKW K01/K02

This contract specializes [the shared editing boundary](knowledge-editing-component-boundary.md).
The component identity remains `org.simplemodeling.textus.KnowledgeWorkbench`.
Service `MaterialWork` exposes authenticated-only QUERY Operations:

| Operation | Input | Public producer Operation | Result |
| --- | --- | --- | --- |
| `searchMaterials` | `MaterialSearch(text?, offset?, limit?)` | KnowledgeLake / Material / searchMaterials | TKL MaterialSearchResult |
| `inspectMaterial` | `MaterialSelection(materialRef)` | KnowledgeLake / Material / getMaterial | TKL MaterialMetadata |
| `readOriginal` | `OriginalSelection(materialRef, resourceId)` | KnowledgeLake / Material / readOriginal | Admitted original binary response |

TKL owns these logical Material/resource value semantics. Consumer bindings
must use its public schema without copying physical provider DTOs. These
selectors are the intended v1 contract, not evidence of generated/live APIs.
Bind actual artifacts and ABI through the normal producer build before acceptance.

Phase 2 binds explicit Candidate creation in
[Material-backed Candidate work v1](material-candidate-work-v1.md). Its authored
command is separate from these queries; its implementation and connected Step
acceptance are complete through P2-04. The logical Material/resource selection is retained in the existing
Candidate's first proposal, without a second Material registry.

For this first slice, searching/inspecting/reading may precede Candidate creation
and performs no work/Candidate mutation. It must not create dummy Candidates to
display an original. Accepted K03 binds selection/evidence to Workbench-owned
Candidate identity and revision; an editor-local workspace is not a competing
Candidate or Information store. A read result alone is not a persisted selection.

Dispatch preserves the caller's CNCF execution/security/observability context
and uses public Operations, never TKL implementation classes. Workbench owns
the application-facing work service but does not own a second Material index,
URN grammar, Drive credentials or provider fallback. It preserves logical
references and TKL structured failures through to the editor. The initial
result passes the admitted original bytes without another byte store.

The original route requires neither OpenClaw nor completion of STEP-3/4 or
KnowledgeHub admission. A second domain-neutral consumer fixture must use the
same selectors without ISBN, Book fields, Studio Entity IDs or Google IDs.

OAuth management stays with the TKL/CNCF owning component. A Studio consent
action can target that existing management surface; Workbench does not mint a
grant or forward tokens. If host authentication cannot admit the same caller
to the target component, report that concrete capability gap rather than
using a privileged identity or duplicating credential custody.

## Explicit initial Material registration work

Connected ESS3-02 / TKL-M03 adds three fixed Operations to the same
`MaterialWork` service for work-assisted initial Material registration. The
three reference queries above remain a separate read-only route. These work
Operations are not an arbitrary-target gateway or a mirror of future TKL
capabilities. They introduce no alternate Candidate/Information lifecycle,
Material index, registration database, grant authority or retry manager in
Workbench. TKL owns registration identity; it is not Candidate identity. A
registered Material is neither reviewed/approved Knowledge nor a persisted
Candidate evidence selection.

The owning public contract is
[TKL Material registration Operations v1](../../../textus-knowledge-lake/docs/spec/material-registration-operations-v1.md).
All three work Operations use `authenticated_only` access and the fixed
component `org.simplemodeling.textus.KnowledgeLake`, service `Material`:

| Workbench Operation | Kind | Inline input value and exact attributes | Fixed producer Operation | Output |
| --- | --- | --- | --- | --- |
| `createMaterialRegistration` | COMMAND | `MaterialRegistrationCreateCommand EXTENDS CommandAction`: `contribution: json`, multiplicity `1` | `createMaterialRegistration` | `MaterialRegistrationResult` |
| `getMaterialRegistration` | QUERY | `MaterialRegistrationSelectionQuery EXTENDS QueryAction`: `registrationId: string`, multiplicity `1` | `getMaterialRegistration` | `MaterialRegistrationResult` |
| `materializeMaterial` | COMMAND | `MaterialRegistrationMaterializeCommand EXTENDS CommandAction`: `registrationId: string`, multiplicity `1` | `materializeMaterial` | `MaterialRegistrationResult` |

### Neutral contribution and opaque selectors

`contribution` is one complete neutral `textus.material-contribution/1` JSON
value. Workbench must forward that JSON unchanged, including opaque documents,
source references, provenance, histories, array order and Unicode. It must not
flatten the contribution, convert it to a JSON string, parse App receipts or
provider mappings, or drop fields it does not interpret. Studio constructs the
neutral contribution after its own source authorization. Domain conversion
belongs to Studio, and source/provider interpretation belongs to the owning TKL
adapter.

The create input contains exactly `contribution`. Neither its work DTO nor the
two selection DTOs add typed Book/ISBN, `sourceEntityId`, owner, URN, lake,
folder, connection, provider or operation-target properties. Opaque content
inside the neutral contribution remains intact; its preservation does not
grant it authority. A second neutral consumer uses the same vocabulary without
Book coupling.

The selection inputs contain exactly `registrationId`. Workbench passes the
native-issued ID exactly and treats it as opaque. It must never derive,
normalize or parse it into a provider key, Material URN or App/business key.
There are no polling, resume or bulk parameters.

### Logical result and availability

The global value `MaterialRegistrationResult` declares exactly the owning TKL
public logical schema:

| Attribute | Type | Multiplicity | Meaning |
| --- | --- | --- | --- |
| `registrationId` | `string` | `1` | Actual native-issued TKL registration ID |
| `revision` | `long` | `1` | Positive observed revision of the actual committed native snapshot |
| `state` | `string` | `1` | Actual saved state: `prepared`, `materializing` or `registered` |
| `materialRef` | `string` | `1` | Assigned planned TKL Material reference, including before availability |

At runtime Workbench must pass the producer `OperationResponse` unchanged,
without reserialization, flattening or local projection. Native default response
JSON therefore retains `registration_id` and `material_ref`; request names
remain `registrationId` and `contribution`. The result contains logical status,
not a physical mapping, contribution, receipt or grant. Workbench must not
synthesize a revision, state, receipt or successful result.

A planned `materialRef` in Prepared or Materializing does not promise that the
Material is available. Only Registered enters ordinary search/get/read. After
observing `registered`, consumers use the existing metadata/original route.
The registration work does not create a Candidate, approve Knowledge or persist
an evidence association.

### Same-caller admission and failure preservation

Dispatch must use the existing fixed-request
`executeOperationResponseInChildContext` route and preserve the actual caller's
CNCF execution, security and observability context. Workbench's
authenticated-only Operation gate is independent of TKL's producer mode,
configuration, allowed-subject admission, native Entity create/read/write
authorization and managed custody/provider authorization. Those remain TKL
responsibilities; Workbench cannot substitute a privileged identity or copy a
grant to bridge admission.

Creation acknowledges Prepared only after native durable acknowledgement.
Materialization requires an acknowledged durable Materializing transition
before physical effects. These are owning TKL obligations, not a distributed
transaction implemented by Workbench. Unknown native acknowledgements, provider
failures and final-save failures must preserve the producer's exact structured
Conclusions and actual status. Workbench must not wrap failures as empty
success, substitute direct Drive/provider access, perform an extra write or
automatically retry, reset or delete output.

An unknown creation outcome remains unresolved; there is no App-key
deduplication or automatic create replay. Explicit `getMaterialRegistration`
reads the actual saved state without provider access or writes. Its result
does not authorize retry. A failed materialization may retain Materializing
and remote bytes; an uncertain final acknowledgement may leave Registered.
Workbench preserves that uncertainty and must never replay physical effects
based on a guessed state.

### Owning configuration and independent reads

Host source, destination, lake, connection and storage/read bindings remain
private owning TKL configuration. Registered-read binding is separately enabled
by that owner; Workbench neither installs nor infers it. Workbench needs no new
setting, credential or runtime mode switch for these fixed forwards.

Existing bootstrap/default reads, reference DTOs and selectors remain unchanged.
Original binary headers and the borrowed Bag remain unchanged. Ordinary reads
start no registration, continuation, OpenClaw processing or Candidate work and
remain available independently of the new commands and their configuration.
Unavailable registration or unfinished materialization does not change the
existing read route's admission or availability semantics.

### Declaration and connected delivery boundary

Normal Workbench `cozyGenerate` produced 51 sources; the actual generated
registration DTOs, actions and factory APIs were inspected before authoring
the adapter. The three generated factory hooks now select handwritten
FunctionalActionCalls through the existing fixed-request child-context route.
Create uses the generated action's typed `io.circe.Json` contribution directly.
Status and materialization decode their respective generated values and pass
only the exact `registrationId`. Every call returns the producer response or
Conclusion without local state, projection or replay.

[MaterialWorkRegistrationSpec](../../src/test/scala/org/simplemodeling/textus/knowledgeworkbench/MaterialWorkRegistrationSpec.scala)
authors generated/public dispatch, command/query kinds, complete nested JSON,
deterministic Unicode edges and at least 100 undiscarded bounded arbitrary
UTF16 contributions, opaque selectors, identical responses/Conclusions,
same-caller context, native input/admission and unavailable-route behavior.
It uses the ordinary fixture's public subsystem dispatch and controlled
producer responses. Those responses are inputs to a forwarding specification,
not a simulated TKL persistence lifecycle or evidence of cloud registration.
The existing reference-read, factory and Candidate specifications retain their
own scope and semantics.

Workbench forwarding coverage is retained by final full receipt 672: 71 tests
in nine suites including PostgreSQL 17.11. Accepted P2-04 at
`166f10c5b95a800a09f457003891ac77a5f72211` adds actual TKL/Workbench connected
receipt 659: 19 tests/three suites, real JPEG inspection, one persistent Candidate,
same-ID Edit/Hold, source navigation and PostgreSQL restart/readback. Complete
1,535,406-byte equality was observed at native Bag/HTTP boundaries before/after
work, and the original was read again after restart. The admitted caller is the
fixture service `editing-studio-mobile-development`, not real end-user OAuth.
Source evidence stays unverified without revision/snapshot; controlled approval
receipt 484 remains separate.

The original registration/Studio planning obligations retain their historical
scope; producer coverage alone does not establish consumer acceptance. K03 and
the Workbench-owned neutral-consumer/failure portion of K04 are delivered by
Phase 2; actual Studio Candidate integration/full K04 remain held with Studio
Phase 1.2. The first whole-Phase review requested documentation reconciliation.
This source repair awaits closing review and native closure/local release and
does not close the Phase.
