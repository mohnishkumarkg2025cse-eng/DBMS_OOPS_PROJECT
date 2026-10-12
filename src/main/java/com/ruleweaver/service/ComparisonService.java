package com.ruleweaver.service;

import com.ruleweaver.engine.DecisionEngine;
import com.ruleweaver.model.*;
import java.util.*;

/**
 * Service driving the 'Decision Time Machine' policy comparison functionality.
 * Analyzes rule version differences, pinpoints exact reasons for flips in candidate eligibility,
 * and generates aggregate statistical reports.
 */
public class ComparisonService {
    private final DecisionEngine decisionEngine;

    public ComparisonService(DecisionEngine decisionEngine) {
        this.decisionEngine = decisionEngine != null ? decisionEngine : new DecisionEngine();
    }

    /**
     * Re-evaluates applicants under two policy versions and compares the outcomes.
     */
    public ComparisonReport compareVersions(List<Applicant> applicants, PolicyVersion v1, PolicyVersion v2) {
        if (v1 == null || v2 == null) {
            throw new IllegalArgumentException("Both policy versions must be provided for comparison");
        }
        if (applicants == null) {
            applicants = Collections.emptyList();
        }

        ComparisonReport report = new ComparisonReport(v1.getPolicyId(), v1.getVersionNumber(), v2.getVersionNumber());
        report.setTotalApplicants(applicants.size());

        int v1Pass = 0, v1Fail = 0, v2Pass = 0, v2Fail = 0;

        for (Applicant applicant : applicants) {
            EvaluationResult res1 = decisionEngine.evaluate(applicant, v1);
            EvaluationResult res2 = decisionEngine.evaluate(applicant, v2);

            if (res1.isOverallDecision()) v1Pass++; else v1Fail++;
            if (res2.isOverallDecision()) v2Pass++; else v2Fail++;

            ApplicantDiff diff = buildApplicantDiff(applicant.getId(), applicant.getName(), res1, res2);
            report.addDiff(diff);
        }

        report.setV1PassedCount(v1Pass);
        report.setV1FailedCount(v1Fail);
        report.setV2PassedCount(v2Pass);
        report.setV2FailedCount(v2Fail);

        return report;
    }

    /**
     * Compares pre-computed historical evaluation results (e.g., loaded from database).
     */
    public ComparisonReport compareEvaluations(List<EvaluationResult> v1Results, List<EvaluationResult> v2Results) {
        if (v1Results == null || v2Results == null) {
            throw new IllegalArgumentException("Evaluation result lists cannot be null");
        }

        List<EvaluationResult> latestV1Results = EvaluationAnalytics.latestPerApplicant(v1Results);
        List<EvaluationResult> latestV2Results = EvaluationAnalytics.latestPerApplicant(v2Results);
        Map<String, EvaluationResult> v2Map = new HashMap<>();
        for (EvaluationResult res : latestV2Results) {
            v2Map.put(res.getApplicantId(), res);
        }

        int v1Num = latestV1Results.isEmpty() ? 1 : latestV1Results.get(0).getPolicyVersionNumber();
        int v2Num = latestV2Results.isEmpty() ? 2 : latestV2Results.get(0).getPolicyVersionNumber();
        String policyId = latestV1Results.isEmpty() ? "unknown" : latestV1Results.get(0).getPolicyId();

        ComparisonReport report = new ComparisonReport(policyId, v1Num, v2Num);
        int comparedApplicants = (int) latestV1Results.stream()
                .filter(result -> v2Map.containsKey(result.getApplicantId()))
                .count();
        report.setTotalApplicants(comparedApplicants);

        int v1Pass = 0, v1Fail = 0, v2Pass = 0, v2Fail = 0;

        for (EvaluationResult res1 : latestV1Results) {
            EvaluationResult res2 = v2Map.get(res1.getApplicantId());
            if (res2 == null) {
                continue;
            }

            if (res1.isOverallDecision()) v1Pass++; else v1Fail++;
            if (res2.isOverallDecision()) v2Pass++; else v2Fail++;

            ApplicantDiff diff = buildApplicantDiff(res1.getApplicantId(), res1.getApplicantName(), res1, res2);
            report.addDiff(diff);
        }

        report.setV1PassedCount(v1Pass);
        report.setV1FailedCount(v1Fail);
        report.setV2PassedCount(v2Pass);
        report.setV2FailedCount(v2Fail);

        return report;
    }

    private ApplicantDiff buildApplicantDiff(String applicantId, String applicantName,
                                            EvaluationResult res1, EvaluationResult res2) {
        ApplicantDiff diff = new ApplicantDiff(
                applicantId,
                applicantName,
                res1.isOverallDecision(),
                res2.isOverallDecision(),
                res1,
                res2
        );

        if (diff.hasDecisionChanged()) {
            // Pinpoint which conditions changed outcome
            if (diff.isV1Decision() && !diff.isV2Decision()) {
                // Passed v1, failed v2 -> find new failed conditions in v2
                for (ConditionResult cr2 : res2.getFailedConditions()) {
                    diff.addReason(String.format("Failed under v2: %s", cr2.getExplanation()));
                }
            } else if (!diff.isV1Decision() && diff.isV2Decision()) {
                // Failed v1, passed v2 -> find conditions satisfied in v2 that failed in v1
                for (ConditionResult cr1 : res1.getFailedConditions()) {
                    diff.addReason(String.format("Satisfied new policy criteria (previously failed v1 condition: %s)", cr1.getConditionName()));
                }
            }
        }

        return diff;
    }
}
