# RuleWeaver — Dynamic Rule Engine & Management System

**Repository:** `DBMS_OOPS_PROJECT`  
**Root Package:** `com.ruleweaver`  
**Tech Stack:** Java 17+, Maven, MongoDB, JavaFX, JUnit 5  

---

## 📖 About The Project

**RuleWeaver** is an Object-Oriented Database Application designed to automate complex eligibility assessment, rule management, and policy enforcement across dynamic datasets.

In traditional software systems, business rules (such as loan approvals, grant eligibility, or insurance qualification) are hardcoded into application logic. RuleWeaver solves this by decoupling business rules from code logic through a dynamic rule engine. Policies and evaluation conditions are stored dynamically in MongoDB and evaluated at runtime against incoming applicant data.

---

## ✨ Key Features

* **Dynamic Policy Engine:** Define, edit, and evaluate complex conditional logic without re-compiling or re-deploying application code.
* **NoSQL Persistence Layer:** Scalable document-based storage for flexible applicant structures, policy rules, and evaluation histories using MongoDB.
* **Modular Layered Architecture:** Strict separation of concerns between presentation (JavaFX), business logic (Services/Engine), domain models, and data access (Repositories).
* **Robust Domain Model:** Built using clean Object-Oriented Software Engineering principles (Encapsulation, Polymorphism, Abstraction, and Single Responsibility).
* **Automated Eligibility Processing:** Evaluates applicants against active policies and outputs pass/fail statuses with granular rule trace logs.

---

## 🏗️ System Architecture

The project strictly follows standard Maven directory layout and Java package hierarchy:

