package com.ruleweaver.model;

import java.util.List;

public final class ProgramConfiguration {
    private final String id;
    private String name;
    private final ProgramTemplate template;
    private List<FieldDefinition> fields;
    private int schemaRevision;
    private boolean archived;

    public ProgramConfiguration(String id, String name, ProgramTemplate template, List<FieldDefinition> fields) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Program ID is required.");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Program name is required.");
        if (template == null) throw new IllegalArgumentException("Program template is required.");
        this.id = id;
        this.name = name.trim();
        this.template = template;
        setFields(fields);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public ProgramTemplate getTemplate() { return template; }
    public List<FieldDefinition> getFields() { return fields; }
    public int getSchemaRevision() { return schemaRevision; }
    public boolean isArchived() { return archived; }
    public FieldRegistry getFieldRegistry() { return new FieldRegistry(fields); }

    public void rename(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Program name is required.");
        this.name = name.trim();
    }

    public void setFields(List<FieldDefinition> fields) {
        List<FieldDefinition> replacement = fields == null ? List.of() : List.copyOf(fields);
        new FieldRegistry(replacement);
        this.fields = replacement;
    }

    public void setSchemaRevision(int schemaRevision) {
        if (schemaRevision < 0) throw new IllegalArgumentException("Schema revision cannot be negative.");
        this.schemaRevision = schemaRevision;
    }

    public void setArchived(boolean archived) { this.archived = archived; }

    public ProgramConfiguration copy() {
        ProgramConfiguration copy = new ProgramConfiguration(id, name, template, fields);
        copy.setSchemaRevision(schemaRevision);
        copy.setArchived(archived);
        return copy;
    }
}
