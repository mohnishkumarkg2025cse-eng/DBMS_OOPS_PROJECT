package com.ruleweaver.service;

import com.ruleweaver.engine.Condition;
import com.ruleweaver.engine.DecisionEngine;
import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.PolicyVersion;

import java.util.List;
import java.util.Objects;

/**
 * Calculates an unsaved policy draft against the same applicant cohort as its baseline.
 */
public final class PolicyDraftImpactService {
    private final DecisionEngine decisionEngine;

    public PolicyDraftImpactService(DecisionEngine decisionEngine) {
        this.decisionEngine = Objects.requireNonNull(decisionEngine, "Decision engine is required.");
    }

    public ImpactSummary compare(PolicyVersion baseline, List<Condition> draftConditions,
                                 List<Applicant> applicants) {
        Objects.requireNonNull(baseline, "A saved or empty-policy baseline is required.");
        Objects.requireNonNull(draftConditions, "Draft conditions are required.");
        Objects.requireNonNull(applicants, "Applicant sample is required.");
        PolicyVersion draft = new PolicyVersion(baseline.getId(), baseline.getPolicyId(),
                baseline.getVersionNumber(), baseline.getDescription(), draftConditions);
        int baselineEligible = 0;
        int draftEligible = 0;
        for (Applicant applicant : applicants) {
            if (Thread.currentThread().isInterrupted()) {
                throw new java.util.concurrent.CancellationException("Draft impact preview cancelled.");
            }
            if (applicant == null) {
                throw new IllegalArgumentException("Applicant sample must not contain null entries.");
            }
            if (decisionEngine.evaluate(applicant, baseline).isOverallDecision()) baselineEligible++;
            if (decisionEngine.evaluate(applicant, draft).isOverallDecision()) draftEligible++;
        }
        return new ImpactSummary(applicants.size(), baselineEligible, draftEligible);
    }

    public record ImpactSummary(int sampleSize, int baselineEligible, int draftEligible) {
        public int eligibleChange() {
            return draftEligible - baselineEligible;
        }
    }
}
