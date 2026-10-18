# Workbench Migration to asamiair and Shared Library Development Plan

Date: 2026-10-14 (Asia/Tokyo)
Status at recording (2026-10-14): Migration policy agreed and recorded. Migration, merges, builds, and local publication had not been executed.

## Current scope update — 2026-10-17

The user reports CNCF Phase 96 complete and pushed to GitHub main. Local CNCF
main and fetched origin/main both select `e455716c`, which integrates the
completed Phase 96; its Phase document records CLOSED and final validation.
The Phase 96 prerequisite is therefore satisfied.

The user explicitly requests completing CNCF Phase 100 as part of this
Workbench migration. Merely retrieving its WIP or restoring Workbench startup
does not complete the migration. Integrate and finish the Phase 100 work and
its Core/SimpleModeler/Cozy producer prerequisites, preserve Phase 96, validate
through shared SNAPSHOT JARs, and satisfy the original Phase 100 checklist,
including generated-model SQL restart/readback and recovery rehearsal. Full
Workbench Phase 1 acceptance remains a separate outcome.

Source integration has started in the canonical directories below, using
fetched GitHub main/scala12 and migration refs. The integration is uncommitted
and has not yet established build, runtime, publication or Phase acceptance.
The current execution ledger is
[Workbench asamiair migration](../../../phase/workbench-asamiair-migration.md).
The original chronological plan below is retained as the 2026-10-14 baseline.

## Purpose and decisions

For the time being, asamiair prioritizes development of nict-editing-studio and
nict-editing-studio-app. Move textus-knowledge-workbench from macmini and
integrate its dependencies, including CNCF and Cozy, into the same development
environment in support of that shared purpose.

- Use each repository's canonical directory and primary development branch.
- Do not introduce project-specific worktrees, separate SNAPSHOT versions, or
  Development Artifact Overlay as part of this migration.
- Consumers use JARs and dependency metadata published to Ivy local. They do not
  directly reference dependency sources under development or classes in a
  dependency's `target` directory.
- All consumers on this machine share the same SNAPSHOT coordinates for each
  library. Publish locally to those coordinates at validated checkpoints,
  rather than during unfinished edits.
- After publication, align consumers with the shared JARs. Accept some
  integration adjustments as part of the common nict development effort;
  do not isolate projects by pinning different versions.
- Complete CNCF Phase 96 before developing Phase 100. The earlier proposal to
  merge both sets of WIP into main at the same time is withdrawn.

This records migration and operational decisions. It does not claim Phase
completion or successful execution of validation.

## Relationship to the earlier handoff

