# Linked Lake assembly specification prerequisite

The authored Workbench assembly declares both Workbench and Knowledge Lake.
`ComponentFactorySpec` therefore supplies both actual repositories through the
native comma-separated `component-dev-dir` configuration. The Lake repository
defaults to the conventional sibling `textus-knowledge-lake`; the test-only JVM
property `textus.test.knowledge-lake.repository` selects another admitted checkout.

Both repositories must contain their actual generated and compiled components
and native `cozyPrepareRuntime` output, including
`target/cncf.d/runtime-classpath.txt`, before the assembly specification runs.
The parent prepares this prerequisite through the registered serial SBT route.

The four existing examples retain their Candidate operation, canonical Workbench
identity, fixed-user profile, MCP publication, and missing optimistic-concurrency
provider expectations. Positive assembly admission additionally requires the
canonical `org.simplemodeling.textus.KnowledgeLake` component to be present.

This repair changes test repository wiring only. Production assembly and ABI,
SQL, security, storage configuration, and the original acceptance gates remain
unchanged. Validation and Phase acceptance remain parent-owned.
