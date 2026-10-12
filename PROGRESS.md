# RuleWeaver X — Project Progress

## A. Project Overview

RuleWeaver X is a JavaFX desktop application for defining versioned decision policies, evaluating applicants with explainable rule results, and comparing policy outcomes. The implementation includes MongoDB persistence and an in-memory repository for service tests/offline initialization.

| Item | Current state |
|---|---|
| Stack | Java 17, JavaFX 20.0.1, Maven, MongoDB Java sync driver 5.12.0, JUnit 5.9.0 |
| Current branch | `main` |
<<<<<<< Updated upstream
| Latest commit | `b3b39ee` — `Restore merged style.css and UI styling updates` |
| Working tree | Task 0 foundation/persistence, Task 1 program-schema changes, Task 2 Stage A dashboard changes, Task 3 Stage A Impact Lab changes, and Task 4 Stage A Policy Studio changes are uncommitted; pre-existing local config remains ignored. |
=======
| Latest commit | `d6e4731` — `Update RuleWeaver X implementation` |
| Working tree | Contains RuleWeaver X theme, icon, shell, and progress updates; screen work is being delivered in sequence. |
>>>>>>> Stashed changes
| Main source/test layout | `src/main/java`, `src/main/resources`, `src/test/java` |

## B. Completed Features

These items have implementation evidence and automated test evidence. Test evidence does not imply that the JavaFX UI was manually exercised.

- [x] **Applicant Mongo repository CRUD:** insert, read, list, update, and delete are exercised against the configured Mongo test database by `ApplicantRepositoryTest.testApplicantCRUD`.
- [x] **Rule condition serialization and compatibility:** `ConditionDocumentMapper` round-trips nested AND/OR composite rules, accepts legacy condition fields, and defaults legacy composites to AND; covered by `ConditionDocumentMapperTest`.
- [x] **Applicant/evaluation BSON mapping and schema shape:** applicant/evaluation mappers and initializer schema fields are covered by mapper and `DatabaseInitializerSchemaTest` tests.
- [x] **Policy service evaluation-run semantics:** in-memory service tests verify append-only repeated runs, progress reporting, cancellation without partial persistence, immutable versions, and active-version selection (`PolicyServicePersistenceTest`).
- [x] **What-if simulation logic:** `WhatIfSimulatorTest` verifies a threshold change against the latest stored baseline, reports the decision delta/flip, and leaves the original version unchanged.
- [x] **Impact Lab simulator service:** Stage A recalculates base and adjusted policy decisions with the shared `DecisionEngine`, supports compatible numeric/Boolean/multi-select rule adjustments, and reports in-memory count changes and changed applicants; covered by `ImpactLabSimulatorTest`.
- [x] **Dashboard analytics service:** Stage A filters and metrics are calculated from stored evaluations, with distinct-applicant counts, explicit denominators, rejection/rule impacts, near-miss counts, and a paired immediately preceding-version baseline; covered by `DashboardAnalyticsTest`.
- [x] **Mongo collection-level persistence checks:** `ApplicantRepositoryTest` exercises applicant CRUD, unique policy-version numbering, and evaluation document save/read against `ruleweaver_test`. These tests do not cover the complete typed application-to-Atlas workflow.
- [x] **Configurable program schemas:** typed field definitions, seven editable templates, program configuration CRUD/archive, custom applicant attributes, and program-linked policy validation are implemented with unit-test coverage.
- [x] **Policy Studio rule-draft service:** in-memory draft trees support nested AND/OR groups, schema validation, immutable condition generation, and matching natural-language/tree previews; covered by offline tests.

## C. Partially Completed Features

