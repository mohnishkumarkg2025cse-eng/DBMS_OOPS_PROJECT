# RuleWeaver X — Project Progress

## A. Project Overview

RuleWeaver X is a JavaFX desktop application for defining versioned decision policies, evaluating applicants with explainable rule results, and comparing policy outcomes. The implementation includes MongoDB persistence and an in-memory repository for service tests/offline initialization.

| Item | Current state |
|---|---|
| Stack | Java 17, JavaFX 20.0.1, Maven, MongoDB Java sync driver 5.12.0, JUnit 5.9.0 |
| Current branch | `main` |
| Latest commit | `d6e4731` — `Update RuleWeaver X implementation` |
| Working tree | This document update is the only intended working-tree change; local config remains ignored. |
| Main source/test layout | `src/main/java`, `src/main/resources`, `src/test/java` |

## B. Completed Features

These items have implementation evidence and automated test evidence. Test evidence does not imply that the JavaFX UI was manually exercised.

- [x] **Applicant Mongo repository CRUD:** insert, read, list, update, and delete are exercised against the configured Mongo test database by `ApplicantRepositoryTest.testApplicantCRUD`.
- [x] **Rule condition serialization and compatibility:** `ConditionDocumentMapper` round-trips nested AND/OR composite rules, accepts legacy condition fields, and defaults legacy composites to AND; covered by `ConditionDocumentMapperTest`.
- [x] **Applicant/evaluation BSON mapping and schema shape:** applicant/evaluation mappers and initializer schema fields are covered by mapper and `DatabaseInitializerSchemaTest` tests.
- [x] **Policy service evaluation-run semantics:** in-memory service tests verify append-only repeated runs, progress reporting, cancellation without partial persistence, immutable versions, and active-version selection (`PolicyServicePersistenceTest`).
- [x] **What-if simulation logic:** `WhatIfSimulatorTest` verifies a threshold change against the latest stored baseline, reports the decision delta/flip, and leaves the original version unchanged.
- [x] **Mongo collection-level persistence checks:** `ApplicantRepositoryTest` exercises applicant CRUD, unique policy-version numbering, and evaluation document save/read against `ruleweaver_test`. These tests do not cover the complete typed application-to-Atlas workflow.

## C. Partially Completed Features

- [/] **JavaFX application and views:** `MainApp`, `ViewRouter`, shared controls, and the Dashboard, Applicants, Policies, Evaluation, and Comparison views are implemented and compile as part of the test build. The UI was not launched or manually checked in this update; layout, dialogs, keyboard shortcuts, progress/cancel/retry, and navigation behavior remain unverified.
- [/] **Policy and evaluation persistence in the running app:** `MongoPolicyRepository`, `PolicyVersionRepository`, `EvaluationRepository`, BSON mappers, and `AppContext` wiring are present. Unit tests cover in-memory service behavior and mapper/schema contracts, while the Atlas tests exercise raw document repository operations; typed policy/evaluation round trips through the full UI are not verified.
- [/] **Dashboard and what-if UI:** `DashboardView` implements result widgets, Evaluate All, and a what-if flow; `WhatIfSimulator` logic is tested. Interaction/rendering and Atlas-backed dashboard refresh have not been manually verified.
- [/] **Policy rule builder:** `ConditionRow` and `PoliciesView` implement numeric/skill rows, validation, preview, row reordering, and version-diff presentation. The editor supports numeric and individual skill conditions, not a visual nested/composite or any-of builder; UI behavior is not tested.
- [/] **Policy administration and operations:** policy category/archive fields, version selection, and active-version persistence exist in the model/repository. A complete archive/search/audit workflow, evaluation export, and end-user operation checks are not established by the current code/tests.

## D. Pending Tasks

