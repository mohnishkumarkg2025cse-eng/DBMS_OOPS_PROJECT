package com.ruleweaver.ui.adapter;

import com.ruleweaver.model.ConditionResult;
import com.ruleweaver.model.EvaluationResult;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

class EvaluationResultDocumentMapperTest {

    @Test
    void persistsConditionTypeAndRequiredValue() {
        EvaluationResult result = new EvaluationResult("A-1", "Applicant", "P-1", "Policy", 2);
        result.setPolicyVersionId("P-1_v2");
        result.setEvaluationRunId("run-abc");
        result.setProgramId("program-1");
        result.setProgramSchemaRevision(3);
        result.setApplicantInputSnapshot(Map.of("gpa", 7.5, "country", "GB"));
        result.setEngineSemanticsVersion("1");
        result.setEvaluatedOn(LocalDateTime.of(2026, 10, 12, 9, 0));
        result.setOverallDecision(false);
        result.addConditionResult(new ConditionResult(
                "GPA >= 8", "NumericCondition", false, 7.5, 8.0, "GPA below minimum"));

        Document document = EvaluationResultDocumentMapper.toDocument(result);
        Document condition = document.getList("conditionResults", Document.class).get(0);

        assertEquals("NumericCondition", condition.getString("conditionType"));
        assertEquals(8.0, condition.getDouble("requiredValue"));
        assertEquals("P-1_v2", document.getString("policyVersionId"));
        assertEquals("run-abc", document.getString("evaluationRunId"));
        assertEquals("P-1", document.getString("policyId"));
        assertEquals("program-1", document.getString("programId"));
        assertEquals(3, document.getInteger("programSchemaRevision"));
        assertEquals("GB", document.get("applicantInputSnapshot", Document.class).getString("country"));
        assertEquals("1", document.getString("engineSemanticsVersion"));
        assertEquals(2, document.getInteger("policyVersion"));
        assertEquals(false, document.getBoolean("overallDecision"));
        assertEquals(result.getEvaluatedOn().toString(), document.getString("evaluatedOn"));
        EvaluationResult restored = EvaluationResultDocumentMapper.fromDocument(document);
        assertEquals("NumericCondition", restored.getConditionResults().get(0).getConditionType());
        assertEquals(8.0, restored.getConditionResults().get(0).getRequiredValue());
        assertEquals("P-1_v2", restored.getPolicyVersionId());
        assertEquals("run-abc", restored.getEvaluationRunId());
        assertEquals("program-1", restored.getProgramId());
        assertEquals(3, restored.getProgramSchemaRevision());
        assertEquals(Map.of("gpa", 7.5, "country", "GB"), restored.getApplicantInputSnapshot());
        assertEquals("1", restored.getEngineSemanticsVersion());
        assertEquals(result.getEvaluatedOn(), restored.getEvaluatedOn());
    }

    @Test
    void readsLegacyResultsWithoutNewConditionFields() {
        Document legacyCondition = new Document("conditionName", "GPA >= 8")
                .append("passed", false)
                .append("actualValue", 7.5)
                .append("explanation", "GPA below minimum");
        Document legacy = new Document("applicantId", "A-1")
                .append("policyVersion", 1)
                .append("decision", "ELIGIBLE")
                .append("conditionResults", java.util.List.of(legacyCondition));

        EvaluationResult restored = EvaluationResultDocumentMapper.fromDocument(legacy);
        ConditionResult condition = restored.getConditionResults().get(0);
        assertNull(condition.getConditionType());
        assertNull(condition.getRequiredValue());
        assertNull(restored.getPolicyVersionId());
        assertNull(restored.getEvaluationRunId());
        assertNull(restored.getProgramId());
        assertEquals(0, restored.getProgramSchemaRevision());
        assertTrue(restored.getApplicantInputSnapshot().isEmpty());
        assertNull(restored.getEngineSemanticsVersion());
        assertTrue(restored.isOverallDecision());
        assertTrue(restored.hasDecisionOutcome());
    }

    @Test
    void preservesUnknownDecisionAndInvalidOrMissingTimestampAsUnavailable() {
        EvaluationResult pending = EvaluationResultDocumentMapper.fromDocument(
                new Document("decision", "PENDING").append("evaluatedOn", "not-a-date"));

        assertFalse(pending.hasDecisionOutcome());
        assertNull(pending.getDecisionOutcome());
        assertNull(pending.getEvaluatedOn());
        assertNull(EvaluationResultDocumentMapper.toDocument(pending).get("overallDecision"));

        EvaluationResult missing = EvaluationResultDocumentMapper.fromDocument(
                new Document("decision", "UNKNOWN"));
        assertFalse(missing.hasDecisionOutcome());
        assertNull(missing.getEvaluatedOn());

        EvaluationResult conflicting = EvaluationResultDocumentMapper.fromDocument(
                new Document("overallDecision", "PENDING").append("decision", "ELIGIBLE"));
        assertFalse(conflicting.hasDecisionOutcome());
    }

    @Test
    void acceptsDateAndOffsetTimestampsUsedByPersistedRecords() {
        Instant instant = Instant.parse("2026-10-12T10:15:30Z");
        EvaluationResult bsonDate = EvaluationResultDocumentMapper.fromDocument(
                new Document("overallDecision", true).append("evaluatedOn", Date.from(instant)));
        EvaluationResult offsetDate = EvaluationResultDocumentMapper.fromDocument(
                new Document("overallDecision", false)
                        .append("evaluatedOn", "2026-10-12T12:15:30+02:00"));

        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        assertEquals(expected, bsonDate.getEvaluatedOn());
        assertEquals(expected, offsetDate.getEvaluatedOn());
    }
}
