package com.ruleweaver.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Policy = a named set of eligibility rules, e.g. "Internship Eligibility".
 * It does not hold conditions itself; it holds numbered PolicyVersions, and each version
 * holds the conditions (composition: Policy HAS-MANY PolicyVersion, PolicyVersion HAS-MANY Condition).
 *
 * PDF fields: id, name, list of PolicyVersions.
 * Version numbers always run 1, 2, 3 ... with no gaps; addVersion() enforces that.
 */
public class Policy {

    private final String id;
    private final String name;
    private final List<PolicyVersion> versions = new ArrayList<>();

    public Policy(String id, String name) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Policy id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Policy name must not be blank");
        }
        this.id = id;
        this.name = name.trim();
    }

    public String getId() { return id; }

    public String getName() { return name; }

    /** Read-only view, oldest first. */
    public List<PolicyVersion> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    /** The number the next added version must have (1 for a brand-new policy). */
    public int getNextVersionNumber() {
        return versions.size() + 1;
    }

    /**
     * Appends a version. The version must belong to this policy and carry exactly the next
     * number. Nothing in this class lets you replace or edit a version that was already added.
     */
    public void addVersion(PolicyVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("version must not be null");
        }
        if (!id.equals(version.getPolicyId())) {
            throw new IllegalArgumentException("Version belongs to policy " + version.getPolicyId()
                    + ", not " + id);
        }
        if (version.getVersionNumber() != getNextVersionNumber()) {
            throw new IllegalArgumentException("Expected version number " + getNextVersionNumber()
                    + " but got " + version.getVersionNumber());
        }
        versions.add(version);
    }

    /** Finds a version by number; empty if it does not exist. */
    public Optional<PolicyVersion> findVersion(int versionNumber) {
        for (PolicyVersion v : versions) {
            if (v.getVersionNumber() == versionNumber) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    /** The highest-numbered version; empty if the policy has no versions yet. */
    public Optional<PolicyVersion> getLatestVersion() {
        if (versions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(versions.get(versions.size() - 1));
    }

    @Override
    public String toString() {
        return "Policy{" + name + ", " + versions.size() + " versions}";
    }
}
