# Changelog

## 2026-10-12

### Task 4 — Policy Studio, Stage A

- Added a visual, in-memory policy draft tree to the existing create-policy and create-version modals, with nested AND/OR groups, group collapse/expand, and add/remove/duplicate/reorder controls.
- Added dynamic condition field, operator, and value editors from the selected program schema for NUMBER, TEXT, BOOLEAN, and MULTI_SELECT types. Legacy policies retain GPA, interview-score, and skills controls.
- Added clear validation for empty groups, unknown fields, missing or malformed values, incompatible operators, and schema bounds. Save remains disabled until the draft is valid.
- Added natural-language and read-only tree previews from the same draft model used to create the decision engine's `Condition` tree.
- Added a debounced asynchronous impact preview using the shared `DecisionEngine`; previews are in memory, cancellable/stale results are ignored, and no evaluations or draft policy versions are persisted by preview.
- Added JUnit tests for nested logical evaluation, field/operator/value validation, preview/tree consistency, draft isolation, duplicate rules, impact calculations, and invalid-save behavior.
- The existing version change-summary chips and explicit save-version action remain in place; no additional version-creation workflow was introduced.
- Verification: `mvn test` **BUILD SUCCESS** — 58 tests, 0 failures, 0 errors, 3 Atlas-gated integration tests skipped. JavaFX interaction and live Atlas behavior were not manually verified.
- Task 4 Stage B and Task 5 have not been started; Stage A is paused for review.

### Task 3 — Impact Lab, Stage A

- Extended the dashboard what-if panel with program, policy, and base policy-version selection.
- Added editors for up to three compatible numeric thresholds, including sliders and numeric input, along with Boolean and multi-select rule controls.
- Recalculates both base and adjusted decisions via the shared `DecisionEngine` using the current applicant cohort; simulated decisions and rule changes are not persisted.
- Added eligible/rejected counts, count and percentage-point deltas, and changed-applicant details with responsible conditions where identifiable.
- Added worker-thread execution, progress, cancellation, explicit errors, and stale-result protection. The existing explicit handoff to an unsaved Policy Studio draft remains available.
- Added simulator tests for boundary/range validation, empty cohorts, Boolean/multi-select and nested-rule adjustments, changed applicants, progress/cancellation, shared-engine consistency, and the absence of a persistence-repository dependency.
- Verification: `mvn test` **BUILD SUCCESS** — 50 tests, 0 failures, 0 errors, 3 Atlas-gated integration tests skipped. JavaFX interaction and live Atlas behavior were not manually verified.
- Task 3 Stage B has not been started; Stage A is paused for review.

### Task 2 — Insight Center, Stage A

- Added program, policy, saved-version, evaluation-run, and decision-status filters to the dashboard while preserving the existing what-if workflow.
- Added stored-data analytics for the decision funnel, applicant totals, eligible/rejected counts, pass rate, near-misses, common rejection reasons, and rule-level outcomes, with sample sizes and denominators.
- Added paired comparison with the immediately preceding saved policy version. The selected comparison cohort excludes the decision-status filter; run filtering applies to current results only.
- Added persistent evaluation-run identifiers for service-generated batches; legacy evaluations without run IDs remain readable and are identified as untracked.
- Added asynchronous dashboard refresh with cancellation/stale-result protection and loading, empty, and retryable error states.
- Added JUnit coverage for filters, denominators, run grouping, empty and legacy data, cross-policy deduplication, and matched-version comparisons.
- Verification: `mvn test` **BUILD SUCCESS** — 44 tests, 0 failures, 0 errors, 3 Atlas-gated integration tests skipped. JavaFX interaction and live Atlas behavior were not manually verified.
- Stage B and Stage C have not been started; Stage A is paused for review.

### Task 1 — Configurable programs and field registry

- Added typed program field definitions, field validation, program templates, program repositories, and BSON persistence.
- Added generic custom-field policy conditions and dynamic applicant custom attributes, with program-linked policy and applicant schema validation.
- Added program management UI actions for create, rename, field add/edit, duplicate, select, and archive.
- Guarded field removal and type changes behind an explicit migration callback; existing unlinked policies remain supported.
- Added JUnit coverage for field types, templates, program lifecycle, validation, generic condition serialization, and program collection schema.
- Verification: `mvn test` **BUILD SUCCESS** — 38 tests, 0 failures, 0 errors, 3 Atlas integration tests skipped because no reachable configured test URI was available.

### Task 0 — Repository audit, foundation, and persistence

- Added explicit policy-version identity to evaluation BSON records while retaining compatibility with older documents that omit it.
- Retained compatibility with legacy evaluation decisions and policy-version documents, including history lookups by version identity.
- Ensured service-generated evaluations record the policy name and preserve the version, timestamp, decision, and condition explanations.
- Made policy removal archive metadata instead of deleting saved versions; demo cleanup no longer deletes evaluation history.
- Made in-memory policy snapshots detached from caller mutations and sample-policy initialization idempotent.
- Updated dashboard aggregates and stored-result comparisons to use each applicant's latest evaluation, preventing repeated runs from inflating applicant analytics.
- Added JUnit coverage for decision consistency, persistence mapping, immutable snapshots, analytics, schema shape, and idempotent sample initialization.
- Verified ordinary unit tests do not require a live MongoDB Atlas connection; Mongo-backed integration tests remain connection-gated.
