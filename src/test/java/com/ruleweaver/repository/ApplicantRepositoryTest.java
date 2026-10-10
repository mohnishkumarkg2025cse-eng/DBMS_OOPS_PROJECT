package com.ruleweaver.repository;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.MongoWriteException;
import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicantRepositoryTest {

    private static ApplicantRepository applicantRepo;
    private static PolicyRepository policyRepo;
    private static PolicyVersionRepository versionRepo;
    private static EvaluationRepository evalRepo;

  
  @BeforeAll
    public static void setUp() {
        // Configure a 3-second server selection timeout
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb+srv://mohnishkumarkg_db_user:RuleWeaver2026@cluster0.wxxzxjx.mongodb.net/?appName=Cluster0")) // Keep your connection string here
                .applyToClusterSettings(builder -> 
                    builder.serverSelectionTimeout(3, TimeUnit.SECONDS))
                .build();

        MongoClient mongoClient = MongoClients.create(settings);
        MongoDatabase database = mongoClient.getDatabase("ruleweaver_test");

        // CLEAR OLD TEST DATA so remote test runs start fresh
        database.getCollection("applicants").deleteMany(new Document());
        database.getCollection("policies").deleteMany(new Document());
        database.getCollection("policy_versions").deleteMany(new Document());
        database.getCollection("evaluations").deleteMany(new Document());

        applicantRepo = new ApplicantRepository(database);
        policyRepo = new PolicyRepository(database);
        versionRepo = new PolicyVersionRepository(database);
        evalRepo = new EvaluationRepository(database);
    }
    @Test
    public void testApplicantCRUD() {
        String testId = "APP_TEST_001";
        
        // 1. Save (including required "status" field for schema validation)
        Document applicant = new Document("_id", testId)
                .append("name", "Alice Test")
                .append("gpa", 3.8)
                .append("status", "ACTIVE");
        applicantRepo.save(applicant);

        // 2. Find
        Document retrieved = applicantRepo.findById(testId);
        assertNotNull(retrieved);
        assertEquals("Alice Test", retrieved.getString("name"));

        // 3. Retrieve All
        List<Document> all = applicantRepo.findAll();
        assertFalse(all.isEmpty());

        // 4. Update
        applicantRepo.update(testId, new Document("gpa", 3.9));
        Document updated = applicantRepo.findById(testId);
        assertEquals(3.9, updated.getDouble("gpa"));

        // 5. Delete
        applicantRepo.delete(testId);
        assertNull(applicantRepo.findById(testId));
    }

    @Test
    public void testPolicyAndVersionPreservation() {
        String policyId = "POLICY001";

        // Save base policy (including required "status" field)
        Document policy = new Document("policyId", policyId)
                .append("name", "Academic Eligibility")
                .append("status", "ACTIVE");
        policyRepo.save(policy);

        // Version 1 (Min Score 60)
        Document v1 = new Document("policyId", policyId)
                .append("version", 1)
                .append("minScore", 60);
        versionRepo.saveVersion(v1);

        // Version 2 (Min Score 70)
        Document v2 = new Document("policyId", policyId)
                .append("version", 2)
                .append("minScore", 70);
        versionRepo.saveVersion(v2);

        // Verify preservation of both versions
        Document fetchedV1 = versionRepo.findByPolicyIdAndVersion(policyId, 1);
        Document fetchedV2 = versionRepo.findByPolicyIdAndVersion(policyId, 2);

        assertNotNull(fetchedV1);
        assertNotNull(fetchedV2);
        assertEquals(60, fetchedV1.getInteger("minScore"));
        assertEquals(70, fetchedV2.getInteger("minScore"));

        // Verify Unique Compound Index Rejects Duplicates (policyId + version)
        Document duplicateV1 = new Document("policyId", policyId)
                .append("version", 1)
                .append("minScore", 90);

        assertThrows(MongoWriteException.class, () -> {
            versionRepo.saveVersion(duplicateV1);
        }, "Database should reject duplicate (policyId, version) combinations!");
    }

    @Test
    public void testEvaluationRepository() {
        String applicantId = "APP001";
        String policyId = "POLICY001";

        Document evaluation = new Document("applicantId", applicantId)
                .append("policyId", policyId)
                .append("policyVersion", 1)
                .append("decision", "ELIGIBLE")
                .append("status", "PENDING")
                .append("reasons", Arrays.asList("Minimum score requirement satisfied", "Attendance requirement satisfied"));
        
        evalRepo.save(evaluation);

        // Retrieve and assert
        List<Document> results = evalRepo.findByApplicantId(applicantId);
        assertFalse(results.isEmpty());
        
        Document savedEval = results.get(0);
        assertEquals("ELIGIBLE", savedEval.getString("decision"));
        assertEquals(1, savedEval.getInteger("policyVersion"));
    }
}