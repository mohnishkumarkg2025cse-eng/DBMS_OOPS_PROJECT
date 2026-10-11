package com.ruleweaver.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the decision diff for a single applicant between two policy versions.
 * Identifies whether eligibility changed (e.g., Passed in v1 -> Rejected in v2)
 * and pinpoints the exact conditions that caused the transition.
 */
public class ApplicantDiff {
    private String applicantId;
    private String applicantName;
    private boolean v1Decision;
    private boolean v2Decision;
    private EvaluationResult v1Result;
    private EvaluationResult v2Result;
    private List<String> differentiatingReasons;

    public ApplicantDiff(String applicantId, String applicantName, boolean v1Decision, boolean v2Decision,
                         EvaluationResult v1Result, EvaluationResult v2Result) {
        this.applicantId = applicantId;
        this.applicantName = applicantName;
        this.v1Decision = v1Decision;
        this.v2Decision = v2Decision;
        this.v1Result = v1Result;
        this.v2Result = v2Result;
        this.differentiatingReasons = new ArrayList<>();
    }

    public String getApplicantId() {
        return applicantId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public boolean isV1Decision() {
        return v1Decision;
    }

    public boolean isV2Decision() {
        return v2Decision;
    }

    public EvaluationResult getV1Result() {
        return v1Result;
    }

    public EvaluationResult getV2Result() {
        return v2Result;
    }

    public List<String> getDifferentiatingReasons() {
        return differentiatingReasons;
    }

    public void addReason(String reason) {
        this.differentiatingReasons.add(reason);
    }

    public boolean hasDecisionChanged() {
        return v1Decision != v2Decision;
    }

    public String getChangeType() {
        if (!hasDecisionChanged()) {
            return "UNCHANGED (" + (v1Decision ? "Eligible" : "Rejected") + ")";
        }
        if (v1Decision && !v2Decision) {
            return "LOST ELIGIBILITY (Passed -> Rejected)";
        }
        return "GAINED ELIGIBILITY (Rejected -> Passed)";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("• %s: %s\n", applicantName, getChangeType()));
        for (String reason : differentiatingReasons) {
            sb.append(String.format("   ↳ Reason: %s\n", reason));
        }
        return sb.toString();
    }
}
