package com.ruleweaver.repository;

import com.ruleweaver.model.ProgramConfiguration;

import java.util.List;
import java.util.Optional;

public interface ProgramRepository {
    ProgramConfiguration save(ProgramConfiguration program);
    Optional<ProgramConfiguration> findById(String id);
    List<ProgramConfiguration> findAll();
    long countPrograms();
    long countActivePrograms();
}
