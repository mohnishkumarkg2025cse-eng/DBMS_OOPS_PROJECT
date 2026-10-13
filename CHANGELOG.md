# Changelog

## 2026-10-13

### Time Machine — saved history and decision inspection

- Expanded the existing Time Machine route with reference-aligned Version History, Compare Versions, and Decision Log tabs; retained the existing comparison workflow and added side-by-side inspection of the selected stored policy-version conditions.
- Added a historical decision table selected by policy and version, an outcome filter that keeps unknown outcomes distinct, and a detail panel for recorded applicant snapshots and stored condition outcomes/values. Historical loads use the existing asynchronous evaluation service path.
- Added loading, empty, load-error/retry, and offline states. No replay is presented as executed, and publisher-change details are identified as unavailable because those events are not stored by the current model.
- Updated stored-outcome comparison to omit applicants whose latest outcome is unknown, and repeated histories with missing timestamps when their latest record cannot be established; added regression coverage.
- Validation: `mvn clean compile` **BUILD SUCCESS** (98 production source files); `mvn clean test` **BUILD SUCCESS** (94 tests, 0 failures, 0 errors, 0 skipped); `git diff --check` passed. Launched JavaFX against a loopback-only offline MongoDB endpoint and inspected all three Time Machine tabs at 1366x768; navigation and tab switching worked, and empty/selection states rendered without seeding records. Live Atlas data and populated history/comparison/audit interactions remain unverified. Launch output included an existing shared `.search-bar` CSS value-conversion warning; no CSS parse errors or missing resources were observed.

### Programs & Applicants — usability fixes

- Fixed the Programs list pane so it renders the actual persisted program list instead of empty card/table containers. Rows are compact and show the program name, template/category, configured-field count, and Active/Archived/Template status; actions that do not apply to archived or built-in templates are disabled.
- Reduced Applicants table rows to 54px, prevented name/email and skill content from wrapping into oversized rows, added ellipsis/tooltips for long values, and aligned compact row actions. Numeric columns now sort numerically, with deterministic comparators for applicant names and skills.
- Expanded applicant search to name, email, and skills with case-insensitive, whitespace-trimmed matching. Added explicit loading, empty-database, filtered-empty, unavailable, and load-error states.
- Applicant edits preserve stored program association and custom attributes. Mongo repository updates and deletes now fail explicitly for missing IDs; successful save/delete operations refresh asynchronously and refresh failures are reported.
- Added applicant search unit tests. Validation: `mvn clean compile` **BUILD SUCCESS** (97 production sources); `mvn test` **BUILD SUCCESS** (89 tests, 0 failures, 0 errors, 0 skipped). All 4 `ApplicantRepositoryTest` tests ran, including Mongo applicant CRUD; applicant search tests passed (2/2), and program criteria/service tests passed.
- JavaFX interaction, actual on-screen row counts, resizing/scrolling, and UI form workflows were not manually exercised. No production records were manually modified.

### Programs & Applicants — Milestone checkpoint

- Preserved the existing dashboard/program screen implementation and teammate changes while validating the reference image and repo worktree.
- Added the Programs & Applicants header, KPI strip, tab shell, and search/filter toolbar in the existing dark-navy JavaFX layout without altering unrelated screens.
- Hooked KPI values to repository-backed counts for total programs, total applicants, and active programs using the existing executor pattern and explicit unavailable/error handling.
- Added the missing `TableView` imports and a minimal helper method required by the current milestone so the screen compiles cleanly.
- Validation: `mvn clean compile -q` **BUILD SUCCESS**; `mvn clean test -q` **BUILD SUCCESS** (exit code 0). The run surfaced JavaFX native-library warnings only; there were no compile or test failures.
- Scope stopped here: later visual stages and drawers remain unimplemented in this checkpoint.

### Programs & Applicants — Stage 1

- Refined the existing Program Manager header and added a real-data KPI strip for configured programs, applicant records, and active programs. Program counts exclude built-in templates; total programs includes archived programs; active programs are non-archived configured records.
- Added asynchronous count loading, explicit unavailable/error behavior, KPI definition tooltips, Programs/Applicants navigation to the existing Applicants destination, program search, status and template filters, removable filter chips, alphabetical sorting, result count, and clear-filters action.
- Added efficient active-program count support to MongoDB and in-memory program repositories plus tests for active-versus-archived counts, program filtering, and alphabetical sort order. Existing create, archive, selection, schema edit, and applicant routes remain unchanged.
- Validation: `mvn clean compile` **BUILD SUCCESS** — 96 production sources. `mvn clean test` **BUILD SUCCESS** — 87 tests, 0 failures, 0 errors, 0 skipped; Mongo-dependent repository/migration tests ran. `git diff --check` passed.
- Stage 1 only; no JavaFX window/live data was exercised. Remaining stages: program cards/table, program detail drawer, applicant table/detail drawer, and final forms/states/motion polish.

