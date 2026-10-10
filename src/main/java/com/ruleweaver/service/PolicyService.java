package com.ruleweaver.service;

import com.ruleweaver.model.Condition;
import com.ruleweaver.model.NumericCondition;
import com.ruleweaver.model.Policy;
import com.ruleweaver.model.PolicyVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * PolicyService (Phase 2: IN-MEMORY storage).
 *
 * All policies live in an ArrayList inside this object. When the program exits, everything is
 * lost. That is deliberate for now: it lets Mukesh test the logic without MongoDB. In Phase 5
 * the storage is swapped for Mohnish's repositories; the public methods below are designed to
 * stay the same so callers (Main, the JavaFX screens, tests) do not need to change.
 *
 * Ids are 24-character hex strings so they can later be converted to MongoDB ObjectIds with
 * new ObjectId(id).
 */
public class PolicyService {

    private final List<Policy> policies = new ArrayList<>();

    // ---------------------------------------------------------------- creating

    /** Creates an empty policy (no versions yet). Names must be non-blank and unique (ignoring case). */
    public Policy createPolicy(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Policy name must not be blank");
        }
        for (Policy p : policies) {
            if (p.getName().equalsIgnoreCase(name.trim())) {
                // Implementation choice: duplicate names are rejected to avoid confusing the UI.
                throw new IllegalArgumentException("A policy named '" + p.getName() + "' already exists");
            }
        }
        Policy policy = new Policy(newId(), name);
        policies.add(policy);
        return policy;
    }

    /**
     * Adds the next numbered version (v1 for a new policy, then v2, v3 ...) with exactly the
     * conditions given.
     */
    public PolicyVersion createVersion(String policyId, List<Condition> conditions) {
        Policy policy = getPolicy(policyId);
        PolicyVersion version = new PolicyVersion(policyId, policy.getNextVersionNumber(), conditions);
        policy.addVersion(version);
        return version;
    }

    /**
     * Creates a NEW version based on an existing one. The editor receives a modifiable COPY of
     * the base version's conditions and returns the list for the new version. The base version
     * is never touched.
     */
    public PolicyVersion createVersionFrom(String policyId, int baseVersionNumber,
                                           UnaryOperator<List<Condition>> editor) {
        PolicyVersion base = getVersion(policyId, baseVersionNumber);
        List<Condition> workingCopy = new ArrayList<>(base.getConditions());
        List<Condition> edited = editor.apply(workingCopy);
        return createVersion(policyId, edited);
    }

    /**
     * Convenience for the PDF's main scenario: copy a version and change one numeric threshold
     * (e.g. gpa 8.0 -> 8.5). Throws IllegalArgumentException if the base version has no
     * NumericCondition on that field.
     *
     * Needs NumericCondition.getFieldName/getOperator/getThreshold from Rizwan's class.
     */
    public PolicyVersion createVersionWithNumericThreshold(String policyId, int baseVersionNumber,
                                                           String fieldName, double newThreshold) {
        PolicyVersion base = getVersion(policyId, baseVersionNumber);
        List<Condition> updated = new ArrayList<>();
        boolean changed = false;
        for (Condition c : base.getConditions()) {
            if (c instanceof NumericCondition) {
                NumericCondition n = (NumericCondition) c;
                if (n.getFieldName().equalsIgnoreCase(fieldName)) {
                    // Conditions are immutable, so we build a NEW one rather than editing the old.
                    updated.add(new NumericCondition(n.getFieldName(), n.getOperator(), newThreshold));
                    changed = true;
                    continue;
                }
            }
            updated.add(c); // unchanged conditions are shared (safe because they are immutable)
        }
        if (!changed) {
            throw new IllegalArgumentException("Version " + base.getVersionLabel()
                    + " has no numeric condition on field '" + fieldName + "'");
        }
        return createVersion(policyId, updated);
    }

    // ---------------------------------------------------------------- reading

    /** @throws PolicyNotFoundException if the id is unknown */
    public Policy getPolicy(String policyId) {
        for (Policy p : policies) {
            if (p.getId().equals(policyId)) {
                return p;
            }
        }
        throw new PolicyNotFoundException(policyId);
    }

    /** All policies in creation order (read-only list). */
    public List<Policy> getAllPolicies() {
        return Collections.unmodifiableList(policies);
    }

    /**
     * @throws PolicyNotFoundException  if the policy does not exist
     * @throws VersionNotFoundException if that version number does not exist
     */
    public PolicyVersion getVersion(String policyId, int versionNumber) {
        return getPolicy(policyId).findVersion(versionNumber)
                .orElseThrow(() -> new VersionNotFoundException(policyId, versionNumber));
    }

    /**
     * @throws PolicyNotFoundException  if the policy does not exist
     * @throws VersionNotFoundException if the policy has no versions yet
     */
    public PolicyVersion getLatestVersion(String policyId) {
        return getPolicy(policyId).getLatestVersion()
                .orElseThrow(() -> new VersionNotFoundException(policyId));
    }

    /** All versions of a policy, oldest first (read-only list). */
    public List<PolicyVersion> getAllVersions(String policyId) {
        return getPolicy(policyId).getVersions();
    }

    // ---------------------------------------------------------------- helpers

    private static String newId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
