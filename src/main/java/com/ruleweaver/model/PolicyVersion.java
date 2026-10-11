package com.ruleweaver.model;

import com.ruleweaver.engine.Condition;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class PolicyVersion {
    private String id;
    private String policyId;
    private int versionNumber;
    private String description;
    private List<Condition> conditions;
    private LocalDateTime createdAt = LocalDateTime.now();

    // Default constructor
    public PolicyVersion() {
        this.conditions = new ArrayList<>();
    }

    // Constructor matching Policy.java createNextVersion call
    public PolicyVersion(String id, String policyId, int versionNumber, String description, List<Condition> conditions) {
        this.id = id;
        this.policyId = policyId;
        this.versionNumber = versionNumber;
        this.description = description;
        this.conditions = conditions != null ? new ArrayList<>(conditions) : new ArrayList<>();
    }

    // Alternative constructor for simple initialization
    public PolicyVersion(String policyId, int versionNumber, List<Condition> conditions) {
        this.policyId = policyId;
        this.versionNumber = versionNumber;
        this.conditions = conditions != null ? new ArrayList<>(conditions) : new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions != null ? new ArrayList<>(conditions) : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}