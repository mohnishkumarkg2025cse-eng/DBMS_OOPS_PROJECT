package com.ruleweaver.repository;

import com.ruleweaver.model.ProgramSchemaRevision;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryProgramSchemaRevisionRepository implements ProgramSchemaRevisionRepository {
    private final ConcurrentHashMap<String, ProgramSchemaRevision> revisions = new ConcurrentHashMap<>();

    @Override
    public ProgramSchemaRevision save(ProgramSchemaRevision revision) {
        String key = key(revision.getProgramId(), revision.getRevision());
        if (revisions.putIfAbsent(key, revision) != null) {
            throw new IllegalStateException("Program schema revision already exists: " + key);
        }
        return revision;
    }

    @Override
    public Optional<ProgramSchemaRevision> findByProgramIdAndRevision(String programId, int revision) {
        return Optional.ofNullable(revisions.get(key(programId, revision)));
    }

    @Override
    public List<ProgramSchemaRevision> findByProgramId(String programId) {
        return revisions.values().stream()
                .filter(revision -> revision.getProgramId().equals(programId))
                .sorted(Comparator.comparingInt(ProgramSchemaRevision::getRevision))
                .toList();
    }

    private static String key(String programId, int revision) {
        return programId + ":" + revision;
    }
}
