package com.ruleweaver.repository;

import com.ruleweaver.engine.ConditionDocumentMapper;
import com.ruleweaver.model.Policy;
import com.ruleweaver.model.PolicyVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPolicyRepository implements PolicyRepository {
    private final Map<String, Policy> storage = new ConcurrentHashMap<>();

    @Override
    public Policy save(Policy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Policy is required.");
        }
        if (policy.getId() == null || policy.getId().isBlank()) {
            policy.setId(UUID.randomUUID().toString());
        }
        Policy existing = storage.get(policy.getId());
        Policy stored = copyPolicy(policy);
        if (existing != null) {
            stored.setVersions(copyVersions(existing.getVersions()));
        }
        storage.put(stored.getId(), stored);
        return copyPolicy(stored);
    }

    @Override
    public Optional<Policy> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id)).map(InMemoryPolicyRepository::copyPolicy);
    }

    @Override
    public List<Policy> findAll() {
        return storage.values().stream().map(InMemoryPolicyRepository::copyPolicy).toList();
    }

    @Override
    public long countActivePolicies() {
        return storage.values().stream()
                .filter(policy -> !policy.isArchived())
                .filter(policy -> !policy.getVersions().isEmpty())
                .count();
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        Policy policy = storage.get(id);
        if (policy == null) {
            return false;
        }
        Policy archived = copyPolicy(policy);
        archived.setArchived(true);
        storage.put(id, archived);
        return true;
    }

    @Override
    public Optional<PolicyVersion> findVersion(String policyId, int versionNumber) {
        if (policyId == null || policyId.isBlank()) {
            return Optional.empty();
        }

        Policy policy = storage.get(policyId);
        if (policy == null) {
            return Optional.empty();
        }
        return policy.getVersion(versionNumber).map(InMemoryPolicyRepository::copyVersion);
    }

    @Override
    public Optional<PolicyVersion> findLatestVersion(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            return Optional.empty();
        }

        Policy policy = storage.get(policyId);
        if (policy == null) {
            return Optional.empty();
        }
        return policy.getLatestVersion().map(InMemoryPolicyRepository::copyVersion);
    }

    @Override
    public void saveVersion(String policyId, PolicyVersion version) {
        if (policyId == null || policyId.isBlank() || version == null) {
            throw new IllegalArgumentException("Policy ID and policy version are required.");
        }

        Policy policy = storage.get(policyId);
        if (policy == null) {
            throw new IllegalArgumentException("Policy not found: " + policyId);
        }

        if (version.getVersionNumber() <= 0) {
            throw new IllegalArgumentException("Version number must be greater than zero.");
        }
        if (policy.getVersion(version.getVersionNumber()).isPresent()) {
            throw new IllegalStateException("Policy version " + version.getVersionNumber()
                    + " already exists for policy " + policyId + "; versions are immutable.");
        }

        version.setPolicyId(policyId);
        List<PolicyVersion> versions = new ArrayList<>(policy.getVersions());
        versions.add(copyVersion(version));
        policy.setVersions(versions);
    }

    private static Policy copyPolicy(Policy source) {
        Policy copy = new Policy(source.getId(), source.getName(), source.getDescription());
        copy.setCategory(source.getCategory());
        copy.setProgramId(source.getProgramId());
        copy.setActiveVersionNumber(source.getPersistedActiveVersionNumber());
        copy.setArchived(source.isArchived());
        copy.setVersions(copyVersions(source.getVersions()));
        return copy;
    }

    private static List<PolicyVersion> copyVersions(List<PolicyVersion> versions) {
        return versions.stream().map(InMemoryPolicyRepository::copyVersion).toList();
    }

    private static PolicyVersion copyVersion(PolicyVersion version) {
        PolicyVersion copy = new PolicyVersion(
                version.getId(),
                version.getPolicyId(),
                version.getVersionNumber(),
                version.getDescription(),
                version.getConditions().stream()
                        .map(ConditionDocumentMapper::toDocument)
                        .map(ConditionDocumentMapper::fromDocument)
                        .toList());
        copy.setCreatedAt(version.getCreatedAt());
        return copy;
    }
}
