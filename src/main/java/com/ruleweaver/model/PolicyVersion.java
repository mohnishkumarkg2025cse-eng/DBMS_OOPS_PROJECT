package com.ruleweaver.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PolicyVersion = one numbered, frozen snapshot of a Policy's conditions.
 *
 * PDF fields: versionNumber, List<Condition> conditions, creationDate, policyId.
 * (In MongoDB these are stored as: policyId, version, conditions, created.)
 *
 * This class is IMMUTABLE: every field is final and the conditions list is copied into an
 * unmodifiable list. Once a version exists, nothing can change its conditions. That is how we
 * guarantee "creating Version 2 never modifies Version 1".
 */
public class PolicyVersion {

    private final String policyId;
    private final int versionNumber;
    private final List<Condition> conditions;
    private final Instant createdAt;

    /** Normal constructor: the creation time is "now". */
    public PolicyVersion(String policyId, int versionNumber, List<Condition> conditions) {
        this(policyId, versionNumber, conditions, Instant.now());
    }

    /** Used when loading an existing version back from the database (Phase 5). */
    public PolicyVersion(String policyId, int versionNumber, List<Condition> conditions, Instant createdAt) {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId must not be blank");
        }
        if (versionNumber < 1) {
            throw new IllegalArgumentException("versionNumber must be 1 or higher, got " + versionNumber);
        }
        if (conditions == null || conditions.isEmpty()) {
            // Implementation choice (the PDF leaves this open): a version with no rules is rejected.
            throw new IllegalArgumentException("A policy version needs at least one condition");
        }
        for (Condition c : conditions) {
            if (c == null) {
                throw new IllegalArgumentException("conditions must not contain null");
            }
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        this.policyId = policyId;
        this.versionNumber = versionNumber;
        // Defensive copy: later changes to the caller's list cannot leak into this version.
        this.conditions = Collections.unmodifiableList(new ArrayList<>(conditions));
        this.createdAt = createdAt;
    }

    public String getPolicyId() { return policyId; }

    public int getVersionNumber() { return versionNumber; }

    /** Read-only list. Calling add/remove on it throws UnsupportedOperationException. */
    public List<Condition> getConditions() { return conditions; }

    public Instant getCreatedAt() { return createdAt; }

    /** e.g. "v1" */
    public String getVersionLabel() { return "v" + versionNumber; }

    /** Multi-line text listing every condition, used for console display. */
    public String describeConditions() {
        StringBuilder sb = new StringBuilder();
        int n = 1;
        for (Condition c : conditions) {
            sb.append("  ").append(n++).append(". ").append(c.getMessage()).append(System.lineSeparator());
        }
        return sb.toString().stripTrailing();
    }

    @Override
    public String toString() {
        return "PolicyVersion{" + getVersionLabel() + ", " + conditions.size() + " conditions}";
    }
}
