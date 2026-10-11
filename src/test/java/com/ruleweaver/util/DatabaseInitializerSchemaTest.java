package com.ruleweaver.util;

import com.ruleweaver.repository.EvaluationRepository;
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
        Document result = EvaluationResultDocumentMapper.toDocument(
                new EvaluationResult("applicant-1", "Applicant", "policy-1", "Policy", 1));
        Document schema = DatabaseInitializer.evaluationValidator().get("$jsonSchema", Document.class);
        List<String> required = schema.getList("required", String.class);

        assertTrue(required.contains("applicantId"));
        assertTrue(result.keySet().containsAll(required));
        assertEquals(List.of("evaluations"), List.of(EvaluationRepository.COLLECTION_NAME));
    }

    @Test
    void policyVersionValidatorRequiresStoredSnapshotShape() {
        Document schema = DatabaseInitializer.policyVersionValidator().get("$jsonSchema", Document.class);
        List<String> required = schema.getList("required", String.class);

        assertEquals(List.of("policyId", "version"), required);
        Document properties = schema.get("properties", Document.class);
        assertEquals("array", properties.get("conditions", Document.class).getString("bsonType"));
    }
}
