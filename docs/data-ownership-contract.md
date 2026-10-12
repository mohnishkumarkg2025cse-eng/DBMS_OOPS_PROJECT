# RuleWeaver X — Data Ownership and Legacy Compatibility Contract (T002)

**Status:** Approved as the implementation contract on 2026-10-12. The applicant hard-delete versus archive decision remains open; no source, schema, or persisted data was changed.
**Date:** 2026-10-12
**Basis:** Direct inspection of the current domain models, BSON mappers, Mongo repositories, and `PolicyService`. This document is a proposal, not an approved migration authorization.

## 1. Product and data boundaries

1. A **Program** is the scope for applicants, policies, schema definitions, evaluations, and program-scoped metrics inside one RuleWeaver X installation.
2. This is **not** an organization/user tenancy or authorization model. The current app has no user identity, tenant, or permission layer. Program separation prevents accidental business-data mixing; it must not be represented as SaaS tenant security.
3. An **Applicant** belongs to zero or one program. New applicant records must belong to exactly one active program. `null` is reserved for records created before program ownership or explicitly retained as legacy/unassigned.
4. A **Policy** belongs to zero or one program for backward compatibility. New program-linked policies must reference one active program. A legacy unassigned policy may only be run against the explicit legacy/unassigned applicant cohort; it must not implicitly include every program.
5. A **PolicyVersion** belongs to exactly one policy and inherits its program. Its parent link and version number are immutable. Denormalized program/schema references may be stored for query/index efficiency but must be validated against the policy on write/read.
6. An **Evaluation** is an immutable record for exactly one applicant snapshot, one policy-version snapshot, and one program scope (or the explicit legacy/unassigned scope). It is append-only; correcting an outcome creates a new run, never edits an old result.

## 2. Recommended relationships

```text
Program 1 ─── 0..N Applicants
Program 1 ─── 0..N Policies
Policy  1 ─── 1..N immutable PolicyVersions
PolicyVersion 1 ─── 0..N Evaluations
Applicant 1 ─── 0..N Evaluations
Program schema revision 1 ─── 0..N PolicyVersions and Evaluations
EvaluationRun 1 ─── 1..N Evaluations
```

Legacy `programId == null` remains a named, isolated scope. It is not a wildcard and is not silently assigned to a scenario. An explicit “All Programs” analytics view may aggregate per-program results if every participating record is program-assigned; unassigned legacy data is shown as a separate scope or deliberately excluded with a visible count.

## 3. Schema evolution and policy binding

- A program's field schema is versioned with a monotonically increasing `schemaRevision`. A schema change that affects field meaning, type, validation, or supported operators creates a new immutable revision. A display-name-only edit can be treated as metadata only if field keys/semantics remain unchanged.
- Store schema revisions separately as immutable `program_schema_versions` records keyed by `(programId, revision)` or an equivalent immutable representation. `ProgramConfiguration` identifies the current revision; old revisions remain readable.
- A published `PolicyVersion` records its `programId` and `programSchemaRevision` (or immutable schema-snapshot ID). Rule validation uses the schema revision against which that version was authored. If the live program schema has moved on, the app must either validate a new draft against the current schema or explicitly migrate/re-author it; it must not rewrite an already-published version.
- For minimum lifecycle complexity, a draft can remain an editable in-memory `PolicyRuleDraft`. Publishing writes one immutable `PolicyVersion`; activation updates the policy's active-version pointer. Existing versions are treated as already published for compatibility. Archive prevents new normal evaluations but does not remove policy versions or evaluations.
- Policy and program archive are reversible lifecycle flags. They never cascade-delete applicants, versions, or evaluations.

## 4. Evaluation reproducibility and explainability

New evaluation records should include:

- `programId` (nullable only for the legacy scope);
- applicant ID and display name, policy ID/name, policy-version ID/number, and program schema revision/snapshot ID;
- `evaluationRunId`, evaluation timestamp, durable overall decision, and ordered condition results (condition identity/type, actual value, required value, pass/fail, explanation);
- a minimal immutable `applicantInputSnapshot` of the fields actually referenced by that policy version, with labels/field keys sufficient to explain the decision; do not copy unrelated personal data;
- an engine/evaluation-semantics version identifier if future engine changes could alter replay semantics.

The saved decision and saved condition results are authoritative for viewing historical outcomes. Historical display must not re-evaluate using today's applicant profile, policy, schema, or active version. If exact re-execution is supported later, it must use the saved applicant input, immutable policy version, schema revision, and compatible engine semantics; otherwise identify the result as historical display only. Legacy results without these references remain readable and must be labeled as legacy/partially reproducible rather than backfilled with invented values.

## 5. Dashboard metric persistence and consistency

