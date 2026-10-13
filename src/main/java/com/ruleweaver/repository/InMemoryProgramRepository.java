package com.ruleweaver.repository;

import com.ruleweaver.model.ProgramConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryProgramRepository implements ProgramRepository {
    private final ConcurrentHashMap<String, ProgramConfiguration> programs = new ConcurrentHashMap<>();

    @Override
    public ProgramConfiguration save(ProgramConfiguration program) {
        ProgramConfiguration saved = program.copy();
        programs.put(saved.getId(), saved);
        return saved.copy();
    }

    @Override
    public Optional<ProgramConfiguration> findById(String id) {
        return Optional.ofNullable(programs.get(id)).map(ProgramConfiguration::copy);
    }

    @Override
    public List<ProgramConfiguration> findAll() {
        return new ArrayList<>(programs.values()).stream().map(ProgramConfiguration::copy).toList();
    }

    @Override
    public long countPrograms() {
        return programs.values().stream()
                .filter(program -> !program.getId().startsWith("template-"))
                .count();
    }

    @Override
    public long countActivePrograms() {
        return programs.values().stream()
                .filter(program -> !program.getId().startsWith("template-"))
                .filter(program -> !program.isArchived())
                .count();
    }
}
