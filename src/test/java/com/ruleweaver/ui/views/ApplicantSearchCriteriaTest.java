package com.ruleweaver.ui.views;

import com.ruleweaver.model.Applicant;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicantSearchCriteriaTest {
    private final Applicant applicant = new Applicant(
            "applicant-1", "Priya Sharma", "priya@example.com", 8.5, 91,
            List.of("Java", "Data Analysis"), Map.of());

    @Test
    void matchesNameEmailAndSkillsIgnoringCaseAndSurroundingWhitespace() {
        assertTrue(ApplicantSearchCriteria.matches(applicant, "  PRIYA "));
        assertTrue(ApplicantSearchCriteria.matches(applicant, "EXAMPLE.COM"));
        assertTrue(ApplicantSearchCriteria.matches(applicant, " data "));
        assertFalse(ApplicantSearchCriteria.matches(applicant, "unrelated"));
    }

    @Test
    void emptySearchMatchesAllApplicants() {
        assertTrue(ApplicantSearchCriteria.matches(applicant, null));
        assertTrue(ApplicantSearchCriteria.matches(applicant, "  "));
    }
}
