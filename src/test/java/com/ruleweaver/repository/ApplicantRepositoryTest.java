package com.ruleweaver.repository;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicantRepositoryTest {

    @Test
    public void testSaveAndFindApplicant() {
        ApplicantRepository repo = new ApplicantRepository();

        Document testApplicant = new Document("name", "Test User")
                .append("gpa", 9.0)
                .append("interviewScore", 88)
                .append("skills", Arrays.asList("Java", "Spring"));

        repo.save(testApplicant);

        List<Document> applicants = repo.findAll();
        assertFalse(applicants.isEmpty(), "Applicants list should not be empty");

        Document retrieved = repo.findById(testApplicant.getObjectId("_id").toHexString());
        assertNotNull(retrieved, "Retrieved document should exist");
        assertEquals("Test User", retrieved.getString("name"));

        // Cleanup
        repo.delete(retrieved.getObjectId("_id").toHexString());
    }
}