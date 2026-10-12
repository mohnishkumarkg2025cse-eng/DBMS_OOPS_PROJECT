package com.ruleweaver.util;

import com.ruleweaver.model.Policy;
import com.ruleweaver.repository.InMemoryPolicyRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SeedDataInitializationTest {

    @Test
    void samplePolicyInitializationCanBeRepeatedWithoutDuplicatingVersion() {
        InMemoryPolicyRepository repository = new InMemoryPolicyRepository();

        SeedData.ensureSamplePolicy(repository);
        SeedData.ensureSamplePolicy(repository);

        Policy policy = repository.findById("POL-INTERN").orElseThrow();
        assertEquals(1, policy.getVersions().size());
        assertEquals(1, repository.findVersion("POL-INTERN", 1).orElseThrow().getVersionNumber());
        assertEquals(1, policy.getActiveVersionNumber());
        assertFalse(policy.isArchived());
    }
}
