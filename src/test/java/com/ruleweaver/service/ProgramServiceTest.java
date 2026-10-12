package com.ruleweaver.service;

import com.ruleweaver.engine.FieldCondition;
import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.engine.Operator;
import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.FieldDefinition;
import com.ruleweaver.model.FieldOperator;
import com.ruleweaver.model.FieldType;
import com.ruleweaver.model.FieldValidationRules;
import com.ruleweaver.model.ProgramConfiguration;
import com.ruleweaver.model.ProgramTemplate;
import com.ruleweaver.repository.InMemoryProgramRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgramServiceTest {
    private final InMemoryProgramRepository repository = new InMemoryProgramRepository();
    private final ProgramService service = new ProgramService(repository);

    @Test
    void exposesAllSevenEditableProgramTemplates() {
        service.ensureTemplates();

        assertEquals(7, service.listActivePrograms().size());
        for (ProgramTemplate template : ProgramTemplate.values()) {
            ProgramConfiguration config = service.selectProgram(
                    "template-" + template.name().toLowerCase(java.util.Locale.ROOT));
            assertEquals(template, config.getTemplate());
            assertFalse(config.getFields().isEmpty());
        }
    }

    @Test
    void createsRenamesEditsDuplicatesSelectsAndArchivesProgramConfigs() {
        ProgramConfiguration created = service.createFromTemplate(ProgramTemplate.HACKATHON, "Demo Hackathon");
        ProgramConfiguration renamed = service.rename(created.getId(), "Campus Hackathon");
        List<FieldDefinition> fields = new ArrayList<>(renamed.getFields());
        fields.add(field("sponsor", "Sponsor", FieldType.TEXT, false));
        ProgramConfiguration edited = service.editFields(created.getId(), fields);
        ProgramConfiguration duplicate = service.duplicate(edited.getId(), "Copy");

        assertEquals("Campus Hackathon", service.selectProgram(created.getId()).getName());
        assertEquals(fields.size(), service.selectProgram(duplicate.getId()).getFields().size());
        service.archive(created.getId());
        assertThrows(IllegalStateException.class, () -> service.selectProgram(created.getId()));
        assertThrows(IllegalStateException.class, () -> service.rename(created.getId(), "Archived"));
    }

    @Test
    void validatesKnownConditionTypesOperatorsAndValuesAgainstProgramSchema() {
        ProgramConfiguration program = service.createProgram("program-1", "Custom",
                ProgramTemplate.CUSTOM, List.of(
                        field("score", "Score", FieldType.NUMBER, true),
                        field("status", "Status", FieldType.TEXT, false),
                        new FieldDefinition("skills", "Skills", FieldType.MULTI_SELECT, false,
                                FieldValidationRules.none(), List.of("Java", "SQL"), null)));

        service.validateConditions(program.getId(), List.of(
                new NumericCondition("score", Operator.GREATER_THAN, 50),
                new FieldCondition("status", FieldOperator.EQUALS, "Accepted"),
                new FieldCondition("skills", FieldOperator.CONTAINS, "Java")));
        assertThrows(IllegalArgumentException.class, () -> service.validateConditions(program.getId(),
                List.of(new NumericCondition("unknown", Operator.GREATER_THAN, 10))));
        assertThrows(IllegalArgumentException.class, () -> service.validateConditions(program.getId(),
                List.of(new FieldCondition("score", FieldOperator.CONTAINS, 10))));
        assertThrows(IllegalArgumentException.class, () -> service.validateConditions(program.getId(),
                List.of(new FieldCondition("status", FieldOperator.EQUALS, 5))));
        assertThrows(IllegalArgumentException.class, () -> service.validateConditions(program.getId(),
                List.of(new NumericCondition("score", Operator.GREATER_THAN, Double.NaN))));
    }

    @Test
    void validatesMissingAndInvalidRequiredApplicantValues() {
        ProgramConfiguration program = service.createProgram("program-2", "Custom",
                ProgramTemplate.CUSTOM, List.of(
                        field("name", "Applicant name", FieldType.TEXT, true),
                        new FieldDefinition("rating", "Rating", FieldType.NUMBER, true,
                                new FieldValidationRules(0.0, 10.0, null, null, null), List.of(), null)));
        Applicant applicant = new Applicant("A-1", " ", 8, 70, List.of());

        List<String> missing = service.validateApplicant(program.getId(), applicant);
        assertTrue(missing.stream().anyMatch(message -> message.contains("Applicant name is required")));
        assertTrue(missing.stream().anyMatch(message -> message.contains("Rating is required")));
        applicant.setName("A Student");
        applicant.setCustomAttribute("rating", 11.0);
        assertTrue(service.validateApplicant(program.getId(), applicant).get(0).contains("at most 10"));
    }

    @Test
    void requiresMigrationBeforeRemovingAFieldOrChangingItsType() {
        ProgramConfiguration program = service.createProgram("program-3", "Custom",
                ProgramTemplate.CUSTOM, List.of(field("score", "Score", FieldType.NUMBER, false)));
        FieldDefinition textScore = field("score", "Score", FieldType.TEXT, false);

        assertThrows(IllegalStateException.class,
                () -> service.editFields(program.getId(), List.of(textScore)));
        assertThrows(IllegalStateException.class,
                () -> service.editFields(program.getId(), List.of(), null));
        ProgramConfiguration migrated = service.editFields(program.getId(), List.of(textScore),
                (before, after) -> assertEquals(FieldType.TEXT, after.getFieldRegistry().require("score").getType()));
        assertEquals(FieldType.TEXT, migrated.getFieldRegistry().require("score").getType());
    }

    private static FieldDefinition field(String key, String label, FieldType type, boolean required) {
        return new FieldDefinition(key, label, type, required,
                FieldValidationRules.none(), List.of(), null);
    }
}
