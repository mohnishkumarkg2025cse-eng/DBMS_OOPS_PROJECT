# RuleWeaver X — Project Handoff

## Project summary

RuleWeaver X is an explainable rule-based decision system for evaluating applicants against versioned policies. Stack: Java 17 source/target, JavaFX 20 controls, MongoDB Atlas driver, Maven, and JUnit 5. Source-of-truth order: existing code > prompts > project PDF. This document reflects the working tree inspected on 2026-10-11; it deliberately does not contain secrets.

## Phase status

| Phase | Scope | Status | Verified how |
|---|---|---|---|
| Backend + tests | Domain models, engine, BSON mappers, Mongo repositories, unit/integration tests | Partial | `mvn clean test`: 16 run, 0 failures/errors, 3 Atlas integration tests skipped because the endpoint was unreachable |
| UI shell/CSS/components | `MainApp`, sidebar/router/header, shared controls and dark CSS | Partial | `mvn clean compile`; rendered theme needs manual check |
| Phase 3 Applicants | Table, search, create/edit/delete modal, async repository calls | Partial | Compile/test only; CRUD and clicks need manual check |
| Foundation changes A–J | Configuration, condition fields, policy persistence, packaging, seed data, shortcuts | Partial | Test/compile/package were run; Atlas/UI behavior needs manual check |
| Phase 4 Policies | Policy master/detail, immutable versions, persisted active version, version history, rule-builder polish, simulator link/draft | Partial | Unit tests pass; rule-builder and simulator UI need manual check |
| Phase 5 Evaluation | Policy/version evaluation, append-only result persistence, progress/cancel/retry/history | Partial | Service and mapper unit tests pass; Atlas persistence/UI need manual check |
| Phase 6 Dashboard | Responsive KPI/chart/evaluation/attention widgets from stored results; background Evaluate All and read-only what-if simulator | Partial | `mvn clean compile` and `mvn clean test`: passed; rendered layout, interactions, and Atlas-backed behavior need manual check |
| Phase 7 Comparison | Compare stored results for selected policy versions | Partial | Compiles; UI and Atlas-backed behavior need manual check |
| Phase 8 Polish | Dark control/popup theme and motion helpers | Partial | `mvn clean compile`; rendered appearance needs manual check |
| Packaging/deployment | Shade plugin, `MainLauncher`, JavaFX platform profiles | Done | `mvn clean package -DskipTests`; shaded JAR was launched briefly and started |
| CI/CD | CI configuration | Not started | UNKNOWN - check `.github`; no CI workflow was inspected/verified |
| Docs | README exists; this handoff added | Partial | README revision exists in Git history; documentation accuracy needs ongoing updates |

## Foundation items A–J status

| Item | Status | Evidence / files |
|---|---|---|
| A — Mongo configuration | Done | `config/MongoConfig.java`, `ui/components/MongoConnectionDialog.java`, `config.example.properties` |
| B — `ConditionResult.requiredValue` and type | Done | `model/ConditionResult.java`, engine conditions, `ui/adapter/EvaluationResultDocumentMapper.java` |
| C | UNKNOWN - check original task/prompt | No A–J definition for C is present in source |
| D — policy category, active version, archive fields | Done | `model/Policy.java`, `repository/MongoPolicyRepository.java` |
| E | UNKNOWN - check original task/prompt | No A–J definition for E is present in source |
| F | UNKNOWN - check original task/prompt | No A–J definition for F is present in source |
| G — typed Mongo policy repository and recursive conditions | Done | `repository/PolicyRepository.java`, `MongoPolicyRepository.java`, `engine/ConditionDocumentMapper.java` |
| H — shaded executable | Done | `pom.xml`, `MainLauncher.java` |
| I — Java/Maven `.gitignore` | Done | `.gitignore` |
| J — idempotent seed/clear demo data | Partial | `ui/util/SampleDataSeeder.java`, confirmation in `ui/MainApp.java`; Atlas behavior needs manual check |

## Architecture map

```text
com.ruleweaver
├─ config        MongoConfig: resolve config, create/close Mongo client
├─ engine        Condition hierarchy, operators, decision engine, BSON condition mapper
├─ model         Applicant, Policy/PolicyVersion, evaluation and comparison DTOs
├─ repository    Mongo and in-memory persistence adapters
├─ service       PolicyService and ComparisonService
├─ ui            AppContext, JavaFX application, shell, views, components, mappers, utilities
├─ util          DatabaseInitializer and command-line SeedData
└─ demo          Console demo/test runner
```

