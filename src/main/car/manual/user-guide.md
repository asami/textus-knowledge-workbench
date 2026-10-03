# Textus Knowledge Workbench user guide

This guide runs the starter Candidate operations for
`org.simplemodeling.textus.KnowledgeWorkbench` version `0.1.0-SNAPSHOT`.
The [reference manual](index.md) explains results, security and scope.

## Prepare the repository

Use Java compatible with the Scala/SBT toolchain, SBT `1.9.7`, Cozy
`0.3.3-SNAPSHOT` on `PATH`, sbt-cozy `0.1.18-SNAPSHOT`, CNCF
`0.5.3-SNAPSHOT`, Scala `3.3.8` and ScalaTest `3.2.19`. The build resolves
through local Ivy, local Maven and SimpleModeling repositories. The sbt-cozy
version may be overridden with `-Dsbt.cozy.version=...` or `SBT_COZY_VERSION`.

Run every command below from the repository root. The ordinary focused workflow
is:

```sh
sbt --batch clean cozyGenerate cozyPrepareRuntime 'testOnly org.simplemodeling.textus.knowledgeworkbench.ComponentFactorySpec' cozyBuildCAR
```

For agent execution, the parent routes this invocation through
`cncf_command_runner` and the machine's serial SBT wrapper. Runtime preparation
must precede the assembly scenarios on a fresh build. It writes
`target/cncf.d/runtime-classpath.txt`; rerun preparation after dependency changes.
Ordinary clean also refreshes source-bound Scaladoc staging.

`project.yaml` owns the CNCF version under `build.dependencies.compile`, currently
`org.goldenport::goldenport-cncf:0.5.3-SNAPSHOT`. Missing, blank or duplicate CNCF
declarations fail build configuration before dependency resolution. Legacy
`CNCF_VERSION`, `CNCF_VERSION_FILE`, `CNCF_SAMPLES_ROOT` and `CNCF_BIN` do not
select a version or launcher.

## Select an operator instance

Choose an absolute path in an existing operator-owned directory and copy the
complete authored assembly descriptor:

```sh
export WORKBENCH_ASSEMBLY_DESCRIPTOR="/absolute/path/to/workbench-instance.yaml"
cp src/main/car/assembly-descriptor.yaml "$WORKBENCH_ASSEMBLY_DESCRIPTOR"
```

Edit that copy's existing security mapping to set your explicitly selected
stable standalone identity. Replace the placeholder before running:

```yaml
security:
  authentication:
    convention: disabled
    fallbackPrivilege: disabled
    localSubject:
      id: <selected-stable-user-id>
```

Use the identity from an admitted HOME standalone profile when present. Keep
all components, providers and existing configuration from the complete copy.
The base descriptor has no production user ID, and no interpolation or `extends`
syntax is assumed. A conflicting fixed-user ID requires explicit migration or
datastore isolation under CNCF's existing rule.

Optionally add an absolute isolated storage root to the copy's existing `config`
mapping, retaining both existing entries:

```yaml
config:
  textus.subsystem.user-mode: standalone
  textus.component.org.simplemodeling.textus.knowledge-workbench.datastores.application.policy: local-default
  textus.local-data.root: /absolute/path/to/workbench-data
```

This remains the ordinary framework-managed persistent application datastore.
No extra application database or privilege override is required. Use the same
instance, user and storage root for subsequent commands. Development projection
excludes the adjacent assembly descriptor, so runtime preparation alone does not
select this instance. Only `WORKBENCH_ASSEMBLY_DESCRIPTOR` needs exporting for
the examples below.

## Run CLI operations

The project launcher consumes the prepared classpath, invokes
`org.goldenport.cncf.CncfMain` and passes discovery and assembly arguments after
`--`. Show canonical component help:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command meta.help org.simplemodeling.textus.KnowledgeWorkbench
```

Help should contain both Candidate operations without assembly/profile failure
diagnostics; exit zero alone does not establish successful activation. Native
component Help retains the framework's generic summary. The authored operation
descriptions also supply Cozy metadata.

Register a new starter candidate with optional description:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.Candidate.registerCandidate --name "Research note" --description "Evidence for review"
```

Omit `--description` when it is not needed. Successful registration returns
`id` and `name`. Retain the native ID to recognize the stored entity. Launch a
separate process using the same instance to list accessible candidates:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.Candidate.listCandidates
```

Find the stored ID/name in `items`. Its optional description appears as
`description.value`; registration itself returns only ID/name. Search by a
case-insensitive substring of the description or name:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.Candidate.listCandidates --text "evidence"
```

Use native paging controls:

```sh
bin/launcher --classpath-file target/cncf.d/runtime-classpath.txt --main-class org.goldenport.cncf.CncfMain -- --discover=classes "--textus.assembly.descriptor=$WORKBENCH_ASSEMBLY_DESCRIPTOR" command KnowledgeWorkbench.Candidate.listCandidates --offset 1 --limit 1
```

With a single accessible row, that offset produces an empty page. Empty search
or page results render as `{}` because generated `Record.dataAuto` omits empty
`items`. This is also the expected shape of a list with no accessible rows.
The [reference examples](index.md#listcandidates) show nonempty YAML/JSON output.

## Troubleshooting and validation boundary

If startup fails, confirm the prepared classpath exists and is nonempty, the
selected descriptor is a readable regular file and its complete component and
provider mappings remain intact. Check the selected stable user against the
standalone profile and confirm the required native providers are present. For
input diagnostics, supply the required name and valid optional query inputs.
Use the actual native admission, authorization and input diagnostics; no
additional starter-specific error codes are defined.

Declared server commands are `scripts/run-server.sh` and
`scripts/run-server-debug.sh`; debug uses `DEBUG_PORT` (default `5005`). Both
require the prepared classpath and exported instance path. Controlled routing
checks cover their guards and exact arguments. Actual supplied runtime checks
were CLI help, registration, separate-process list, description filtering and
paging, with one persistent SQLite row in isolated storage. No Web/manual,
OpenAPI or MCP route execution is established by those checks.

The predecessor clean build passed four focused factory/assembly scenarios and
CAR packaging. The new manuals and DESCRIPTION changes remain subject to fresh
generation/package/help/lint validation, independent Step review and a local
Step commit. Phase 1 is still in progress and retains its final bootstrap full
SBT suite. Proposal formation, human lifecycle actions and views are subsequent
Phase 1 work; actual KnowledgeHub Admission belongs to Phase 1.1.
