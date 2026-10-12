package com.ruleweaver.service;

import com.ruleweaver.engine.CompositeCondition;
import com.ruleweaver.engine.Condition;
import com.ruleweaver.engine.FieldCondition;
import com.ruleweaver.engine.LogicalOperator;
import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.engine.SkillCondition;
import com.ruleweaver.model.FieldDefinition;
import com.ruleweaver.model.FieldOperator;
import com.ruleweaver.model.FieldRegistry;
import com.ruleweaver.model.FieldType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mutable, in-memory rule tree used only while editing an unsaved policy draft.
 */
public final class PolicyRuleDraft {
    private final Group root = new Group(LogicalOperator.AND);

    public Group root() {
        return root;
    }

    public List<String> validate(FieldRegistry registry) {
        if (registry == null) {
            return List.of("Select a program schema or use a legacy policy.");
        }
        List<String> errors = new ArrayList<>();
        validateGroup(root, registry, errors, "Rules", new HashSet<>());
        return List.copyOf(errors);
    }

    public List<Condition> buildConditions(FieldRegistry registry) {
        List<String> errors = validate(registry);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", errors));
        }
        List<Condition> conditions = new ArrayList<>();
        for (Object child : root.children) {
            conditions.add(buildCondition(child, registry));
        }
        return List.copyOf(conditions);
    }

    public String naturalLanguage(FieldRegistry registry) {
        if (root.children.isEmpty()) {
            return "Eligible if …";
        }
        return "Eligible if " + renderGroup(root, registry, " AND ");
    }

    public String readOnlyTree(FieldRegistry registry) {
        StringBuilder output = new StringBuilder("AND");
        appendTree(root, registry, output, 1);
        return output.toString();
    }

    public static PolicyRuleDraft fromConditions(List<Condition> conditions) {
        PolicyRuleDraft draft = new PolicyRuleDraft();
        if (conditions != null) {
            for (Condition condition : conditions) {
                draft.root.children.add(fromCondition(condition));
            }
        }
        return draft;
    }

    private static void validateGroup(Group group, FieldRegistry registry, List<String> errors,
                                      String label, Set<String> signatures) {
        if (group.children.isEmpty()) {
            errors.add(label + " logical group cannot be empty.");
            return;
        }
        for (Object child : group.children) {
            if (child instanceof Group nested) {
                validateGroup(nested, registry, errors, "Nested " + nested.operator + " group", signatures);
            } else if (child instanceof Rule rule) {
                FieldDefinition definition = registry.find(rule.fieldKey).orElse(null);
                if (definition == null) {
                    errors.add("Unknown field: " + rule.fieldKey + ".");
                    continue;
                }
                if (rule.operator == null || !definition.getSupportedOperators().contains(rule.operator)) {
                    errors.add(rule.operator == null
                            ? "An operator is required for " + definition.getLabel() + "."
                            : "Operator " + rule.operator.getSymbol() + " is not supported for "
                            + definition.getLabel() + ".");
                    continue;
                }
                Object value;
                try {
                    value = parseValue(definition, rule.operator, rule.value);
                } catch (IllegalArgumentException exception) {
                    errors.add(exception.getMessage());
                    continue;
                }
                errors.addAll(definition.validateConditionValue(rule.operator, value));
                if (value != null) {
                    String signature = rule.fieldKey + "|" + rule.operator + "|" + value;
                    if (!signatures.add(signature)) {
                        errors.add("This rule duplicates another condition: " + definition.getLabel() + ".");
                    }
                }
            } else {
                errors.add("Unsupported rule draft node.");
            }
        }
    }

    private static Object parseValue(FieldDefinition definition, FieldOperator operator, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return switch (definition.getType()) {
                case NUMBER -> {
                    double value = Double.parseDouble(raw.trim());
                    if (!Double.isFinite(value)) {
                        throw new IllegalArgumentException(definition.getLabel() + " must be a finite number.");
                    }
                    yield value;
                }
                case BOOLEAN -> {
                    String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
                    if (!value.equals("true") && !value.equals("false")) {
                        throw new IllegalArgumentException(definition.getLabel() + " must be true or false.");
                    }
                    yield Boolean.valueOf(value);
                }
                case MULTI_SELECT -> {
                    if (operator == FieldOperator.CONTAINS_ANY || operator == FieldOperator.CONTAINS_ALL) {
                        List<String> values = java.util.Arrays.stream(raw.split(","))
                                .map(String::trim).filter(value -> !value.isEmpty()).toList();
                        yield values;
                    }
                    yield raw.trim();
                }
                case TEXT -> raw;
            };
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(definition.getLabel() + " must be a valid number.");
        }
    }

    private static Condition buildCondition(Object node, FieldRegistry registry) {
        if (node instanceof Group group) {
            List<Condition> children = group.children.stream()
                    .map(child -> buildCondition(child, registry)).toList();
            return new CompositeCondition(group.operator + " group", group.operator, children);
        }
        Rule rule = (Rule) node;
        FieldDefinition definition = registry.require(rule.fieldKey);
        Object value = parseValue(definition, rule.operator, rule.value);
        if (definition.getType() == FieldType.NUMBER
                && ("gpa".equals(rule.fieldKey) || "interviewScore".equals(rule.fieldKey))) {
            return new NumericCondition(rule.fieldKey, com.ruleweaver.engine.Operator.valueOf(rule.operator.name()),
                    ((Number) value).doubleValue());
        }
        if ("skills".equals(rule.fieldKey) && rule.operator == FieldOperator.CONTAINS
                && value instanceof String skill) {
            return new SkillCondition(skill);
        }
        return new FieldCondition(rule.fieldKey, rule.operator, value);
    }

    private static String renderGroup(Group group, FieldRegistry registry, String separator) {
        List<String> pieces = new ArrayList<>();
        for (Object child : group.children) {
            if (child instanceof Group nested) {
                pieces.add("(" + renderGroup(nested, registry, " " + nested.operator + " ") + ")");
            } else {
                Rule rule = (Rule) child;
                FieldDefinition definition = registry.find(rule.fieldKey).orElse(null);
                String field = definition == null ? rule.fieldKey : definition.getLabel();
                pieces.add(field + " " + operatorPhrase(rule.operator)
                        + " " + (rule.value == null || rule.value.isBlank() ? "…" : rule.value.trim()));
            }
        }
        return String.join(separator, pieces);
    }

    private static String operatorPhrase(FieldOperator operator) {
        if (operator == null) return "…";
        return switch (operator) {
            case EQUALS -> "is";
            case NOT_EQUALS -> "is not";
            case GREATER_THAN -> "is greater than";
            case GREATER_THAN_OR_EQUAL -> "is at least";
            case LESS_THAN -> "is less than";
            case LESS_THAN_OR_EQUAL -> "is at most";
            case CONTAINS -> "includes";
            case NOT_CONTAINS -> "does not include";
            case CONTAINS_ANY -> "includes any of";
            case CONTAINS_ALL -> "includes all of";
        };
    }

    private static void appendTree(Group group, FieldRegistry registry, StringBuilder output, int depth) {
        for (Object child : group.children) {
            output.append('\n').append("  ".repeat(depth));
            if (child instanceof Group nested) {
                output.append(nested.operator);
                appendTree(nested, registry, output, depth + 1);
            } else {
                Rule rule = (Rule) child;
                FieldDefinition definition = registry.find(rule.fieldKey).orElse(null);
                output.append(definition == null ? rule.fieldKey : definition.getLabel())
                        .append(' ').append(rule.operator == null ? "…" : rule.operator.getSymbol())
                        .append(' ').append(rule.value == null || rule.value.isBlank() ? "…" : rule.value.trim());
            }
        }
    }

    private static Object fromCondition(Condition condition) {
        if (condition instanceof CompositeCondition composite) {
            Group group = new Group(composite.getLogicalOperator());
            composite.getSubconditions().forEach(child -> group.children.add(fromCondition(child)));
            return group;
        }
        if (condition instanceof NumericCondition numeric) {
            return new Rule(numeric.getFieldName(), FieldOperator.valueOf(numeric.getOperator().name()),
                    Double.toString(numeric.getThreshold()));
        }
        if (condition instanceof SkillCondition skill) {
            return new Rule("skills", FieldOperator.CONTAINS, skill.getRequiredSkill());
        }
        if (condition instanceof FieldCondition field) {
            Object value = field.getExpectedValue();
            String text = value instanceof List<?> values
                    ? String.join(", ", values.stream().map(String::valueOf).toList())
                    : String.valueOf(value);
            return new Rule(field.getFieldKey(), field.getOperator(), text);
        }
        throw new IllegalArgumentException("Unsupported condition type in draft: " + condition.getClass().getSimpleName());
    }

    public static final class Group {
        private LogicalOperator operator;
        private final List<Object> children = new ArrayList<>();

        public Group(LogicalOperator operator) {
            this.operator = operator == null ? LogicalOperator.AND : operator;
        }

        public LogicalOperator operator() { return operator; }
        public void setOperator(LogicalOperator operator) {
            this.operator = java.util.Objects.requireNonNull(operator, "Logical operator is required.");
        }
        public List<Object> children() { return children; }
    }

    public static final class Rule {
        private String fieldKey;
        private FieldOperator operator;
        private String value;

        public Rule(String fieldKey, FieldOperator operator, String value) {
            this.fieldKey = fieldKey;
            this.operator = operator;
            this.value = value;
        }

        public String fieldKey() { return fieldKey; }
        public FieldOperator operator() { return operator; }
        public String value() { return value; }
        public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
        public void setOperator(FieldOperator operator) { this.operator = operator; }
        public void setValue(String value) { this.value = value; }
    }
}
