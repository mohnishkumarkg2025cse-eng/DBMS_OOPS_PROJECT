package com.ruleweaver.ui.views;

import com.ruleweaver.model.Applicant;

import java.util.Locale;

final class ApplicantSearchCriteria {
    private ApplicantSearchCriteria() {
    }

    static boolean matches(Applicant applicant, String searchText) {
        if (applicant == null) {
            return false;
        }
        String query = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        return query.isEmpty()
                || contains(applicant.getName(), query)
                || contains(applicant.getEmail(), query)
                || applicant.getSkills().stream().anyMatch(skill -> contains(skill, query));
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
