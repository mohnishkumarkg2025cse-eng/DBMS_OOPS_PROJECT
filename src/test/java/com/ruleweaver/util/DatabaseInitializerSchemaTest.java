package com.ruleweaver.util;

import com.ruleweaver.repository.EvaluationRepository;
import com.ruleweaver.repository.PolicyVersionRepository;
import com.ruleweaver.repository.MongoProgramRepository;
import com.ruleweaver.ui.adapter.EvaluationResultDocumentMapper;
import com.ruleweaver.model.EvaluationResult;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseInitializerSchemaTest {

    @Test
    void evaluationCollectionAndValidatorMatchMapperFields() {
        assertEquals("evaluations", EvaluationRepository.COLLECTION_NAME);
        assertEquals("policy_versions", PolicyVersionRepository.COLLECTION_NAME);
        Document result = EvaluationResultDocumentMapper.toDocument(
                new EvaluationResult("applicant-1", "Applicant", "policy-1", "Policy", 1));
        Document schema = DatabaseInitializer.evaluationValidator().get("$jsonSchema", Document.class);
        List<String> required = schema.getList("required", String.class);

        assertTrue(required.contains("applicantId"));
        assertTrue(result.keySet().containsAll(required));
        assertEquals(List.of("evaluations"), List.of(EvaluationRepository.COLLECTION_NAME));
        Document properties = schema.get("properties", Document.class);
        assertEquals(List.of("string", "null"),
                properties.get("policyVersionId", Document.class).getList("bsonType", String.class));
        assertEquals(List.of("string", "null"),
                properties.get("evaluationRunId", Document.class).getList("bsonType", String.class));
        assertEquals(List.of("bool", "null"),
                properties.get("overallDecision", Document.class).getList("bsonType", String.class));
        assertNull(result.get("overallDecision"));
    }

    @Test
    void policyVersionValidatorRequiresStoredSnapshotShape() {
        Document schema = DatabaseInitializer.policyVersionValidator().get("$jsonSchema", Document.class);
        List<String> required = schema.getList("required", String.class);

        assertEquals(List.of("policyId", "version"), required);
        Document properties = schema.get("properties", Document.class);
        assertEquals("array", properties.get("conditions", Document.class).getString("bsonType"));
    }

    @Test
    void programSchemaAllowsTypedFieldConfigurationCollection() {
        assertEquals("programs", MongoProgramRepository.COLLECTION_NAME);
        Document schema = DatabaseInitializer.programValidator().get("$jsonSchema", Document.class);
        assertEquals(List.of("_id", "name", "template", "fields"),
                schema.getList("required", String.class));
        assertEquals("array", schema.get("properties", Document.class)
                .get("fields", Document.class).getString("bsonType"));
    }
}
