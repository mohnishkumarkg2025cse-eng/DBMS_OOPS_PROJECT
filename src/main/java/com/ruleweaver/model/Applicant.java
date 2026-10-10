package com.ruleweaver.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * *** TEMPORARY STUB - owned by Rizwan. ***
 * Replace this file with Rizwan's real Applicant.java when it is ready.
 * It exists only so Mukesh's module compiles and can be tested alone.
 *
 * Fields follow the PDF: id, name, gpa, interviewScore, skills.
 */
public class Applicant {

    private final String id;
    private final String name;
    private final double gpa;
    private final int interviewScore;
    private final List<String> skills;

    public Applicant(String id, String name, double gpa, int interviewScore, List<String> skills) {
        this.id = id;
        this.name = name;
        this.gpa = gpa;
        this.interviewScore = interviewScore;
        this.skills = Collections.unmodifiableList(new ArrayList<>(skills));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getGpa() { return gpa; }
    public int getInterviewScore() { return interviewScore; }
    public List<String> getSkills() { return skills; }

    /** Lets NumericCondition read a numeric field by name (the PDF assumes a method like this). */
    public double getFieldValue(String fieldName) {
        switch (fieldName.toLowerCase()) {
            case "gpa":
                return gpa;
            case "interviewscore":
                return interviewScore;
            default:
                throw new IllegalArgumentException("Unknown numeric field: " + fieldName);
        }
    }

    @Override
    public String toString() {
        return name + " (GPA " + gpa + ", interview " + interviewScore + ", skills " + skills + ")";
    }
}
