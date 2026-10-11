package com.ruleweaver.engine;

import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.ConditionResult;

/**
 * Core Condition abstraction interface.
 * Represents an eligibility criterion in the business policy.
 * 
 * Enforces Abstraction & Polymorphism:
 * The DecisionEngine interacts purely through this interface without needing
 * to know whether a concrete condition is numeric, skill-based, or composite.
 */
public interface Condition {

    /**
     * Evaluates whether the applicant passes or fails this condition.
     *
     * @param applicant the candidate being evaluated
     * @return true if condition passes, false otherwise
     */
    boolean test(Applicant applicant);

    /**
     * Returns the human-readable rule specification (e.g., "GPA >= 8.0").
     *
     * @return rule description
     */
    String getMessage();

    /**
     * Generates a transparent, user-friendly explanation showing rule threshold,
     * pass/fail verdict, and the applicant's actual value.
     *
     * @param applicant the candidate evaluated
     * @return explanation string
     */
    String explain(Applicant applicant);

    /**
     * Performs a complete evaluation producing an encapsulated ConditionResult.
     *
     * @param applicant the candidate evaluated
     * @return ConditionResult containing status, message, and explanation
     */
    ConditionResult evaluate(Applicant applicant);

    /**
     * Identifies the category/type of condition (e.g. "NumericCondition", "SkillCondition").
     *
     * @return condition type
     */
    String getType();
}