- `AppContext` is the composition root/state holder. It owns a four-thread daemon executor, observable applicants/policies/results, selected policy/version properties, repositories, and services. It starts in-memory and swaps to Mongo repositories after a successful ping.
- `ViewRouter` replaces the centre view and calls `onHide()`/`onShow()` on `BaseView` instances.
- Views: `DashboardView` builds KPI/charts; `ApplicantsView` manages applicant table/modals; `PoliciesView` is policy/version master-detail with a live rule builder (preview sentence, row add/remove/reorder, Save disabled until valid, diff chips vs previous version); `EvaluationView` evaluates and shows results; `ComparisonView` compares versions.
- `PolicyService` creates policies/versions, evaluates applicants, and compares versions. It retains a reflection-compatible constructor path. `ComparisonService` compares evaluation results for two versions.
- `MongoPolicyRepository` persists `policies` and `policy_versions`; `ApplicantRepository`, `PolicyVersionRepository`, and `EvaluationRepository` use Mongo `Document`; in-memory repositories support local/demo use.
- `ApplicantDocumentMapper`, `EvaluationResultDocumentMapper`, and `ConditionDocumentMapper` map between Java models and BSON documents.
- Current intended data flow: UI -> `Task`/`AppContext` -> service or repository -> Atlas/in-memory store -> JavaFX task completion -> observable UI state. Evaluation now routes through `PolicyService`; dashboards and comparisons query stored results.

## API inventory (current public signatures)

| Type | Current API |
|---|---|
| `PolicyService` (recent) | `PolicyService(PolicyRepository,Object,DecisionEngine,ComparisonService)`; `PolicyService(Object,Object,DecisionEngine,ComparisonService)`; `createPolicy(String,String,String,List<Condition>)`; `createNewVersion(String,String,List<Condition>)`; `evaluateApplicants(String,int,List<Applicant>)`; `compareVersions(String,int,int,List<Applicant>)` |
| `PolicyRepository` | `save(Policy)`; `findById(String)`; `findAll()`; `deleteById(String)`; `findVersion(String,int)`; `findLatestVersion(String)`; `saveVersion(String,PolicyVersion)` |
| policy implementations | `MongoPolicyRepository(MongoDatabase)` and `InMemoryPolicyRepository()` implement `PolicyRepository` |
| `PolicyVersionRepository` | `PolicyVersionRepository(MongoDatabase)`; `save(Document)`; `saveVersion(Document)`; `findByPolicyIdAndVersion(String,int)`; `findByPolicyId(String)`; `findAll()` |
| `EvaluationRepository` | `EvaluationRepository(MongoDatabase)`; collection `evaluations`; `save(Document)`; `save(EvaluationResult)`; `saveAll(List<EvaluationResult>)`; `findByPolicyAndVersion(String,int)`; `countByPolicyAndVersion(String,int)`; `findAll()`; legacy applicant/version lookups and demo cleanup |
| `EvaluationResultRepository` | `save(EvaluationResult)`; `saveAll(List<EvaluationResult>)`; `findByPolicyAndVersion(String,int)`; `findAll()`; `countByPolicyAndVersion(String,int)` |
| `PolicyService` (current) | `evaluateApplicants(String,int,List<Applicant>)`; progress/cancellation overload; `createNewVersion(String,String,List<Condition>)`; `setActiveVersion(String,int)` |
| `AuditLogRepository` | UNKNOWN - class is absent from `src/main/java` |
| `DecisionEngine` | `evaluate(Applicant,PolicyVersion)`; `evaluateAll(List<Applicant>,PolicyVersion)` |
| `ConditionResult` (recent) | no-arg constructor; `(String,String,boolean,Object,String)`; `(String,String,boolean,Object,Object,String)`; getters/setters for name, type, pass, actual, required, explanation |
| conditions | `Condition`; `NumericCondition(String,String,double)` / `(String,Operator,double)`; `SkillCondition(String)`; `CompositeCondition(String,LogicalOperator)` / `(String,LogicalOperator,List<Condition>)` |
| `Operator` | `GREATER_THAN_OR_EQUAL`, `LESS_THAN_OR_EQUAL`, `GREATER_THAN`, `LESS_THAN`, `EQUALS`, `NOT_EQUALS`; `getSymbol()`; `fromString(String)`; `apply(double,double)` |
| `FieldRegistry` | UNKNOWN - class is absent from `src/main/java` |