- [/] **JavaFX application and views:** `MainApp`, `ViewRouter`, shared controls, and the Dashboard, Applicants, Policies, Evaluation, and Comparison views are implemented and compile as part of the test build. The UI was not launched or manually checked in this update; layout, dialogs, keyboard shortcuts, progress/cancel/retry, and navigation behavior remain unverified.
- [/] **Policy and evaluation persistence in the running app:** `MongoPolicyRepository`, `PolicyVersionRepository`, `EvaluationRepository`, BSON mappers, and `AppContext` wiring are present. Unit tests cover in-memory service behavior and mapper/schema contracts, while the Atlas tests exercise raw document repository operations; typed policy/evaluation round trips through the full UI are not verified.
- [/] **Dashboard and what-if UI:** `DashboardView` implements Task 2 Stage A analytics plus the Task 3 Stage A Impact Lab program/policy/base-version selectors, up to three numeric threshold editors, Boolean/multi-select controls, asynchronous simulations, and changed-applicant explanations. Analytics and simulator logic are tested; interaction/rendering and Atlas-backed refresh have not been manually verified.
- [/] **Policy rule builder:** `PolicyRuleBuilder` is integrated into new-policy and new-version modals. It supports schema-driven field/operator/value editors, nested AND/OR groups, add/remove/duplicate/reorder actions, group collapse, validation-gated saving, natural-language/tree previews, and a debounced asynchronous unsaved impact preview. Automated tests cover draft tree semantics and validation; JavaFX interaction has not been manually exercised.
- [/] **Policy administration and operations:** policy category/archive fields, version selection, and active-version persistence exist in the model/repository. A complete archive/search/audit workflow, evaluation export, and end-user operation checks are not established by the current code/tests.

## D. Pending Tasks

- [ ] Manually launch the JavaFX application and exercise the five views, including CRUD dialogs, rule-builder validation, navigation, evaluation history, comparison, Dashboard widgets, and what-if draft flow.
- [ ] Add integration coverage for `MongoPolicyRepository` and typed `EvaluationRepository.saveAll`/readback, then verify policy and evaluation flows through `AppContext` against a non-production database.
- [x] Add offline decision-consistency, repeated-run analytics, and initialization coverage for Task 0.
- [x] Implement Task 1 program templates, typed field definitions, schema validation, program administration, and offline tests.
- [ ] Decide and implement remaining product scope: policy archive/search/audit and evaluation export.
- [ ] Establish CI and verify platform-specific Maven packaging/launch behavior; the configured shade and JavaFX platform profiles have not been exercised in this update.

## E. Database and Persistence

- **Application database:** `ruleweaver` is the default in `MongoConfig` and in `config.example.properties`; `RULEWEAVER_DB_NAME` can override it. The Java configuration loader does not embed a MongoDB URI.
- **Integration-test database:** `ApplicantRepositoryTest` connects to `ruleweaver_test`, pings it, and removes its generated test records in cleanup.
- **Collections and records:**

| Collection | Persisted shape / purpose |
|---|---|
| `applicants` | `_id`, `name`, `email`, `gpa`, `interviewScore`, `skills`, `status`, and optional `customAttributes`; mapped by `ApplicantDocumentMapper`. |
| `policies` | `_id`, `name`, `description`, `category`, `activeVersionNumber`, and `archived`; saved by `MongoPolicyRepository`. |
| `policy_versions` | Immutable policy snapshots with `_id`, `policyId`, `version`, `description`, `createdAt`, and serialized `conditions`. `ConditionDocumentMapper` handles numeric, skill, and nested composite conditions. Policy removal archives metadata and preserves snapshots. |
| `evaluations` | Append-only evaluation results with applicant/policy IDs and names, optional `evaluationRunId`, `policyVersionId`, `policyVersion`, `overallDecision`, `evaluatedOn`, and per-condition results (including actual/required values and explanation). Legacy records without a run ID remain readable and are shown as untracked history. |
| `programs` | Configurable program templates and field schemas, including type, label, required status, validation rules, permitted values, and supported operators. |

- **Repositories:** Mongo adapters are `ApplicantRepository`, `MongoPolicyRepository`, `PolicyVersionRepository`, and `EvaluationRepository`. In-memory adapters include applicant, policy, and evaluation-result repositories.
- **Versioning:** policy snapshots are inserted rather than replaced. A unique `{policyId, version}` index is created by policy-version repository code and by `DatabaseInitializer`. Active version and archive/category fields are stored on the policy document. Removing a policy archives it; snapshots remain available for historical traceability.
- **Indexes/validation:** `DatabaseInitializer` defines validators for the four collections; it creates a sparse unique `applicantId` index, unique compound policy/version index, and applicant/policy-version evaluation indexes. Updating existing validators uses MongoDB `collMod` and requires appropriate privileges.
- **Evaluation writes:** completed service runs are saved as a batch through `EvaluationResultRepository`; cancellation is tested not to persist partial runs. Results retain the policy name, version identity, timestamp, decision, and per-condition explanations. Demo cleanup removes demo applicants and archives the demo policy without deleting evaluations or snapshots.

## I. Task 1 — Configurable programs and field registry

