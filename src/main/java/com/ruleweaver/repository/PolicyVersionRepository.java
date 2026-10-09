
package com.ruleweaver.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.ruleweaver.config.MongoConfig;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class PolicyVersionRepository {

    private final MongoCollection<Document> collection;

    public PolicyVersionRepository() {
        MongoDatabase db = MongoConfig.getDatabase();
        this.collection = db.getCollection("policy_versions");

        // Prevent duplicate version numbers for the same policy
        createIndexes();
    }

    // Create database indexes
    private void createIndexes() {
        collection.createIndex(
                Indexes.ascending("policyId", "version"),
                new IndexOptions().unique(true));
    }

    // Save a new policy version
    public void save(Document policyVersion) {
        collection.insertOne(policyVersion);
    }

    // Alias method to support existing SeedData calls
    public void saveVersion(Document policyVersion) {
        save(policyVersion);
    }

    // Find a specific version of a policy
    public Document findByPolicyIdAndVersion(String policyId, int version) {
        return collection.find(
                Filters.and(
                        Filters.eq("policyId", policyId),
                        Filters.eq("version", version)))
                .first();
    }

    // Retrieve all versions belonging to a policy
    public List<Document> findByPolicyId(String policyId) {
        return collection.find(
                Filters.eq("policyId", policyId)).into(new ArrayList<>());
    }

    // Retrieve all versions, ordered by policy ID and version number
    public List<Document> findAll() {
        return collection.find()
                .sort(Indexes.ascending("policyId", "version"))
                .into(new ArrayList<>());
    }
}
