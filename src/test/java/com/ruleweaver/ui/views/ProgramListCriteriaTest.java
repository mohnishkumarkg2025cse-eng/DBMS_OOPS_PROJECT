package com.ruleweaver.ui.views;

import com.ruleweaver.model.ProgramConfiguration;
import com.ruleweaver.model.ProgramTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgramListCriteriaTest {
    @Test
    void filtersBySearchStatusAndTemplateWithoutTreatingTemplatesAsActivePrograms() {
        ProgramConfiguration active = program("program-active", "Campus Program",
                ProgramTemplate.CAMPUS_PLACEMENT, false);
        ProgramConfiguration archived = program("program-archived", "Archived Program",
                ProgramTemplate.CUSTOM, true);
        ProgramConfiguration template = program("template-custom", "Custom Program",
                ProgramTemplate.CUSTOM, false);

        assertTrue(ProgramListCriteria.matches(active, "CAMPUS", ProgramListCriteria.Status.ACTIVE, null));
        assertFalse(ProgramListCriteria.matches(archived, "", ProgramListCriteria.Status.ACTIVE, null));
        assertTrue(ProgramListCriteria.matches(archived, "", ProgramListCriteria.Status.ARCHIVED, null));
        assertTrue(ProgramListCriteria.matches(template, "", ProgramListCriteria.Status.TEMPLATES, null));
        assertFalse(ProgramListCriteria.matches(template, "", ProgramListCriteria.Status.ACTIVE, null));
        assertTrue(ProgramListCriteria.matches(active, "", ProgramListCriteria.Status.ALL,
                ProgramTemplate.CAMPUS_PLACEMENT.getDisplayName()));
        assertFalse(ProgramListCriteria.matches(active, "", ProgramListCriteria.Status.ALL,
                ProgramTemplate.CUSTOM.getDisplayName()));
    }

    @Test
    void sortsProgramNamesInBothDirections() {
        List<ProgramConfiguration> programs = List.of(
                program("program-z", "Zulu", ProgramTemplate.CUSTOM, false),
                program("program-a", "Alpha", ProgramTemplate.CUSTOM, false));

        assertEquals(List.of("Alpha", "Zulu"), programs.stream()
                .sorted(ProgramListCriteria.byName(false)).map(ProgramConfiguration::getName).toList());
        assertEquals(List.of("Zulu", "Alpha"), programs.stream()
                .sorted(ProgramListCriteria.byName(true)).map(ProgramConfiguration::getName).toList());
    }

    private static ProgramConfiguration program(String id, String name, ProgramTemplate template, boolean archived) {
        ProgramConfiguration program = new ProgramConfiguration(id, name, template, List.of());
        program.setArchived(archived);
        return program;
    }
}