The source document is the [handoff from macmini](https://github.com/asami/textus-knowledge-workbench/blob/codex/workbench-asamiair-20261014/docs/journal/2026/10/2026-10-14-asamiair-development-handoff.md).
Its dedicated-checkout layout and clone instructions are superseded by this
plan to integrate into canonical directories. Preserve its implementation,
unresolved issues, and history handoff requirements.
The source document's “push pending” status predates preservation of the work;
the user has reported successful pushes for all five repositories. Confirm the
fetched remote refs when executing the migration.

The source branch in each repository is
`codex/workbench-asamiair-20261014`. Reported preservation commits are:

| Repository | Preservation commit | Treatment |
| --- | --- | --- |
| Workbench | `d0261dc` | WIP checkpoint |
| CNCF | `79a455e` | Phase 100 WIP checkpoint |
| Cozy | `713965d` | Phase 78-related WIP checkpoint |
| SimpleModeler | `2338fcf` | Type and generator WIP checkpoint |
| Core | `8c0d31e` | Existing commit; inspect differences requiring integration even though no new changes were made for this checkpoint |

Do not reinterpret preservation commits as Step acceptance or Phase releases.
Merge the required changes into the current primary branches instead of
replacing current main with the migration branches.

## Integration destinations

| Target | Canonical directory on asamiair | Development branch |
| --- | --- | --- |
| Workbench | `/Users/asami/src/dev2026/textus-knowledge-workbench` | `main` |
| CNCF | `/Users/asami/src/dev2025/cloud-native-component-framework` | `main` |
| Cozy | `/Users/asami/src/dev2025/cozy` | `main` |
| Core (`asami/goldenport-core`) | `/Users/asami/src/dev2025/simplemodeling-lib` | `main` |
| SimpleModeler | `/Users/asami/src/dev2025/simple-modeler` | `scala12` |
| nict server | `/Users/asami/src/Project2026/nict-editing-studio` | `main` |
| nict App | `/Users/asami/src/Project2026/nict-editing-studio-app` | `main` |

SimpleModeler's current primary branch is `scala12`; renaming it to `main` is
unnecessary. Do not confuse Core with the separate goldenport-scala-library.

## Observed starting state

At the local inspection on 2026-10-14, CNCF main was at `d6413272`, Cozy main at
`03fded5b`, Core main at `bd24286`, and SimpleModeler scala12 at `21837c9`.
These working trees were clean.

The Cozy Phase 76 Logical UI producer has been integrated into main. CNCF
Phase 96 implementation has not: it remains on `codex/phase-96-display-service`
in the existing worktree
`/Users/asami/src/dev2025/cloud-native-component-framework-phase96`.
Its HEAD is `01a95cf2`, with seven commits absent from main and additional
uncommitted changes and new files covering Display mutation, Entity observed
delete, DataStore, UnitOfWork, and related work. Merging only the committed
portion would leave the migration incomplete.

Workbench local main is at `ccf59d6`. The nict server has local commits and
uncommitted specification and planning documents. The nict App is nine commits
ahead of its local tracking ref. Do not overwrite these with migration
branches. Recheck differences against the remotes after fetching at execution
time.

## Execution sequence

### 1. Preserve current work and obtain migration materials

1. Check changes and current work ownership in each canonical checkout and the
   existing Phase 96 worktree. Coordinate integration checkpoints so editing
   and publication do not conflict within a repository.
2. Preserve local commits, uncommitted implementation, and untracked
   specifications and executable specs. Inspect directories such as `.cozy/`
   and distinguish source, configuration, and generated output.
3. Fetch each remote's primary and migration branches. Prepare integration
   with the current primary branches while preserving local work ahead of
   them. Retain Phase 100-related WIP on its branches.
4. Locate the necessary local history, unresolved findings, configuration,
   and actual data on macmini. Transfer only required Git-ignored material
   separately; do not copy entire caches or `target` directories.

Fetching source from GitHub does not transfer the original chat, Goal, or
execution records. Preserve existing Phase and review history and correction
counts, and continue through the supported handoff procedure. Do not treat a
historical PASS as a PASS for the newly integrated result.

### 2. Complete CNCF Phase 96 on main

1. Integrate both the committed Phase 96 implementation and its uncommitted
   changes into canonical CNCF main. Preserve improvements already on main.
2. Establish remaining work from the current Phase 96 specifications and
   checklist. Connect the basic Display read and update paths using Cozy
   Phase 76 output first, then resolve the necessary remaining issues.
3. Verify the relevant Executable Specifications, generated-code compilation,
   and the target Display paths in the nict server. Verify App integration
   within the applicable acceptance scope.
4. Complete validation and independent review required by the existing Phase
   contract, and close the work with a Phase 96 release commit. Git integration
   alone does not complete the Phase.
5. Publish accepted artifacts to Ivy local at the shared SNAPSHOT coordinates
   and refresh the nict consumers.

At this stage, do not bundle in Workbench Phase 100 and its related Cozy,
SimpleModeler, and Core WIP. Handle dependency fixes needed by Phase 96 within
that necessary scope. If acceptance validation requires local publication,
publish the shared SNAPSHOT at a testable checkpoint, align the affected
consumers, and then validate.

### 3. Integrate Workbench changes after Phase 96 is complete

1. Integrate Workbench's current main and migration branch while preserving
   local changes.
2. Bring the Core, SimpleModeler, Cozy, and CNCF Phase 100 migration changes
   into their canonical checkouts. Inspect actual dependencies and integrate
   and build from dependencies toward consumers.
3. For CNCF Phase 100, check consistency with Phase 96 around Entity,
   DataStore, and UnitOfWork. Do not replace Phase 96 with older implementations.
4. Carry forward the SimpleModeler issues: repeated Record/List Update
   admission with Noop/SetNull, typed collection elements alongside the
   existing Record/createC path, and private-parameter naming in the new
   projector.
5. First establish the basic Workbench path: generated-code compilation,
   startup, and persistence/readback through the target store. Then continue
   Phase 100 and Workbench Phase 1 according to their respective plans.

Source retrieval and preservation may happen earlier, but Phase 100 development
resumes only after Phase 96 completion. Completion of the entire Workbench
Phase 1 is separate from completion of the machine migration.

### 4. Align shared JARs and validate consumers

- Publish locally according to the actual dependency graph. Check the
  Core-to-CNCF, SimpleModeler-to-Cozy, and Cozy-generation-to-CNCF/consumer
  relationships.
- Where direct references are active, such as project references, development
  checkout classpaths, or `--runtime-dev-dir`, switch to the normal path that
  selects published JARs. Apply the same policy to the Cozy generator.
- Refresh consumer dependency resolution and retained classpaths, restarting
  SBT and servers as needed. Publication alone does not switch running
  processes to the new JARs.
- Verify nict generation, compilation, and the affected basic paths first,
  followed by Workbench's basic path. Meet existing validation obligations
  for the change scope without repeating unrelated checks.
- Do not make wholesale cache deletion part of the update procedure.
  Diagnose problems from actual resolution destinations and execution results.

Follow this machine's existing SBT execution policy: use the registered runner
and shared serialization wrapper. Do not start independent top-level SBT
processes concurrently.

## Operation after publication and recovery from problems

Publishing the same SNAPSHOT updates the dependency for the whole machine.
Do not retain different older SNAPSHOTs pinned for individual consumers.
Edits alone do not affect consumers; publication is the point at which affected
consumers are aligned and integration adjustments are made.

If a problem appears, fix the shared source and republish at a validated
checkpoint. If a rollback is needed, use Git history to make the shared
correction or restoration, rebuild, republish, and align consumers. Do not
transplant macmini JARs or introduce new comparison ledgers, content copies,
or tamper-detection mechanisms.

Retain the original worktrees and macmini history until the necessary
implementation, data, and history have been transferred. Preserving old work
is distinct from continuing development in new dedicated worktrees.

## Migration completion criteria

- Development can continue in canonical directories with the relevant source,
  history, and data preserved.
- CNCF proceeds to Phase 100 only after Phase 96 is complete.
- CNCF Phase 100 is completed against its original checklist, with the required
  producer handoff, consumer/provider restart evidence, recovery rehearsal,
  independent review and release evidence; WIP integration alone is insufficient.
- Dependencies resolve to shared SNAPSHOT JARs in Ivy local without relying on
  direct references to development checkouts.
- The target nict basic paths and Workbench generation, compilation, and
  startup have been verified.
- Unfinished work, including Workbench Phase 1 and outstanding acceptance
  items, remains recorded and carried forward.

Work performed for this record was limited to recording the plan and
inspecting Git state and the original handoff. No source changes, migration
merges, tests, publication, commits, or pushes were executed.