### Insight Center Decision Pipeline

- Integrated the Decision Pipeline into the responsive Insight Center layout and trigger refreshes on dashboard display, program/policy/version selection, applicant changes, and saved-result changes.
- Added scoped counts for applicants, awaiting evaluation, distinct completed evaluations, and latest known approval/rejection outcomes. Results are limited to the selected policy/version, with the optional program filter applied to the applicant cohort (or the full repository when unset). Multiple saved runs count once per applicant; legacy policy-version IDs remain supported.
- Added explicit unavailable handling for failed/incomplete outcomes (not represented by the domain model) and ambiguous latest decisions. Zero-condition versions show a warning instead of metrics. Missing evaluation records remain awaiting, never rejected.
- Added async summary/attention loading, error/retry toasts, offline and empty states, responsive six-stage cards, privacy-conscious attention rows, and navigation only to existing unfiltered destinations.
- Added calculator invariant and edge-case tests plus in-memory legacy-version query coverage.
- Observed test fixture cross-check: expected and calculated values matched at Total 3, Awaiting 1, Completed 2, Approved 1, Rejected 1. No configured MongoDB/demo records were read or seeded.
- Validation: `mvn clean compile` **BUILD SUCCESS** — 95 production sources compiled. `mvn clean test` **BUILD SUCCESS** — 85 tests, 0 failures, 0 errors, 3 Atlas-gated applicant repository tests skipped. `git diff --check` passed. Existing JavaFX module-path/effective-model and Applicants unchecked-operation warnings remain.
- No live persisted/demo metric values or JavaFX interactions were observed; visual sizing, keyboard/navigation, and widget-state checklist remain **Needs manual check**.

## 2026-10-12

### T004 — Safe Program Schema Migration and Scoped Indexes

- Added `DatabaseMigrationRunner` with read-only `preview()` and separately invoked `apply()`, preflight compatibility checks, collection counts, explicit legacy-unassigned counts, report serialization, and a durable completion marker.
- Explicit apply creates immutable schema revision 1 snapshots for legacy program documents whose schema revision is absent or zero, then sets the current revision pointer without rewriting field definitions. It is safe to rerun after partial completion and recognizes later unversioned programs.
- Conflicting existing revision 1 snapshots, malformed schemas, invalid revision pointers, and dangling positive revision references are reported during preflight and prevent migration writes.
- Applicant, policy, policy-version, and evaluation records are not reassigned or rewritten. Ambiguous legacy ownership remains unassigned; historical evaluation/version documents and IDs are preserved.
- Added indexes for applicant program/status, policy program/archive, policy-version program/policy/version (retaining unique policy/version), evaluation program/policy/version/time, evaluation run ID, policy-version ID, applicant/time, and unique program/schema-revision.
- `DatabaseInitializer` now ensures these additive indexes. The normal application path does not automatically apply the migration.
- Added isolated MongoDB integration coverage for dry-run non-mutation, snapshot migration, count/history preservation, unassigned legacy records, index creation, conflicting revision refusal, and idempotent rerun. Tests use fresh UUID-named databases and remove only those test-created databases.
- Migration was **not** run against the application's configured database. Before a real apply, verify a backup and review `preview()` output and compatibility issues.
- Validation: `mvn clean test` **BUILD SUCCESS** — 69 tests, 0 failures, 0 errors, 0 skipped; `git diff --check` passed. Five Mongo-dependent integration tests actually executed: 3 existing applicant-repository tests and 2 migration tests.
- Known limitations/manual checks: verify a real dry-run report against a backed-up target before explicit apply; check index creation permissions and existing duplicate policy-version data; startup/index behavior was not visually or manually exercised. This task did not wire schema changes to live `ProgramService` edits or populate new evaluation snapshots; those remain in their separately planned tasks.

### Insight Center Visual Layout

- Reworked the existing Dashboard layout into the Insight Center: page title and description, four data-backed KPI cards, a JavaFX approval/rejection trends chart, Recent Activity, Data Quality Warnings, and quick-access actions.
- The trend chart groups saved evaluation outcomes by date across the last 30 days and shows an explicit empty state when no recent decisions are available. The data-quality panel clearly states that validated checks are not currently available rather than inventing findings.
- Wired quick-access actions to the existing view router. Existing filters, analytics, async loading/error handling, rule-level outcomes, and Impact Lab remain in place.
- Updated Insight Center CSS for compact KPI cards and chart accents. No schema, evaluation engine, or unrelated module changes were made.
- Validation: `mvn -DskipTests clean compile` **BUILD SUCCESS** — all 94 production Java sources compiled. Existing JavaFX effective-model/module-path and Applicants unchecked-operation warnings remain. UI rendering/navigation have not been manually verified.

