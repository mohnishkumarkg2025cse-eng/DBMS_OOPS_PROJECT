package com.ruleweaver.ui.adapter;

import com.ruleweaver.model.ConditionResult;
import com.ruleweaver.model.EvaluationResult;
import org.bson.Document;

import java.time.LocalDateTime;
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
                .append("overallDecision", result.isOverallDecision())
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
        Boolean overallDecision = document.getBoolean("overallDecision");
        if (overallDecision == null) {
            overallDecision = legacyDecision(document.getString("decision"));
        }
        result.setOverallDecision(Boolean.TRUE.equals(overallDecision));
        Object snapshot = document.get("applicantInputSnapshot");
        if (snapshot instanceof Map<?, ?> values) {
            Map<String, Object> typedSnapshot = new LinkedHashMap<>();
            values.forEach((key, value) -> {
                if (key instanceof String stringKey) typedSnapshot.put(stringKey, value);
            });
            result.setApplicantInputSnapshot(typedSnapshot);
        }
        result.setEngineSemanticsVersion(document.getString("engineSemanticsVersion"));
        String evaluatedOn = document.getString("evaluatedOn");
        result.setEvaluatedOn(evaluatedOn == null ? null : LocalDateTime.parse(evaluatedOn));

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
}