- [ ] Manually launch the JavaFX application and exercise the five views, including CRUD dialogs, rule-builder validation, navigation, evaluation history, comparison, Dashboard widgets, and what-if draft flow.
- [ ] Add integration coverage for `MongoPolicyRepository` and typed `EvaluationRepository.saveAll`/readback, then verify policy and evaluation flows through `AppContext` against a non-production database.
- [ ] Add direct tests for decision-engine edge cases and stored-result comparison behavior; current tests primarily cover mappers, service persistence semantics, repository operations, and what-if behavior.
- [ ] Decide and implement remaining product scope: policy archive/search/audit, nested or any-of rule editing, and evaluation export.
- [ ] Establish CI and verify platform-specific Maven packaging/launch behavior; the configured shade and JavaFX platform profiles have not been exercised in this update.

## E. Database and Persistence

- **Application database:** `ruleweaver` is the default in `MongoConfig` and in `config.example.properties`; `RULEWEAVER_DB_NAME` can override it. The Java configuration loader does not embed a MongoDB URI.
- **Integration-test database:** `ApplicantRepositoryTest` connects to `ruleweaver_test`, pings it, and removes its generated test records in cleanup.
- **Collections and records:**

| Collection | Persisted shape / purpose |
|---|---|
| `applicants` | `_id`, `name`, `email`, `gpa`, `interviewScore`, `skills`, `status`, and optional `customAttributes`; mapped by `ApplicantDocumentMapper`. |
| `policies` | `_id`, `name`, `description`, `category`, `activeVersionNumber`, and `archived`; saved by `MongoPolicyRepository`. |
| `policy_versions` | Immutable policy snapshots with `_id`, `policyId`, `version`, `description`, `createdAt`, and serialized `conditions`. `ConditionDocumentMapper` handles numeric, skill, and nested composite conditions. |
| `evaluations` | Append-only evaluation results with applicant/policy IDs and names, `policyVersion`, `overallDecision`, `evaluatedOn`, and per-condition results (including actual/required values and explanation). |

- **Repositories:** Mongo adapters are `ApplicantRepository`, `MongoPolicyRepository`, `PolicyVersionRepository`, and `EvaluationRepository`. In-memory adapters include applicant, policy, and evaluation-result repositories.
- **Versioning:** policy snapshots are inserted rather than replaced. A unique `{policyId, version}` index is created by policy-version repository code and by `DatabaseInitializer`. Active version and archive/category fields are stored on the policy document.
- **Indexes/validation:** `DatabaseInitializer` defines validators for the four collections; it creates a sparse unique `applicantId` index, unique compound policy/version index, and applicant/policy-version evaluation indexes. Updating existing validators uses MongoDB `collMod` and requires appropriate privileges.
- **Evaluation writes:** completed service runs are saved as a batch through `EvaluationResultRepository`; cancellation is tested not to persist partial runs. `EvaluationRepository` also retains raw-document save and demo cleanup methods.

## F. Testing and Build Status

| Command executed during this update | Outcome |
|---|---|
| `mvn clean test` | **BUILD SUCCESS** — 16 tests, 0 failures, 0 errors, 0 skipped. The three `ApplicantRepositoryTest` Mongo integration tests ran rather than being skipped. |

The test build compiled 65 production and 7 test Java source files. Maven reported JavaFX effective-model warnings, a `-source`/`-target` module-path warning (suggesting `--release`), and deprecated/unchecked use in `ApplicantsView`. Package/shaded-JAR creation and JavaFX application startup were not run in this update.

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

## I. Next Steps

1. **Run a UI smoke pass** across all five views and record observed behavior; first confirm the app can connect using local/environment configuration.
2. **Test typed Atlas persistence end-to-end** for policy snapshots, evaluation batches, restart/readback, and initializer validators using a non-production database.
3. **Close test gaps** for rule-engine edge cases, comparison of stored evaluation runs, and Mongo policy/evaluation mappers/repositories.
4. **Confirm product scope and finish deferred features** (archive/search/audit, composite rule editing, export) before scheduling implementation.
5. **Add CI and packaging checks** for the supported JavaFX platforms after the test and UI smoke paths are reproducible.