- Evaluation documents are the source of truth. Dashboard metric documents are rebuildable materialized snapshots/caches, never replacements for evaluation history.
- Snapshot key includes program scope (including a distinct legacy key), normalized filters, metric/calculation version, and the source evaluation data watermark/revision.
- A snapshot is current only after source freshness is validated. Recompute or show an explicit stale/loading/error state when the watermark is missing or does not match. Never display a failed refresh as a successful current dashboard.
- Evaluation writes and aggregate snapshots may span Mongo documents. Unless the deployed Mongo topology guarantees transactions, do not claim atomic cross-collection updates. Prefer append-only evaluation data plus deterministic snapshot rebuild and reconciliation on refresh; an unavailable/stale snapshot must not obscure a committed evaluation.
- Metrics must reconcile to saved evaluation records for the same cohort/run/version and must document denominators. Simulated Impact Lab results never update persisted evaluation metrics.

## 6. Legacy documents and migration rules

| Collection/document | Current observed shape | Read behavior before migration | Allowed automatic migration |
|---|---|---|---|
| `applicants` | `_id`, name/email, GPA/interview/skills, optional custom attributes and status; no `programId` in current mapper. | Missing program ID maps to legacy/unassigned. Existing `ObjectId` and string IDs remain unchanged. | Additive optional field support only. Do not infer a program from name, GPA, skills, or ObjectId. |
| `policies` | Policy metadata includes optional `programId`, active version, archived flag. | Missing/null program ID remains legacy/unassigned. | No assignment by policy name or category. |
| `policy_versions` | Immutable policy/version documents with unique `(policyId, version)` index and serialized conditions. | Resolve program through parent policy for old versions; missing parent is reported as orphaned history. | Add schema revision only to new versions. Do not rewrite conditions/version documents in place. |
| `evaluations` | Applicant/policy identity, version/run IDs when available, decision and condition results; old documents may lack run/version identifiers. | Preserve as legacy; old version identity may use existing documented ID/number compatibility lookup. | No fabricated program, schema, applicant snapshot, run ID, or condition data. |
| `programs` | Program definitions and mutable field list; current `ProgramDocumentMapper` stores template/name/archive/fields. | Existing program is schema revision 0/legacy baseline until explicitly recorded. | Create immutable revision records by copying current definitions only with an audited, operator-approved migration. |

Migration process:

1. Dry-run is the default; it reports source counts, missing/ambiguous links, proposed actions, and unchanged IDs without writing.
2. Apply requires explicit operator intent, verified backup, isolated target confirmation, and reviewed expected counts. Never read or print a connection string while producing the report.
3. Infer links only from an explicit mapping approved by the data owner or a strong stable foreign key already present. Fixed demo IDs/names alone are not sufficient proof in arbitrary user data.
4. Migration is idempotent; it records version/completion metadata and can resume safely. It reports every skipped ambiguous/orphan record.
5. Compare before/after document counts, IDs, policy-version counts, and evaluation counts. No document/history deletion is allowed in this migration.
6. Preserve current write/read compatibility until the migrated dataset and rollback/restore procedure are verified.

## 7. Index proposal

Create indexes only after query patterns and existing index state are checked:

- applicants: `(programId, status)`; optional text/name search strategy only if justified by current search requirements;
- policies: `(programId, archived)`;
- policy versions: retain unique `(policyId, version)`; add `(programId, policyId, version)` only if denormalized program ID is adopted and query plans justify it;
- evaluations: `(programId, policyId, policyVersion, evaluatedOn)`, `(evaluationRunId)`, and `(applicantId, evaluatedOn)` as supported by query patterns;
- program schema versions: unique `(programId, revision)`;
- metric snapshots: unique normalized scope/filter/calculation/watermark key.

Index creation must be idempotent and tested against existing data before rollout; existing unique index failures must stop migration with a clear report, not be silently ignored.

## 8. Lifecycle decisions requiring explicit product confirmation

1. **Applicant deletion (open):** source code physically deletes applicant documents while retaining saved evaluation documents. Recommended product behavior is archive/deactivate applicants by default so references remain navigable; hard deletion should be a separate privacy/retention-controlled operation. No implementation may change the current behavior until this decision is separately approved.
2. **Unassigned legacy records:** keep in isolated legacy scope until an operator explicitly maps them. Do not auto-assign the current internship examples solely by fixed IDs or names.
3. **Schema revision retention:** retain every schema revision referenced by an immutable policy version or evaluation; pruning requires a separate retention policy.
4. **Metrics freshness:** choose and document whether dashboards synchronously reconcile on each refresh or permit a bounded stale window. Until chosen, show freshness and never label an unverified snapshot current.
5. **Meaning of “SaaS”:** plan assumes a polished multi-program product experience in this desktop app, not authentication, organization tenancy, cloud hosting, or network deployment.

## 9. Concrete acceptance cases for T003–T004

- Old applicant/policy/evaluation BSON without program fields loads unchanged and remains in legacy scope.
- Program-scoped evaluation rejects applicants from a different program and does not accept unassigned applicants by wildcard.
- New policy versions reference their parent policy and schema revision; prior version bytes/condition tree do not change after schema edits.
- Evaluation history displays its saved decision/explanations after source applicant fields and active policy version change.
- Dry-run migration produces no writes; applying twice produces the same final counts and IDs.
- Ambiguous legacy records are reported and left unassigned; no historical evaluations or policy versions are deleted.
- A metric snapshot with a stale/missing source watermark is rebuilt or shown as stale/error, never returned as confirmed current.
