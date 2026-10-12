package com.ruleweaver.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a business policy (e.g., "Internship Eligibility", "Merit Scholarship Policy").
 * Demonstrates Composition by owning a collection of versioned condition snapshots (PolicyVersion).
 */
public class Policy {
    private String id;
    private String name;
    private String description;
    private String category = "Other";
    private String programId;
    private int activeVersionNumber;
    private boolean archived;
    private List<PolicyVersion> versions;

    public Policy() {
        this.versions = new ArrayList<>();
    }

    public Policy(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.versions = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category == null || category.isBlank() ? "Other" : category;
    }

    public void setCategory(String category) {
        this.category = category == null || category.isBlank() ? "Other" : category;
    }

    public String getProgramId() { return programId; }

    public void setProgramId(String programId) { this.programId = programId; }

    public int getActiveVersionNumber() {
        if (activeVersionNumber > 0 && getVersion(activeVersionNumber).isPresent()) {
            return activeVersionNumber;
        }
        return getLatestVersion().map(PolicyVersion::getVersionNumber).orElse(0);
    }

    public void setActiveVersionNumber(int activeVersionNumber) {
        this.activeVersionNumber = activeVersionNumber;
    }

    public int getPersistedActiveVersionNumber() {
        return activeVersionNumber;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public List<PolicyVersion> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    public void setVersions(List<PolicyVersion> versions) {
        this.versions = versions != null ? new ArrayList<>(versions) : new ArrayList<>();
    }

    public void addVersion(PolicyVersion version) {
        if (version != null) {
            version.setPolicyId(this.id);
            this.versions.add(version);
        }
    }

    public Optional<PolicyVersion> getVersion(int versionNumber) {
        return versions.stream()
                .filter(v -> v.getVersionNumber() == versionNumber)
                .findFirst();
    }

    public Optional<PolicyVersion> getLatestVersion() {
        return versions.stream()
                .max((v1, v2) -> Integer.compare(v1.getVersionNumber(), v2.getVersionNumber()));
    }

    public PolicyVersion createNextVersion(String description) {
        int nextVersionNumber = getLatestVersion()
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        List<com.ruleweaver.engine.Condition> clonedConditions = new ArrayList<>();
        getLatestVersion().ifPresent(latest -> clonedConditions.addAll(latest.getConditions()));

        PolicyVersion nextVersion = new PolicyVersion(
                id + "_v" + nextVersionNumber,
                this.id,
                nextVersionNumber,
                description,
                clonedConditions
        );
        addVersion(nextVersion);
        return nextVersion;
    }

    @Override
    public String toString() {
        return String.format("Policy[id='%s', name='%s', versions=%d]", id, name, versions.size());
    }
}
