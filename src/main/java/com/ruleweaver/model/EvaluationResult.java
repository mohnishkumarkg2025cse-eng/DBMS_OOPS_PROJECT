package com.ruleweaver.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds the transparent outcome of evaluating an applicant against a policy version.
 * Stores individual condition pass/fail results, actual data values, and final verdict.
 * Fully supports explainability and historical auditing.
 */
public class EvaluationResult {
    private String id;
    private String applicantId;
    private String applicantName;
    private String policyId;
    private String policyName;
    private int policyVersionNumber;
    private boolean overallDecision;
    private List<ConditionResult> conditionResults;
    private LocalDateTime evaluatedOn;

    public EvaluationResult() {
        this.conditionResults = new ArrayList<>();
        this.evaluatedOn = LocalDateTime.now();
    }

    public EvaluationResult(String applicantId, String applicantName, String policyId, 
                            String policyName, int policyVersionNumber) {
        this.applicantId = applicantId;
        this.applicantName = applicantName;
        this.policyId = policyId;
        this.policyName = policyName;
        this.policyVersionNumber = policyVersionNumber;
        this.conditionResults = new ArrayList<>();
        this.evaluatedOn = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public int getPolicyVersionNumber() {
        return policyVersionNumber;
    }

    public void setPolicyVersionNumber(int policyVersionNumber) {
        this.policyVersionNumber = policyVersionNumber;
    }

    public boolean isOverallDecision() {
        return overallDecision;
    }

    public void setOverallDecision(boolean overallDecision) {
        this.overallDecision = overallDecision;
    }

    public List<ConditionResult> getConditionResults() {
        return Collections.unmodifiableList(conditionResults);
    }

    public void setConditionResults(List<ConditionResult> conditionResults) {
        this.conditionResults = conditionResults != null ? new ArrayList<>(conditionResults) : new ArrayList<>();
    }

    public void addConditionResult(ConditionResult result) {
        if (result != null) {
            this.conditionResults.add(result);
        }
    }

    public LocalDateTime getEvaluatedOn() {
        return evaluatedOn;
    }

    public void setEvaluatedOn(LocalDateTime evaluatedOn) {
        this.evaluatedOn = evaluatedOn;
    }

    public boolean allConditionsPassed() {
        if (conditionResults.isEmpty()) {
            return true;
        }
        return conditionResults.stream().allMatch(ConditionResult::isPassed);
    }

    public List<ConditionResult> getFailedConditions() {
        return conditionResults.stream()
                .filter(cr -> !cr.isPassed())
                .collect(Collectors.toList());
    }

    public List<ConditionResult> getPassedConditions() {
        return conditionResults.stream()
                .filter(ConditionResult::isPassed)
                .collect(Collectors.toList());
    }

    /**
     * Generates a fully explainable audit string as specified in the RuleWeaver architecture.
     */
    public String getSummaryExplanation() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Evaluation for Applicant: %s (ID: %s)\n", applicantName, applicantId));
        sb.append(String.format("Policy: %s (v%d)\n", policyName != null ? policyName : policyId, policyVersionNumber));
        sb.append("--------------------------------------------------\n");
        for (ConditionResult cr : conditionResults) {
            sb.append(String.format(" • %s\n", cr.getExplanation()));
        }
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("-> Final Decision: %s%s\n",
                overallDecision ? "Eligible (Accepted)" : "Not Eligible (Rejected)",
                overallDecision ? " (All criteria met)" : " (" + getFailedConditions().size() + " condition(s) failed)"));
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("EvaluationResult[applicant=%s, policy=%s_v%d, decision=%s]",
                applicantName, policyId, policyVersionNumber, overallDecision ? "PASS" : "FAIL");
    }
}
