package com.ruleweaver.service;

import com.ruleweaver.model.ProgramConfiguration;

@FunctionalInterface
public interface ProgramMigration {
    void migrate(ProgramConfiguration previous, ProgramConfiguration replacement);
}
