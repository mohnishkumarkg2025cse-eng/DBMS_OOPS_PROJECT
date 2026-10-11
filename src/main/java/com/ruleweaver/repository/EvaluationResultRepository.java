package com.ruleweaver.repository;

import com.ruleweaver.model.EvaluationResult;

import java.util.List;

public interface EvaluationResultRepository {
    EvaluationResult save(EvaluationResult result);

    List<EvaluationResult> saveAll(List<EvaluationResult> results);

    List<EvaluationResult> findByPolicyAndVersion(String policyId, int versionNumber);

    List<EvaluationResult> findAll();

    long countByPolicyAndVersion(String policyId, int versionNumber);
}