- Added `NUMBER`, `TEXT`, `BOOLEAN`, and `MULTI_SELECT` field definitions with labels, required status, validation rules, permitted values, and type-compatible operators.
- Added Internship, Scholarship, Campus Placement, Master's Admissions, Hackathon, Training Program, and Custom templates, plus in-memory and MongoDB program repositories and BSON mapping.
- Added dynamic applicant custom-attribute lookup and generic `FieldCondition` evaluation/serialization without introducing a Java model class per custom field.
- Added program administration for creation, template-based creation, rename, field add/edit, duplication, selection, and archive. Program operations and database-backed template initialization run through the existing executor.
- Added program-linked policy schema checks for known fields, condition type/operator/value compatibility, and applicant required/value validation. Existing program-unlinked policies and applicant storage remain supported.
- Schema field removal and type changes fail unless an explicit `ProgramMigration` callback is supplied. This is a guard/extension point, not an automatic Atlas migration; callers must implement and verify data and policy migration before accepting such a change.
- Program schema validation and service behavior have JUnit coverage. Live Atlas persistence and JavaFX interaction were not exercised.

## F. Testing and Build Status

| Command executed during this update | Outcome |
|---|---|
<<<<<<< Updated upstream
| Task 0 baseline `mvn test` | **BUILD SUCCESS** — 16 tests, 0 failures, 0 errors, 3 Mongo integration tests skipped because no reachable configured test URI was available. |
| Task 0 updated `mvn test` | **BUILD SUCCESS** — 23 tests, 0 failures, 0 errors, 3 Mongo integration tests skipped because no reachable configured test URI was available. |
| Task 1 final `mvn test` | **BUILD SUCCESS** — 38 tests, 0 failures, 0 errors, 3 Mongo integration tests skipped because no reachable configured test URI was available. |
| Task 2 Stage A final `mvn test` | **BUILD SUCCESS** — 44 tests, 0 failures, 0 errors, 3 Atlas-gated integration tests skipped. Compiled 83 production and 14 test Java source files. |
| Task 3 Stage A final `mvn test` | **BUILD SUCCESS** — 50 tests, 0 failures, 0 errors, 3 Atlas-gated integration tests skipped. Compiled 83 production and 15 test Java source files. |
| Task 4 Stage A final `mvn test` | **BUILD SUCCESS** — 58 tests, 0 failures, 0 errors, 3 Atlas-gated integration tests skipped. |

The Task 0 final test build compiled 67 production and 10 test Java source files. The Task 1 final test build compiled 82 production and 13 test Java source files. Maven reported JavaFX effective-model warnings, a `-source`/`-target` module-path warning (suggesting `--release`), and deprecated/unchecked use in `ApplicantsView`. Package/shaded-JAR creation, JavaFX application startup, and live MongoDB verification were not run.
=======
| `mvn clean compile` | **BUILD SUCCESS** — compiled all 66 production Java source files. |
| `mvn clean test` | **BUILD SUCCESS** — 16 tests reported, 0 failures, 0 errors, 3 skipped. The `ApplicantRepositoryTest` Mongo integration tests were skipped because MongoDB was unavailable. |
| `mvn package -DskipTests` | **BUILD SUCCESS** — shade plugin created `target/ruleweaver-x-1.0-SNAPSHOT.jar`. Its manifest names `com.ruleweaver.MainLauncher`; the entrypoint and `css/style.css` are embedded. |

The theme-stage test build compiled 66 production and 7 test Java source files. Maven reported JavaFX effective-model warnings, a `-source`/`-target` module-path warning (suggesting `--release`), and deprecated/unchecked use in `ApplicantsView`; the UI had not yet been launched at that stage.

### Ikonli startup fix (2026-10-12)

- **Root cause:** The Material Design 2 pack is installed at Ikonli 12.3.1. Its generated handlers use a separate prefix for each icon-letter group (for example, `mdi2p-plus` and `mdi2v-view-dashboard`); the shared icon map incorrectly used the unsupported generic `mdi2-` prefix. All 20 mapped literals were checked against the installed pack and corrected.
- **Regression coverage:** `IconsTest` constructs every configured icon and confirms an unknown application icon fails fast with a readable `IllegalArgumentException`; no dependency changes were needed.
- **Verification:** `mvn clean compile` succeeded; `mvn test` succeeded with 19 tests, 0 failures/errors/skips. `mvn javafx:run` started without the icon-resolution exception and remained running after initial startup. The process was stopped after the startup smoke check, so Maven exited with code 1 on termination.
- **Environment note:** The first clean-compile attempt encountered a JVM 25 access violation; retrying the same command succeeded. The test command also passed after the final icon mappings were applied.
>>>>>>> Stashed changes

