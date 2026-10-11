package com.ruleweaver.repository;

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
        storage.put(policy.getId(), policy);
        return policy;
    }

    @Override
    public Optional<Policy> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Policy> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return storage.remove(id) != null;
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
        return policy.getVersion(versionNumber);
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
        return policy.getLatestVersion();
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
        versions.add(version);
        policy.setVersions(versions);
    }
}
