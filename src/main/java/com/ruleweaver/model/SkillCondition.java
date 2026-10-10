package com.ruleweaver.model;

/**
 * *** TEMPORARY STUB - owned by Rizwan. ***
 * Requires that the applicant has one particular skill (case-insensitive match).
 */
public class SkillCondition implements Condition {

    private final String skill;

    public SkillCondition(String skill) {
        if (skill == null || skill.isBlank()) {
            throw new IllegalArgumentException("skill must not be blank");
        }
        this.skill = skill;
    }

    public String getSkill() { return skill; }

    @Override
    public boolean test(Applicant a) {
        for (String s : a.getSkills()) {
            if (s.equalsIgnoreCase(skill)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getMessage() {
        return "skill " + skill + " present";
    }

    @Override
    public String explain(Applicant a) {
        return getMessage() + ": " + (test(a) ? "Passed" : "Failed");
    }
}
