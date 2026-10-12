package com.ruleweaver.repository;

import com.ruleweaver.model.ProgramSchemaRevision;

import java.util.List;
import java.util.Optional;

public interface ProgramSchemaRevisionRepository {
    ProgramSchemaRevision save(ProgramSchemaRevision revision);
    Optional<ProgramSchemaRevision> findByProgramIdAndRevision(String programId, int revision);
    List<ProgramSchemaRevision> findByProgramId(String programId);
}
