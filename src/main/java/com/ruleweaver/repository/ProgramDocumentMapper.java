package com.ruleweaver.repository;

import com.ruleweaver.model.FieldDefinition;
import com.ruleweaver.model.FieldOperator;
import com.ruleweaver.model.FieldType;
import com.ruleweaver.model.FieldValidationRules;
import com.ruleweaver.model.ProgramConfiguration;
import com.ruleweaver.model.ProgramTemplate;
import org.bson.Document;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public final class ProgramDocumentMapper {
    private ProgramDocumentMapper() { }

    public static Document toDocument(ProgramConfiguration program) {
        List<Document> fields = program.getFields().stream().map(ProgramDocumentMapper::toFieldDocument).toList();
        return new Document("_id", program.getId())
                .append("name", program.getName())
                .append("template", program.getTemplate().name())
                .append("archived", program.isArchived())
                .append("schemaRevision", program.getSchemaRevision())
                .append("fields", fields);
    }

    public static ProgramConfiguration fromDocument(Document document) {
        List<Document> rawFields = document.getList("fields", Document.class, List.of());
        List<FieldDefinition> fields = rawFields.stream().map(ProgramDocumentMapper::fromFieldDocument).toList();
        Object rawId = document.get("_id");
        ProgramConfiguration program = new ProgramConfiguration(
                rawId == null ? null : rawId.toString(),
                document.getString("name"),
                ProgramTemplate.valueOf(document.getString("template")),
                fields);
        program.setSchemaRevision(document.getInteger("schemaRevision", 0));
        program.setArchived(Boolean.TRUE.equals(document.getBoolean("archived")));
        return program;
    }

    public static Document toFieldDocument(FieldDefinition field) {
        FieldValidationRules rules = field.getValidationRules();
        Document validation = new Document("minimum", rules.minimum())
                .append("maximum", rules.maximum())
                .append("minimumLength", rules.minimumLength())
                .append("maximumLength", rules.maximumLength())
                .append("pattern", rules.pattern());
        return new Document("key", field.getKey())
                .append("label", field.getLabel())
                .append("type", field.getType().name())
                .append("required", field.isRequired())
                .append("validationRules", validation)
                .append("permittedValues", field.getPermittedValues())
                .append("supportedOperators", field.getSupportedOperators().stream().map(Enum::name).toList());
    }

    public static FieldDefinition fromFieldDocument(Document raw) {
        Document rules = raw.get("validationRules", Document.class);
        if (rules == null) rules = new Document();
        return new FieldDefinition(
                raw.getString("key"),
                raw.getString("label"),
                FieldType.valueOf(raw.getString("type")),
                Boolean.TRUE.equals(raw.getBoolean("required")),
                new FieldValidationRules(number(rules.get("minimum")), number(rules.get("maximum")),
                        integer(rules.get("minimumLength")), integer(rules.get("maximumLength")),
                        rules.getString("pattern")),
                raw.getList("permittedValues", String.class, List.of()),
                operators(raw.getList("supportedOperators", String.class, List.of())));
    }

    private static Double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private static Integer integer(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static EnumSet<FieldOperator> operators(List<String> names) {
        if (names == null || names.isEmpty()) return null;
        EnumSet<FieldOperator> operators = EnumSet.noneOf(FieldOperator.class);
        names.forEach(name -> operators.add(FieldOperator.valueOf(name)));
        return operators;
    }
}
