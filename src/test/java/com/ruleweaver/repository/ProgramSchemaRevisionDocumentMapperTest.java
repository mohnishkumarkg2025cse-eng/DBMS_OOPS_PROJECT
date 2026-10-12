package com.ruleweaver.repository;

import com.ruleweaver.model.FieldDefinition;
import com.ruleweaver.model.FieldType;
import com.ruleweaver.model.FieldValidationRules;
import com.ruleweaver.model.ProgramSchemaRevision;
import com.ruleweaver.model.ProgramTemplate;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgramSchemaRevisionDocumentMapperTest {

    @Test
    void roundTripsImmutableProgramSchemaRevision() {
        ProgramSchemaRevision revision = new ProgramSchemaRevision(
                "program-1:r2", "program-1", 2, ProgramTemplate.MASTERS_ADMISSIONS,
                List.of(new FieldDefinition("score", "Score", FieldType.NUMBER, true,
                        new FieldValidationRules(0.0, 100.0, null, null, null), List.of(), null)),
                LocalDateTime.of(2026, 5, 4, 12, 30));

        Document document = ProgramSchemaRevisionDocumentMapper.toDocument(revision);
        ProgramSchemaRevision restored = ProgramSchemaRevisionDocumentMapper.fromDocument(document);

        assertEquals(revision.getId(), restored.getId());
        assertEquals(revision.getProgramId(), restored.getProgramId());
        assertEquals(revision.getRevision(), restored.getRevision());
        assertEquals(revision.getTemplate(), restored.getTemplate());
        assertEquals(revision.getFields().get(0).getKey(), restored.getFields().get(0).getKey());
        assertEquals(revision.getCreatedAt(), restored.getCreatedAt());
    }
}
