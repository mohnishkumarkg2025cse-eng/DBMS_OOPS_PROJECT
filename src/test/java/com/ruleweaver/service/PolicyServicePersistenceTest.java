package com.ruleweaver.service;

import com.ruleweaver.engine.DecisionEngine;
import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.engine.Operator;
import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.Policy;
import com.ruleweaver.model.PolicyVersion;
import com.ruleweaver.model.EvaluationResult;
import com.ruleweaver.model.FieldDefinition;
import com.ruleweaver.model.FieldOperator;
import com.ruleweaver.model.FieldType;
import com.ruleweaver.model.FieldValidationRules;
import com.ruleweaver.model.ProgramTemplate;
import com.ruleweaver.repository.InMemoryEvaluationResultRepository;
import com.ruleweaver.repository.InMemoryPolicyRepository;
import com.ruleweaver.repository.InMemoryProgramRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PolicyServicePersistenceTest {

    @Test
    void savesEvaluationRunsAppendOnlyAndReportsProgress() {
        InMemoryPolicyRepository policies = new InMemoryPolicyRepository();
        InMemoryEvaluationResultRepository evaluations = new InMemoryEvaluationResultRepository();
        PolicyService service = service(policies, evaluations);
        service.createPolicy("policy-1", "Policy", "Initial", List.of(
                new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.0)));
        List<Applicant> applicants = List.of(
                new Applicant("a-1", "Alice", 8.5, 80, List.of()),
                new Applicant("a-2", "Bob", 7.0, 75, List.of()));
        AtomicInteger progress = new AtomicInteger();

        List<EvaluationResult> firstRun = service.evaluateApplicants(
                "policy-1", 1, applicants, progress::set, () -> false);
        List<EvaluationResult> secondRun = service.evaluateApplicants(
                "policy-1", 1, applicants, ignored -> { }, () -> false);

        assertEquals(2, progress.get());
        assertEquals(4, evaluations.findByPolicyAndVersion("policy-1", 1).size());
        assertEquals(4, evaluations.countByPolicyAndVersion("policy-1", 1));
        assertNotNull(firstRun.get(0).getEvaluationRunId());
        assertEquals(firstRun.get(0).getEvaluationRunId(), firstRun.get(1).getEvaluationRunId());
        assertNotEquals(firstRun.get(0).getEvaluationRunId(), secondRun.get(0).getEvaluationRunId());
    }

    @Test
    void cancellationDoesNotPersistPartialEvaluationResults() {
        InMemoryPolicyRepository policies = new InMemoryPolicyRepository();
        InMemoryEvaluationResultRepository evaluations = new InMemoryEvaluationResultRepository();
        PolicyService service = service(policies, evaluations);
        service.createPolicy("policy-1", "Policy", "Initial", List.of());
        AtomicInteger completed = new AtomicInteger();
        List<Applicant> applicants = List.of(
                new Applicant("a-1", "Alice", 8.5, 80, List.of()),
                new Applicant("a-2", "Bob", 7.0, 75, List.of()));

        assertThrows(CancellationException.class, () -> service.evaluateApplicants(
                "policy-1", 1, applicants, completed::set, () -> completed.get() == 1));
        assertTrue(evaluations.findByPolicyAndVersion("policy-1", 1).isEmpty());
    }

    @Test
    void versionsAreImmutableAndActiveVersionIsStored() {
        InMemoryPolicyRepository policies = new InMemoryPolicyRepository();
        PolicyService service = service(policies, new InMemoryEvaluationResultRepository());
        service.createPolicy("policy-1", "Policy", "Initial", List.of());
        PolicyVersion second = service.createNewVersion("policy-1", "Revised", List.of());

        assertThrows(IllegalStateException.class,
                () -> policies.saveVersion("policy-1", second));
        service.setActiveVersion("policy-1", second.getVersionNumber());

        Policy stored = policies.findById("policy-1").orElseThrow();
        assertEquals(2, stored.getActiveVersionNumber());
        assertEquals(2, stored.getVersions().size());
    }

    @Test
    void evaluationHistoryCapturesPolicyVersionAndApplicantExplanation() {
        InMemoryPolicyRepository policies = new InMemoryPolicyRepository();
        InMemoryEvaluationResultRepository evaluations = new InMemoryEvaluationResultRepository();
        PolicyService service = service(policies, evaluations);
        service.createPolicy("policy-1", "Policy name", "Initial version", List.of(
                new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.0)));

        EvaluationResult saved = service.evaluateApplicants("policy-1", 1,
                List.of(new Applicant("a-1", "Alice", 7.5, 80, List.of()))).get(0);

        assertEquals("Policy name", saved.getPolicyName());
        assertEquals("policy-1_v1", saved.getPolicyVersionId());
        assertEquals(1, saved.getPolicyVersionNumber());
        assertNotNull(saved.getEvaluatedOn());
        assertFalse(saved.isOverallDecision());
        assertEquals("Gpa >= 8", saved.getFailedConditions().get(0).getConditionName());
        assertFalse(saved.getFailedConditions().get(0).getExplanation().isBlank());
        assertSame(saved, evaluations.findByPolicyAndVersion("policy-1", 1).get(0));
    }

    @Test
    void savedInMemoryVersionIsDetachedFromMutableCallerSnapshots() {
        InMemoryPolicyRepository policies = new InMemoryPolicyRepository();
        PolicyService service = service(policies, new InMemoryEvaluationResultRepository());
        service.createPolicy("policy-1", "Policy", "Initial", List.of(
                new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.0)));

        PolicyVersion loaded = policies.findVersion("policy-1", 1).orElseThrow();
        ((NumericCondition) loaded.getConditions().get(0)).setThreshold(2.0);
        loaded.setDescription("Mutated outside the repository");

        PolicyVersion stored = policies.findVersion("policy-1", 1).orElseThrow();
        assertEquals(8.0, ((NumericCondition) stored.getConditions().get(0)).getThreshold());
        assertEquals("Initial", stored.getDescription());
    }

    @Test
    void archivingPolicyKeepsItsImmutableVersionSnapshots() {
        InMemoryPolicyRepository policies = new InMemoryPolicyRepository();
        PolicyService service = service(policies, new InMemoryEvaluationResultRepository());
        service.createPolicy("policy-1", "Policy", "Initial", List.of());

        assertTrue(policies.deleteById("policy-1"));

        Policy archived = policies.findById("policy-1").orElseThrow();
        assertTrue(archived.isArchived());
        assertEquals(1, archived.getVersions().size());
    }

    @Test
    void programLinkedPoliciesValidateCustomRulesAndApplicantRequiredValues() {
        InMemoryPolicyRepository policies = new InMemoryPolicyRepository();
        InMemoryEvaluationResultRepository evaluations = new InMemoryEvaluationResultRepository();
        var programs = new com.ruleweaver.service.ProgramService(new InMemoryProgramRepository());
        programs.createProgram("program-1", "Custom", ProgramTemplate.CUSTOM, List.of(
                new FieldDefinition("portfolioScore", "Portfolio score", FieldType.NUMBER, true,
                        new FieldValidationRules(0.0, 100.0, null, null, null), List.of(), null)));
        PolicyService service = service(policies, evaluations);
        service.setProgramService(programs);

        assertThrows(IllegalArgumentException.class, () -> service.createPolicy(
                "policy-1", "Policy", "Description", "program-1",
                List.of(new NumericCondition("unknown", Operator.GREATER_THAN, 1))));
        service.createPolicy("policy-1", "Policy", "Description", "program-1",
                List.of(new com.ruleweaver.engine.FieldCondition(
                        "portfolioScore", FieldOperator.GREATER_THAN, 50)));
        assertThrows(IllegalArgumentException.class, () -> service.evaluateApplicants(
                "policy-1", 1, List.of(new Applicant("a-1", "Alice", 8, 70, List.of()))));

        programs.editFields("program-1", List.of(new FieldDefinition(
                "portfolioScore", "Portfolio score", FieldType.NUMBER, true,
                new FieldValidationRules(0.0, 100.0, null, null, null), List.of(),
                java.util.Set.of(FieldOperator.EQUALS))));
        Applicant completeApplicant = new Applicant("a-2", "Alice", 8, 70, List.of());
        completeApplicant.setCustomAttribute("portfolioScore", 90);
        assertThrows(IllegalArgumentException.class,
                () -> service.evaluateApplicants("policy-1", 1, List.of(completeApplicant)));
    }

    @Test
    void unlinkedLegacyPolicyStillEvaluatesWithoutProgramSchema() {
        InMemoryPolicyRepository policies = new InMemoryPolicyRepository();
        InMemoryEvaluationResultRepository evaluations = new InMemoryEvaluationResultRepository();
        PolicyService service = service(policies, evaluations);
        service.createPolicy("legacy", "Legacy", "No program", List.of());

        assertTrue(service.evaluateApplicants("legacy", 1,
                List.of(new Applicant("a-1", "Alice", 8, 70, List.of()))).get(0).isOverallDecision());
    }

    private static PolicyService service(InMemoryPolicyRepository policies,
                                         InMemoryEvaluationResultRepository evaluations) {
        DecisionEngine engine = new DecisionEngine();
        return new PolicyService(policies, evaluations, engine, new ComparisonService(engine));
    }
}
