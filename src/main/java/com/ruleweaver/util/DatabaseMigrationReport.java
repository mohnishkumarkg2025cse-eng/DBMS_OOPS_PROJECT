package com.ruleweaver.util;

import org.bson.Document;

import java.util.List;

public record DatabaseMigrationReport(
        Mode mode,
        Status status,
        CollectionCounts before,
        CollectionCounts after,
        long legacyUnassignedApplicants,
        long legacyUnassignedPolicies,
        long legacyEvaluations,
        int programSchemaRevisionsPrepared,
        List<String> compatibilityIssues) {

    public DatabaseMigrationReport {
        compatibilityIssues = compatibilityIssues == null ? List.of() : List.copyOf(compatibilityIssues);
    }

    public Document toDocument() {
        return new Document("mode", mode.name())
                .append("status", status.name())
                .append("before", before.toDocument())
                .append("after", after.toDocument())
                .append("legacyUnassignedApplicants", legacyUnassignedApplicants)
                .append("legacyUnassignedPolicies", legacyUnassignedPolicies)
                .append("legacyEvaluations", legacyEvaluations)
                .append("programSchemaRevisionsPrepared", programSchemaRevisionsPrepared)
                .append("compatibilityIssues", compatibilityIssues);
    }

    public enum Mode {
        DRY_RUN,
        APPLY
    }

    public enum Status {
        PREVIEWED,
        APPLIED,
        ALREADY_APPLIED
    }

    public record CollectionCounts(
            long applicants,
            long policies,
            long policyVersions,
            long evaluations,
            long programs,
            long programSchemaRevisions) {

        public Document toDocument() {
            return new Document("applicants", applicants)
                    .append("policies", policies)
                    .append("policyVersions", policyVersions)
                    .append("evaluations", evaluations)
                    .append("programs", programs)
                    .append("programSchemaRevisions", programSchemaRevisions);
        }
    }
}
