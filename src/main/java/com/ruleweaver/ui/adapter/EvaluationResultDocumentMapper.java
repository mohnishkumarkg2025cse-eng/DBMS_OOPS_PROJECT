package com.ruleweaver.ui.adapter;

import com.ruleweaver.model.ConditionResult;
import com.ruleweaver.model.EvaluationResult;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EvaluationResultDocumentMapper {

    private EvaluationResultDocumentMapper() {
    }

    public static Document toDocument(EvaluationResult result) {
        Document document = new Document();
        if (result.getId() != null) {
            document.append("_id", result.getId());
        }
        document.append("applicantId", result.getApplicantId())
                .append("applicantName", result.getApplicantName())
                .append("programId", result.getProgramId())
                .append("programSchemaRevision", result.getProgramSchemaRevision())
                .append("policyId", result.getPolicyId())
                .append("policyName", result.getPolicyName())
                .append("policyVersionId", result.getPolicyVersionId())
                .append("evaluationRunId", result.getEvaluationRunId())
                .append("policyVersion", result.getPolicyVersionNumber())
                .append("overallDecision", result.getDecisionOutcome())
                .append("applicantInputSnapshot", new Document(result.getApplicantInputSnapshot()))
                .append("engineSemanticsVersion", result.getEngineSemanticsVersion())
                .append("evaluatedOn", result.getEvaluatedOn() == null
                        ? null : result.getEvaluatedOn().toString());

        List<Document> conditions = new ArrayList<>();
        for (ConditionResult condition : result.getConditionResults()) {
            conditions.add(new Document("conditionName", condition.getConditionName())
                    .append("conditionType", condition.getConditionType())
                    .append("passed", condition.isPassed())
                    .append("actualValue", condition.getActualValue())
                    .append("requiredValue", condition.getRequiredValue())
                    .append("explanation", condition.getExplanation()));
        }
        document.append("conditionResults", conditions);
        return document;
    }

    public static EvaluationResult fromDocument(Document document) {
        EvaluationResult result = new EvaluationResult();
        Object id = document.get("_id");
        result.setId(id == null ? null : id.toString());
        result.setApplicantId(document.getString("applicantId"));
        result.setApplicantName(document.getString("applicantName"));
        result.setProgramId(document.getString("programId"));
        result.setProgramSchemaRevision(document.getInteger("programSchemaRevision", 0));
        result.setPolicyId(document.getString("policyId"));
        result.setPolicyName(document.getString("policyName"));
        result.setPolicyVersionId(document.getString("policyVersionId"));
        result.setEvaluationRunId(document.getString("evaluationRunId"));
        result.setPolicyVersionNumber(document.getInteger("policyVersion", 0));
        Object rawDecision = document.get("overallDecision");
        Boolean overallDecision = rawDecision != null
                ? rawDecision instanceof Boolean decision ? decision : null
                : legacyDecision(document.get("decision") instanceof String decision ? decision : null);
        result.setOverallDecision(overallDecision);
        Object snapshot = document.get("applicantInputSnapshot");
        if (snapshot instanceof Map<?, ?> values) {
            Map<String, Object> typedSnapshot = new LinkedHashMap<>();
            values.forEach((key, value) -> {
                if (key instanceof String stringKey) typedSnapshot.put(stringKey, value);
            });
            result.setApplicantInputSnapshot(typedSnapshot);
        }
        result.setEngineSemanticsVersion(document.getString("engineSemanticsVersion"));
        result.setEvaluatedOn(evaluatedOn(document.get("evaluatedOn")));

        List<ConditionResult> results = new ArrayList<>();
        List<Document> conditionDocuments = document.getList("conditionResults", Document.class, List.of());
        for (Document condition : conditionDocuments) {
            ConditionResult mapped = new ConditionResult(
                    condition.getString("conditionName"),
                    condition.getString("conditionType"),
                    Boolean.TRUE.equals(condition.getBoolean("passed")),
                    condition.get("actualValue"),
                    condition.get("requiredValue"),
                    condition.getString("explanation"));
            results.add(mapped);
        }
        result.setConditionResults(results);
        return result;
    }

    private static Boolean legacyDecision(String decision) {
        if (decision == null) {
            return null;
        }
        return switch (decision.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "ELIGIBLE", "APPROVED", "PASS", "PASSED" -> true;
            case "INELIGIBLE", "NOT ELIGIBLE", "REJECTED", "FAIL", "FAILED" -> false;
            default -> null;
        };
    }

    private static LocalDateTime evaluatedOn(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        if (!(value instanceof String timestamp) || timestamp.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(OffsetDateTime.parse(timestamp).toInstant(),
                    ZoneId.systemDefault());
        } catch (DateTimeParseException offsetFailure) {
            try {
                return LocalDateTime.parse(timestamp);
            } catch (DateTimeParseException localDateTimeFailure) {
                try {
                    return LocalDateTime.ofInstant(Instant.parse(timestamp), ZoneId.systemDefault());
                } catch (DateTimeParseException ignored) {
                    return null;
                }
            }
        }
    }
}