## Data model and Atlas collections

| Collection / model | Current persisted fields |
|---|---|
| `applicants` | `_id`, `name`, `email`, `gpa`, `interviewScore`, `skills`, `status`, optional `customAttributes` (`ApplicantDocumentMapper`) |
| `policies` | `_id`, `name`, `description`, `category`, `activeVersionNumber`, `archived` (`MongoPolicyRepository`) |
| `policy_versions` | `_id`, `policyId`, `version`, `description`, `conditions`; condition documents carry `conditionType` plus type-specific fields |
| `evaluations` | The single evaluation collection name used by `EvaluationRepository` and `DatabaseInitializer`; mapper fields are `_id`, `applicantId`, `applicantName`, `policyId`, `policyName`, `policyVersion`, `overallDecision`, `evaluatedOn`, and `conditionResults` |
| `policy_versions` | `_id`, `policyId`, `version`, `description`, `createdAt`, `conditions`; a unique `{policyId, version}` index enforces uniqueness |

Additive/default-compatible fields: `Policy.category` defaults to `Other`; `Policy.activeVersionNumber` resolves to latest version (or `0`); `Policy.archived` defaults to `false`; `ConditionResult.requiredValue` may be `null`; `conditionType` and `requiredValue` are read as nullable for old evaluation documents. Old numeric BSON conditions may use `type`/`value`; the mapper accepts those. Legacy composite conditions without `logicalOperator` default to `AND`.

## Decisions and scope cuts

- Atlas policy persistence exists via `MongoPolicyRepository`; policy versions are inserted, never replaced, and duplicate version numbers raise an explicit immutable-version error. A unique compound index backs the check.
- `Policy.activeVersionNumber` is saved on the policy document. Legacy policies with no active-version value continue to resolve to their latest version.
- Evaluation runs are append-only. `PolicyService.evaluateApplicants` accumulates a run, reports per-applicant progress, checks cancellation, and batch-inserts results only after the full run completes.
- `EvaluationView`, `DashboardView`, and `ComparisonView` now use `PolicyService`/stored results rather than the evaluation view or dashboard recomputing directly through `DecisionEngine`. `AppContext.resultsChangedProperty()` signals successful persisted runs.
- The Dashboard what-if simulator deep-clones conditions through `ConditionDocumentMapper`, changes only the selected numeric threshold, and evaluates in a background `Task`. It reads applicants and saved results but performs no writes. Delta uses saved baseline decisions for applicants with matching stored results; applicants without a saved baseline are excluded from the comparison.
- Dashboard KPI, pie, compliance, recent-evaluation, and attention widgets each load in their own background `Task`, with skeleton/empty/error-retry states. The responsive GridPane uses two columns below 1300px and three at/above it; Evaluate All reports progress and supports cancellation through `PolicyService`.
- The Dashboard donut overlays mouse-transparent center labels on the PieChart; compliance bars and outcome slices expose tooltips. Visual sizing and click behavior have not been observed in a live window.
- “Save as New Version” routes to Policies and opens the existing version editor prefilled with the simulated conditions; saving remains a separate user action.
- Numeric operators are exactly the six values in `Operator`; there is no `BETWEEN` operator.
- `CompositeCondition` supports `AND`/`OR`; an any-of skill UI builder is UNKNOWN - current `ConditionRow` only builds numeric or one skill condition. The builder preview joins those rows with AND (for example `Eligible if GPA is at least 8.0 AND Interview Score is at least 70 AND has skill Java`). Save stays disabled until there is at least one rule, no exact duplicates, numeric GPA 0–10 / interview 0–100, and (for new policies) a name. New inline errors shake via `Motion.shake`. Diff chips compare the draft or displayed version against the previous version.
- Mandatory/Preferred severity is UNKNOWN - no severity model exists in the code.
- Field registry design is UNKNOWN - no `FieldRegistry` exists; `Applicant.getNumericFieldValue()` currently branches on built-in fields/custom attributes.
- Atlas-backed `ApplicantRepositoryTest` uses JUnit assumptions and skips without `RULEWEAVER_MONGO_URI`; it uses `ruleweaver_test` and cleans its generated IDs.
- Old evaluation documents without `conditionType`, `requiredValue`, or an evaluation timestamp remain readable; missing timestamps display as `Unknown`.
- The collection validator update in `DatabaseInitializer` uses MongoDB `collMod` for existing collections and therefore needs an account with the required database privileges.

