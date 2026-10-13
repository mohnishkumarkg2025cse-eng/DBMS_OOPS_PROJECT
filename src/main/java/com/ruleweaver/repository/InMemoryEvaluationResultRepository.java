package com.ruleweaver.repository;

import com.ruleweaver.model.EvaluationResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryEvaluationResultRepository implements EvaluationResultRepository {
    private final Map<String, EvaluationResult> storage = new ConcurrentHashMap<>();

    @Override
    public EvaluationResult save(EvaluationResult result) {
        if (result == null) {
            throw new IllegalArgumentException("Evaluation result is required.");
        }
        if (result.getId() == null || result.getId().trim().isEmpty()) {
            result.setId(UUID.randomUUID().toString());
        }
        if (storage.putIfAbsent(result.getId(), result) != null) {
            throw new IllegalStateException("Evaluation result already exists: " + result.getId());
        }
        return result;
    }

    @Override
    public List<EvaluationResult> saveAll(List<EvaluationResult> results) {
        if (results == null) {
            return Collections.emptyList();
        }
        results.forEach(this::save);
        return results;
    }

    public Optional<EvaluationResult> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<EvaluationResult> findByApplicantId(String applicantId) {
        return storage.values().stream()
                .filter(r -> Objects.equals(r.getApplicantId(), applicantId))
                .collect(Collectors.toList());
    }

    @Override
    public List<EvaluationResult> findByPolicyAndVersion(String policyId, int versionNumber) {
        String policyVersionId = policyId + "_v" + versionNumber;
        return storage.values().stream()
                .filter(r -> (Objects.equals(r.getPolicyId(), policyId)
                        && r.getPolicyVersionNumber() == versionNumber)
                        || Objects.equals(r.getPolicyVersionId(), policyVersionId))
                .collect(Collectors.toList());
    }

    @Override
    public long countByPolicyAndVersion(String policyId, int versionNumber) {
        return findByPolicyAndVersion(policyId, versionNumber).size();
    }

    @Override
    public List<EvaluationResult> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public long countDecisions() {
        return storage.size();
    }
}
