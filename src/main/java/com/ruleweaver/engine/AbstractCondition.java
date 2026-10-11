package com.ruleweaver.engine;

import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.ConditionResult;

/**
 * Abstract base class for all conditions, demonstrating Inheritance.
 * Encapsulates common state and delegates evaluation execution.
 */
public abstract class AbstractCondition implements Condition {
    protected String name;

    public AbstractCondition(String name) {
        this.name = name;
    }

    @Override
    public String getMessage() {
        return name;
    }

    @Override
    public ConditionResult evaluate(Applicant applicant) {
        boolean passed = test(applicant);
        String explanation = explain(applicant);
        Object actualVal = resolveActualValue(applicant);
        return new ConditionResult(
                getMessage(), getType(), passed, actualVal, resolveRequiredValue(), explanation);
    }

    /**
     * Resolves actual candidate value relevant to this condition.
     */
    protected abstract Object resolveActualValue(Applicant applicant);

    protected Object resolveRequiredValue() {
        return null;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