## G. Configuration and Security

- `MongoConfig` checks environment variables first: `RULEWEAVER_MONGO_URI` and `RULEWEAVER_DB_NAME`. If unset, it loads `config.local.properties` from the application directory and then `~/.ruleweaver/config.local.properties`; the application-directory file takes precedence when both define the same setting. The database-name fallback is `ruleweaver`.
- `MongoConnectionDialog` verifies a connection with `ping` before saving settings locally. `MongoConfig` sanitizes Mongo credential text in error messages.
- `config.example.properties` is the safe template: it contains only the URI placeholders `<username>`, `<password>`, and `<cluster-host>`, plus the default database name. No secret values are recorded here.
- `config.local.properties` is intended to remain local and `.gitignore` excludes it; `git check-ignore` confirmed it is ignored. Ignoring a file is not a substitute for avoiding accidental force-adds or committing secrets.
- The local config file contains connection settings and is not reproduced here. Keep its credentials private and rotate them if they remain active. Use a least-privilege account for the application/test databases; reserve `DatabaseInitializer` for an account with required collection/index/`collMod` privileges. If credentials have ever been committed, rotate them and review repository history; this update did not audit or alter Git history.

## H. Recent Changes

- `d6e4731` (2026-10-11), current branch: progress/handoff documentation update; no application source files changed in that commit.
- `da3b224` (2026-10-11): Dashboard rebuild and what-if simulator, rule-builder polish, JavaFX styling/components, and a `WhatIfSimulatorTest`.
- `b185730` (2026-10-11): foundational domain/engine, Mongo configuration and repositories, service layer, JavaFX shell/views, schema initialization, Maven packaging setup, and initial tests.
- Earlier history includes persistence/integration-test work (`4317107`), the `Main.java` entry point (`7001084`), and codebase/domain consolidation (`bc072cd`).

## J. Next Steps

<<<<<<< Updated upstream
1. **Review and approve Task 4 Stage A** before proceeding. Task 4 Stage B and Task 5 have not been started.
2. **Run a UI smoke pass** across all views, including the Task 3 Impact Lab controls and asynchronous refresh; first confirm the app can connect using local/environment configuration.
3. **Test typed Atlas persistence end-to-end** for policy snapshots, evaluation batches, restart/readback, and initializer validators using a non-production database.
4. **Close test gaps** for comparison of stored evaluation runs and Mongo policy/evaluation/program mappers and repositories.
5. **Determine an application-level migration strategy** before enabling destructive program-schema migrations.
6. **Add CI and packaging checks** for the supported JavaFX platforms after the test and UI smoke paths are reproducible.

## K. Task 2 — Insight Center Stage A

- Added program, policy, saved-version, evaluation-run, and decision-status filters while retaining existing dashboard and what-if functionality.
- Added testable analytics for evaluated/eligible/rejected counts, pass rate, near-misses, the decision funnel, common rejection reasons, and rule-level impacts; displayed sample sizes and metric denominators.
- Compared a selected saved version with the immediately preceding saved version, pairing by applicant ID. The comparison cohort excludes the decision-status filter; the evaluation-run filter applies only to current results. No causal trend is inferred.
- Assigned a persisted run ID to each service evaluation batch. Legacy records without IDs remain readable and are grouped as untracked rather than assigned fabricated run history.
- Dashboard refresh runs off the JavaFX Application Thread, cancels superseded tasks, ignores stale results, and renders loading, empty, and retryable error states.
- Verification: `mvn test` **BUILD SUCCESS** — 44 tests, 0 failures, 0 errors, 3 Atlas-gated integration tests skipped; `git diff --check` passed. JavaFX interaction and live Atlas behavior remain unverified.
- **Stage B and Stage C are not started; implementation is paused for review as requested.**

## L. Task 3 — Impact Lab Stage A

