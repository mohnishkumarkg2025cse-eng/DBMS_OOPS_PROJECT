package com.ruleweaver.repository;

import com.ruleweaver.model.FieldDefinition;
import com.ruleweaver.model.FieldOperator;
import com.ruleweaver.model.FieldType;
import com.ruleweaver.model.FieldValidationRules;
import com.ruleweaver.model.ProgramConfiguration;
import com.ruleweaver.model.ProgramTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgramDocumentMapperTest {

    @Test
    void roundTripsProgramFieldSchemasThroughBson() {
        ProgramConfiguration original = new ProgramConfiguration("p-1", "Admissions",
                ProgramTemplate.MASTERS_ADMISSIONS, List.of(
                new FieldDefinition("score", "Test score", FieldType.NUMBER, true,
                        new FieldValidationRules(0.0, 100.0, null, null, null), List.of(),
                        Set.of(FieldOperator.GREATER_THAN, FieldOperator.EQUALS)),
                new FieldDefinition("status", "Status", FieldType.TEXT, false,
                        new FieldValidationRules(null, null, 2, 20, null),
                        List.of("Ready", "Pending"), Set.of(FieldOperator.EQUALS))));
        original.setArchived(true);

        ProgramConfiguration restored = ProgramDocumentMapper.fromDocument(
                ProgramDocumentMapper.toDocument(original));

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getTemplate(), restored.getTemplate());
        assertEquals(original.getFields().get(0).getType(), restored.getFields().get(0).getType());
        assertEquals(original.getFields().get(0).getSupportedOperators(),
                restored.getFields().get(0).getSupportedOperators());
        assertEquals(original.getFields().get(1).getPermittedValues(),
                restored.getFields().get(1).getPermittedValues());
        assertEquals(2, restored.getFields().get(1).getValidationRules().minimumLength());
        assertEquals(true, restored.isArchived());
    }
}
