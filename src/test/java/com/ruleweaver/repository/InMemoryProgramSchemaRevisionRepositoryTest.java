package com.ruleweaver.repository;

import com.ruleweaver.model.ProgramSchemaRevision;
import com.ruleweaver.model.ProgramTemplate;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryProgramSchemaRevisionRepositoryTest {

    @Test
    void storesRevisionsInOrderAndRejectsReplacement() {
        InMemoryProgramSchemaRevisionRepository repository = new InMemoryProgramSchemaRevisionRepository();
        ProgramSchemaRevision second = revision(2);
        ProgramSchemaRevision first = revision(1);

        repository.save(second);
        repository.save(first);

        assertEquals(List.of(first, second), repository.findByProgramId("program-1"));
        assertThrows(IllegalStateException.class, () -> repository.save(first));
    }

    private static ProgramSchemaRevision revision(int revision) {
        return new ProgramSchemaRevision("program-1:r" + revision, "program-1", revision,
                ProgramTemplate.MASTERS_ADMISSIONS, List.of(), LocalDateTime.of(2026, 5, revision, 12, 0));
    }
}
