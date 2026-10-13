package com.ruleweaver.util;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.ruleweaver.config.MongoConfig;
import com.ruleweaver.repository.MongoProgramSchemaRevisionRepository;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationRunnerTest {
    private MongoClient client;
    private MongoDatabase database;

    @BeforeEach
    void connectToIsolatedMongoDatabase() {
        Optional<String> configuredUri = MongoConfig.getConfiguredUri();
        Assumptions.assumeTrue(configuredUri.isPresent() && canReachMongo(configuredUri.orElseThrow()),
                "Mongo migration integration test requires a reachable configured MongoDB.");
        client = MongoClients.create(configuredUri.orElseThrow());
        String databaseName = "rwmt_" + UUID.randomUUID().toString().replace("-", "");
        assertFalse(client.listDatabaseNames().into(new java.util.ArrayList<>()).contains(databaseName));
        database = client.getDatabase(databaseName);
    }

    @AfterEach
    void dropOnlyTheFreshTestDatabase() {
        if (database != null) database.drop();
        if (client != null) client.close();
    }

    @Test
    void previewsThenAppliesAndRerunsWithoutChangingLegacyHistory() {
        Document legacyProgram = legacyProgram("legacy-program");
        database.getCollection("programs").insertOne(legacyProgram);
        database.getCollection("applicants").insertOne(
                new Document("_id", "legacy-applicant").append("name", "Legacy"));
        database.getCollection("policies").insertOne(
                new Document("_id", "legacy-policy").append("name", "Legacy policy"));
        database.getCollection("policy_versions").insertOne(
                new Document("_id", "legacy-policy_v1")
                        .append("policyId", "legacy-policy").append("version", 1));
        database.getCollection("evaluations").insertOne(
                new Document("_id", "legacy-evaluation")
                        .append("applicantId", "legacy-applicant")
                        .append("policyId", "legacy-policy")
                        .append("policyVersion", 1));

        DatabaseMigrationRunner runner = new DatabaseMigrationRunner(database);
        DatabaseMigrationReport preview = runner.preview();

        assertEquals(DatabaseMigrationReport.Mode.DRY_RUN, preview.mode());
        assertEquals(DatabaseMigrationReport.Status.PREVIEWED, preview.status());
        assertEquals(1, preview.programSchemaRevisionsPrepared());
        assertEquals(1, preview.legacyUnassignedApplicants());
        assertEquals(1, preview.legacyUnassignedPolicies());
        assertEquals(1, preview.legacyEvaluations());
        assertEquals(0, database.getCollection(MongoProgramSchemaRevisionRepository.COLLECTION_NAME).countDocuments());
        assertNull(database.getCollection("programs").find(Filters.eq("_id", "legacy-program"))
                .first().get("schemaRevision"));
        assertNull(database.getCollection("migration_runs").find().first());

        DatabaseMigrationReport applied = runner.apply();

        assertEquals(DatabaseMigrationReport.Status.APPLIED, applied.status());
        assertEquals(applied.before().applicants(), applied.after().applicants());
        assertEquals(applied.before().policies(), applied.after().policies());
        assertEquals(applied.before().policyVersions(), applied.after().policyVersions());
        assertEquals(applied.before().evaluations(), applied.after().evaluations());
        assertEquals(applied.before().programs(), applied.after().programs());
        assertEquals(1, applied.after().programSchemaRevisions());
        assertEquals(1, database.getCollection("programs").find(Filters.eq("_id", "legacy-program"))
                .first().getInteger("schemaRevision"));
        assertEquals(1, database.getCollection("program_schema_revisions")
                .find(Filters.eq("programId", "legacy-program")).first().getInteger("revision"));
        assertNull(database.getCollection("applicants").find(Filters.eq("_id", "legacy-applicant"))
                .first().get("programId"));
        assertNull(database.getCollection("policies").find(Filters.eq("_id", "legacy-policy"))
                .first().get("programId"));
        assertNull(database.getCollection("evaluations").find(Filters.eq("_id", "legacy-evaluation"))
                .first().get("programId"));
        assertEquals("legacy-policy_v1",
                database.getCollection("policy_versions").find().first().getString("_id"));

        DatabaseMigrationReport rerun = runner.apply();

        assertEquals(DatabaseMigrationReport.Status.ALREADY_APPLIED, rerun.status());
        assertEquals(1, database.getCollection("program_schema_revisions").countDocuments());
        assertEquals(1, database.getCollection("migration_runs").countDocuments());
        assertNotNull(database.getCollection("migration_runs").find().first().getString("completedAt"));
        assertHasIndex("applicants", new Document("programId", 1).append("status", 1));
        assertHasIndex("policies", new Document("programId", 1).append("archived", 1));
        assertHasIndex("evaluations", new Document("programId", 1).append("policyId", 1)
                .append("policyVersion", 1).append("evaluatedOn", 1));
        assertHasIndex("evaluations", new Document("evaluationRunId", 1));
        assertHasIndex("evaluations", new Document("applicantId", 1).append("evaluatedOn", 1));
    }

    @Test
    void reportsConflictingExistingRevisionWithoutChangingDocuments() {
        database.getCollection("programs").insertOne(legacyProgram("program-conflict"));
        database.getCollection("program_schema_revisions").insertOne(
                new Document("_id", "program-conflict:r1")
                        .append("programId", "program-conflict")
                        .append("revision", 1)
                        .append("template", "MASTERS_ADMISSIONS")
                        .append("fields", List.of()));

        DatabaseMigrationRunner runner = new DatabaseMigrationRunner(database);
        DatabaseMigrationReport preview = runner.preview();

        assertFalse(preview.compatibilityIssues().isEmpty());
        assertNull(database.getCollection("programs").find(Filters.eq("_id", "program-conflict"))
                .first().get("schemaRevision"));
        assertEquals(1, database.getCollection("program_schema_revisions").countDocuments());
        assertThrows(IllegalStateException.class, runner::apply);
        assertNull(database.getCollection("programs").find(Filters.eq("_id", "program-conflict"))
                .first().get("schemaRevision"));
        assertEquals(1, database.getCollection("program_schema_revisions").countDocuments());
        assertEquals(0, database.getCollection("migration_runs").countDocuments());
    }

    private static boolean canReachMongo(String uri) {
        try (MongoClient probeClient = MongoClients.create(uri)) {
            probeClient.getDatabase("admin").runCommand(new Document("ping", 1));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static Document legacyProgram(String id) {
        Document field = new Document("key", "score")
                .append("label", "Score")
                .append("type", "NUMBER")
                .append("required", true)
                .append("validationRules", new Document("minimum", 0.0).append("maximum", 100.0))
                .append("permittedValues", List.of())
                .append("supportedOperators", List.of("GREATER_THAN", "GREATER_THAN_OR_EQUAL",
                        "LESS_THAN", "LESS_THAN_OR_EQUAL", "EQUALS", "NOT_EQUALS"));
        return new Document("_id", id)
                .append("name", "Legacy program")
                .append("template", "MASTERS_ADMISSIONS")
                .append("archived", false)
                .append("fields", List.of(field));
    }

    private void assertHasIndex(String collectionName, Document expectedKeys) {
        List<Document> indexes = database.getCollection(collectionName).listIndexes()
                .into(new java.util.ArrayList<>());
        assertTrue(indexes.stream().anyMatch(index -> expectedKeys.equals(index.get("key", Document.class))),
                "Missing expected index on " + collectionName + ": " + expectedKeys);
    }
}
