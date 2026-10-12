package com.ruleweaver.repository;

import com.ruleweaver.model.Policy;
import com.ruleweaver.model.PolicyVersion;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository {
    Policy save(Policy policy);

    Optional<Policy> findById(String id);

    List<Policy> findAll();

    /** Archives a policy without removing its saved versions or evaluation history. */
    boolean deleteById(String id);

    Optional<PolicyVersion> findVersion(String policyId, int versionNumber);

    Optional<PolicyVersion> findLatestVersion(String policyId);

    void saveVersion(String policyId, PolicyVersion version);
}
