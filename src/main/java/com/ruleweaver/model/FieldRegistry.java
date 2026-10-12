package com.ruleweaver.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FieldRegistry {
    private final Map<String, FieldDefinition> definitions;

    public FieldRegistry(Collection<FieldDefinition> definitions) {
        Map<String, FieldDefinition> indexed = new LinkedHashMap<>();
        if (definitions != null) {
            for (FieldDefinition definition : definitions) {
                if (indexed.putIfAbsent(definition.getKey(), definition) != null) {
                    throw new IllegalArgumentException("Duplicate field key: " + definition.getKey());
                }
            }
        }
        this.definitions = Collections.unmodifiableMap(indexed);
    }

    public Optional<FieldDefinition> find(String key) {
        return Optional.ofNullable(definitions.get(key));
    }

    public FieldDefinition require(String key) {
        return find(key).orElseThrow(() -> new IllegalArgumentException("Unknown field: " + key));
    }

    public List<FieldDefinition> getDefinitions() {
        return List.copyOf(definitions.values());
    }

    public List<String> validateApplicant(Applicant applicant) {
        if (applicant == null) return List.of("Applicant is required.");
        return definitions.values().stream()
                .flatMap(definition -> definition.validateValue(applicant.getFieldValue(definition.getKey())).stream())
                .toList();
    }
}
