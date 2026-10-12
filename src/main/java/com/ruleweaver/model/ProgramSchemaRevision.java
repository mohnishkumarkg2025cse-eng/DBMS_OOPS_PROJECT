package com.ruleweaver.model;

import java.time.LocalDateTime;
import java.util.List;

public final class ProgramSchemaRevision {
    private final String id;
    private final String programId;
    private final int revision;
    private final ProgramTemplate template;
    private final List<FieldDefinition> fields;
    private final LocalDateTime createdAt;

    public ProgramSchemaRevision(String id, String programId, int revision, ProgramTemplate template,
                                 List<FieldDefinition> fields, LocalDateTime createdAt) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Revision ID is required.");
        if (programId == null || programId.isBlank()) throw new IllegalArgumentException("Program ID is required.");
        if (revision <= 0) throw new IllegalArgumentException("Schema revision must be greater than zero.");
        if (template == null) throw new IllegalArgumentException("Program template is required.");
        if (createdAt == null) throw new IllegalArgumentException("Revision creation time is required.");
        this.id = id;
        this.programId = programId;
        this.revision = revision;
        this.template = template;
        this.fields = fields == null ? List.of() : List.copyOf(fields);
        new FieldRegistry(this.fields);
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getProgramId() { return programId; }
    public int getRevision() { return revision; }
    public ProgramTemplate getTemplate() { return template; }
    public List<FieldDefinition> getFields() { return fields; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
