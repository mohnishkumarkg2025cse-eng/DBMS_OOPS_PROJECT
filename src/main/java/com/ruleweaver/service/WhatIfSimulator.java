package com.ruleweaver.service;

import com.ruleweaver.engine.CompositeCondition;
import com.ruleweaver.engine.Condition;
import com.ruleweaver.engine.ConditionDocumentMapper;
import com.ruleweaver.engine.DecisionEngine;
import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.ConditionResult;
import com.ruleweaver.model.EvaluationResult;
import com.ruleweaver.model.PolicyVersion;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs a threshold experiment against a cloned policy version without persisting results.
 */
public final class WhatIfSimulator {
    private final DecisionEngine decisionEngine;

    public WhatIfSimulator(DecisionEngine decisionEngine) {
        if (decisionEngine == null) {
            throw new IllegalArgumentException("Decision engine is required.");
        }
        this.decisionEngine = decisionEngine;
    }

    public List<NumericRule> findNumericRules(PolicyVersion version) {
        if (version == null) {
            return List.of();
        }
        List<NumericRule> rules = new ArrayList<>();
        for (int i = 0; i < version.getConditions().size(); i++) {
            collectNumericRules(version.getConditions().get(i), List.of(i), rules);
        }
        return List.copyOf(rules);
    }

    public SimulationResult simulate(PolicyVersion realVersion,
                                     NumericRule selectedRule,
                                     double threshold,
                                     List<Applicant> applicants,
                                     List<EvaluationResult> storedResults) {
        if (realVersion == null || selectedRule == null) {
            throw new IllegalArgumentException("A policy version and numeric rule are required.");
        }
        if (applicants == null || storedResults == null) {
            throw new IllegalArgumentException("Applicants and stored evaluation results are required.");
        }

        List<Condition> clonedConditions = realVersion.getConditions().stream()
                .map(condition -> ConditionDocumentMapper.fromDocument(
                        ConditionDocumentMapper.toDocument(condition)))
                .toList();
        NumericCondition clonedRule = numericConditionAt(clonedConditions, selectedRule.path());
        if (!clonedRule.getFieldName().equals(selectedRule.fieldName())) {
            throw new IllegalArgumentException("The selected numeric rule does not belong to this version.");
        }
        clonedRule.setThreshold(threshold);

        PolicyVersion simulatedVersion = new PolicyVersion(
                realVersion.getId(),
                realVersion.getPolicyId(),
                realVersion.getVersionNumber(),
                realVersion.getDescription(),
                clonedConditions);

        Map<String, EvaluationResult> baselineByApplicant = new HashMap<>();
        for (EvaluationResult result : storedResults) {
            if (result.getApplicantId() != null) {
                baselineByApplicant.merge(result.getApplicantId(), result, (previous, current) -> {
                    if (previous.getEvaluatedOn() == null) {
                        return current;
                    }
                    if (current.getEvaluatedOn() == null) {
                        return previous;
                    }
                    return current.getEvaluatedOn().isAfter(previous.getEvaluatedOn())
                            ? current : previous;
                });
            }
        }

        List<ApplicantFlip> flips = new ArrayList<>();
        int simulatedPassCount = 0;
        int baselinePassCount = 0;
        int comparedCount = 0;
        for (Applicant applicant : applicants) {
            EvaluationResult baseline = applicant.getId() == null
                    ? null : baselineByApplicant.get(applicant.getId());
            if (baseline == null) {
                continue;
            }

            EvaluationResult simulated = decisionEngine.evaluate(applicant, simulatedVersion);
            comparedCount++;
            if (simulated.isOverallDecision()) {
                simulatedPassCount++;
            }
            if (baseline.isOverallDecision()) {
                baselinePassCount++;
            }
            if (simulated.isOverallDecision() != baseline.isOverallDecision()) {
                List<ConditionResult> failedConditions = simulated.isOverallDecision()
                        ? baseline.getFailedConditions() : simulated.getFailedConditions();
                String ruleDetails = failedConditions.stream()
                        .map(ConditionResult::getConditionName)
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("No failed-rule details are available");
                String failingRules = simulated.isOverallDecision()
                        ? "Previously failed (real version): " + ruleDetails
                        : "Fails what-if version: " + ruleDetails;
                flips.add(new ApplicantFlip(applicant.getName(), failingRules));
            }
        }
        if (comparedCount == 0) {
            throw new IllegalStateException(
                    "No stored results match the current applicants for this policy version. Evaluate this version first.");
        }

        return new SimulationResult(simulatedVersion, simulatedPassCount, baselinePassCount,
                simulatedPassCount - baselinePassCount, comparedCount, flips);
    }

    private static void collectNumericRules(Condition condition, List<Integer> path, List<NumericRule> rules) {
        if (condition instanceof NumericCondition numeric) {
            rules.add(new NumericRule(path, numeric.getMessage(), numeric.getFieldName(), numeric.getThreshold()));
        } else if (condition instanceof CompositeCondition composite) {
            for (int i = 0; i < composite.getSubconditions().size(); i++) {
                List<Integer> childPath = new ArrayList<>(path);
                childPath.add(i);
                collectNumericRules(composite.getSubconditions().get(i), childPath, rules);
            }
        }
    }

    private static NumericCondition numericConditionAt(List<Condition> conditions, List<Integer> path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("A numeric rule path is required.");
        }
        Condition current = conditions.get(path.get(0));
        for (int i = 1; i < path.size(); i++) {
            if (!(current instanceof CompositeCondition composite)) {
                throw new IllegalArgumentException("The selected numeric rule path is invalid.");
            }
            current = composite.getSubconditions().get(path.get(i));
        }
        if (!(current instanceof NumericCondition numeric)) {
            throw new IllegalArgumentException("The selected rule is not numeric.");
        }
        return numeric;
    }

    public record NumericRule(List<Integer> path, String label, String fieldName, double threshold) {
        public NumericRule {
            path = List.copyOf(path);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public record ApplicantFlip(String applicantName, String failingRule) {
    }

    public record SimulationResult(PolicyVersion simulatedVersion,
                                  int passCount,
                                  int baselinePassCount,
                                  int delta,
                                  int comparedCount,
                                  List<ApplicantFlip> flips) {
        public SimulationResult {
            flips = List.copyOf(flips);
        }
    }
}
