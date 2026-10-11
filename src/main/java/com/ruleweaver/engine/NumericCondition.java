package com.ruleweaver.engine;

import com.ruleweaver.model.Applicant;

/**
 * Concrete condition evaluating numeric applicant properties (e.g., GPA >= 8.0, Interview Score >= 70).
 * Demonstrates Polymorphism and Inheritance by extending AbstractCondition.
 */
public class NumericCondition extends AbstractCondition {
    private String fieldName;
    private Operator operator;
    private double threshold;

    public NumericCondition(String fieldName, String operatorSymbol, double threshold) {
        this(fieldName, Operator.fromString(operatorSymbol), threshold);
    }

    public NumericCondition(String fieldName, Operator operator, double threshold) {
        super(formatRuleName(fieldName, operator, threshold));
        this.fieldName = fieldName;
        this.operator = operator;
        this.threshold = threshold;
    }

    private static String formatRuleName(String field, Operator op, double thresh) {
        String displayField = field.substring(0, 1).toUpperCase() + field.substring(1);
        if (thresh == (long) thresh) {
            return String.format("%s %s %d", displayField, op.getSymbol(), (long) thresh);
        }
        return String.format("%s %s %.2f", displayField, op.getSymbol(), thresh);
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
        this.name = formatRuleName(this.fieldName, this.operator, this.threshold);
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
        this.name = formatRuleName(this.fieldName, this.operator, this.threshold);
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
        this.name = formatRuleName(this.fieldName, this.operator, this.threshold);
    }

    @Override
    public boolean test(Applicant applicant) {
        if (applicant == null) {
            return false;
        }
        Double actualValue = applicant.getNumericFieldValue(fieldName);
        if (actualValue == null) {
            return false; // Safe fallback for missing fields
        }
        return operator.apply(actualValue, threshold);
    }

    @Override
    public String explain(Applicant applicant) {
        if (applicant == null) {
            return getMessage() + ": Failed (Applicant is null)";
        }
        Double actualValue = applicant.getNumericFieldValue(fieldName);
        if (actualValue == null) {
            return String.format("%s: Failed (Field '%s' missing or invalid)", getMessage(), fieldName);
        }

        boolean passed = operator.apply(actualValue, threshold);
        String status = passed ? "Passed" : "Failed";

        // Display nicely formatted actual value
        String actualStr = (actualValue == (long) (double) actualValue) 
                ? String.valueOf(actualValue.intValue()) 
                : String.format("%.2f", actualValue);

        return String.format("%s: %s (actual: %s)", getMessage(), status, actualStr);
    }

    @Override
    protected Object resolveActualValue(Applicant applicant) {
        return applicant != null ? applicant.getNumericFieldValue(fieldName) : null;
    }

    @Override
    protected Object resolveRequiredValue() {
        return threshold;
    }

    @Override
    public String getType() {
        return "NumericCondition";
    }
}