## Configuration

- URI lookup order in `MongoConfig`: environment `RULEWEAVER_MONGO_URI`, application-local `config.local.properties`, then user-home `.ruleweaver/config.local.properties`.
- Database name: `RULEWEAVER_DB_NAME`, otherwise `ruleweaver`.
- The connection dialog pings before saving locally; `config.example.properties` documents variable names only.
- Atlas integration tests use `ruleweaver_test`, not the application database.

## Known bugs and unverified items

- `ApplicantsView.java:92-126`: scene-level Ctrl+N/Delete registration exists and unregisters on hide; manual focus/modal/leak test is needed.
- `ApplicantsView.java:260-285`: row Edit/Delete handlers exist; manually click both after Atlas connection.
- `LoadingOverlay.java`: configured as mouse-transparent; verify it does not intercept table or toolbar clicks when visible.
- `PoliciesView.java`: policy list dark styling, ellipsis, and horizontal-scroll suppression were compiled but not visually inspected.
- `Theme.java`: stylesheet attachment for Dialog/Alert/Tooltip popup scenes needs manual rendered verification.
- Full UI behavior for progress/cancel/retry, persisted version selection, history counts, dashboard refresh, and comparison still needs manual verification.
- Rule builder live preview, Save-disabled validation, duplicate shake, reorder, and version diff chips: needs manual check (`PoliciesView` / `ConditionRow`).
- Dashboard layout, responsive wrapping, KPI/chart/table/attention states, Evaluate All progress/cancel/retry, what-if controls/results/flips, Policies link, and prefilled new-version draft: needs manual check. A selected version must have stored results to provide a real baseline.
- Evaluation export and the broader audit-log/archive/delete/search workflows are not implemented.
- Atlas connectivity was unavailable during the latest test run, so all three Atlas integration tests were skipped. Successful Atlas-backed behavior remains unverified.

## Security issues

- Current source scan found no hardcoded credential. `config.example.properties` contains placeholders only.
- Git history was searched for Mongo URI patterns. Earlier history includes a committed credential in `MongoConfig.java` and `SeedData.java`; do not rewrite history without explicit approval.
- Rotate the Atlas database-user password, update only `RULEWEAVER_MONGO_URI` or local config, and review access/audit logs.
- `.gitignore` contains `config.local.properties`, `.env`, `target/`, and `logs/` entries.

## Commands that work

| Command | Result actually observed |
|---|---|
| `mvn clean test` | BUILD SUCCESS: Tests run 15, Failures 0, Errors 0, Skipped 3 (`ApplicantRepositoryTest` Atlas cases skipped) |
| `mvn -Dtest=WhatIfSimulatorTest test` | BUILD SUCCESS: 1 test, 0 failures/errors/skips |
| `mvn clean test` (after what-if simulator) | BUILD SUCCESS: Tests run 16, Failures 0, Errors 0, Skipped 3 (`ApplicantRepositoryTest` Atlas cases skipped) |
| `mvn clean compile` (Dashboard rebuild) | BUILD SUCCESS |
| `mvn clean test` (Dashboard rebuild) | BUILD SUCCESS: Tests run 16, Failures 0, Errors 0, Skipped 0 |
| `mvn "-Dtest=ConditionDocumentMapperTest,ApplicantDocumentMapperTest,EvaluationResultDocumentMapperTest,PolicyServicePersistenceTest,DatabaseInitializerSchemaTest" clean test` | Passed: 12 tests, 0 failures/errors/skips |
| `mvn clean package -DskipTests` | Passed; shaded `target/ruleweaver-x-1.0-SNAPSHOT.jar` produced (tests were run separately above) |
| `java -jar target/ruleweaver-x-1.0-SNAPSHOT.jar` | JAR was launched briefly and started, then only that test process was stopped |
| `mvn clean javafx:run` | UNKNOWN - command is configured in `pom.xml`, but was not successfully observed in this handoff session |

## Seed data and expected results

`SampleDataSeeder` creates Alice (8.2, 65, Java/SQL), Bob (7.9, 75, Python/Java), and Carol (8.5, 72, C++/Java), plus Dave and Eve. Demo policy v1 is GPA >= 8.0 AND Interview >= 70 AND Java; v2 changes GPA to >= 8.5.

