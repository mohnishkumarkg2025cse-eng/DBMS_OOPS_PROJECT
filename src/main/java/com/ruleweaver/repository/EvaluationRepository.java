package com.ruleweaver.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class EvaluationRepository {
    private final MongoCollection<Document> collection;

    // Constructor to accept MongoDatabase
    public EvaluationRepository(MongoDatabase database) {
        this.collection = database.getCollection("evaluations");
    }

    // Save evaluation result
    public void save(Document evaluation) {
        collection.insertOne(evaluation);
    }

    // Retrieve evaluation results by applicant ID
    public List<Document> findByApplicantId(String applicantId) {
        return collection.find(Filters.eq("applicantId", applicantId)).into(new ArrayList<>());
    }

    // Retrieve evaluation results by policy version ID / policy ID
    public List<Document> findByPolicyVersionId(String policyVersionId) {
        return collection.find(Filters.eq("policyVersionId", policyVersionId)).into(new ArrayList<>());
    }
}