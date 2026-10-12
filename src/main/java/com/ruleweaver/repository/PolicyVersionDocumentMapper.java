package com.ruleweaver.repository;

import com.ruleweaver.engine.Condition;
import com.ruleweaver.engine.ConditionDocumentMapper;
import com.ruleweaver.model.PolicyVersion;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.List;

public final class PolicyVersionDocumentMapper {
    private PolicyVersionDocumentMapper() { }

    public static Document toDocument(PolicyVersion version) {
        List<Document> conditions = version.getConditions().stream()
                .map(ConditionDocumentMapper::toDocument)
                .toList();
        return new Document("_id", version.getId())
                .append("policyId", version.getPolicyId())
                .append("programId", version.getProgramId())
                .append("programSchemaRevision", version.getProgramSchemaRevision())
                .append("version", version.getVersionNumber())
                .append("description", version.getDescription())
                .append("createdAt", version.getCreatedAt() == null ? null : version.getCreatedAt().toString())
                .append("conditions", conditions);
    }

    public static PolicyVersion fromDocument(Document document) {
        List<Document> rawConditions = document.getList("conditions", Document.class);
        if (rawConditions == null) {
            rawConditions = document.getList("rules", Document.class, List.of());
        }
        List<Condition> conditions = rawConditions.stream()
                .map(ConditionDocumentMapper::fromDocument)
                .toList();
        Object id = document.get("_id");
        PolicyVersion version = new PolicyVersion(
                id == null ? null : id.toString(),
                document.getString("policyId"),
                document.getInteger("version", 0),
                document.getString("description"),
                conditions);
        version.setProgramId(document.getString("programId"));
        version.setProgramSchemaRevision(document.getInteger("programSchemaRevision", 0));
        String createdAt = document.getString("createdAt");
        version.setCreatedAt(createdAt == null ? null : LocalDateTime.parse(createdAt));
        return version;
    }
}