| Applicant/case | v1 | v2 |
|---|---:|---:|
| Alice | FAIL (interview 65) | FAIL |
| Bob | FAIL (GPA 7.9) | FAIL |
| Carol | PASS | PASS |
| GPA 8.3, interview 80, Java | PASS | FAIL |

## Next steps

1. Manually verify the Dashboard at widths below and above 1300px: header wraps without horizontal scrolling, five KPI cards appear, donut/bar tooltips work, recent/attention empty and populated states display, Evaluate All progress/cancel/retry works, and what-if shows pass delta/flips; verify the Policies link and prefilled new-version draft.
2. Restore Atlas connectivity, then verify CRUD/policy/evaluation flows with a non-production database; Atlas integration tests are currently skipped when the endpoint is unreachable.
3. Manually verify evaluation progress and cancellation, retry/error toast, saved history after restart, active-version persistence/history counts, dashboard scoping/refresh, and comparison of two evaluated versions.
4. Verify existing Atlas collection validators/indexes by running `DatabaseInitializer` with appropriate privileges; check legacy documents remain readable.
5. Complete remaining policy workflows (category/archive/search, composite/any-of skill builder, audit logging) and evaluation export if still required.
6. Perform Phase 8 manual accessibility/theme/shortcut QA; then add CI/CD and deployment documentation after a repeatable Atlas-free test path is established.

## Rules for any agent working here

- No Mongo/service calls on the FX thread; use `Task`/executor and return UI updates through `Platform.runLater`.
- Every new `Task` needs `setOnFailed` logging plus a toast.
- CSS limits from prior direction: no `var()`, `box-shadow`, `backdrop-filter`, `border-radius`, or `transition`; animations belong in Java and must respect `reducedMotion`.
- Do not change existing test assertions. Run `mvn clean test` after each phase.
- Mark UI behavior “verified by running” only when actually observed; otherwise say “needs manual check.”

## Resume instructions

Read this file, run `mvn clean test`, inspect `git status` because substantial work is currently uncommitted, then begin with the first Next steps item. Keep scope anchored in source; if a requested class or workflow is absent, write `UNKNOWN - check <file>` rather than assuming it exists.

### Update protocol

After each phase, update the status table, decisions/scope cuts, known bugs, and next steps. Add one dated Changelog line that identifies whether the change is committed or uncommitted and names the verification command/result.

## Changelog

- 2026-10-11 — Uncommitted Dashboard what-if simulator: background, read-only evaluation on cloned conditions; version/numeric-rule/threshold controls; saved-result delta and applicant flips; Policies link and prefilled new-version draft. `mvn -Dtest=WhatIfSimulatorTest test` passed (1 test) and `mvn clean test` passed (16 run, 0 fail, 0 error, 3 skipped). UI needs manual check.
- 2026-10-11 — Uncommitted Dashboard rebuild: responsive two/three-column layout, header controls, five KPI cards, donut and horizontal compliance charts, recent evaluations, needs-attention list, per-widget background loading/empty/error-retry states, Evaluate All progress/cancel, and retained read-only what-if simulator. `mvn clean compile` passed; `mvn clean test` passed (16 run, 0 failures/errors/skips). UI and interaction behavior need manual check; no live visual observation was made.
- 2026-10-11 — Uncommitted rule-builder polish in `ConditionRow` / `PoliciesView`: live AND preview, duplicate/range validation with Save disabled and `Motion.shake`, add/remove/reorder rows, diff chips vs previous version. `mvn clean test` BUILD SUCCESS (15 run, 0 fail, 0 error, 3 skipped). UI needs manual check.
- 2026-10-11 — Uncommitted core-loop update: standardized evaluation collection/schema, batch append-only persistence, service-driven progress/cancel/retry/history, persisted active policy versions and history counts, stored-result dashboard/comparison refresh; 12 non-Atlas tests pass and `mvn clean package -DskipTests` succeeds. Atlas integration tests are skipped when the endpoint is unreachable. UI behavior needs manual verification.
- 2026-10-10 — `4317107`: persistence layer and integration tests added (Git history).
- 2026-10-10 — `7001084`: `Main.java` entry point added (Git history).
- 2026-10-10 — `bc072cd`: codebase/domain/persistence consolidation (Git history).
- 2026-10-10 — `7c645cd`: README revised (Git history).
- 2026-10-11 — Uncommitted working tree: configuration, engine/repository/UI/theme/packaging changes exist; `mvn clean test` and `mvn clean compile` were run. Review `git status` before committing.
