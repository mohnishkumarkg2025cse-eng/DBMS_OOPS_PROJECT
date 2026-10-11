package com.ruleweaver.model;

/**
 * Encapsulates the evaluation outcome for a single condition.
 * Stores condition rule description, pass/fail status, actual evaluated value,
 * and an explainable message.
 */
public class ConditionResult {
    private String conditionName;
    private String conditionType;
    private boolean passed;
    private Object actualValue;
    private Object requiredValue;
    private String explanation;

    public ConditionResult() {
    }

    public ConditionResult(String conditionName, String conditionType, boolean passed, 
                           Object actualValue, String explanation) {
        this(conditionName, conditionType, passed, actualValue, null, explanation);
    }

    public ConditionResult(String conditionName, String conditionType, boolean passed,
                           Object actualValue, Object requiredValue, String explanation) {
        this.conditionName = conditionName;
        this.conditionType = conditionType;
        this.passed = passed;
        this.actualValue = actualValue;
        this.requiredValue = requiredValue;
        this.explanation = explanation;
    }

    public String getConditionName() {
        return conditionName;
    }

    public void setConditionName(String conditionName) {
        this.conditionName = conditionName;
    }

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public Object getActualValue() {
        return actualValue;
    }

    public void setActualValue(Object actualValue) {
        this.actualValue = actualValue;
    }

    public Object getRequiredValue() {
        return requiredValue;
    }

    public void setRequiredValue(Object requiredValue) {
        this.requiredValue = requiredValue;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", 
                passed ? "PASS" : "FAIL", 
                conditionName, 
                explanation);
    }
}
