package com.ruleweaver.ui.adapter;

import com.ruleweaver.model.ConditionResult;
import com.ruleweaver.model.EvaluationResult;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationResultDocumentMapperTest {

    @Test
    void persistsConditionTypeAndRequiredValue() {
        EvaluationResult result = new EvaluationResult("A-1", "Applicant", "P-1", "Policy", 2);
        result.addConditionResult(new ConditionResult(
                "GPA >= 8", "NumericCondition", false, 7.5, 8.0, "GPA below minimum"));

        Document document = EvaluationResultDocumentMapper.toDocument(result);
        Document condition = document.getList("conditionResults", Document.class).get(0);

        assertEquals("NumericCondition", condition.getString("conditionType"));
        assertEquals(8.0, condition.getDouble("requiredValue"));
        EvaluationResult restored = EvaluationResultDocumentMapper.fromDocument(document);
        assertEquals("NumericCondition", restored.getConditionResults().get(0).getConditionType());
        assertEquals(8.0, restored.getConditionResults().get(0).getRequiredValue());
    }

    @Test
    void readsLegacyResultsWithoutNewConditionFields() {
        Document legacyCondition = new Document("conditionName", "GPA >= 8")
                .append("passed", false)
                .append("actualValue", 7.5)
                .append("explanation", "GPA below minimum");
        Document legacy = new Document("applicantId", "A-1")
                .append("policyVersion", 1)
                .append("conditionResults", java.util.List.of(legacyCondition));

        EvaluationResult restored = EvaluationResultDocumentMapper.fromDocument(legacy);
        ConditionResult condition = restored.getConditionResults().get(0);
        assertNull(condition.getConditionType());
        assertNull(condition.getRequiredValue());
    }
}
