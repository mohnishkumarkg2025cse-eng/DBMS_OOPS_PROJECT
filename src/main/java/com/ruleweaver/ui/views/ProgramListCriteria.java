package com.ruleweaver.ui.views;

import com.ruleweaver.model.ProgramConfiguration;

import java.util.Comparator;
import java.util.Locale;

final class ProgramListCriteria {
    enum Status {
        ALL("All entries"),
        ACTIVE("Active programs"),
        ARCHIVED("Archived programs"),
        TEMPLATES("Built-in templates");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private ProgramListCriteria() {
    }

    static boolean matches(ProgramConfiguration program, String searchText, Status status,
                           String templateDisplayName) {
        String query = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        String name = program.getName().toLowerCase(Locale.ROOT);
        String templateName = program.getTemplate().getDisplayName();
        boolean textMatches = query.isEmpty() || name.contains(query)
                || templateName.toLowerCase(Locale.ROOT).contains(query);
        boolean template = program.getId().startsWith("template-");
        boolean statusMatches = switch (status) {
            case ALL -> true;
            case ACTIVE -> !template && !program.isArchived();
            case ARCHIVED -> !template && program.isArchived();
            case TEMPLATES -> template;
        };
        boolean typeMatches = templateDisplayName == null || "All types".equals(templateDisplayName)
                || templateName.equals(templateDisplayName);
        return textMatches && statusMatches && typeMatches;
    }

    static Comparator<ProgramConfiguration> byName(boolean descending) {
        Comparator<ProgramConfiguration> comparator = Comparator.comparing(
                program -> program.getName().toLowerCase(Locale.ROOT));
        return descending ? comparator.reversed() : comparator;
    }
}
