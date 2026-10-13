package com.ruleweaver.service;

import com.ruleweaver.engine.CompositeCondition;
import com.ruleweaver.engine.Condition;
import com.ruleweaver.engine.FieldCondition;
import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.engine.SkillCondition;
import com.ruleweaver.model.FieldDefinition;
import com.ruleweaver.model.FieldOperator;
import com.ruleweaver.model.FieldType;
import com.ruleweaver.model.ProgramConfiguration;
import com.ruleweaver.model.ProgramTemplate;
import com.ruleweaver.repository.ProgramRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProgramService {
    private final ProgramRepository repository;

    public ProgramService(ProgramRepository repository) {
        this.repository = java.util.Objects.requireNonNull(repository, "Program repository is required.");
    }

    public List<ProgramConfiguration> listPrograms() {
        return repository.findAll();
    }

    public List<ProgramConfiguration> listActivePrograms() {
        return repository.findAll().stream().filter(program -> !program.isArchived()).toList();
    }

    public long countPrograms() {
        return repository.countPrograms();
    }

    public long countActivePrograms() {
        return repository.countActivePrograms();
    }

    public ProgramConfiguration findProgram(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Program not found: " + id));
    }

    public ProgramConfiguration selectProgram(String id) {
        ProgramConfiguration program = findProgram(id);
        if (program.isArchived()) throw new IllegalStateException("Archived programs cannot be selected.");
        return program;
    }

    public ProgramConfiguration createProgram(String id, String name, ProgramTemplate template,
                                              List<FieldDefinition> fields) {
        ProgramConfiguration candidate = new ProgramConfiguration(id, name, template, fields);
        if (repository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Program already exists: " + id);
        }
        return repository.save(candidate);
    }

    public ProgramConfiguration createFromTemplate(ProgramTemplate template, String name) {
        if (template == null) throw new IllegalArgumentException("Program template is required.");
        String id = template.name().toLowerCase(java.util.Locale.ROOT) + "-" + UUID.randomUUID();
        ProgramConfiguration source = repository.findById(
                "template-" + template.name().toLowerCase(java.util.Locale.ROOT))
                .orElseGet(() -> new ProgramConfiguration("template",
                        template.getDisplayName(), template, template.defaultFields()));
        if (source.isArchived()) {
            throw new IllegalStateException("This template is archived and cannot be used to create a program.");
        }
        return createProgram(id, name, template, source.getFields());
    }

    public void ensureTemplates() {
        for (ProgramTemplate template : ProgramTemplate.values()) {
            String id = "template-" + template.name().toLowerCase(java.util.Locale.ROOT);
            if (repository.findById(id).isEmpty()) {
                repository.save(new ProgramConfiguration(id, template.getDisplayName(), template,
                        template.defaultFields()));
            }
        }
    }

    public ProgramConfiguration rename(String id, String name) {
        ProgramConfiguration program = editable(id);
        program.rename(name);
        return repository.save(program);
    }

    public ProgramConfiguration editFields(String id, List<FieldDefinition> fields) {
        return editFields(id, fields, null);
    }

    public ProgramConfiguration editFields(String id, List<FieldDefinition> fields, ProgramMigration migration) {
        ProgramConfiguration previous = editable(id);
        ProgramConfiguration replacement = previous.copy();
        replacement.setFields(fields);
        if (hasBreakingFieldChanges(previous, replacement)) {
            if (migration == null) {
                throw new IllegalStateException("Removing fields or changing field types requires an explicit data and policy migration.");
            }
            migration.migrate(previous.copy(), replacement.copy());
        }
        return repository.save(replacement);
    }

    public ProgramConfiguration duplicate(String id, String newName) {
        ProgramConfiguration source = editable(id);
        String newId = source.getTemplate().name().toLowerCase(java.util.Locale.ROOT)
                + "-" + UUID.randomUUID();
        return createProgram(newId, newName, source.getTemplate(), source.getFields());
    }

    public ProgramConfiguration archive(String id) {
        ProgramConfiguration program = editable(id);
        program.setArchived(true);
        return repository.save(program);
    }

    public void validateConditions(String programId, List<Condition> conditions) {
        if (conditions == null) return;
        ProgramConfiguration program = selectProgram(programId);
        for (Condition condition : conditions) {
            validateCondition(program, condition);
        }
    }

    public List<String> validateApplicant(String programId, com.ruleweaver.model.Applicant applicant) {
        return selectProgram(programId).getFieldRegistry().validateApplicant(applicant);
    }

    private void validateCondition(ProgramConfiguration program, Condition condition) {
        if (condition == null) throw new IllegalArgumentException("Policy condition must not be null.");
        if (condition instanceof CompositeCondition composite) {
            composite.getSubconditions().forEach(child -> validateCondition(program, child));
            return;
        }
        String key;
        FieldOperator operator;
        Object value;
        if (condition instanceof NumericCondition numeric) {
            key = normalizeNumericAlias(numeric.getFieldName());
            operator = FieldOperator.valueOf(numeric.getOperator().name());
            value = numeric.getThreshold();
        } else if (condition instanceof SkillCondition skill) {
            key = "skills";
            operator = FieldOperator.CONTAINS;
            value = skill.getRequiredSkill();
        } else if (condition instanceof FieldCondition field) {
            key = field.getFieldKey();
            operator = field.getOperator();
            value = field.getExpectedValue();
        } else {
            throw new IllegalArgumentException("Unsupported condition type for program validation: "
                    + condition.getClass().getSimpleName());
        }
        FieldDefinition definition = program.getFieldRegistry().require(key);
        FieldType expectedType = condition instanceof NumericCondition ? FieldType.NUMBER
                : condition instanceof SkillCondition ? FieldType.MULTI_SELECT : definition.getType();
        if (definition.getType() != expectedType) {
            throw new IllegalArgumentException("Field '" + definition.getLabel() + "' is " + definition.getType()
                    + " but the condition requires " + expectedType + ".");
        }
        List<String> errors = definition.validateConditionValue(operator, value);
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join(" ", errors));
    }

    private ProgramConfiguration editable(String id) {
        ProgramConfiguration program = findProgram(id);
        if (program.isArchived()) throw new IllegalStateException("Archived programs cannot be edited.");
        return program;
    }

    private static boolean hasBreakingFieldChanges(ProgramConfiguration previous, ProgramConfiguration replacement) {
        Map<String, FieldDefinition> nextFields = new HashMap<>();
        replacement.getFields().forEach(field -> nextFields.put(field.getKey(), field));
        for (FieldDefinition oldField : previous.getFields()) {
            FieldDefinition next = nextFields.get(oldField.getKey());
            if (next == null || oldField.getType() != next.getType()) return true;
        }
        return false;
    }

    private static String normalizeNumericAlias(String fieldName) {
        return switch (fieldName) {
            case "interview_score", "interview" -> "interviewScore";
            default -> fieldName;
        };
    }
}
