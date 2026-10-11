package com.ruleweaver.ui.adapter;

import com.ruleweaver.model.Applicant;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicantDocumentMapperTest {

    @Test
    public void testMappingToAndFromDocument() {
        Applicant applicant = new Applicant("APP-999", "David Test", 8.45, 82, Arrays.asList("Java", "PostgreSQL"));
        applicant.setEmail("david.test@example.com");

        Document doc = ApplicantDocumentMapper.toDocument(applicant);
        assertNotNull(doc);
        assertEquals("David Test", doc.getString("name"));
        assertEquals("david.test@example.com", doc.getString("email"));
        assertEquals(8.45, doc.getDouble("gpa"));
        assertEquals(82, doc.getInteger("interviewScore"));
        assertEquals("ACTIVE", doc.getString("status"));
        assertEquals(Arrays.asList("Java", "PostgreSQL"), doc.getList("skills", String.class));

        Applicant mappedBack = ApplicantDocumentMapper.toApplicant(doc);
        assertNotNull(mappedBack);
        assertEquals("David Test", mappedBack.getName());
        assertEquals("david.test@example.com", mappedBack.getEmail());
        assertEquals(8.45, mappedBack.getGpa(), 0.001);
        assertEquals(82, mappedBack.getInterviewScore());
        assertEquals(Arrays.asList("Java", "PostgreSQL"), mappedBack.getSkills());
    }
}
