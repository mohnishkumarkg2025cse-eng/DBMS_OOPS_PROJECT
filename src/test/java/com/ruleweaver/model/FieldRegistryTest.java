package com.ruleweaver.model;

import com.ruleweaver.model.Applicant;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldRegistryTest {

    @Test
    void validatesAllSupportedFieldTypesAndTheirConfiguredRules() {
        FieldRegistry registry = new FieldRegistry(List.of(
                new FieldDefinition("score", "Score", FieldType.NUMBER, true,
                        new FieldValidationRules(0.0, 100.0, null, null, null), List.of(), null),
                new FieldDefinition("major", "Major", FieldType.TEXT, true,
                        new FieldValidationRules(null, null, 2, 30, "[A-Za-z ]+"),
                        List.of("Computer Science", "Mathematics"), null),
                new FieldDefinition("eligible", "Eligible", FieldType.BOOLEAN, true,
                        FieldValidationRules.none(), List.of(), Set.of(FieldOperator.EQUALS)),
                new FieldDefinition("skills", "Skills", FieldType.MULTI_SELECT, false,
                        FieldValidationRules.none(), List.of("Java", "SQL"), null)));

        assertTrue(registry.require("score").validateValue(89.5).isEmpty());
        assertTrue(registry.require("major").validateValue("Computer Science").isEmpty());
        assertTrue(registry.require("eligible").validateValue(true).isEmpty());
        assertTrue(registry.require("skills").validateValue(List.of("Java", "SQL")).isEmpty());
        assertTrue(registry.require("score").validateValue(101).get(0).contains("at most 100"));
        assertTrue(registry.require("major").validateValue("1").stream()
                .anyMatch(message -> message.contains("format")));
        assertTrue(registry.require("eligible").validateValue("yes").get(0).contains("true or false"));
        assertTrue(registry.require("skills").validateValue(List.of("Go")).get(0).contains("invalid value"));
    }

    @Test
    void checksMissingRequiredValuesAndDynamicCustomApplicantFields() {
        FieldRegistry registry = new FieldRegistry(List.of(
                new FieldDefinition("portfolioScore", "Portfolio score", FieldType.NUMBER, true,
                        FieldValidationRules.none(), List.of(), null)));
        Applicant applicant = new Applicant("A-1", "Applicant", 8.0, 70, List.of());

        assertEquals(List.of("Portfolio score is required."), registry.validateApplicant(applicant));
        applicant.setCustomAttribute("portfolioScore", 94.0);
        assertTrue(registry.validateApplicant(applicant).isEmpty());
    }

    @Test
    void rejectsOperatorsIncompatibleWithDeclaredFieldType() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FieldDefinition("status", "Status", FieldType.BOOLEAN, false,
                        FieldValidationRules.none(), List.of(), Set.of(FieldOperator.GREATER_THAN)));
    }

    @Test
    void rejectsBlankRequiredRuleValuesAndEmptyMultiSelectRuleValues() {
        FieldDefinition requiredText = new FieldDefinition("status", "Status", FieldType.TEXT, true,
                FieldValidationRules.none(), List.of(), null);
        FieldDefinition skills = new FieldDefinition("skills", "Skills", FieldType.MULTI_SELECT, false,
                FieldValidationRules.none(), List.of("Java"), null);

        assertTrue(requiredText.validateConditionValue(FieldOperator.EQUALS, " ").get(0)
                .contains("must not be blank"));
        assertTrue(skills.validateConditionValue(FieldOperator.CONTAINS_ANY, List.of()).get(0)
                .contains("At least one"));
    }

    @Test
    void rejectsNonFiniteNumericValidationBounds() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FieldValidationRules(Double.NaN, null, null, null, null));
    }
}
