package com.ruleweaver.repository;

import com.ruleweaver.model.EvaluationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryEvaluationResultRepositoryTest {

    @Test
    void findsLegacyResultsByPolicyVersionId() {
        InMemoryEvaluationResultRepository repository = new InMemoryEvaluationResultRepository();
        EvaluationResult legacy = new EvaluationResult();
        legacy.setApplicantId("applicant-1");
        legacy.setPolicyVersionId("policy-1_v2");
        legacy.setOverallDecision(true);
        repository.save(legacy);

        assertEquals(1, repository.findByPolicyAndVersion("policy-1", 2).size());
        assertEquals(1, repository.countByPolicyAndVersion("policy-1", 2));
    }
}