### Insight Center KPI Data Integration

- Connected the four Insight Center metrics to repository counts instead of using the dashboard's applicant/evaluation analysis as a proxy.
- `Total Programs` counts configured program records, excluding persisted built-in templates and including archived records. `Total Applicants` counts applicant records, independently of whether they were evaluated.
- `Decisions Made` counts persisted evaluation-result records, with MongoDB `countDocuments()` and an in-memory repository count. Repeated saved decisions for one applicant are counted individually.
- `Active Policies` counts non-archived policies with at least one saved version. MongoDB performs an indexed version-existence lookup limited to one match per policy; versionless and archived policies are excluded.
- MongoDB applicant, evaluation, program, and policy counts run on the existing background task. Offline applicant counts use the already-loaded applicant state. KPI totals remain global regardless of dashboard filters.
- Loading uses the existing KPI skeleton; empty metrics show zero with explanatory text, and errors use the existing retryable dashboard error state. No historical comparisons were added.
- Added service/repository and MongoDB integration coverage for these definitions. `mvn clean test` **BUILD SUCCESS** — 71 tests, 0 failures, 0 errors, 0 skipped; all 94 production sources and 21 test sources compiled, including the policy lifecycle assertions. Existing JavaFX module-path, MongoDB SLF4J/native-access, and Applicants unchecked-operation warnings remain.

## 2026-10-13

### Insight Center Approval / Rejection Trend

- Implemented the trend chart using persisted evaluation records and the domain's true/false decision outcome (`Eligible` / `Rejected`). Aggregates valid decisions by date for the last 30 days; pending/unknown outcomes and missing, malformed, or out-of-range timestamps are excluded.
- The JavaFX chart shows readable date labels, Date and Evaluations axes, and a legend. No unsupported date selector or fabricated historical data was added; the existing background dashboard task loads records and calculates aggregates before updating the UI.
- Preserved unknown legacy outcomes as unavailable in `EvaluationResult` and its BSON mapper. Recognized legacy outcome aliases remain supported, offset/BSON timestamps are normalized, and the existing optional `overallDecision` field's validator now accepts null so unknown records can round-trip without being misclassified.
- Added test coverage for date grouping, range boundaries, unknown/undated exclusion, legacy decisions, timestamp formats, and BSON validation.
- Validation: `mvn clean test` **BUILD SUCCESS** — 74 tests, 0 failures, 0 errors, 0 skipped; 94 production sources and 21 test sources compiled. MongoDB-backed repository/migration tests ran. Existing JavaFX model/module-path, MongoDB logging/native-access, and Applicants unchecked-operation warnings remain. `git diff --check` passed.
- JavaFX rendering has not been manually verified.

### Insight Center Policy Health Monitor

- Added a responsive Policy Health section with Active, Draft, Needs Attention, and Versioned summary cards, plus a compact policy list with lifecycle badges, current version, last-updated timestamp, and saved-version details.
- Policy rows are loaded through the existing `PolicyRepository` in the dashboard background task, sorted by saved-version update time, and clicking a row opens the matching policy in the existing Policies view. The section links to the full policy list.
- Statuses reflect persisted lifecycle data: non-archived records with versions are Active; non-archived records without versions are Draft; archived records remain marked Archived in the list. Versioned counts policies with saved versions.
- Needs Attention displays unavailable because the model/repository exposes no policy validation findings. Missing timestamps and unversioned records are explicitly represented without inventing dates or health conclusions.
- Added loading skeletons and an empty state for an empty policy repository. Existing dashboard retry handling covers load failures.
- Validation: `mvn clean test` **BUILD SUCCESS** — 74 tests, 0 failures, 0 errors, 0 skipped; 94 production sources and 21 test sources compiled. `git diff --check` passed. Existing JavaFX module-path, MongoDB logging/native-access, and Applicants unchecked-operation warnings remain.
- The JavaFX rendering and navigation behavior have not been manually exercised.

### T003 — Backward-Compatible Program and Snapshot Relationships

