package com.ruleweaver.repository;

import com.ruleweaver.model.Applicant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryApplicantRepository {
    private final Map<String, Applicant> storage = new ConcurrentHashMap<>();

    public Applicant save(Applicant applicant) {
        if (applicant.getId() == null || applicant.getId().trim().isEmpty()) {
            applicant.setId(UUID.randomUUID().toString());
        }
        storage.put(applicant.getId(), applicant);
        return applicant;
    }

    public Optional<Applicant> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<Applicant> findAll() {
        return new ArrayList<>(storage.values());
    }

    public List<Applicant> findBySkill(String skill) {
        return storage.values().stream()
                .filter(a -> a.hasSkill(skill))
                .collect(Collectors.toList());
    }

    public List<Applicant> findByMinGpa(double minGpa) {
        return storage.values().stream()
                .filter(a -> a.getGpa() >= minGpa)
                .collect(Collectors.toList());
    }

    public boolean deleteById(String id) {
        return storage.remove(id) != null;
    }

    public long count() {
        return storage.size();
    }
}
