package com.ruleweaver.repository;

import com.ruleweaver.model.FieldDefinition;
import com.ruleweaver.model.ProgramSchemaRevision;
import com.ruleweaver.model.ProgramTemplate;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.List;

public final class ProgramSchemaRevisionDocumentMapper {
    private ProgramSchemaRevisionDocumentMapper() { }

    public static Document toDocument(ProgramSchemaRevision revision) {
        List<Document> fields = revision.getFields().stream()
                .map(ProgramDocumentMapper::toFieldDocument)
                .toList();
        return new Document("_id", revision.getId())
                .append("programId", revision.getProgramId())
                .append("revision", revision.getRevision())
                .append("template", revision.getTemplate().name())
                .append("fields", fields)
                .append("createdAt", revision.getCreatedAt().toString());
    }

    public static ProgramSchemaRevision fromDocument(Document document) {
        List<FieldDefinition> fields = document.getList("fields", Document.class, List.of()).stream()
                .map(ProgramDocumentMapper::fromFieldDocument)
                .toList();
        Object id = document.get("_id");
        return new ProgramSchemaRevision(
                id == null ? null : id.toString(),
                document.getString("programId"),
                document.getInteger("revision", 0),
                ProgramTemplate.valueOf(document.getString("template")),
                fields,
                LocalDateTime.parse(document.getString("createdAt")));
    }
}
