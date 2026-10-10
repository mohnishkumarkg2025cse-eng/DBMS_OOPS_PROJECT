package com.ruleweaver.model;

/**
 * *** TEMPORARY STUB - owned by Rizwan. ***
 * Numeric comparison such as gpa >= 8.0. Fields match the PDF: fieldName, operator, threshold.
 * All fields are final, so a NumericCondition can never change after construction.
 */
public class NumericCondition implements Condition {

    private final String fieldName;
    private final String operator;
    private final double threshold;

    public NumericCondition(String fieldName, String operator, double threshold) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("fieldName must not be blank");
        }
        if (!(operator.equals(">=") || operator.equals(">") || operator.equals("<=")
                || operator.equals("<") || operator.equals("=="))) {
            throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
        this.fieldName = fieldName;
        this.operator = operator;
        this.threshold = threshold;
    }

    // Getters are used by PolicyService to build a changed copy of a condition.
    public String getFieldName() { return fieldName; }
    public String getOperator() { return operator; }
    public double getThreshold() { return threshold; }

    @Override
    public boolean test(Applicant a) {
        double value = a.getFieldValue(fieldName);
        switch (operator) {
            case ">=": return value >= threshold;
            case ">":  return value > threshold;
            case "<=": return value <= threshold;
            case "<":  return value < threshold;
            default:   return value == threshold;
        }
    }

    @Override
    public String getMessage() {
        return fieldName + " " + operator + " " + threshold;
    }

    @Override
    public String explain(Applicant a) {
        return getMessage() + ": " + (test(a) ? "Passed" : "Failed")
                + " (actual " + a.getFieldValue(fieldName) + ")";
    }
}