- Extended the existing dashboard what-if panel into an Impact Lab with program, policy, and base-version selection.
- Added up to three compatible numeric threshold editors with sliders and exact numeric input, plus Boolean selectors and multi-select controls driven by the selected program schema where values are declared.
- Recalculates both the selected saved base version and the adjusted in-memory version over the current applicant cohort using the shared `DecisionEngine`. No evaluation or proposed-policy write is made by the simulator.
- Displays eligible/rejected counts, absolute changes, pass-rate percentage-point change, and applicants whose decision changed with changed condition explanations where determinable. Empty cohorts remain explicit.
- Runs simulations on the existing worker executor with progress, cancellation, error reporting, and generation checks to discard stale results. The existing explicit Policy Studio draft handoff remains available.
- Verification: `mvn test` **BUILD SUCCESS** — 50 tests, 0 failures, 0 errors, 3 Atlas-gated integration tests skipped; `git diff --check` passed. JavaFX interaction and live Atlas behavior remain unverified.
- Applicant records have no program-membership field, so program selection scopes compatible policies/schema controls but does not independently segment the applicant cohort. Task 3 Stage B and Task 4 Stage B are not started.

## M. Task 4 — Policy Studio Stage A

- Replaced the flat-only modal editor with a visual draft tree supporting nested AND/OR groups, condition/group add, removal, duplication, condition reordering, and group collapse/expand.
- Added schema-driven field, operator, and value editors for NUMBER, TEXT, BOOLEAN, and MULTI_SELECT, with a built-in legacy GPA/interview/skills schema when no program is selected.
- Added validation for empty groups, unknown fields, missing/invalid values, unsupported operators, and numeric/schema bounds. Saving remains disabled until the draft is valid.
- Added debounced natural-language and read-only tree previews from the same draft representation used to build engine `Condition` objects.
- Added an asynchronous draft impact preview using the shared `DecisionEngine` and current applicant cohort. New drafts are compared with an empty-policy baseline; version drafts are compared with their source saved version. Results remain in memory and stale/cancelled work is ignored.
- Kept existing create-policy, save-version, and rule-change summary behaviors. Failed policy validation leaves the modal draft intact; no new Stage B version-creation, change-note, or audit workflow was added.
- Offline rule-draft tests cover nested logical evaluation, field types and validation, preview/tree consistency, draft isolation, duplicate rules, impact calculations, and rejected saves without repository writes.
- Task 4 Stage B and Task 5 have not been started; Stage A is paused for review.
=======
1. **Run a UI smoke pass** across all five views and record observed behavior; first confirm the app can connect using local/environment configuration.
2. **Test typed Atlas persistence end-to-end** for policy snapshots, evaluation batches, restart/readback, and initializer validators using a non-production database.
3. **Close test gaps** for rule-engine edge cases, comparison of stored evaluation runs, and Mongo policy/evaluation mappers/repositories.
4. **Confirm product scope and finish deferred features** (archive/search/audit, composite rule editing, export) before scheduling implementation.
5. **Add CI and packaging checks** for the supported JavaFX platforms after the test and UI smoke paths are reproducible.

## J. UI Theme Modernization — Staged Delivery

- **Dependency compatibility:** AtlantaFX 2.0.1 requires JavaFX 17 or newer, so it is compatible with JavaFX 20.0.1 and does not require changing the Java 17 baseline. Ikonli 12.3.1 is included with its JavaFX bridge and Material Design 2 pack.
- **Theme and author CSS:** `Theme` selects `PrimerDark` as the sole AtlantaFX user-agent stylesheet entry point. `style.css` remains applied as the scene author stylesheet; default list, combo box, table, scrollbar, dialog, and control styling overrides that conflict with AtlantaFX were removed.
- **Icons:** Replaced in-house SVG path data with Ikonli FontIcons and CSS-backed icon colors. Updated shared components and icon call sites to use the new node type without redesigning later screens.
- **Stage completed:** Sidebar and header markup/styles now use AtlantaFX button and typography helpers where applicable, and inline styles were removed from those shell components.
- **Style-specific tests:** No `StyleSheetTest` or `StyleClassUsageTest` exists in `src/test/java`.

| Screen stage | Implementation status | Visual result |
|---|---|---|
| Sidebar and header | Theme foundation and shell stage implemented | **Needs manual check** |
| Dashboard | Not started; awaiting sidebar/header review | **Needs manual check** |
| Applicants | Not started; awaiting earlier stages | **Needs manual check** |
| Policies | Not started; awaiting earlier stages | **Needs manual check** |
| Evaluation | Not started; awaiting earlier stages | **Needs manual check** |
| Time Machine | Not started; awaiting earlier stages | **Needs manual check** |
>>>>>>> Stashed changes
