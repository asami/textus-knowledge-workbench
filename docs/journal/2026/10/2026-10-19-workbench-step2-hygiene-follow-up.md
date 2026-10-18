# Workbench STEP-2 Hygiene follow-up

Date: 2026-10-19
Status: nonblocking maintenance records; no implementation authorized

<!-- cncf-phase-finding:HYG-TKW-S2-003:begin -->
## HYG-TKW-S2-003: Qualify the proposal specification SQL-fixture description

Owner: Workbench. Status: OPEN, nonblocking. docs/spec/candidate-proposals.md says the native fixture uses no SQL write, while CandidateProposalSpec deliberately corrupts its closed test-owned SQLite fixture for the unknown-code scenario. Normal registration uses the production route. At the next selected documentation correction, qualify the no-direct-SQL claim and identify deliberate fault injection. No product-path change is required; this acceptance synchronization does not silently repair the separate item.
<!-- cncf-phase-finding:HYG-TKW-S2-003:end -->

<!-- cncf-phase-finding:HYG-TKW-S2-004:begin -->
## HYG-TKW-S2-004: ProposalReference semantic leaf types

Owner: Workbench. Status: OPEN, nonblocking. Development CAR lint retains raw-string advisories for ProposalReference kind, revision, snapshotRef, retrievalState, retentionUntil and limitation. Resume semantic type elaboration when those vocabulary contracts are explicitly selected. Preserve opaque supplied source facts and scalar compatibility; do not invent a closed vocabulary, provider inference or upstream workaround during STEP-2.
<!-- cncf-phase-finding:HYG-TKW-S2-004:end -->

<!-- cncf-phase-finding:HYG-TKW-S2-005:begin -->
## HYG-TKW-S2-005: Unreleased ABI comparison baseline

Owner: Workbench release preparation. Status: OPEN, nonblocking. This unreleased CAR has no declared previous-release ABI baseline, so development lint has no release-comparison input. Select an actual prior published ABI or an explicit first-release disposition at the later release boundary. Do not fabricate a baseline, infer published compatibility from the current manifest or block the bounded development Step.
<!-- cncf-phase-finding:HYG-TKW-S2-005:end -->
