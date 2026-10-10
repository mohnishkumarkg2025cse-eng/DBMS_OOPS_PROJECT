package com.ruleweaver.model;

/**
 * *** TEMPORARY STUB - owned by Rizwan. ***
 * The PDF's Condition interface: every rule criterion implements it, so the rest of the
 * system can treat all conditions the same way (polymorphism).
 *
 * IMPORTANT for the whole team: implementations should be IMMUTABLE (all fields final).
 * PolicyVersion shares Condition objects between versions instead of cloning them, which is
 * only safe if a Condition can never change after it is created.
 */
public interface Condition {

    /** Returns true if the applicant satisfies this condition. */
    boolean test(Applicant a);

    /** Short description of the rule itself, e.g. "gpa >= 8.0". */
    String getMessage();

    /** Human-friendly result for one applicant, e.g. "gpa >= 8.0: Passed (actual 8.2)". */
    String explain(Applicant a);
}
