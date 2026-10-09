package com.ruleweaver.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.ruleweaver.config.MongoConfig;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class PolicyRepository {
    private final MongoCollection<Document> collection;

    public PolicyRepository() {
        MongoDatabase db = MongoConfig.getDatabase();
        this.collection = db.getCollection("policies");
    }

    // Save policy
    public void save(Document policy) {
        collection.insertOne(policy);
    }

    // Find policy by ID
    // Find policy by MongoDB _id or application-level policyId
    public Document findById(String id) {
        Document policy = null;

        try {
            policy = collection.find(
                    Filters.eq("_id", new ObjectId(id))).first();
        } catch (IllegalArgumentException ignored) {
            // The supplied ID is not a valid ObjectId string.
        }

        if (policy == null) {
            policy = collection.find(
                    Filters.eq("_id", id)).first();
        }

        if (policy == null) {
            policy = collection.find(
                    Filters.eq("policyId", id)).first();
        }

        return policy;
    }

    // Retrieve all policies
    public List<Document> findAll() {
        return collection.find().into(new ArrayList<>());
    }
}