package com.ruleweaver.engine;

import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.ConditionResult;
import com.ruleweaver.model.EvaluationResult;
import com.ruleweaver.model.PolicyVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core Algorithmic Decision Engine.
 * 
 * Demonstrates Polymorphism:
 * Iterates through a collection of abstract Condition interfaces, invoking
 * test() and explain() polymorphically at runtime without coupling to any concrete
 * subtype (NumericCondition, SkillCondition, CompositeCondition, etc.).
 */
public class DecisionEngine {

    /**
     * Evaluates a single applicant against a given policy version.
     *
     * @param applicant the candidate being assessed
     * @param policyVersion the specific immutable version of the policy
     * @return explainable EvaluationResult containing condition breakdowns and final verdict
     */
    public EvaluationResult evaluate(Applicant applicant, PolicyVersion policyVersion) {
        if (applicant == null) {
            throw new IllegalArgumentException("Applicant cannot be null for evaluation");
        }
        if (policyVersion == null) {
            throw new IllegalArgumentException("PolicyVersion cannot be null for evaluation");
        }

        EvaluationResult result = new EvaluationResult(
                applicant.getId(),
                applicant.getName(),
                policyVersion.getPolicyId(),
                policyVersion.getDescription(),
                policyVersion.getVersionNumber()
        );
        result.setId(UUID.randomUUID().toString());

        List<Condition> conditions = policyVersion.getConditions();
        if (conditions.isEmpty()) {
            // Vacuously true if policy has no criteria defined
            result.setOverallDecision(true);
            return result;
        }

        boolean allPassed = true;
        for (Condition condition : conditions) {
            // Polymorphic dispatch: Condition.evaluate() runs the concrete implementation
            ConditionResult conditionResult = condition.evaluate(applicant);
            result.addConditionResult(conditionResult);

            if (!conditionResult.isPassed()) {
                allPassed = false;
            }
        }

        result.setOverallDecision(allPassed);
        return result;
    }

    /**
     * Batch evaluates multiple applicants against a policy version.
     *
     * @param applicants list of candidates
     * @param policyVersion the targeted policy version
     * @return list of EvaluationResults
     */
    public List<EvaluationResult> evaluateAll(List<Applicant> applicants, PolicyVersion policyVersion) {
        if (applicants == null) {
            return new ArrayList<>();
        }
        List<EvaluationResult> results = new ArrayList<>();
        for (Applicant applicant : applicants) {
            results.add(evaluate(applicant, policyVersion));
        }
        return results;
    }
}
