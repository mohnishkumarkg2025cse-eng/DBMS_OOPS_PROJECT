package com.ruleweaver.repository;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.ruleweaver.config.MongoConfig;
import com.ruleweaver.model.ProgramConfiguration;
import com.ruleweaver.model.ProgramTemplate;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicantRepositoryTest {

    private static ApplicantRepository applicantRepo;
    private static PolicyVersionRepository versionRepo;
    private static EvaluationRepository evalRepo;
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static final String testRunId = UUID.randomUUID().toString();
    private static final String applicantId = "APP_TEST_" + testRunId;
    private static final String policyId = "POLICY_TEST_" + testRunId;
    private static final String programId = "PROGRAM_TEST_" + testRunId;
    private static final String archivedPolicyId = "ARCHIVED_POLICY_TEST_" + testRunId;
    private static final String versionlessPolicyId = "VERSIONLESS_POLICY_TEST_" + testRunId;
    private static final String evaluationId = "EVALUATION_TEST_" + testRunId;

    @BeforeEach
    public void setUp() {
        Optional<String> configuredUri = MongoConfig.getConfiguredUri();
        Assumptions.assumeTrue(configuredUri.isPresent() && canReachMongo(configuredUri.get()),
                "Atlas integration tests require a reachable RULEWEAVER_MONGO_URI.");

        mongoClient = MongoClients.create(configuredUri.orElseThrow());
        database = mongoClient.getDatabase("ruleweaver_test");
        applicantRepo = new ApplicantRepository(database);
        versionRepo = new PolicyVersionRepository(database);
        evalRepo = new EvaluationRepository(database);
    }

    private static boolean canReachMongo(String uri) {
        try (MongoClient probeClient = MongoClients.create(uri)) {
            probeClient.getDatabase("admin").runCommand(new Document("ping", 1));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @AfterEach
    public void cleanUp() {
        if (database != null) {
            database.getCollection("applicants").deleteOne(Filters.eq("_id", applicantId));
            database.getCollection("policies").deleteOne(Filters.eq("_id", policyId));
            database.getCollection("policy_versions").deleteMany(Filters.eq("policyId", policyId));
            database.getCollection("policies").deleteMany(Filters.in("_id",
                    archivedPolicyId, versionlessPolicyId));
            database.getCollection("policy_versions").deleteMany(Filters.in("policyId",
                    archivedPolicyId, versionlessPolicyId));
            database.getCollection("programs").deleteOne(Filters.eq("_id", programId));
            database.getCollection("evaluations").deleteMany(Filters.eq("applicantId", applicantId));
            database.getCollection("evaluations").deleteOne(Filters.eq("_id", evaluationId));
        }
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
        database = null;
    }

    @Test
    public void testApplicantCRUD() {
        long previousCount = applicantRepo.count();
        Document applicant = new Document("_id", applicantId)
                .append("name", "Alice Test")
                .append("gpa", 3.8)
                .append("status", "ACTIVE");
        applicantRepo.save(applicant);
        assertEquals(previousCount + 1, applicantRepo.count());

        Document retrieved = applicantRepo.findById(applicantId);
        assertNotNull(retrieved);
        assertEquals("Alice Test", retrieved.getString("name"));

        List<Document> all = applicantRepo.findAll();
        assertFalse(all.isEmpty());

        applicantRepo.update(applicantId, new Document("gpa", 3.9));
        Document updated = applicantRepo.findById(applicantId);
        assertEquals(3.9, updated.getDouble("gpa"));

        applicantRepo.delete(applicantId);
        assertNull(applicantRepo.findById(applicantId));
    }

    @Test
    public void testDashboardKpiRepositoryCounts() {
        long applicantCount = applicantRepo.count();
        applicantRepo.save(new Document("_id", applicantId).append("name", "KPI Applicant"));
        assertEquals(applicantCount + 1, applicantRepo.count());

        MongoProgramRepository programs = new MongoProgramRepository(database);
        long programCount = programs.countPrograms();
        programs.save(new ProgramConfiguration(programId, "KPI Program",
                ProgramTemplate.CUSTOM, List.of()));
        assertEquals(programCount + 1, programs.countPrograms());

        MongoPolicyRepository policies = new MongoPolicyRepository(database);
        long activePolicyCount = policies.countActivePolicies();
        database.getCollection("policies").insertOne(new Document("_id", policyId)
                .append("archived", false)
                .append("activeVersionNumber", 0));
        versionRepo.saveVersion(new Document("policyId", policyId).append("version", 1));
        database.getCollection("policies").insertOne(new Document("_id", archivedPolicyId)
                .append("archived", true));
        versionRepo.saveVersion(new Document("policyId", archivedPolicyId).append("version", 1));
        database.getCollection("policies").insertOne(new Document("_id", versionlessPolicyId)
                .append("archived", false));
        assertEquals(activePolicyCount + 1, policies.countActivePolicies());

        long decisionCount = evalRepo.countDecisions();
        evalRepo.save(new Document("_id", evaluationId).append("applicantId", applicantId));
        assertEquals(decisionCount + 1, evalRepo.countDecisions());
    }

    @Test
    public void testPolicyAndVersionPreservation() {
        Document policy = new Document("_id", policyId)
                .append("name", "Academic Eligibility")
                .append("status", "ACTIVE");
        database.getCollection("policies").insertOne(policy);

        Document v1 = new Document("policyId", policyId)
                .append("version", 1)
                .append("minScore", 60);
        versionRepo.saveVersion(v1);

        Document v2 = new Document("policyId", policyId)
                .append("version", 2)
                .append("minScore", 70);
        versionRepo.saveVersion(v2);

        Document fetchedV1 = versionRepo.findByPolicyIdAndVersion(policyId, 1);
        Document fetchedV2 = versionRepo.findByPolicyIdAndVersion(policyId, 2);

        assertNotNull(fetchedV1);
        assertNotNull(fetchedV2);
        assertEquals(60, fetchedV1.getInteger("minScore"));
        assertEquals(70, fetchedV2.getInteger("minScore"));

        Document duplicateV1 = new Document("policyId", policyId)
                .append("version", 1)
                .append("minScore", 90);

        assertThrows(MongoWriteException.class, () -> versionRepo.saveVersion(duplicateV1),
                "Database should reject duplicate (policyId, version) combinations!");
    }

    @Test
    public void testEvaluationRepository() {
        Document evaluation = new Document("applicantId", applicantId)
                .append("policyId", policyId)
                .append("policyVersion", 1)
                .append("decision", "ELIGIBLE")
                .append("status", "PENDING")
                .append("reasons", Arrays.asList(
                        "Minimum score requirement satisfied", "Attendance requirement satisfied"));

        evalRepo.save(evaluation);

        List<Document> results = evalRepo.findByApplicantId(applicantId);
        assertFalse(results.isEmpty());

        Document savedEval = results.get(0);
        assertEquals("ELIGIBLE", savedEval.getString("decision"));
        assertEquals(1, savedEval.getInteger("policyVersion"));
    }
}
