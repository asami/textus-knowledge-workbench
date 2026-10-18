# Knowledge Workbench: asamiair development handoff

Date: 2026-10-14 (Asia/Tokyo)
Status: migration prepared; checkpoint commits and GitHub push pending

## GitHub selections

Use `codex/workbench-asamiair-20261014` in each repository below. The branches
carry the current development state, including unfinished work. They do not
represent Step acceptance or Phase closure.

| Role | GitHub repository | Source checkout |
| --- | --- | --- |
| Workbench | `asami/textus-knowledge-workbench` | Workbench root |
| CNCF Phase 100 | `asami/goldenport-cncf` | `.codex-workflow/checkouts/goldenport-cncf-phase100` |
| Cozy Phase 78 | `asami/cozy` | `.codex-workflow/checkouts/cozy-phase78` |
| Core types | `asami/goldenport-core` | `.codex-workflow/checkouts/goldenport-core-types` |
| SimpleModeler types | `asami/simple-modeler` | `.codex-workflow/checkouts/simplemodeler-types` |

The four dependency checkouts contain dedicated development history. Obtain
these selected branches rather than substituting the repositories' main
branches or another project's installed artifacts.

## Obtain the source on asamiair

Run the following after the branches have been pushed. Select an unused
destination; preserve any existing checkout instead of overwriting it.

```sh
git clone --branch codex/workbench-asamiair-20261014 git@github.com:asami/textus-knowledge-workbench.git textus-knowledge-workbench
cd textus-knowledge-workbench
mkdir -p .codex-workflow/checkouts
git clone --branch codex/workbench-asamiair-20261014 git@github.com:asami/goldenport-cncf.git .codex-workflow/checkouts/goldenport-cncf-phase100
git clone --branch codex/workbench-asamiair-20261014 git@github.com:asami/cozy.git .codex-workflow/checkouts/cozy-phase78
git clone --branch codex/workbench-asamiair-20261014 git@github.com:asami/goldenport-core.git .codex-workflow/checkouts/goldenport-core-types
git clone --branch codex/workbench-asamiair-20261014 git@github.com:asami/simple-modeler.git .codex-workflow/checkouts/simplemodeler-types
```

Open the Workbench checkout as a local Codex project on asamiair. Read its
current project directives, Phase 1 plan/checklist, and development journals
before executing a build. Resolve current workstation skill and tool locations
on asamiair rather than assuming the original machine's absolute paths.

## Resume the development

Workbench Phase 1 remains open. The retained next state of the original
development workflow is `REVIEW_FIX`. The declared-store generator needs these
bounded corrections in SimpleModeler:

- Preserve repeated Record and supported List types in generated Update
  admission, including Noop and SetNull behavior.
- Admit already typed application elements in generated entity-package
  collections while retaining the existing Record/createC input route.
- Apply the private-parameter flatcase rule to the new mapping projector.

After those corrections, use the required focused validation and independent
convergence review. Generated Scala compilation and captured-codec availability,
strict store/provider integration, SQL String/structured-data separation,
restart/readback, proposal fixtures, lifecycle/UI work and full Phase acceptance
remain required. Preserve the current Workbench CML, implementation, fixtures,
planning changes and journals included in the migration.

Build and refresh dependencies through the ordinary repository/workstation
procedure on asamiair. Keep versions explicitly declared by each project;
another checkout's presence must not select Workbench's CNCF runtime version.
Use the registered serialized SBT runner for independent top-level SBT commands.

## Local workflow records

The original control/review history remains at
`/Users/asami/src/dev2026/textus-knowledge-workbench/.codex-workflow/phases/PHASE-1/`
on the source machine. Command intakes and logs also reference private temporary
locations there. Active workflow records and execution evidence are local,
ignored material and are retained on the source machine.

GitHub branches transfer source and management documents; they do not transfer
the original Codex chat, live Goal binding, private logs or locally published
artifacts. To continue that exact workflow identity on asamiair, transfer its
local records separately and use the current supported continuation and
repository-relocation procedure. Until that is done, treat this document as a
development handoff, not a recovered execution receipt or historical PASS.