- Added nullable applicant program ownership and a schema revision reference on program configurations; old program documents without a revision remain at legacy revision `0`.
- Added immutable program schema revision records, BSON mapping, in-memory persistence, and MongoDB repository support.
- Added program/schema revision references to policy versions and evaluations, plus saved applicant input and engine-semantics metadata for historical evaluation context.
- Preserved old BSON compatibility: missing applicant/evaluation references remain unassigned, and policy-version reads inherit program ownership from the parent policy where available.
- Added round-trip and legacy-shape tests for applicant, program, schema revision, policy version, and evaluation mappings, plus in-memory revision immutability/order coverage.
- No data migration, legacy-record assignment, applicant deletion behavior change, or persisted user-data write was performed. Lifecycle wiring and migration/index rollout remain future tasks.
- Validation: `mvn clean compile` **BUILD SUCCESS** (92 production sources); `mvn clean test` **BUILD SUCCESS** (67 tests, 0 failures, 0 errors, 0 skipped). The three `ApplicantRepositoryTest` cases passed.
- Existing warnings: JavaFX effective-model and source/target module-path warnings, MongoDB SLF4J not found, JDK restricted native access, and unchecked operations in `ApplicantsView`.

### Policies Screen Redesign

- Reorganized the Policies page header and new-policy action into a clear, wrapping layout; added a page scroll surface and a stacked master/detail arrangement at compact widths.
- Refined policy list rows to wrap long names and IDs, styled the empty state, and improved card, version-history, and rule-condition hierarchy and spacing.
- Replaced Policies inline condition/modal title styles and fixed-color action icons with Policies-scoped author CSS classes.
- Preserved policy selection, version history and activation, What-if Simulator, rule-builder dialogs, async service calls, persistence, and existing feedback behavior.
- Validation: `mvn clean compile` **BUILD SUCCESS** (86 production sources); `mvn clean test` **BUILD SUCCESS** (61 tests, 0 failures, 0 errors, 0 skipped). All three `ApplicantRepositoryTest` cases ran and passed.
- Existing warnings: JavaFX effective-model and source/target module-path warnings, MongoDB SLF4J not found, JDK restricted native access, and unchecked operations in `ApplicantsView`.
- Visual verification: **Needs manual check**; the JavaFX application was not launched or visually inspected.
- Files changed for this update: `PoliciesView.java`, `style.css`, `PROGRESS.md`, and `CHANGELOG.md`. Dashboard and Applicants changes from earlier stages were preserved.

### Applicants Screen Redesign — Update 3

- Reorganized the Applicants heading, search, and existing create action into a wrapping layout that remains usable at narrower widths.
- Added a headed applicant-records card with a visible filtered row count; improved table headings, row spacing, name/email hierarchy, metric alignment, and wrapping skill chips.
- Applied CSS classes to applicant-specific table cells, icons, and the existing add/edit form, removing ApplicantsView inline styles without changing form validation or CRUD behavior.
- Preserved search, sorting, create/edit/delete actions, keyboard shortcuts, delete confirmation, empty-state action, async persistence, and toast feedback.
- Validation: `mvn clean compile` **BUILD SUCCESS** — 86 production sources; `mvn clean test` **BUILD SUCCESS** — 61 tests, 0 failures, 0 errors, 0 skipped. All three `ApplicantRepositoryTest` cases ran and passed.
- Visual verification: **Needs manual check**; the JavaFX application was not launched for visual inspection.
- Files changed for this update: `ApplicantsView.java`, `style.css`, `PROGRESS.md`, and `CHANGELOG.md`. Existing uncommitted Dashboard Stage 2 changes from the prior stage were preserved.

### Dashboard UI Redesign — Stage 2

- Reworked the Dashboard header into a left-aligned, wrapping action toolbar and gave the filter row responsive minimum widths so controls wrap instead of crowding.
- Standardized Dashboard card surfaces and spacing on an 8px rhythm, clarified section headings, and arranged KPI cards into responsive one-, two-, or three-column rows.
- Enabled KPI labels, values, and supporting text to wrap instead of truncating; kept the existing metrics and calculations unchanged.
- Replaced the Dashboard's inline threshold-validation color with a CSS style class. Existing analytics, chart data, asynchronous refresh states, recent evaluations, and Impact Lab behavior remain unchanged.
- Validation: `mvn clean compile` **BUILD SUCCESS**; `mvn clean test` **BUILD SUCCESS** — 61 tests, 0 failures, 0 errors, 0 skipped. The three `ApplicantRepositoryTest` cases ran and passed.
- Visual verification: **Needs manual check**; the JavaFX UI was not launched for visual inspection.
- Files changed: `DashboardView.java`, `KpiCard.java`, `style.css`, `PROGRESS.md`, and `CHANGELOG.md`.

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
