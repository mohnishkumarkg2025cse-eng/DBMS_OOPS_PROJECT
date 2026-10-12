package com.ruleweaver.model;

import java.util.List;

public enum ProgramTemplate {
    INTERNSHIP("Internship"),
    SCHOLARSHIP("Scholarship"),
    CAMPUS_PLACEMENT("Campus Placement"),
    MASTERS_ADMISSIONS("Master's Admissions"),
    HACKATHON("Hackathon"),
    TRAINING("Training Program"),
    CUSTOM("Custom Program");

    private final String displayName;

    ProgramTemplate(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public List<FieldDefinition> defaultFields() {
        FieldDefinition name = text("name", "Applicant name", true);
        FieldDefinition email = text("email", "Email", false);
        FieldDefinition gpa = number("gpa", "GPA", false, 0.0, 10.0);
        FieldDefinition interview = number("interviewScore", "Interview score", false, 0.0, 100.0);
        FieldDefinition skills = new FieldDefinition("skills", "Skills", FieldType.MULTI_SELECT, false,
                FieldValidationRules.none(),
                List.of(),
                null);
        return switch (this) {
            case INTERNSHIP, CAMPUS_PLACEMENT -> List.of(name, email, gpa, interview, skills);
            case SCHOLARSHIP -> List.of(name, email, gpa,
                    number("familyIncome", "Family income", false, 0.0, null),
                    bool("firstGeneration", "First-generation student", false));
            case MASTERS_ADMISSIONS -> List.of(name, email, gpa,
                    number("testScore", "Standardized test score", false, 0.0, null), skills);
            case HACKATHON -> List.of(name, email, skills,
                    text("teamName", "Team name", false),
                    bool("hasPrototype", "Has a prototype", false));
            case TRAINING -> List.of(name, email, skills,
                    text("experienceLevel", "Experience level", false));
            case CUSTOM -> List.of(name, email);
        };
    }

    private static FieldDefinition text(String key, String label, boolean required) {
        return new FieldDefinition(key, label, FieldType.TEXT, required,
                FieldValidationRules.none(), List.of(), null);
    }

    private static FieldDefinition number(String key, String label, boolean required, Double min, Double max) {
        return new FieldDefinition(key, label, FieldType.NUMBER, required,
                new FieldValidationRules(min, max, null, null, null), List.of(), null);
    }

    private static FieldDefinition bool(String key, String label, boolean required) {
        return new FieldDefinition(key, label, FieldType.BOOLEAN, required,
                FieldValidationRules.none(), List.of(), null);
    }
}
