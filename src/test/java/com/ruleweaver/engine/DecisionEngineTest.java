package com.ruleweaver.engine;

import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.EvaluationResult;
import com.ruleweaver.model.PolicyVersion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionEngineTest {

    private final DecisionEngine decisionEngine = new DecisionEngine();

    @Test
    void overallDecisionMatchesAllRecordedConditionDecisions() {
        EvaluationResult result = decisionEngine.evaluate(
                new Applicant("A-1", "Applicant", 7.5, 80, List.of("Java")),
                new PolicyVersion("policy-1", 1, List.of(
                        new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.0),
                        new SkillCondition("Java"))));

        assertEquals(result.getConditionResults().stream().allMatch(condition -> condition.isPassed()),
                result.isOverallDecision());
        assertEquals("policy-1_v1", result.getPolicyVersionId());
    }

    @Test
    void policyWithoutConditionsPassesWithoutInventingConditionResults() {
        EvaluationResult result = decisionEngine.evaluate(
                new Applicant("A-1", "Applicant", 7.5, 80, List.of()),
                new PolicyVersion("policy-1", 1, List.of()));

        assertTrue(result.isOverallDecision());
        assertTrue(result.getConditionResults().isEmpty());
    }
}
