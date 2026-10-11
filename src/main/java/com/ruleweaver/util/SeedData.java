package com.ruleweaver.util;

import com.mongodb.client.MongoDatabase;
import com.ruleweaver.config.MongoConfig;
import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.engine.Operator;
import com.ruleweaver.engine.SkillCondition;
import com.ruleweaver.model.Policy;
import com.ruleweaver.repository.ApplicantRepository;
import com.ruleweaver.repository.MongoPolicyRepository;
import org.bson.Document;

import java.util.Arrays;

public final class SeedData {

    private SeedData() {
    }

    public static void main(String[] args) {
        try {
            MongoDatabase database = MongoConfig.getDatabase();
            ApplicantRepository applicantRepository = new ApplicantRepository(database);
            MongoPolicyRepository policyRepository = new MongoPolicyRepository(database);

            applicantRepository.upsert(applicant("APP-101", "Alice Smith", 8.2, 65, "Java", "SQL"));
            applicantRepository.upsert(applicant("APP-102", "Bob Johnson", 7.9, 75, "Python", "Java"));
            applicantRepository.upsert(applicant("APP-103", "Carol White", 8.5, 72, "C++", "Java"));

            Policy policy = policyRepository.findById("POL-INTERN").orElseGet(() ->
                    new Policy("POL-INTERN", "Software Engineering Internship Eligibility",
                            "Screening criteria for student summer internship pool"));
            if (policy.getVersion(1).isEmpty()) {
                policy.addVersion(new com.ruleweaver.model.PolicyVersion(
                        "POL-INTERN_v1",
                        "POL-INTERN",
                        1,
                        "Internship Eligibility",
                        Arrays.asList(
                                new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.0),
                                new NumericCondition("interviewScore", Operator.GREATER_THAN_OR_EQUAL, 70.0),
                                new SkillCondition("Java"))));
            }
            if (policy.getPersistedActiveVersionNumber() == 0 && !policy.getVersions().isEmpty()) {
                policy.setActiveVersionNumber(policy.getLatestVersion().orElseThrow().getVersionNumber());
            }
            policyRepository.save(policy);
            for (com.ruleweaver.model.PolicyVersion version : policy.getVersions()) {
                if (policyRepository.findVersion(policy.getId(), version.getVersionNumber()).isEmpty()) {
                    policyRepository.saveVersion(policy.getId(), version);
                }
            }
            System.out.println("Database successfully seeded with sample data.");
        } finally {
            MongoConfig.close();
        }
    }

    private static Document applicant(String id, String name, double gpa, int score, String... skills) {
        return new Document("_id", id)
                .append("name", name)
                .append("gpa", gpa)
                .append("interviewScore", score)
                .append("status", "ACTIVE")
                .append("skills", Arrays.asList(skills));
    }
}
