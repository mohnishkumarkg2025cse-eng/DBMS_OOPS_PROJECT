package com.ruleweaver.engine;

import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.FieldOperator;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class FieldCondition extends AbstractCondition {
    private final String fieldKey;
    private final FieldOperator operator;
    private final Object expectedValue;

    public FieldCondition(String fieldKey, FieldOperator operator, Object expectedValue) {
        super(fieldKey + " " + (operator == null ? "?" : operator.getSymbol()) + " " + expectedValue);
        if (fieldKey == null || fieldKey.isBlank()) throw new IllegalArgumentException("Field key is required.");
        if (operator == null) throw new IllegalArgumentException("Field operator is required.");
        this.fieldKey = fieldKey;
        this.operator = operator;
        this.expectedValue = expectedValue instanceof List<?> values ? List.copyOf(values) : expectedValue;
    }

    public String getFieldKey() { return fieldKey; }
    public FieldOperator getOperator() { return operator; }
    public Object getExpectedValue() { return expectedValue; }

    @Override
    public boolean test(Applicant applicant) {
        if (applicant == null) return false;
        Object actual = applicant.getFieldValue(fieldKey);
        if (actual == null) return false;
        return switch (operator) {
            case EQUALS -> valuesEqual(actual, expectedValue);
            case NOT_EQUALS -> !valuesEqual(actual, expectedValue);
            case GREATER_THAN -> compareNumbers(actual, expectedValue, (left, right) -> left > right);
            case GREATER_THAN_OR_EQUAL -> compareNumbers(actual, expectedValue, (left, right) -> left >= right);
            case LESS_THAN -> compareNumbers(actual, expectedValue, (left, right) -> left < right);
            case LESS_THAN_OR_EQUAL -> compareNumbers(actual, expectedValue, (left, right) -> left <= right);
            case CONTAINS -> contains(actual, expectedValue);
            case NOT_CONTAINS -> !contains(actual, expectedValue);
            case CONTAINS_ANY -> expectedValues(expectedValue).stream().anyMatch(value -> contains(actual, value));
            case CONTAINS_ALL -> expectedValues(expectedValue).stream().allMatch(value -> contains(actual, value));
        };
    }

    @Override
    public String explain(Applicant applicant) {
        Object actual = applicant == null ? null : applicant.getFieldValue(fieldKey);
        return String.format("%s: %s (actual: %s)", getMessage(),
                test(applicant) ? "Passed" : "Failed", actual == null ? "missing" : actual);
    }

    @Override
    protected Object resolveActualValue(Applicant applicant) {
        return applicant == null ? null : applicant.getFieldValue(fieldKey);
    }

    @Override
    protected Object resolveRequiredValue() { return expectedValue; }

    @Override
    public String getType() { return "FieldCondition"; }

    private static boolean valuesEqual(Object actual, Object expected) {
        if (actual instanceof Number left && expected instanceof Number right) {
            return Double.compare(left.doubleValue(), right.doubleValue()) == 0;
        }
        return Objects.equals(actual, expected);
    }

    private static boolean compareNumbers(Object actual, Object expected,
                                          java.util.function.BiPredicate<Double, Double> comparison) {
        return actual instanceof Number left && expected instanceof Number right
                && comparison.test(left.doubleValue(), right.doubleValue());
    }

    private static boolean contains(Object actual, Object expected) {
        if (actual instanceof String text && expected instanceof String needle) {
            return text.contains(needle);
        }
        if (actual instanceof Collection<?> values) {
            return values.contains(expected);
        }
        return false;
    }

    private static List<?> expectedValues(Object expected) {
        return expected instanceof List<?> values ? values : List.of(expected);
    }
}
