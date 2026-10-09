package com.ruleweaver.util;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ValidationOptions;
import com.ruleweaver.config.MongoConfig;
import org.bson.Document;

import java.util.Arrays;

public class DatabaseInitializer {

    public static void main(String[] args) {
        MongoDatabase db = MongoConfig.getDatabase();
        System.out.println("Initializing MongoDB Atlas Schema & Indexes...");

        // 1. Initialize applicants collection with validation and index
        initApplicantsCollection(db);

        // 2. Initialize policies collection with validation and index
        initPoliciesCollection(db);

        // 3. Initialize policy_versions collection with compound unique index
        initPolicyVersionsCollection(db);

        // 4. Initialize evaluation_results collection with query performance indexes
        initEvaluationResultsCollection(db);

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
                .append("required", Arrays.asList("policyId", "name"))
                .append("properties", new Document()
                        .append("policyId", new Document("bsonType", "string"))
                        .append("name", new Document("bsonType", "string")))));

        db.getCollection("policies").createIndex(
                Indexes.ascending("policyId"),
                new IndexOptions().unique(true));
    }

    private static void initPolicyVersionsCollection(MongoDatabase db) {
        createCollectionIfNotExists(db, "policy_versions", new Document("$jsonSchema", new Document()
                .append("bsonType", "object")
                .append("required", Arrays.asList("policyId", "version", "rules"))
                .append("properties", new Document()
                        .append("policyId", new Document("bsonType", "string"))
                        .append("version", new Document("bsonType", "int")))));

        // Unique Compound Index on policyId + version number
        db.getCollection("policy_versions").createIndex(
                Indexes.compoundIndex(Indexes.ascending("policyId"), Indexes.ascending("version")),
                new IndexOptions().unique(true));
    }

    private static void initEvaluationResultsCollection(MongoDatabase db) {
        createCollectionIfNotExists(db, "evaluation_results", new Document("$jsonSchema", new Document()
                .append("bsonType", "object")
                .append("required", Arrays.asList("applicantId", "status"))));

        // Indexes for quick lookup of evaluations
        db.getCollection("evaluation_results").createIndex(Indexes.ascending("applicantId"));
        db.getCollection("evaluation_results").createIndex(Indexes.ascending("policyVersionId"));
    }

    private static void createCollectionIfNotExists(MongoDatabase db, String collectionName, Document validator) {
        boolean exists = db.listCollectionNames().into(new java.util.ArrayList<>()).contains(collectionName);
        if (!exists) {
            ValidationOptions valOptions = new ValidationOptions().validator(validator);
            db.createCollection(collectionName, new CreateCollectionOptions().validationOptions(valOptions));
            System.out.println("Created collection with schema validation: " + collectionName);
        } else {
            System.out.println("Collection already exists: " + collectionName + " (Applying indexes)");
        }
    }
}