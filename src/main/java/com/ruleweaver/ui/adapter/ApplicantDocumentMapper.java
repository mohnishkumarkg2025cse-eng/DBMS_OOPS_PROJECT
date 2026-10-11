package com.ruleweaver.ui.adapter;

import com.ruleweaver.model.Applicant;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps between the core domain Applicant entity and MongoDB BSON Document.
 * Ensures schema-required fields (e.g. status='ACTIVE', numeric gpa) are always preserved.
 */
public class ApplicantDocumentMapper {

    public static Document toDocument(Applicant applicant) {
        if (applicant == null) {
            return null;
        }

        Document doc = new Document();
        if (applicant.getId() != null && !applicant.getId().trim().isEmpty()) {
            try {
                doc.append("_id", new ObjectId(applicant.getId().trim()));
            } catch (IllegalArgumentException e) {
                doc.append("_id", applicant.getId().trim());
            }
        }

        doc.append("name", applicant.getName() != null ? applicant.getName().trim() : "");
        doc.append("email", applicant.getEmail() != null ? applicant.getEmail().trim() : "");
        doc.append("gpa", applicant.getGpa());
        doc.append("interviewScore", applicant.getInterviewScore());
        doc.append("skills", applicant.getSkills() != null ? new ArrayList<>(applicant.getSkills()) : new ArrayList<>());
        doc.append("status", "ACTIVE"); // Required by MongoDB schema validator

        if (applicant.getCustomAttributes() != null && !applicant.getCustomAttributes().isEmpty()) {
            doc.append("customAttributes", new Document(applicant.getCustomAttributes()));
        }

        return doc;
    }

    public static Applicant toApplicant(Document doc) {
        if (doc == null) {
            return null;
        }

        String id = doc.get("_id") != null ? doc.get("_id").toString() : null;
        String name = doc.getString("name") != null ? doc.getString("name") : "Unnamed";
        String email = doc.getString("email") != null ? doc.getString("email") : "";

        double gpa = 0.0;
        Object gpaVal = doc.get("gpa");
        if (gpaVal instanceof Number) {
            gpa = ((Number) gpaVal).doubleValue();
        }

        int interviewScore = 0;
        Object scoreVal = doc.get("interviewScore");
        if (scoreVal instanceof Number) {
            interviewScore = ((Number) scoreVal).intValue();
        }

        List<String> skills = new ArrayList<>();
        List<?> rawSkills = doc.getList("skills", Object.class);
        if (rawSkills != null) {
            for (Object s : rawSkills) {
                if (s != null) {
                    skills.add(s.toString());
                }
            }
        }

        Map<String, Object> customAttrs = new HashMap<>();
        Object rawAttrs = doc.get("customAttributes");
        if (rawAttrs instanceof Document) {
            customAttrs.putAll((Document) rawAttrs);
        }

        return new Applicant(id, name, email, gpa, interviewScore, skills, customAttrs);
    }
}
