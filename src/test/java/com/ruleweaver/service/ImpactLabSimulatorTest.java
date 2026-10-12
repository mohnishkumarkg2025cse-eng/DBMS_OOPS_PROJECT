package com.ruleweaver.service;

import com.ruleweaver.engine.DecisionEngine;
import com.ruleweaver.engine.CompositeCondition;
import com.ruleweaver.engine.FieldCondition;
import com.ruleweaver.engine.LogicalOperator;
import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.engine.Operator;
import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.FieldDefinition;
import com.ruleweaver.model.FieldOperator;
import com.ruleweaver.model.FieldType;
import com.ruleweaver.model.FieldValidationRules;
import com.ruleweaver.model.PolicyVersion;
import com.ruleweaver.model.ProgramConfiguration;
import com.ruleweaver.model.ProgramTemplate;
import com.ruleweaver.repository.EvaluationResultRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpactLabSimulatorTest {
    private final DecisionEngine engine = new DecisionEngine();
    private final WhatIfSimulator simulator = new WhatIfSimulator(engine);
    private final ProgramConfiguration program = new ProgramConfiguration("program-1", "Test program",
            ProgramTemplate.CUSTOM, List.of(
            new FieldDefinition("portfolioScore", "Portfolio score", FieldType.NUMBER, false,
                    new FieldValidationRules(0.0, 100.0, null, null, null), List.of(), null),
            new FieldDefinition("confirmed", "Confirmed", FieldType.BOOLEAN, false,
                    FieldValidationRules.none(), List.of(), null),
            new FieldDefinition("skills", "Skills", FieldType.MULTI_SELECT, false,
                    FieldValidationRules.none(), List.of("Java", "Python", "Go"), null)));

    @Test
    void recalculatesBothVersionsWithDecisionEngineAndReportsChangedApplicants() {
        PolicyVersion base = version(List.of(
                new NumericCondition("portfolioScore", Operator.GREATER_THAN_OR_EQUAL, 70),
                new FieldCondition("confirmed", FieldOperator.EQUALS, true),
                new FieldCondition("skills", FieldOperator.CONTAINS_ANY, List.of("Java"))));
        List<Applicant> applicants = List.of(
                applicant("a-1", 75, true, List.of("Java")),
                applicant("a-2", 60, false, List.of("Python")));

        WhatIfSimulator.SimulationResult result = simulator.simulate(base, List.of(
                new WhatIfSimulator.RuleAdjustment(List.of(0), 55.0),
                new WhatIfSimulator.RuleAdjustment(List.of(1), false),
                new WhatIfSimulator.RuleAdjustment(List.of(2), List.of("Python"))),
                program, applicants, ignored -> { }, () -> false);

        assertEquals(1, result.baselinePassCount());
        assertEquals(1, result.passCount());
        assertEquals(0, result.delta());
        assertEquals(2, result.comparedCount());
        assertEquals(2, result.flips().size());
        assertEquals("a-1", result.flips().get(0).applicantId());
        assertTrue(result.flips().get(0).baselineEligible());
        assertFalse(result.flips().get(0).simulatedEligible());
        assertTrue(result.flips().get(0).failingRule().contains("confirmed"));
        assertTrue(result.flips().get(1).failingRule().contains("skills"));

        int recomputedPassCount = (int) applicants.stream()
                .filter(applicant -> engine.evaluate(applicant, result.simulatedVersion()).isOverallDecision())
                .count();
        assertEquals(recomputedPassCount, result.passCount());
        assertEquals(70.0, ((NumericCondition) base.getConditions().get(0)).getThreshold());
        assertEquals(Boolean.TRUE, ((FieldCondition) base.getConditions().get(1)).getExpectedValue());
    }

    @Test
    void findsCompatibleControlsAndHonorsProgramNumericBounds() {
        PolicyVersion base = version(List.of(
                new NumericCondition("portfolioScore", Operator.GREATER_THAN_OR_EQUAL, 50),
                new FieldCondition("confirmed", FieldOperator.EQUALS, true),
                new FieldCondition("skills", FieldOperator.CONTAINS_ANY, List.of("Java"))));

        WhatIfSimulator.NumericRule numeric = simulator.findNumericRules(base, program).get(0);
        assertEquals(0.0, numeric.minimum());
        assertEquals(100.0, numeric.maximum());
        assertEquals(1, simulator.findBooleanRules(base, program).size());
        assertEquals(1, simulator.findMultiSelectRules(base, program).size());
        assertEquals(List.of("Java", "Python", "Go"),
                simulator.findMultiSelectRules(base, program).get(0).options());

        assertThrows(IllegalArgumentException.class, () -> simulator.simulate(base, List.of(
                new WhatIfSimulator.RuleAdjustment(List.of(0), 101.0)), program, List.of(), null, null));
        assertThrows(IllegalArgumentException.class, () -> simulator.simulate(base, List.of(
                new WhatIfSimulator.RuleAdjustment(List.of(2), List.of("Not permitted"))),
                program, List.of(), null, null));
        assertThrows(IllegalArgumentException.class, () -> simulator.simulate(base, List.of(
                new WhatIfSimulator.RuleAdjustment(List.of(1), "true")),
                program, List.of(), null, null));
    }

    @Test
    void capsNumericThresholdChangesAtThreeAndAcceptsBoundaryValues() {
        List<NumericCondition> conditions = List.of(
                new NumericCondition("portfolioScore", Operator.GREATER_THAN_OR_EQUAL, 20),
                new NumericCondition("portfolioScore", Operator.GREATER_THAN_OR_EQUAL, 30),
                new NumericCondition("portfolioScore", Operator.GREATER_THAN_OR_EQUAL, 40),
                new NumericCondition("portfolioScore", Operator.GREATER_THAN_OR_EQUAL, 50));
        PolicyVersion base = version(conditions);
        List<WhatIfSimulator.RuleAdjustment> threeThresholds = List.of(
                new WhatIfSimulator.RuleAdjustment(List.of(0), 0.0),
                new WhatIfSimulator.RuleAdjustment(List.of(1), 0.0),
                new WhatIfSimulator.RuleAdjustment(List.of(2), 100.0));

        WhatIfSimulator.SimulationResult result =
                simulator.simulate(base, threeThresholds, program, List.of(), null, null);
        assertEquals(0, result.comparedCount());
        assertEquals(0, result.passCount());

        assertThrows(IllegalArgumentException.class, () -> simulator.simulate(base, List.of(
                new WhatIfSimulator.RuleAdjustment(List.of(0), 0.0),
                new WhatIfSimulator.RuleAdjustment(List.of(1), 0.0),
                new WhatIfSimulator.RuleAdjustment(List.of(2), 0.0),
                new WhatIfSimulator.RuleAdjustment(List.of(3), 0.0)),
                program, List.of(), null, null));
    }

    @Test
    void returnsZeroMetricsForEmptyApplicantCohortAndReportsProgressForNonEmptyRuns() {
        PolicyVersion base = version(List.of(
                new NumericCondition("portfolioScore", Operator.GREATER_THAN_OR_EQUAL, 70)));
        WhatIfSimulator.SimulationResult empty = simulator.simulate(
                base, List.of(), program, List.of(), null, null);
        assertEquals(0, empty.comparedCount());
        assertEquals(0, empty.passCount());
        assertEquals(0, empty.baselinePassCount());

        int[] completed = {0};
        simulator.simulate(base, List.of(), program,
                List.of(applicant("a-1", 80, true, List.of("Java"))),
                count -> completed[0] = count, () -> false);
        assertEquals(1, completed[0]);
        assertThrows(java.util.concurrent.CancellationException.class,
                () -> simulator.simulate(base, List.of(), program,
                        List.of(applicant("a-1", 80, true, List.of("Java"))),
                        null, () -> true));
    }

    @Test
    void appliesNestedRuleAdjustmentsToCloneAndKeepsBaseVersionUntouched() {
        CompositeCondition group = new CompositeCondition("Portfolio gate", LogicalOperator.AND,
                List.of(new NumericCondition("portfolioScore", Operator.GREATER_THAN_OR_EQUAL, 80)));
        PolicyVersion base = version(List.of(group));
        List<WhatIfSimulator.NumericRule> rules = simulator.findNumericRules(base, program);

        WhatIfSimulator.SimulationResult result = simulator.simulate(base, List.of(
                new WhatIfSimulator.RuleAdjustment(rules.get(0).path(), 70.0)), program,
                List.of(applicant("a-1", 75, true, List.of("Java"))), null, null);

        assertEquals(List.of(0, 0), rules.get(0).path());
        assertEquals(80.0, ((NumericCondition) group.getSubconditions().get(0)).getThreshold());
        CompositeCondition simulatedGroup =
                (CompositeCondition) result.simulatedVersion().getConditions().get(0);
        assertEquals(70.0, ((NumericCondition) simulatedGroup.getSubconditions().get(0)).getThreshold());
        assertEquals(1, result.passCount());
    }

    @Test
    void simulatorHasNoEvaluationPersistenceDependency() {
        assertTrue(Arrays.stream(WhatIfSimulator.class.getDeclaredFields())
                .noneMatch(field -> EvaluationResultRepository.class.isAssignableFrom(field.getType())));
    }

    private static Applicant applicant(String id, double score, boolean confirmed, List<String> skills) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("portfolioScore", score);
        attributes.put("confirmed", confirmed);
        return new Applicant(id, id, "", score, 0, skills, attributes);
    }

    private static PolicyVersion version(List<? extends com.ruleweaver.engine.Condition> conditions) {
        return new PolicyVersion("policy-1_v1", "policy-1", 1, "Base version", List.copyOf(conditions));
    }
}
