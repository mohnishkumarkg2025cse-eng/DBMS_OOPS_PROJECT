package com.ruleweaver.service;

import com.ruleweaver.engine.DecisionEngine;
import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.engine.Operator;
import com.ruleweaver.engine.SkillCondition;
import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.EvaluationResult;
import com.ruleweaver.model.PolicyVersion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatIfSimulatorTest {
    private final DecisionEngine decisionEngine = new DecisionEngine();
    private final WhatIfSimulator simulator = new WhatIfSimulator(decisionEngine);

    @Test
    void gpa83Interview80AndJavaPassV1AndFailV2AndWhatIfDoesNotMutateVersion() {
        Applicant applicant = new Applicant("app-1", "Alex Example", 8.3, 80, List.of("Java"));
        PolicyVersion version1 = version(1, 8.0);
        PolicyVersion version2 = version(2, 8.5);

        assertTrue(decisionEngine.evaluate(applicant, version1).isOverallDecision());
        EvaluationResult storedV2 = decisionEngine.evaluate(applicant, version2);
        assertFalse(storedV2.isOverallDecision());
        EvaluationResult olderStoredV2 = new EvaluationResult(
                applicant.getId(), applicant.getName(), "policy-1", "Test policy", 2);
        olderStoredV2.setOverallDecision(true);
        olderStoredV2.setEvaluatedOn(LocalDateTime.now().minusDays(1));

        WhatIfSimulator.NumericRule gpaRule = simulator.findNumericRules(version2).get(0);
        WhatIfSimulator.SimulationResult result = simulator.simulate(
                version2,
                gpaRule,
                8.0,
                List.of(applicant),
                List.of(olderStoredV2, storedV2));

        assertEquals(1, result.passCount());
        assertEquals(0, result.baselinePassCount());
        assertEquals(1, result.delta());
        assertEquals(1, result.comparedCount());
        assertEquals("Alex Example", result.flips().get(0).applicantName());
        assertEquals("Previously failed (real version): Gpa >= 8.50", result.flips().get(0).failingRule());
        assertEquals(8.5, ((NumericCondition) version2.getConditions().get(0)).getThreshold());
        assertNotSame(version2.getConditions(), result.simulatedVersion().getConditions());
    }

    private static PolicyVersion version(int number, double gpaThreshold) {
        return new PolicyVersion("policy-1", number, List.of(
                new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, gpaThreshold),
                new NumericCondition("interviewScore", Operator.GREATER_THAN_OR_EQUAL, 70),
                new SkillCondition("Java")));
    }
}
