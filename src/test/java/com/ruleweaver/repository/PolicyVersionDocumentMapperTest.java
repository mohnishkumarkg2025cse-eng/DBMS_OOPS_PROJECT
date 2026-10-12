package com.ruleweaver.repository;

import com.ruleweaver.model.PolicyVersion;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PolicyVersionDocumentMapperTest {

    @Test
    void roundTripsProgramAndSchemaRevisionLinks() {
        PolicyVersion version = new PolicyVersion("policy-1_v2", "policy-1", 2, "Published", List.of());
        version.setProgramId("program-1");
        version.setProgramSchemaRevision(4);
        version.setCreatedAt(LocalDateTime.of(2026, 5, 4, 12, 30));

        PolicyVersion restored = PolicyVersionDocumentMapper.fromDocument(
                PolicyVersionDocumentMapper.toDocument(version));

        assertEquals("program-1", restored.getProgramId());
        assertEquals(4, restored.getProgramSchemaRevision());
        assertEquals(version.getCreatedAt(), restored.getCreatedAt());
    }

    @Test
    void readsLegacyVersionWithoutProgramOrSchemaRevision() {
        PolicyVersion restored = PolicyVersionDocumentMapper.fromDocument(
                new Document("_id", "policy-1_v1")
                        .append("policyId", "policy-1")
                        .append("version", 1)
                        .append("rules", List.of()));

        assertNull(restored.getProgramId());
        assertEquals(0, restored.getProgramSchemaRevision());
    }
}