```text
com.ruleweaver
├── config/        # MongoDB client instantiation and configuration settings
├── repository/    # Data Access Objects (DAO) handling CRUD operations on MongoDB
├── model/         # Core business domain entities (Applicant, Policy, Condition, etc.)
├── service/       # Business logic operations, rule processing, and domain services
└── Main.java      # Application bootstrap & smoke-testing entry point
RuleWeaver X — Integrated Product Implementation Plan
Date: 2026-10-12 Scope: Coordinated transformation of the existing JavaFX/MongoDB application into a multi-program policy-management product. Basis: User-provided nine-point brief, current source tree, and the implementation/status inventory in PROGRESS.md. No separate completed-audit report, architecture artifact, constitution, or feature-spec file was present in the workspace when this plan was prepared. The first task rechecks the audit and source baseline before implementation.

Summary
Deliver functional integrity and cross-module consistency before visual polish. First establish explicit program ownership for applicants, policies, policy versions, and evaluations, with backward-compatible persistence and a safe migration path. Then provide genuinely distinct seed scenarios, complete policy publishing and reproducible evaluation behavior, synchronize shared application state, and derive/persist dashboard snapshots from authoritative evaluation data. Only after those foundations are verified should the team complete shared UI actions, screen workflows, and the coordinated AtlantaFX/Ikonli SaaS presentation.

The project already has Java 17, JavaFX 20.0.1, AtlantaFX 2.0.1, Ikonli 12.3.1, Mongo repositories, program schemas, versioned policies, an explainable decision engine, a Policy Studio draft builder, dashboard analytics, and an embedded Impact Lab. Work should extend these capabilities instead of replacing them. ComparisonView currently represents the comparison/Time Machine surface; no separate TimeMachineView was found. DashboardView contains the existing Impact Lab. Those locations are treated as current placement, subject to the first-task inventory.

Technical Context
Concern	Current evidence and plan
Language/runtime	Java 17, JavaFX 20.0.1; retain unless a concrete compatibility issue is found.
UI dependencies	AtlantaFX 2.0.1 PrimerDark, Ikonli 12.3.1 Material Design 2, and the existing author stylesheet src/main/resources/css/style.css.
Persistence	MongoDB sync driver 5.12.0; collections/repositories for applicants, programs, policies, immutable policy versions, and append-only evaluations.
Application state	ui/AppContext.java owns observable selection and shared repositories/services; view refreshes and selection propagation need explicit tests.
Decision logic	engine/DecisionEngine.java; conditions are persisted by engine/ConditionDocumentMapper.java; outcomes include condition-level explanations and actual values.
Test stack	JUnit 5.9.0/Maven Surefire. Existing suite recorded as 61 tests passing, including three MongoDB ApplicantRepository tests; Atlas availability and app-level workflows are not fully covered.
Migration approach	Additive/backward-compatible document reads, explicit dry-run/report before any persisted backfill, idempotent migration, no destructive seed cleanup, and preserve existing IDs/history.
Metric consistency	Evaluation records remain the source of truth. Any stored dashboard aggregates are rebuildable snapshots keyed by program/filter scope and data revision, never the only copy of a metric.
Secret boundary	Never inspect, print, or log config.local.properties or environment-variable values. Do not include credentials in tests, logs, migration reports, screenshots, or this plan.
Requirements
The following IDs are planning trace IDs derived from the nine numbered user goals; they are not claimed to be IDs from a separate requirements document.

ID	Requirement
REQ-001	Establish reliable program, applicant, policy, version, and evaluation relationships.
REQ-002	Support at least three meaningfully different, repeatable multi-program seed scenarios.
REQ-003	Find and complete all nonfunctional or partially implemented user-facing actions.
REQ-004	Synchronize cross-module state and maintain persisted, consistent dashboard metrics.
REQ-005	Support validated policy construction, publishing, and immutable version history.
REQ-006	Preserve explainable evaluations and reproducible historical decisions.
REQ-007	Provide a reusable dark SaaS design system on the existing AtlantaFX/Ikonli foundation.
REQ-008	Deliver cohesive Dashboard, Programs, Applicants, Policy Studio, Evaluation, Impact Lab, and Time Machine experiences.
REQ-009	Prove behavior with automated tests, workflow-level verification, and a manual visual acceptance checklist.
Constitution Check
No constitution.md was present in the workspace search. The plan therefore applies the explicit project constraints from the user and the recorded project state:

Pass — preserve behavior/data: migrations are additive and idempotent; existing IDs, policy versions, and evaluation history are retained.
Pass — correctness before decoration: data ownership, transactions/synchronization, policy lifecycle, evaluation reproducibility, and tests precede screen polish.
Pass — reuse current architecture: JavaFX, Mongo repositories, AppContext, domain services, DecisionEngine, AtlantaFX, Ikonli, and style.css remain the foundation.
Pass — protect secrets and local configuration: protected files and environment values are out of scope for inspection and logging.
Pass — incremental review: tasks are staged into small testable increments; no all-at-once implementation, commit, or push is proposed.
Pass — retain author CSS: style.css remains in place; theme tokens/classes are extended rather than replaced.
Applied Guidelines
Persistence/data compatibility: read old documents with absent optional values; explicitly test old and new shapes. Backfills must report affected counts and be rerunnable. Never overwrite immutable policy versions or evaluation history.
Domain boundaries: keep policy validation and decision semantics in service/engine layers; UI controls call existing services rather than duplicating business rules.
JavaFX threading: repository/service I/O stays off the JavaFX application thread; observable UI state changes are marshalled to the FX thread; cancellation and stale-response handling are covered.
Mongo consistency: use source evaluation documents as the durable truth. Aggregate snapshots must carry their scope, calculation/data revision, and refresh timestamp; failed refreshes must be visible rather than presented as success.
UI continuity: one shared PrimerDark/Ikonli theme entry point, author stylesheet, shared controls/tokens, responsive layouts, and keyboard/accessibility checks; screen work composes shared primitives.
Test integrity: preserve existing assertions and add targeted unit, repository, migration, and end-to-end workflow tests. External Mongo/Atlas tests must state prerequisites and must not silently pass when unavailable.
Implementation Steps
Phase 1 — Baseline and functional inventory
Step 1.1: [Cross-cutting] Confirm audit findings and establish a safe baseline
Requirements: REQ-003, REQ-009
Design inputs: PROGRESS.md, current source/test tree, user-provided implementation brief.
Description: Inventory every visible control and its outcome across ui/views, ui/components, ui/shell, and AppContext; mark working, partial, inert, error-only, and missing. Record existing test/build results and current uncommitted changes without inspecting protected configuration or environment values. Confirm how the Programs, Impact Lab, and Time Machine areas are reached today.
Expected files: PROGRESS.md; if a separate action inventory is useful, add docs/ui-action-inventory.md without changing source behavior.
Dependencies: None. This is the baseline gate.
Acceptance: Inventory covers every visible action and records its owning view/service, expected outcome, actual handler, error/loading behavior, and test evidence. Existing worktree modifications remain untouched.
Tests: No product test change is required for inventory; record the existing Maven baseline and identify which tests are offline, Mongo-dependent, or absent.
Risks/data: Do not run seed-clear or migration commands; do not read protected local configuration or environment values; do not treat missing test coverage as proof that a workflow works.
Required validation: Review the inventory against all discovered views/components; record exact baseline command outcomes without claiming newly rerun checks.
Completion gate: Reviewable inventory and baseline report exist before model or UI behavior changes begin.
Phase 2 — Foundational data integrity (P1)
Step 2.1: Define ownership, lifecycle, and legacy compatibility rules
Requirements: REQ-001, REQ-004, REQ-005, REQ-006
Design inputs: existing model/*, repository/*, service/*, mapper implementations, and persistence notes in PROGRESS.md.
Description: Specify required and optional programId relationships; define behavior for legacy applicants/policies/evaluations without program ownership; establish rules for archived programs/policies, field-schema evolution, policy publish/activate, immutable versions, and evaluation snapshot references. Decide document/index changes and a dry-run migration report format before altering persisted data. Keep old unlinked records visible and do not guess their program.
Expected files: docs/data-ownership-contract.md and updates to IMPLEMENTATION_PLAN.md only; this is a proposed design decision, not yet an owner-approved source/schema edit.
Dependencies: T001 review of actual persisted fields and active workflows.
Acceptance: The contract defines relationship cardinality, null/legacy handling, cross-program rules, immutable snapshot contents, migration inference boundaries, and operator confirmation before applying any backfill.
Tests: No runtime changes in this task. Define executable model/mapper/migration cases that subsequent tasks must add.
Risks/data: Choosing a program for ambiguous historical records could corrupt attribution; evaluation input snapshots may contain sensitive data; require product/privacy review and never infer ownership.
Required validation: Review the proposed contract against existing model constructors, BSON documents, tests, and representative sanitized fixtures; record unresolved decisions before T003/T004.
Completion gate: A small reviewed relationship/migration contract identifies backfill ambiguity and a safe opt-in resolution policy. No code or data changes in this item.
Step 2.2: Implement backward-compatible relationship models and document mappers
Requirements: REQ-001, REQ-006
Design inputs: approved Step 2.1 contract.
Description: Add or normalize explicit program references and stable snapshot/version references across ProgramConfiguration, Applicant, Policy, PolicyVersion, and EvaluationResult; extend BSON mappers while preserving reads of legacy documents. Add validation for mismatched program references and keep old unassigned data explicitly unassigned.
Expected files: src/main/java/com/ruleweaver/model/{ProgramConfiguration,ProgramSchemaRevision,Applicant,Policy,PolicyVersion,EvaluationResult}.java; new src/main/java/com/ruleweaver/repository/{ProgramSchemaRevisionRepository,MongoProgramSchemaRevisionRepository,InMemoryProgramSchemaRevisionRepository}.java; src/main/java/com/ruleweaver/ui/adapter/{ApplicantDocumentMapper,EvaluationResultDocumentMapper}.java; src/main/java/com/ruleweaver/repository/{ProgramDocumentMapper,MongoProgramRepository}.java; src/main/java/com/ruleweaver/engine/ConditionDocumentMapper.java only if required by the contract.
Dependencies: Step 2.1.
Acceptance: New entities round-trip program/snapshot links; legacy documents still deserialize; mismatched cross-program policy/applicant evaluation is rejected or explicitly handled per contract; versions remain immutable.
Tests: extend ApplicantDocumentMapperTest, EvaluationResultDocumentMapperTest, ProgramDocumentMapperTest, and ConditionDocumentMapperTest; add model/relationship tests under matching src/test/java/com/ruleweaver/{model,ui/adapter} paths.
Risks/data: avoid making programId mandatory on historical data before migration; preserve unknown legacy fields and IDs; never recalculate historical decisions during mapping.
Required validation: focused mapper/model tests, then mvn clean compile and mvn clean test; document counts and zero unintended document deletions for any fixture migration.
Step 2.3: Add safe, idempotent Mongo migration and indexes
Requirements: REQ-001, REQ-004, REQ-006, REQ-009
Design inputs: approved Step 2.1 contract and Step 2.2 schema.
Description: Add a migration runner with dry-run default/report, explicit apply mode, idempotency marker, pre/post counts, and compatibility checks. Backfill only relationships that are deterministically inferable; leave ambiguous records unassigned for user resolution. Add indexes for program-scoped applicants/policies/evaluations and evaluation run/version lookups.
Expected files: new src/main/java/com/ruleweaver/util/DatabaseMigrationRunner.java; src/main/java/com/ruleweaver/util/DatabaseInitializer.java; src/main/java/com/ruleweaver/repository/{ApplicantRepository,EvaluationRepository,MongoPolicyRepository,MongoProgramRepository,PolicyVersionRepository,ProgramSchemaRevisionRepository,MongoProgramSchemaRevisionRepository}.java; new src/test/java/com/ruleweaver/util/DatabaseMigrationRunnerTest.java and repository migration/index tests.
Dependencies: Steps 2.1–2.2.
Acceptance: dry-run does not write; apply can be repeated safely; historical versions/evaluations and IDs remain; uncertain records are reported and not reassigned; legacy reads continue.
Tests: synthetic Mongo-compatible fixtures for old/new schema, rerun/idempotency, ambiguous references, and count preservation; integration test against isolated ruleweaver_test namespace or ephemeral database.
Risks/data: production migration is explicitly opt-in and requires a separately verified backup; no migration is run against user data during plan approval or ordinary tests.
Required validation: migration tests and repository integration suite pass; review the report schema and before/after counts; full Maven suite passes.
Step 2.4: Make shared selection and refresh state coherent
Requirements: REQ-001, REQ-003, REQ-004, REQ-008
Design inputs: AppContext, ViewRouter, all selectors in views, and Step 2.1 relationship rules.
Description: Centralize selected program/policy/version semantics and refresh/invalidation signals. Switching programs refreshes only matching applicants, policies, evaluations, and analytics; archived selections clear or move to a valid selection; async results cannot repopulate the wrong program after a fast switch. Keep persistence I/O asynchronous and failures visible.
Expected files: src/main/java/com/ruleweaver/ui/AppContext.java; src/main/java/com/ruleweaver/ui/shell/{ViewRouter,Sidebar,AppHeader}.java; src/main/java/com/ruleweaver/ui/views/{DashboardView,ProgramManagerView,ApplicantsView,PoliciesView,EvaluationView,ComparisonView}.java; new src/test/java/com/ruleweaver/ui/AppContextSelectionTest.java and service/view-state tests as appropriate.
Dependencies: Steps 2.1–2.3.
Acceptance: program switching keeps every module scoped consistently; no stale async response overwrites current selection; user receives an actionable error when refresh fails.
Tests: program-switch sequence tests, archived/deleted selection tests, stale-result/cancellation tests, and event/invalidation tests.
Risks/data: avoid silently changing existing cross-program selections; preserve app startup behavior for legacy unassigned data.
Required validation: focused tests plus full Maven suite; manual workflow check of switching programs in every module.
Phase 3 — Durable scenarios and business behavior (P1)
Step 3.1: Seed three distinct, idempotent program scenarios
Requirements: REQ-001, REQ-002, REQ-009
Design inputs: ProgramTemplate, field registry, migration rules, current seeder behavior.
Description: Provide three distinct scenarios with their own applicant attributes, rules, policy versions, and evaluation results. Recommended examples: scholarship selection (academic/financial need), internship hiring (skills/interview), and community grant review (impact/budget). Seed data must be deterministic, idempotent, clearly labeled demo data, and never clear or overwrite unrelated records.
Expected files: src/main/java/com/ruleweaver/ui/util/SampleDataSeeder.java; src/main/java/com/ruleweaver/util/SeedData.java; src/main/java/com/ruleweaver/model/ProgramTemplate.java; src/test/java/com/ruleweaver/util/SeedDataInitializationTest.java; add isolated seeder tests.
Dependencies: Steps 2.1–2.4.
Acceptance: selecting each scenario presents meaningfully different fields, applicants, policies, rule types, and outcomes; rerunning seed creates no duplicates and leaves non-demo records unchanged.
Tests: seed counts, distinct schemas/outcomes, idempotency, and preservation of unrelated records in an in-memory repository and isolated Mongo fixture.
Risks/data: current demo cleanup archives a fixed policy and deletes fixed applicants; replace broad/unsafe assumptions with explicit demo ownership and confirmation; never delete evaluations/history.
Required validation: offline and isolated repository tests, then full Maven suite; manual verify seed/clear behavior using only a disposable database.
Step 3.2: Complete policy lifecycle and publish/version guarantees
Requirements: REQ-001, REQ-003, REQ-005
Design inputs: PolicyService, ProgramService, PolicyRuleDraft, PolicyRuleBuilder, PoliciesView, repository semantics.
Description: Unify draft validation with program schemas; expose explicit draft/save/publish/activate/archive actions only where supported; enforce unique immutable versions and active-version rules. Preserve prior snapshots and record lifecycle metadata needed for audit. Reject publishing against archived/mismatched programs.
Expected files: src/main/java/com/ruleweaver/service/{PolicyService,ProgramService,PolicyRuleDraft}.java; src/main/java/com/ruleweaver/model/{Policy,PolicyVersion}.java; src/main/java/com/ruleweaver/repository/{PolicyRepository,MongoPolicyRepository,InMemoryPolicyRepository,PolicyVersionRepository}.java; src/main/java/com/ruleweaver/ui/components/PolicyRuleBuilder.java; src/main/java/com/ruleweaver/ui/views/PoliciesView.java; tests under src/test/java/com/ruleweaver/{service,repository,ui/components}.
Dependencies: Steps 2.1–2.4.
Acceptance: invalid or stale drafts cannot publish; published versions cannot mutate; activation changes take effect consistently; archived entities cannot be newly selected; history remains readable.
Tests: draft invalidation, duplicate version race, active version, archived/mismatched program, immutable snapshot, and migration compatibility tests.
Risks/data: no destructive rewrite of existing versions; legacy policies may have no program association; maintain legacy view path until resolved.
Required validation: policy/service/repository tests and full Maven suite; workflow test creates, publishes, switches, and evaluates two versions.
Step 3.3: Make evaluations explainable and reproducible from saved snapshots
Requirements: REQ-001, REQ-004, REQ-006
Design inputs: DecisionEngine, condition result models, evaluation mapper/repository, immutable policy versions, schema history contract.
Description: Ensure each evaluation stores the exact policy/version and program/schema snapshot identity used, run ID/time, applicant identity and evaluated input snapshot (or a privacy-reviewed immutable subset), final decision, and condition explanations/actual values. Historical display must use stored outcomes and must not silently rerun against current applicant data or current policy fields. Add an explicit “re-evaluate now” path distinct from viewing history.
Expected files: src/main/java/com/ruleweaver/{model/EvaluationResult.java,model/ConditionResult.java,engine/DecisionEngine.java,ui/adapter/EvaluationResultDocumentMapper.java,repository/EvaluationRepository.java,service/PolicyService.java,ui/views/EvaluationView.java,ui/views/ComparisonView.java}; focused service/mapper/repository tests.
Dependencies: Steps 2.1–2.4 and 3.2.
Acceptance: an old result reproduces its displayed explanation and decision after applicant or policy edits; re-evaluation creates a new result/run; missing legacy snapshot fields are presented as legacy/unverified, not fabricated.
Tests: applicant/policy mutation after evaluation, exact snapshot/version links, explanation stability, legacy result rendering, and repeated-run append-only semantics.
Risks/data: snapshotting applicant attributes has privacy/retention implications; capture only fields necessary to explain the decision and define retention before enabling. Do not mutate historical evaluation documents in normal reads.
Required validation: deterministic engine and repository tests; isolated Mongo save/read; full Maven suite; manual verify old/new result history.
Step 3.4: Complete, audit, and wire every existing UI action
Requirements: REQ-003, REQ-008, REQ-009
Design inputs: Step 1.1 control inventory; source handlers in all views and shared components.
Description: For every inventoried action, either implement its intended service workflow, make the unavailable state explicit with a reason, or remove/disable it only with product approval. Cover Program create/rename/duplicate/archive/select/field edits; Applicant create/edit/delete/import/export where controls already exist; policy create/version/publish/archive; evaluate/filter/inspect; comparison/history; dashboard refresh/evaluate/Impact Lab save/cancel; navigation, retry, toast, and dialog actions. No decorative or inert action may masquerade as functional.
Expected files: as indicated by inventory, primarily ui/views/{ProgramManagerView,ApplicantsView,PoliciesView,EvaluationView,ComparisonView,DashboardView,PoliciesView}.java, ui/components/{EmptyState,Toast,ConfirmDialog,PolicyRuleBuilder}.java, ui/AppContext.java, and corresponding services/repositories.
Dependencies: Steps 2.1–2.4, 3.2–3.3.
Acceptance: every control has a verified outcome, loading/disabled state, error feedback, and appropriate confirmation; no handler performs a silent no-op.
Tests: action-to-service workflow tests and validation/error paths; add UI integration tests for critical create/edit/archive/evaluate/history actions.
Risks/data: destructive actions require confirmation and archive/preservation policy; avoid changing previously supported keyboard and navigation behavior.
Required validation: all action inventory rows marked verified or explicitly deferred; automated workflows pass; manual control-by-control checklist completed.
Step 3.5: Persist rebuildable dashboard metric snapshots
Requirements: REQ-004, REQ-006, REQ-008
Design inputs: DashboardAnalytics, persisted evaluation run IDs/results, program scoping and invalidation contract.
Description: Persist dashboard aggregate snapshots by program and supported filter scope, with a data revision/watermark and refreshed timestamp. Rebuild or invalidate snapshots when evaluation records, policy activation, program assignment, or migration changes relevant inputs. Evaluation records remain the authoritative source; dashboard reads must detect stale/unavailable snapshots and show refresh/error state rather than stale success.
Expected files: src/main/java/com/ruleweaver/service/DashboardAnalytics.java; new src/main/java/com/ruleweaver/model/DashboardMetricsSnapshot.java; new src/main/java/com/ruleweaver/repository/{DashboardMetricsRepository,MongoDashboardMetricsRepository,InMemoryDashboardMetricsRepository}.java; src/main/java/com/ruleweaver/ui/AppContext.java; src/main/java/com/ruleweaver/ui/views/DashboardView.java; Mongo initialization and focused tests.
Dependencies: Steps 2.1–2.4 and 3.3.
Acceptance: snapshot totals match aggregation from persisted evaluations for the same scope; invalidation/rebuild is idempotent; a failed refresh is visible and cannot be reported as current.
Tests: aggregate parity, program isolation, filter key stability, stale revision detection, rebuild idempotency, empty data, concurrent/partial writes, and legacy unscoped results.
Risks/data: avoid double-counting by mixing snapshot totals with raw evaluation totals; schema changes need cache/snapshot invalidation; define freshness and scope before implementation.
Required validation: unit and isolated Mongo tests, full Maven suite, repeated-refresh consistency test, and workflow check after evaluation write.
Phase 4 — Shared interaction and visual foundation (P2)
Step 4.1: Build shared SaaS visual primitives and interaction states
Requirements: REQ-007, REQ-008, REQ-009
Design inputs: existing Theme.java, Icons.java, style.css, common components, and prior screen redesigns.
Description: Consolidate reusable spacing, typography, surfaces, focus/selection, status, empty/loading/error, toolbar, dialog, and icon color classes. Keep PrimerDark as the single user-agent theme, retain style.css, avoid fixed-color inline styles, and preserve AtlantaFX defaults where appropriate. Add accessibility/keyboard focus and reduced-motion behavior to shared components.
Expected files: src/main/java/com/ruleweaver/ui/util/Theme.java; src/main/java/com/ruleweaver/ui/components/{Icons,EmptyState,LoadingOverlay,StatusBadge,Toast,ConfirmDialog,SearchBar}.java; src/main/resources/css/style.css; src/test/java/com/ruleweaver/ui/components/IconsTest.java and new stylesheet/style-class tests if useful.
Dependencies: Steps 2.1–2.4; no functional workflow may be blocked on this styling task.
Acceptance: one consistent dark theme and reusable class vocabulary; no missing Ikonli literals; keyboard focus and narrow-window layout remain usable; no removal of author stylesheet.
Tests: icon literal coverage, style-class/resource existence checks, reduced-motion and shared-component tests where headless-compatible.
Risks/data: existing screens depend on legacy classes; migrate consumers incrementally and keep compatibility classes until all are updated.
Required validation: mvn clean compile, mvn clean test, icon/style checks, plus a UI smoke launch; record visual result as “Needs manual check” until inspected.
Phase 5 — Cohesive product experiences (P2, after functional foundations)
Step 5.1: Complete Programs workspace
Requirements: REQ-002, REQ-003, REQ-008
Design inputs: program relationship rules, scenario seeds, shared UI system, existing ProgramManagerView.
Description: Present program overview, schema, lifecycle state, scenario selection, and safe field editing/migration affordances in a cohesive workspace; ensure the selected program is the single source of scope for dependent screens.
Expected files: src/main/java/com/ruleweaver/ui/views/ProgramManagerView.java; ui/shell/{Sidebar,ViewRouter}.java; service/ProgramService.java; model/{ProgramConfiguration,ProgramTemplate}.java; related tests and style.css.
Dependencies: Steps 2.1–2.4, 3.1, 3.4, and 4.1.
Acceptance: users can select among at least three seeded scenarios and see the correct schema and dependent data; field-breaking changes explain and enforce migration; archive/duplicate actions are safe.
Tests: program workflow, selection propagation, schema compatibility, archive, and duplicate scenario tests.
Risks/data: do not make field removal/type changes silently alter stored applicant values or historical policy meanings.
Required validation: focused tests and full suite; workflow verify all three seeded scenarios and program switching; visual checklist at desktop and compact sizes.
Step 5.2: Complete Applicants workspace
Requirements: REQ-001, REQ-003, REQ-008
Design inputs: program ownership, applicant validation, current Applicants screen, shared visual primitives.
Description: Complete program-scoped search/filter, create/edit/delete, custom fields, status, empty/error/loading states, and any already-present import/export action; keep existing validation and confirmations.
Expected files: src/main/java/com/ruleweaver/ui/views/ApplicantsView.java; src/main/java/com/ruleweaver/model/Applicant.java; src/main/java/com/ruleweaver/repository/{ApplicantRepository,InMemoryApplicantRepository}.java; src/main/java/com/ruleweaver/ui/adapter/ApplicantDocumentMapper.java; related service/UI tests and style.css.
Dependencies: Steps 2.1–2.4 and 4.1.
Acceptance: applicant operations cannot cross program boundaries accidentally; schema validation is actionable; deletion does not erase historical evaluations.
Tests: CRUD scoped by program, validation by schema, action feedback, preserved evaluation history, and narrow-layout interaction.
Risks/data: legacy applicants remain explicitly unassigned until a safe mapping is chosen; deletion semantics must preserve evaluation references.
Required validation: repository/service/UI workflow tests and full suite; manual applicant CRUD and history-retention checklist.
Step 5.3: Complete Policy Studio authoring and history experience
Requirements: REQ-005, REQ-008
Design inputs: policy lifecycle in Step 3.2, existing PolicyRuleDraft/PolicyRuleBuilder, shared design system.
Description: Compose schema-aware rule authoring, nested logic, validation, impact preview, publish/activate/archive, immutable version timeline, and rule diffs as one coherent workflow. Avoid duplicating engine semantics in UI.
Expected files: src/main/java/com/ruleweaver/ui/views/PoliciesView.java; src/main/java/com/ruleweaver/ui/components/{PolicyRuleBuilder,ConditionRow}.java; src/main/java/com/ruleweaver/service/{PolicyRuleDraft,PolicyDraftImpactService,PolicyService}.java; matching tests and style.css.
Dependencies: Steps 3.2–3.4 and 4.1.
Acceptance: draft and published states are distinguishable; publish is blocked for invalid/incompatible rules; version diffs and activation are accurate; previous versions remain inspectable.
Tests: full draft-to-publish-to-activate workflows, nested rules, program schema changes, immutable history, and impact-preview parity with the engine.
Risks/data: no reinterpreting historical conditions using new field schemas; keep explicitly unassigned legacy policies usable only under documented compatibility rules.
Required validation: focused policy tests, full suite, workflow verification, and manual dialog/history checklist.
Step 5.4: Complete Evaluation workspace
Requirements: REQ-003, REQ-006, REQ-008
Design inputs: evaluation snapshots, run semantics, existing Evaluation screen and shared design system.
Description: Provide scoped applicant/policy/version selection, progress/cancel/error behavior, evaluation run grouping, filter/search, condition-by-condition explanations, and a clear distinction between viewing a historical decision and creating a new evaluation.
Expected files: src/main/java/com/ruleweaver/ui/views/EvaluationView.java; src/main/java/com/ruleweaver/service/{PolicyService,EvaluationAnalytics}.java; src/main/java/com/ruleweaver/model/{EvaluationResult,ConditionResult}.java; evaluation repository/mapper; tests and style.css.
Dependencies: Steps 2.1–2.4, 3.3–3.4, and 4.1.
Acceptance: results are grouped by persisted run; explanation values match the saved snapshot; cancel/error does not show partial success; historical detail never triggers a hidden re-evaluation.
Tests: run grouping, cancellation, persistence, explanation stability, filtering, and old-schema result handling.
Risks/data: preserve append-only results and avoid unintended bulk reruns on refresh.
Required validation: engine/service/repository/workflow tests, full suite, and manual run-to-history checklist.
Step 5.5: Complete Impact Lab as a coordinated simulation workflow
Requirements: REQ-003, REQ-006, REQ-008
Design inputs: existing DashboardView Impact Lab, WhatIfSimulator, PolicyDraftImpactService, and evaluation snapshot rules.
Description: Keep simulation clearly separate from persistence; compare a selected saved baseline with draft changes using the shared engine; label sample size/denominator; expose cancel, stale-result, validation, and save-to-draft handoff paths. Persist only when the user explicitly performs the supported policy action.
Expected files: src/main/java/com/ruleweaver/ui/views/DashboardView.java; src/main/java/com/ruleweaver/service/{WhatIfSimulator,PolicyDraftImpactService}.java; shared policy builder components; matching tests and style.css.
Dependencies: Steps 3.2–3.4, 4.1, and 5.3.
Acceptance: same input and baseline produce deterministic output; no evaluation or policy is persisted by simulation alone; invalid or cancelled simulations cannot present stale success.
Tests: engine parity, no-write guarantee, baseline selection, sample-size labels, cancellation, validation, and draft handoff.
Risks/data: do not contaminate persisted dashboard/evaluation counts with simulations.
Required validation: service/UI workflow suite, full Maven suite, and manual simulation/cancel/save checklist.
Step 5.6: Complete Time Machine and comparison history
Requirements: REQ-003, REQ-006, REQ-008
Design inputs: ComparisonView, ComparisonService, immutable policy versions, persisted evaluation snapshots.
Description: Present historical run/version comparisons with explicit program, policy, version, and cohort context; preserve existing comparison behavior and distinguish recorded outcomes from recomputed comparisons.
Expected files: src/main/java/com/ruleweaver/ui/views/ComparisonView.java; src/main/java/com/ruleweaver/service/{ComparisonService,EvaluationAnalytics}.java; evaluation and policy repositories; tests and style.css. Add TimeMachineView.java only if the approved interaction inventory demonstrates that a separate view is necessary.
Dependencies: Steps 3.2–3.4, 5.3, and 5.4.
Acceptance: old run comparison remains stable after current applicant/policy changes; missing legacy snapshots are clearly marked; cross-program comparisons are blocked or explicitly supported by product rules.
Tests: comparison of stored snapshots, policy-version pairing, cohort matching, legacy records, and program isolation.
Risks/data: never infer an unavailable historical input or silently recompute old outcomes.
Required validation: comparison service/repository workflow tests, full suite, and manual history navigation checklist.
Step 5.7: Complete Dashboard as persisted, scoped product overview
Requirements: REQ-004, REQ-008
Design inputs: persisted metric snapshot service, shared selection/invalidation, existing DashboardView.
Description: Show only supported persisted metrics for the current program and filters, with source/update time, loading/empty/error states, and navigation into matching applicant/policy/evaluation cohorts. Include the existing Impact Lab without conflating its simulated output with persisted metrics.
Expected files: src/main/java/com/ruleweaver/ui/views/DashboardView.java; src/main/java/com/ruleweaver/service/DashboardAnalytics.java; new metrics snapshot model/repository; ui/AppContext.java; style.css; focused workflow tests.
Dependencies: Steps 2.4, 3.5, 4.1, and 5.1–5.5.
Acceptance: every displayed total can be traced to an evaluation source/snapshot scope; switching programs refreshes all cards/charts; stale/error states are explicit.
Tests: snapshot parity, navigation scope, refresh after evaluation, filters/denominators, empty program, and failure display.
Risks/data: pre-existing Stage 2 Dashboard changes are user changes to preserve; do not replace metrics or calculations without parity tests.
Required validation: full suite and end-to-end evaluation-to-dashboard refresh workflow; visual acceptance checklist.
Phase 6 — Product-wide verification and acceptance (P1 gate)
Step 6.1: Add integrated multi-program workflow coverage
Requirements: REQ-001 through REQ-009
Design inputs: completed foundation and product workflows.
Description: Verify the full journey: create/select a program, manage applicants, author and publish a policy, evaluate a cohort, inspect explanations/history, refresh dashboard metrics, simulate Impact Lab changes, compare historical runs, and switch to another program without cross-contamination.
Expected files: src/test/java/com/ruleweaver/{service,repository,ui}; new workflow fixtures/harness; Maven test configuration only if required.
Dependencies: Steps 2.1–5.7.
Acceptance: all core journeys pass against isolated deterministic data; no secret-bearing environment/config is read by tests; external Mongo prerequisites are explicitly reported.
Tests: offline in-memory end-to-end workflow and separately tagged/isolated Mongo integration workflow.
Risks/data: integration tests must never point at production/default user data; use generated IDs and cleanup only test-owned records.
Required validation: mvn clean compile, mvn clean test, and integration profile with isolated DB; report exact pass/fail/skip causes.
Step 6.2: Perform manual visual and operational acceptance
Requirements: REQ-003, REQ-007, REQ-008, REQ-009
Design inputs: completed shared design system and screen workflows.
Description: Walk all actions and layouts at standard and narrow window sizes, keyboard-only navigation, dialogs, empty/loading/error states, toasts, reduced motion, and real seeded scenarios. Check startup and shaded runnable-JAR packaging on the supported platform matrix. Capture only non-sensitive observations.
Expected files: update PROGRESS.md, CHANGELOG.md, and an acceptance checklist such as docs/manual-acceptance-checklist.md; fix only confirmed task-related defects.
Dependencies: Steps 4.1–6.1.
Acceptance: checklist has a result/evidence per screen and workflow; visual results say “Needs manual check” until actually inspected; startup/package result records platform and exact command.
Tests: mvn clean compile, mvn clean test, mvn package, java -jar smoke test where platform dependencies permit, and an actual GUI walkthrough.
Risks/data: no destructive migration or seed-clear action against user data; no screenshots/logs with credentials or environment values.
Required validation: all automated commands and manual checks are recorded honestly; unresolved failures are listed rather than marked complete.
Project Structure / Expected Change Areas
src/main/java/com/ruleweaver/
  model/                         program ownership and evaluation snapshot metadata
  repository/                    scoped queries, metric snapshots, migration-compatible persistence
  service/                       lifecycle, analytics, synchronization, simulation, evaluation semantics
  engine/                        unchanged shared decision semantics except required snapshot support
  ui/
    AppContext.java               selected-program state and cross-module invalidation
    adapter/                      backward-compatible BSON mapping
    components/                   shared controls, rules, status, empty/loading/error states
    shell/                        navigation and global program context
    util/                         Theme and safe/demo data migration/seed orchestration
    views/                        Programs, Applicants, Dashboard/Impact Lab,
                                  Policy Studio, Evaluation, Comparison/Time Machine
src/main/resources/css/style.css shared author stylesheet and reusable dark design system
src/test/java/com/ruleweaver/     model, mapper, service, repository, UI, migration, workflow coverage
docs/manual-acceptance-checklist.md  final manually observed acceptance evidence
Testing Strategy
appType: JavaFX desktop client with MongoDB persistence and in-memory service/repository tests.
Critical user journeys:
Select each seeded program and confirm fields, applicants, policies, and dashboard scope match.
Create/edit an applicant, publish a schema-valid policy version, evaluate, and inspect saved explanations.
Change current applicant/policy data and confirm historical evaluation and comparison remain reproducible.
Refresh persisted metrics after an evaluation and verify program/filter scope and denominators.
Run and cancel an Impact Lab simulation, confirm no persistence, then explicitly hand a draft to Policy Studio.
Use all inventoried actions and confirm loading, error, empty, validation, and destructive-confirmation states.
Primary validation stack: JUnit 5, Maven Surefire, deterministic in-memory service workflows, isolated MongoDB repository/migration integration tests, JavaFX startup/interaction smoke checks, and manual visual acceptance.
Fallback matrix:
persistence-tier: isolated MongoDB integration fixture → in-memory repositories for ordinary CI. In-memory coverage does not prove Mongo query/index/migration behavior; retain at least one separately reported isolated Mongo run.
ui-tier: JavaFX GUI walkthrough on supported desktop → headless component/style/resource tests. Headless tests do not prove layout, accessibility, or event integration; retain a manual acceptance gate.
Environment requirements: Java 17; Maven; supported JavaFX native runtime for launch; isolated disposable MongoDB for integration and migration tests; no config/env secret inspection or echoing.
Test data strategy: deterministic program scenarios and generated test IDs; isolate Mongo collections/database; cleanup only records created by the test; do not delete or mutate user data.
Acceptance criteria: all task-specific assertions pass; old persisted documents remain readable; migrations preserve IDs/history and are idempotent; cross-program state never leaks; all visible actions resolve; metrics reconcile against persisted evaluation records; manual checklist records observed behavior.
Validation review expectations: verify tests exercise actual boundary cases (legacy links, schema changes, stale async responses, version immutability, repeated refresh, cancellation, cross-program filters), review migration counts before apply, and ensure docs distinguish automated evidence from manual visual inspection.
Worktree and execution policy
The current worktree already contains uncommitted Dashboard, Applicants, Policies, stylesheet, PROGRESS.md, and CHANGELOG.md changes. Preserve those changes; the plan does not imply they should be reverted or committed.
Implement one task or small dependent task batch at a time, run that task's required validation, review the diff, and pause for review at the product owner’s requested checkpoints.
Do not commit or push automatically.
Requirement Mapping
REQ ID	Description	Plan Items	Implementation Evidence
REQ-001	Foundational program/applicant/policy/version/evaluation relationships	2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 5.1, 5.2	Models, mappers, migration runner, repositories, AppContext, and relationship tests
REQ-002	At least three meaningfully different multi-program scenarios	3.1, 5.1	SampleDataSeeder, SeedData, ProgramTemplate, seeder tests, Programs workspace
REQ-003	Complete inert/partial UI actions	1.1, 2.4, 3.2, 3.4, 5.1–5.6, 6.2	Action inventory, view/component handlers, services, workflow tests, manual checklist
REQ-004	Cross-module synchronization and persisted dashboard metrics	2.2–2.4, 3.3, 3.5, 5.4, 5.7	AppContext, scoped repositories, metrics snapshot model/repository/service, workflow tests
REQ-005	Rule construction, validation, publishing, version history	2.1, 3.2, 5.3	PolicyRuleDraft, PolicyRuleBuilder, PolicyService, policy repositories, lifecycle tests
REQ-006	Explainable and reproducible historical decisions	2.1–2.3, 3.2, 3.3, 3.5, 5.3–5.6	EvaluationResult, ConditionResult, mappers/repositories, DecisionEngine, snapshot/history tests
REQ-007	Reusable dark SaaS design system on AtlantaFX/Ikonli	4.1, 5.1–5.7, 6.2	Theme, Icons, shared components, style.css, style checks and visual checklist
REQ-008	Cohesive seven product experiences	2.4, 3.4–3.5, 4.1, 5.1–5.7	ProgramManagerView, ApplicantsView, PoliciesView, EvaluationView, DashboardView, ComparisonView, shared navigation
REQ-009	Automated, workflow, and manual visual validation	1.1, 2.2–2.4, 3.1–3.5, 4.1, 5.1–5.7, 6.1–6.2	JUnit, isolated Mongo tests, end-to-end journeys, migration report, manual acceptance checklist
Reviewable Task Breakdown
Each task below is deliberately small enough to review and validate independently. Detailed dependencies, file scope, behavior, tests, risks, and required evidence are specified in the corresponding plan item above.

Phase 1 — Baseline
 T001 [Plan:1.1] Inventory all UI actions, confirm feature placement, and record a safe source/test baseline in docs/ui-action-inventory.md and PROGRESS.md without inspecting protected configuration or environment values.
Phase 2 — Foundational data integrity
 T002 [Plan:2.1] Define and review program ownership, legacy-data, schema-evolution, and migration rules for the domain models and Mongo documents.
 T003 [Plan:2.2] Add backward-compatible program/snapshot relationship fields and mapper coverage in the model, repository, and adapter modules.
 T004 [Plan:2.3] Add dry-run-first, idempotent relationship migration and program/evaluation indexes with isolated migration tests.
 T005 [Plan:2.4] Synchronize selected program/policy/version and stale async refresh handling across AppContext, navigation, and dependent views.
Phase 3 — Durable scenarios and business behavior
 T006 [Plan:3.1] Implement three deterministic, non-destructive demo program scenarios and seed-data regression tests.
 T007 [Plan:3.2] Enforce program-aware policy validation, immutable publishing/versioning, and archive/activation lifecycle rules.
 T008 [Plan:3.3] Persist sufficient evaluation snapshots for stable explanations and historical decisions; distinguish history view from re-evaluation.
 T009 [Plan:3.4] Resolve every action in the approved UI inventory with a functional workflow or explicit approved unavailable state.
 T010 [Plan:3.5] Persist rebuildable dashboard metric snapshots with scope, revision, freshness, and error handling.
Phase 4 — Shared interaction and visual foundation
 T011 [Plan:4.1] Consolidate reusable PrimerDark/Ikonli design tokens and shared loading, empty, error, focus, spacing, and responsive interaction states while retaining style.css.
Phase 5 — Cohesive product experiences
 T012 [Plan:5.1] Complete the program selection/schema/lifecycle workspace using the shared program state.
 T013 [Plan:5.2] Complete program-scoped applicant management while preserving evaluations on applicant changes/deletion.
 T014 [Plan:5.3] Complete the Policy Studio draft-to-publish-to-history workflow using shared engine validation.
 T015 [Plan:5.4] Complete evaluation run, progress, filtering, explanation, and saved-history workflows.
 T016 [Plan:5.5] Complete Impact Lab simulation/cancel/draft handoff without implicit persistence.
 T017 [Plan:5.6] Complete reproducible Time Machine/comparison history; create a separate view only if inventory justifies it.
 T018 [Plan:5.7] Complete the dashboard with current persisted scoped metrics and functional navigation to source cohorts.
Phase 6 — Verification and acceptance
 T019 [Plan:6.1] Add and pass the end-to-end multi-program workflows and separately reported isolated Mongo integration coverage.
 T020 [Plan:6.2] Run the complete manual action/visual checklist and verify startup/shaded-JAR behavior; record exact evidence and limitations.
Recommended first foundational task
T001–T004 are complete. The approved contract in docs/data-ownership-contract.md remains the implementation basis; applicant hard-delete versus archive behavior remains an explicit open decision. The next bounded task is T005: Synchronize shared program selection and refresh state. T004 added an explicit dry-run/apply migration for initial program schema snapshots and additive indexes; it did not run against the application's configured database or assign legacy applicant, policy, or evaluation records.
