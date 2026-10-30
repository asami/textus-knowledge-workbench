# Saved semantic formation work

SemanticWork opens an owner-readable immutable Information through Hub, persists the proposal-selected complete semantic target and lets callers save corrections. SemanticFormationTarget is the Workbench wire binding of Hub SemanticTarget, with identical fields; it defines no alternate canonical Information or Knowledge model. Corrections are validated by Hub. Decisions require native observed revision, authenticated execution actor, clock and nonblank reason. review precedes approve. Any meaningful correction invalidates prior approval; rejected/unknown/ambiguous/unapproved targets cannot form. A native result identity and exact approved target/decision are persisted before the Hub child call. Failure retains Unconfirmed and confirm performs only an owning read; missing/denied reads remain unresolved, never create another result. Original access reuses existing MaterialWork/TKL.

The pre-call durability guarantee is literal: an independent reconstructed
Workbench instance reading PostgreSQL through getSemanticWork must already observe
the Requested reservation and the same native work revision before the Hub
invocation. Native entity_update_detached uses the versioned provider mutation
path; SqlDataStore commits that mutation before returning acknowledgement. The
outer UnitOfWork event/finalization boundary is not used as a substitute for this
provider acknowledgement. No handwritten transaction or product fault API is added.

The composed Studio executable specification reconstructs an independent Workbench
component/realm/context for a public owning read before calling Hub, then injects
a ControlThrowable after successful Hub return. The normal Workbench post-call
save/return does not run. A subsequent runtime must recover the already durable
Requested identity and confirm the actual canonical result by read only. This is
a modeled interruption specification, not evidence of execution or a physical
process-kill trial. Native qualification remains required.

The confirmation command uses its own CML request type
`ConfirmSemanticFormation(id, expectedRevision)`, distinct from
`SemanticWorkFormation` for creation. Public operation names and payload fields
are unchanged. Separate operation request wrappers preserve native generated
command ownership. Native bootstrap specifications require the ordinary
cozyPrepareRuntime evidence, provided by the Test / compile dependency; no runtime
classpath file is synthesized by application or fixture code.
