package com.ruleweaver.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Encapsulates the aggregate report comparing two policy versions across an applicant pool.
 * Drives the "Decision Time Machine" visualization and statistical reporting.
 */
public class ComparisonReport {
    private String policyId;
    private int v1Number;
    private int v2Number;
    private int totalApplicants;
    private int v1PassedCount;
    private int v1FailedCount;
    private int v2PassedCount;
    private int v2FailedCount;
    private List<ApplicantDiff> changedApplicants;
    private List<ApplicantDiff> unchangedApplicants;

    public ComparisonReport(String policyId, int v1Number, int v2Number) {
        this.policyId = policyId;
        this.v1Number = v1Number;
        this.v2Number = v2Number;
        this.changedApplicants = new ArrayList<>();
        this.unchangedApplicants = new ArrayList<>();
    }

    public String getPolicyId() {
        return policyId;
    }

    public int getV1Number() {
        return v1Number;
    }

    public int getV2Number() {
        return v2Number;
    }

    public int getTotalApplicants() {
        return totalApplicants;
    }

    public void setTotalApplicants(int totalApplicants) {
        this.totalApplicants = totalApplicants;
    }

    public int getV1PassedCount() {
        return v1PassedCount;
    }

    public void setV1PassedCount(int v1PassedCount) {
        this.v1PassedCount = v1PassedCount;
    }

    public int getV1FailedCount() {
        return v1FailedCount;
    }

    public void setV1FailedCount(int v1FailedCount) {
        this.v1FailedCount = v1FailedCount;
    }

    public int getV2PassedCount() {
        return v2PassedCount;
    }

    public void setV2PassedCount(int v2PassedCount) {
        this.v2PassedCount = v2PassedCount;
    }

    public int getV2FailedCount() {
        return v2FailedCount;
    }

    public void setV2FailedCount(int v2FailedCount) {
        this.v2FailedCount = v2FailedCount;
    }

    public List<ApplicantDiff> getChangedApplicants() {
        return changedApplicants;
    }

    public List<ApplicantDiff> getUnchangedApplicants() {
        return unchangedApplicants;
    }

    public void addDiff(ApplicantDiff diff) {
        if (diff.hasDecisionChanged()) {
            this.changedApplicants.add(diff);
        } else {
            this.unchangedApplicants.add(diff);
        }
    }

    public long getLostEligibilityCount() {
        return changedApplicants.stream().filter(d -> d.isV1Decision() && !d.isV2Decision()).count();
    }

    public long getGainedEligibilityCount() {
        return changedApplicants.stream().filter(d -> !d.isV1Decision() && d.isV2Decision()).count();
    }

    public String getSummaryText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=================================================================\n");
        sb.append(String.format(" POLICY IMPACT REPORT: v%d vs v%d (Policy: %s)\n", v1Number, v2Number, policyId));
        sb.append("=================================================================\n");
        sb.append(String.format("Total Candidates Evaluated : %d\n", totalApplicants));
        sb.append(String.format("Version %d Outcome           : %d Passed (%.1f%%), %d Failed\n", 
                v1Number, v1PassedCount, totalApplicants > 0 ? (v1PassedCount * 100.0 / totalApplicants) : 0, v1FailedCount));
        sb.append(String.format("Version %d Outcome           : %d Passed (%.1f%%), %d Failed\n", 
                v2Number, v2PassedCount, totalApplicants > 0 ? (v2PassedCount * 100.0 / totalApplicants) : 0, v2FailedCount));
        sb.append("-----------------------------------------------------------------\n");
        sb.append(String.format("Total Decision Shifts      : %d candidate(s)\n", changedApplicants.size()));
        sb.append(String.format(" • Lost Eligibility        : %d\n", getLostEligibilityCount()));
        sb.append(String.format(" • Gained Eligibility      : %d\n", getGainedEligibilityCount()));
        sb.append("=================================================================\n");

        if (!changedApplicants.isEmpty()) {
            sb.append("\nDetailed Candidate Impact:\n");
            for (ApplicantDiff diff : changedApplicants) {
                sb.append(diff.toString());
            }
        } else {
            sb.append("\nNo candidates changed status between these versions.\n");
        }
        return sb.toString();
    }
}
