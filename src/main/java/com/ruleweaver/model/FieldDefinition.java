package com.ruleweaver.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class FieldDefinition {
    private final String key;
    private final String label;
    private final FieldType type;
    private final boolean required;
    private final FieldValidationRules validationRules;
    private final List<String> permittedValues;
    private final Set<FieldOperator> supportedOperators;

    public FieldDefinition(String key, String label, FieldType type, boolean required,
                           FieldValidationRules validationRules, List<String> permittedValues,
                           Set<FieldOperator> supportedOperators) {
        if (key == null || !key.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Field key must start with a letter and contain only letters, numbers, or underscores.");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Field label is required.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Field type is required.");
        }
        this.key = key;
        this.label = label.trim();
        this.type = type;
        this.required = required;
        this.validationRules = validationRules == null ? FieldValidationRules.none() : validationRules;
        this.permittedValues = permittedValues == null
                ? List.of() : Collections.unmodifiableList(new ArrayList<>(permittedValues));
        EnumSet<FieldOperator> operators = supportedOperators == null || supportedOperators.isEmpty()
                ? defaultOperators(type) : EnumSet.copyOf(supportedOperators);
        validateOperators(type, operators);
        this.supportedOperators = Collections.unmodifiableSet(operators);
        if ((type == FieldType.NUMBER || type == FieldType.BOOLEAN) && !this.permittedValues.isEmpty()) {
            throw new IllegalArgumentException("Permitted values are supported only for text and multi-select fields.");
        }
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }
    public FieldType getType() { return type; }
    public boolean isRequired() { return required; }
    public FieldValidationRules getValidationRules() { return validationRules; }
    public List<String> getPermittedValues() { return permittedValues; }
    public Set<FieldOperator> getSupportedOperators() { return supportedOperators; }

    public List<String> validateValue(Object value) {
        List<String> errors = new ArrayList<>();
        if (value == null) {
            if (required) errors.add(label + " is required.");
            return errors;
        }
        if (required && (value instanceof String text && text.isBlank()
                || value instanceof List<?> values && values.isEmpty())) {
            errors.add(label + " is required.");
            return errors;
        }
        switch (type) {
            case NUMBER -> validateNumber(value, errors);
            case TEXT -> validateText(value, errors);
            case BOOLEAN -> {
                if (!(value instanceof Boolean)) errors.add(label + " must be true or false.");
            }
            case MULTI_SELECT -> validateMultiSelect(value, errors);
        }
        return List.copyOf(errors);
    }

    public List<String> validateConditionValue(FieldOperator operator, Object value) {
        if (value == null) return List.of("A rule value is required for " + label + ".");
        if (operator == null) return List.of("A rule operator is required for " + label + ".");
        if (!supportedOperators.contains(operator)) {
            return List.of("Operator " + operator.getSymbol() + " is not supported for " + label + ".");
        }
        if (type == FieldType.MULTI_SELECT) {
            List<?> candidates = operator == FieldOperator.CONTAINS_ANY || operator == FieldOperator.CONTAINS_ALL
                    ? value instanceof List<?> values ? values : java.util.Collections.singletonList(value)
                    : List.of(value);
            if (candidates.isEmpty()) return List.of("At least one rule value is required for " + label + ".");
            for (Object candidate : candidates) {
                if (!(candidate instanceof String text)) {
                    return List.of("Rule value for " + label + " must be text.");
                }
                if (!permittedValues.isEmpty() && !permittedValues.contains(text)) {
                    return List.of("Rule value '" + text + "' is not permitted for " + label + ".");
                }
            }
            return List.of();
        }
        if (required && value instanceof String text && text.isBlank()) {
            return List.of(label + " rule value must not be blank.");
        }
        return validateValue(value);
    }

    private void validateNumber(Object value, List<String> errors) {
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) {
            errors.add(label + " must be a finite number.");
            return;
        }
        if (validationRules.minimum() != null && number.doubleValue() < validationRules.minimum()) {
            errors.add(label + " must be at least " + validationRules.minimum() + ".");
        }
        if (validationRules.maximum() != null && number.doubleValue() > validationRules.maximum()) {
            errors.add(label + " must be at most " + validationRules.maximum() + ".");
        }
    }

    private void validateText(Object value, List<String> errors) {
        if (!(value instanceof String text)) {
            errors.add(label + " must be text.");
            return;
        }
        if (validationRules.minimumLength() != null && text.length() < validationRules.minimumLength()) {
            errors.add(label + " must contain at least " + validationRules.minimumLength() + " characters.");
        }
        if (validationRules.maximumLength() != null && text.length() > validationRules.maximumLength()) {
            errors.add(label + " must contain at most " + validationRules.maximumLength() + " characters.");
        }
        if (validationRules.pattern() != null && !Pattern.matches(validationRules.pattern(), text)) {
            errors.add(label + " does not match its required format.");
        }
        if (!permittedValues.isEmpty() && !permittedValues.contains(text)) {
            errors.add(label + " must be one of: " + String.join(", ", permittedValues) + ".");
        }
    }

    private void validateMultiSelect(Object value, List<String> errors) {
        if (!(value instanceof List<?> values)) {
            errors.add(label + " must be a list of permitted values.");
            return;
        }
        Set<String> permitted = new LinkedHashSet<>(permittedValues);
        for (Object item : values) {
            if (!(item instanceof String text) || !permitted.isEmpty() && !permitted.contains(text)) {
                errors.add(label + " contains an invalid value: " + item + ".");
            }
        }
    }

    private static EnumSet<FieldOperator> defaultOperators(FieldType type) {
        return switch (type) {
            case NUMBER -> EnumSet.of(FieldOperator.EQUALS, FieldOperator.NOT_EQUALS,
                    FieldOperator.GREATER_THAN, FieldOperator.GREATER_THAN_OR_EQUAL,
                    FieldOperator.LESS_THAN, FieldOperator.LESS_THAN_OR_EQUAL);
            case TEXT, BOOLEAN -> EnumSet.of(FieldOperator.EQUALS, FieldOperator.NOT_EQUALS);
            case MULTI_SELECT -> EnumSet.of(FieldOperator.CONTAINS, FieldOperator.NOT_CONTAINS,
                    FieldOperator.CONTAINS_ANY, FieldOperator.CONTAINS_ALL);
        };
    }

    private static void validateOperators(FieldType type, Set<FieldOperator> operators) {
        if (!defaultOperators(type).containsAll(operators)) {
            throw new IllegalArgumentException("One or more operators are incompatible with field type " + type + ".");
        }
    }
}
