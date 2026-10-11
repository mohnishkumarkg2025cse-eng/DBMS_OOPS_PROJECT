package com.ruleweaver.engine;

import com.ruleweaver.model.Applicant;

/**
 * Concrete condition validating the presence of required competencies/skills.
 * Demonstrates Polymorphism and clean handling of collection-based attributes.
 */
public class SkillCondition extends AbstractCondition {
    private String requiredSkill;

    public SkillCondition(String requiredSkill) {
        super("Skill '" + requiredSkill + "' required");
        this.requiredSkill = requiredSkill;
    }

    public String getRequiredSkill() {
        return requiredSkill;
    }

    public void setRequiredSkill(String requiredSkill) {
        this.requiredSkill = requiredSkill;
        this.name = "Skill '" + requiredSkill + "' required";
    }

    @Override
    public boolean test(Applicant applicant) {
        if (applicant == null) {
            return false;
        }
        return applicant.hasSkill(requiredSkill);
    }

    @Override
    public String explain(Applicant applicant) {
        if (applicant == null) {
            return getMessage() + ": Failed (Applicant is null)";
        }
        boolean hasSkill = applicant.hasSkill(requiredSkill);
        if (hasSkill) {
            return String.format("Required skill '%s': Passed", requiredSkill);
        } else {
            String existingSkills = applicant.getSkills().isEmpty() 
                    ? "none listed" 
                    : applicant.getSkills().toString();
            return String.format("Required skill '%s': Failed (candidate skills: %s)", 
                    requiredSkill, existingSkills);
        }
    }

    @Override
    protected Object resolveActualValue(Applicant applicant) {
        return applicant != null ? applicant.getSkills() : null;
    }

    @Override
    protected Object resolveRequiredValue() {
        return requiredSkill;
    }

    @Override
    public String getType() {
        return "SkillCondition";
    }
}
