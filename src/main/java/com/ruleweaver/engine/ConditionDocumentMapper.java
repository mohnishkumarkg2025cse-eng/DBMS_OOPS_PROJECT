package com.ruleweaver.engine;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializes policy conditions recursively for MongoDB persistence.
 */
public final class ConditionDocumentMapper {

    private ConditionDocumentMapper() {
    }

    public static Document toDocument(Condition condition) {
        if (condition instanceof NumericCondition numeric) {
            return new Document("conditionType", numeric.getType())
                    .append("field", numeric.getFieldName())
                    .append("operator", numeric.getOperator().getSymbol())
                    .append("threshold", numeric.getThreshold());
        }
        if (condition instanceof SkillCondition skill) {
            return new Document("conditionType", skill.getType())
                    .append("requiredSkill", skill.getRequiredSkill());
        }
        if (condition instanceof CompositeCondition composite) {
            List<Document> children = composite.getSubconditions().stream()
                    .map(ConditionDocumentMapper::toDocument)
                    .toList();
            return new Document("conditionType", composite.getType())
                    .append("name", composite.getMessage())
                    .append("logicalOperator", composite.getLogicalOperator().name())
                    .append("subconditions", children);
        }
        throw new IllegalArgumentException("Unsupported condition type: "
                + (condition == null ? "null" : condition.getClass().getName()));
    }

    public static Condition fromDocument(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("Condition document must not be null.");
        }
        String type = firstString(document, "conditionType", "type");
        if ("NumericCondition".equals(type)) {
            String field = document.getString("field");
            String operator = document.getString("operator");
            Object rawThreshold = document.containsKey("threshold")
                    ? document.get("threshold")
                    : document.get("value");
            if (!(rawThreshold instanceof Number threshold)) {
                throw new IllegalArgumentException("Numeric condition requires a numeric threshold.");
            }
            return new NumericCondition(field, operator, threshold.doubleValue());
        }
        if ("SkillCondition".equals(type)) {
            return new SkillCondition(firstString(document, "requiredSkill", "skill"));
        }
        if ("CompositeCondition".equals(type)) {
            String name = document.getString("name");
            String rawOperator = document.getString("logicalOperator");
            LogicalOperator operator = rawOperator == null || rawOperator.isBlank()
                    ? LogicalOperator.AND
                    : LogicalOperator.valueOf(rawOperator.toUpperCase());
            List<Document> rawChildren = document.getList("subconditions", Document.class, new ArrayList<>());
            List<Condition> children = new ArrayList<>(rawChildren.size());
            for (Document child : rawChildren) {
                children.add(fromDocument(child));
            }
            return new CompositeCondition(name, operator, children);
        }
        throw new IllegalArgumentException("Unsupported condition type: " + type);
    }

    private static String firstString(Document document, String primary, String legacy) {
        String value = document.getString(primary);
        return value != null ? value : document.getString(legacy);
    }
}
