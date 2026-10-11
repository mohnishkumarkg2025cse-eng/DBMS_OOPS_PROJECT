package com.ruleweaver.service;

import com.ruleweaver.engine.DecisionEngine;
import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.engine.Operator;
import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.Policy;
import com.ruleweaver.model.PolicyVersion;
import com.ruleweaver.repository.InMemoryEvaluationResultRepository;
import com.ruleweaver.repository.InMemoryPolicyRepository;
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

        service.evaluateApplicants("policy-1", 1, applicants, progress::set, () -> false);
        service.evaluateApplicants("policy-1", 1, applicants, ignored -> { }, () -> false);

        assertEquals(2, progress.get());
        assertEquals(4, evaluations.findByPolicyAndVersion("policy-1", 1).size());
        assertEquals(4, evaluations.countByPolicyAndVersion("policy-1", 1));
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

    private static PolicyService service(InMemoryPolicyRepository policies,
                                         InMemoryEvaluationResultRepository evaluations) {
        DecisionEngine engine = new DecisionEngine();
        return new PolicyService(policies, evaluations, engine, new ComparisonService(engine));
    }
}
