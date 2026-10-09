package com.ruleweaver.util;

import com.ruleweaver.repository.*;
import org.bson.Document;
import java.util.Arrays;
import java.util.Date;

public class SeedData {
    public static void main(String[] args) {
        ApplicantRepository applicantRepo = new ApplicantRepository();
        PolicyRepository policyRepo = new PolicyRepository();
        PolicyVersionRepository versionRepo = new PolicyVersionRepository();

        // 1. Create sample applicants
        Document alice = new Document("name", "Alice Smith")
                .append("gpa", 8.2)
                .append("interviewScore", 65)
                .append("skills", Arrays.asList("Java", "SQL"));

        Document bob = new Document("name", "Bob Johnson")
                .append("gpa", 7.9)
                .append("interviewScore", 75)
                .append("skills", Arrays.asList("Python", "Java"));

        Document carol = new Document("name", "Carol White")
                .append("gpa", 8.5)
                .append("interviewScore", 72)
                .append("skills", Arrays.asList("C++", "Java"));

        applicantRepo.save(alice);
        applicantRepo.save(bob);
        applicantRepo.save(carol);

        // 2. Create sample policy
        Document policy = new Document("name", "Internship Eligibility");
        policyRepo.save(policy);

        // 3. Create Policy Version 1
        Document condition1 = new Document("type", "NumericCondition")
                .append("field", "gpa")
                .append("operator", ">=")
                .append("value", 8.0);

        Document condition2 = new Document("type", "NumericCondition")
                .append("field", "interviewScore")
                .append("operator", ">=")
                .append("value", 70);

        Document condition3 = new Document("type", "SkillCondition")
                .append("skill", "Java");

        Document version1 = new Document("policyId", policy.getObjectId("_id"))
                .append("version", 1)
                .append("created", new Date())
                .append("conditions", Arrays.asList(condition1, condition2, condition3));

        versionRepo.saveVersion(version1);

        System.out.println("Database successfully seeded with sample data!");
    }
}