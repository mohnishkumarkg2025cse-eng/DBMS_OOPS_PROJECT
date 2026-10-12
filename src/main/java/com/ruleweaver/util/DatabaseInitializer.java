package com.ruleweaver.util;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ValidationOptions;
import com.ruleweaver.config.MongoConfig;
import com.ruleweaver.repository.EvaluationRepository;
import com.ruleweaver.repository.PolicyVersionRepository;
import com.ruleweaver.repository.MongoProgramRepository;
import org.bson.Document;

import java.util.Arrays;
import java.util.List;

public class DatabaseInitializer {

    public static void main(String[] args) {
        MongoDatabase db = MongoConfig.getDatabase();
        System.out.println("Initializing MongoDB Atlas Schema & Indexes...");

        // 1. Initialize applicants collection with validation and index
        initApplicantsCollection(db);

        // 2. Initialize policies collection with validation and index
        initPoliciesCollection(db);

        // 3. Initialize reusable program configurations
        createCollectionIfNotExists(db, MongoProgramRepository.COLLECTION_NAME, programValidator());

        // 4. Initialize policy_versions collection with compound unique index
        initPolicyVersionsCollection(db);

        // 5. Initialize evaluations collection with query performance indexes
        initEvaluationsCollection(db);

        System.out.println("Database initialization completed successfully!");
    }

    private static void initApplicantsCollection(MongoDatabase db) {
        createCollectionIfNotExists(db, "applicants", new Document("$jsonSchema", new Document()
                .append("bsonType", "object")
                .append("required", Arrays.asList("name", "gpa"))
                .append("properties", new Document()
                        .append("name",
                                new Document("bsonType", "string").append("description",
                                        "must be a string and is required"))
                        .append("gpa", new Document("bsonType", Arrays.asList("double", "int", "decimal"))
                                .append("description", "must be numeric and is required")))));

        // Index: Unique lookup on applicantId (if custom field) or default _id
        db.getCollection("applicants").createIndex(
                Indexes.ascending("applicantId"),
                new IndexOptions().unique(true).sparse(true));
    }

    private static void initPoliciesCollection(MongoDatabase db) {
        createCollectionIfNotExists(db, "policies", new Document("$jsonSchema", new Document()
                .append("bsonType", "object")
                .append("required", Arrays.asList("_id", "name"))
                .append("properties", new Document()
                        .append("_id", new Document("bsonType", "string"))
                        .append("name", new Document("bsonType", "string"))
                        .append("description", new Document("bsonType", Arrays.asList("string", "null")))
                        .append("category", new Document("bsonType", Arrays.asList("string", "null")))
                        .append("programId", new Document("bsonType", Arrays.asList("string", "null")))
                        .append("activeVersionNumber", new Document("bsonType", "int"))
                        .append("archived", new Document("bsonType", "bool")))));
    }

    static Document programValidator() {
        Document schema = new Document("bsonType", "object")
                .append("required", List.of("_id", "name", "template", "fields"))
                .append("properties", new Document()
                        .append("_id", new Document("bsonType", "string"))
                        .append("name", new Document("bsonType", "string"))
                        .append("template", new Document("bsonType", "string"))
                        .append("archived", new Document("bsonType", "bool"))
                        .append("fields", new Document("bsonType", "array")));
        return new Document("$jsonSchema", schema);
    }

    private static void initPolicyVersionsCollection(MongoDatabase db) {
        createCollectionIfNotExists(db, PolicyVersionRepository.COLLECTION_NAME, policyVersionValidator());

        // Unique Compound Index on policyId + version number
        db.getCollection(PolicyVersionRepository.COLLECTION_NAME).createIndex(
                Indexes.compoundIndex(Indexes.ascending("policyId"), Indexes.ascending("version")),
                new IndexOptions().unique(true));
    }

    static Document evaluationValidator() {
        Document properties = new Document()
                .append("applicantId", new Document("bsonType", "string"))
                .append("applicantName", new Document("bsonType", Arrays.asList("string", "null")))
                .append("policyId", new Document("bsonType", "string"))
                .append("policyName", new Document("bsonType", Arrays.asList("string", "null")))
                .append("policyVersionId", new Document("bsonType", Arrays.asList("string", "null")))
                .append("evaluationRunId", new Document("bsonType", Arrays.asList("string", "null")))
                .append("policyVersion", new Document("bsonType", "int"))
                .append("overallDecision", new Document("bsonType", "bool"))
                .append("evaluatedOn", new Document("bsonType", Arrays.asList("string", "null")))
                .append("conditionResults", new Document("bsonType", "array"));
        Document schema = new Document("bsonType", "object")
                .append("required", List.of("applicantId"))
                .append("properties", properties);
        return new Document("$jsonSchema", schema);
    }

    static Document policyVersionValidator() {
        Document properties = new Document()
                .append("_id", new Document("bsonType", Arrays.asList("string", "objectId")))
                .append("policyId", new Document("bsonType", "string"))
                .append("version", new Document("bsonType", "int"))
                .append("description", new Document("bsonType", Arrays.asList("string", "null")))
                .append("createdAt", new Document("bsonType", Arrays.asList("string", "null")))
                .append("conditions", new Document("bsonType", "array"))
                .append("rules", new Document("bsonType", "array"));
        Document schema = new Document("bsonType", "object")
                .append("required", Arrays.asList("policyId", "version"))
                .append("properties", properties);
        return new Document("$jsonSchema", schema);
    }

    private static void initEvaluationsCollection(MongoDatabase db) {
        createCollectionIfNotExists(db, EvaluationRepository.COLLECTION_NAME, evaluationValidator());
        db.getCollection(EvaluationRepository.COLLECTION_NAME).createIndex(Indexes.ascending("applicantId"));
        db.getCollection(EvaluationRepository.COLLECTION_NAME).createIndex(Indexes.ascending("policyVersionId"));
        db.getCollection(EvaluationRepository.COLLECTION_NAME).createIndex(
                Indexes.ascending("policyId", "policyVersion"));
    }

    private static void createCollectionIfNotExists(MongoDatabase db, String collectionName, Document validator) {
        boolean exists = db.listCollectionNames().into(new java.util.ArrayList<>()).contains(collectionName);
        if (!exists) {
            ValidationOptions valOptions = new ValidationOptions().validator(validator);
            db.createCollection(collectionName, new CreateCollectionOptions().validationOptions(valOptions));
            System.out.println("Created collection with schema validation: " + collectionName);
        } else {
            db.runCommand(new Document("collMod", collectionName)
                    .append("validator", validator)
                    .append("validationLevel", "moderate"));
            System.out.println("Collection already exists: " + collectionName + " (Updated validator)");
        }
    }
}