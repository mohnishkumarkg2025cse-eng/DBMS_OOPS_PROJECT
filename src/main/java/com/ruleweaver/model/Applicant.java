package com.ruleweaver.model;

import java.util.*;

/**
 * Represents an applicant being evaluated against business policies.
 * Demonstrates Encapsulation by protecting private state and exposing
 * validated accessors and dynamic field retrieval for condition evaluation.
 */
public class Applicant {
    private String id;
    private String name;
    private String email;
    private double gpa;
    private int interviewScore;
    private List<String> skills;
    private Map<String, Object> customAttributes;

    public Applicant() {
        this.skills = new ArrayList<>();
        this.customAttributes = new HashMap<>();
    }

    public Applicant(String id, String name, double gpa, int interviewScore, List<String> skills) {
        this(id, name, "", gpa, interviewScore, skills, new HashMap<>());
    }

    public Applicant(String id, String name, String email, double gpa, int interviewScore,
                     List<String> skills, Map<String, Object> customAttributes) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.gpa = gpa;
        this.interviewScore = interviewScore;
        this.skills = skills != null ? new ArrayList<>(skills) : new ArrayList<>();
        this.customAttributes = customAttributes != null ? new HashMap<>(customAttributes) : new HashMap<>();
    }

    // Accessors demonstrating Encapsulation
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getGpa() {
        return gpa;
    }

    public void setGpa(double gpa) {
        this.gpa = gpa;
    }

    public int getInterviewScore() {
        return interviewScore;
    }

    public void setInterviewScore(int interviewScore) {
        this.interviewScore = interviewScore;
    }

    public List<String> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    public void setSkills(List<String> skills) {
        this.skills = skills != null ? new ArrayList<>(skills) : new ArrayList<>();
    }

    public void addSkill(String skill) {
        if (skill != null && !skill.trim().isEmpty() && !hasSkill(skill)) {
            this.skills.add(skill.trim());
        }
    }

    public boolean hasSkill(String requiredSkill) {
        if (requiredSkill == null || skills == null) {
            return false;
        }
        return skills.stream().anyMatch(s -> s.equalsIgnoreCase(requiredSkill.trim()));
    }

    public Map<String, Object> getCustomAttributes() {
        return Collections.unmodifiableMap(customAttributes);
    }

    public Object getFieldValue(String fieldName) {
        if (fieldName == null) {
            return null;
        }
        return switch (fieldName) {
            case "id" -> id;
            case "name" -> name;
            case "email" -> email;
            case "gpa" -> gpa;
            case "interviewScore", "interview_score", "interview" -> interviewScore;
            case "skills" -> getSkills();
            default -> customAttributes.get(fieldName);
        };
    }

    public void setCustomAttribute(String key, Object value) {
        this.customAttributes.put(key, value);
    }

    /**
     * Resolves a numeric field value dynamically by field name.
     * Supports built-in properties ("gpa", "interviewScore") and custom numeric attributes.
     *
     * @param fieldName the field identifier (case-insensitive)
     * @return Double value of the field, or null if not found/not numeric
     */
    public Double getNumericFieldValue(String fieldName) {
        if (fieldName == null) {
            return null;
        }
        String normalized = fieldName.trim().toLowerCase();
        if (normalized.equals("gpa")) {
            return this.gpa;
        } else if (normalized.equals("interviewscore") || normalized.equals("interview_score") || normalized.equals("interview")) {
            return (double) this.interviewScore;
        } else if (customAttributes.containsKey(fieldName)) {
            Object val = customAttributes.get(fieldName);
            if (val instanceof Number) {
                return ((Number) val).doubleValue();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("Applicant[id='%s', name='%s', gpa=%.2f, interviewScore=%d, skills=%s]",
                id, name, gpa, interviewScore, skills);
    }
}
