package com.ruleweaver.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.ruleweaver.model.EvaluationResult;
import com.ruleweaver.ui.adapter.EvaluationResultDocumentMapper;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class EvaluationRepository implements EvaluationResultRepository {
    public static final String COLLECTION_NAME = "evaluations";
    private final MongoCollection<Document> collection;

    public EvaluationRepository(MongoDatabase database) {
        this.collection = database.getCollection(COLLECTION_NAME);
    }

    public void save(Document evaluation) {
        collection.insertOne(evaluation);
    }

    @Override
    public EvaluationResult save(EvaluationResult evaluation) {
        saveAll(List.of(evaluation));
        return evaluation;
    }

    @Override
    public List<EvaluationResult> saveAll(List<EvaluationResult> evaluations) {
        if (evaluations == null || evaluations.isEmpty()) {
            return List.of();
        }
        List<Document> documents = evaluations.stream()
                .map(EvaluationResultDocumentMapper::toDocument)
                .toList();
        collection.insertMany(documents);
        return evaluations;
    }

    public List<Document> findByApplicantId(String applicantId) {
        return collection.find(Filters.eq("applicantId", applicantId)).into(new ArrayList<>());
    }

    public List<Document> findByPolicyVersionId(String policyVersionId) {
        int marker = policyVersionId == null ? -1 : policyVersionId.lastIndexOf("_v");
        if (marker > 0) {
            try {
                String policyId = policyVersionId.substring(0, marker);
                int version = Integer.parseInt(policyVersionId.substring(marker + 2));
                return collection.find(Filters.or(
                                Filters.eq("policyVersionId", policyVersionId),
                                Filters.and(Filters.eq("policyId", policyId), Filters.eq("policyVersion", version))))
                        .into(new ArrayList<>());
            } catch (NumberFormatException ignored) {
                // Fall through to the legacy identifier field.
            }
        }
        return collection.find(Filters.eq("policyVersionId", policyVersionId)).into(new ArrayList<>());
    }

    @Override
    public List<EvaluationResult> findByPolicyAndVersion(String policyId, int versionNumber) {
        String policyVersionId = policyId + "_v" + versionNumber;
        return collection.find(Filters.or(
                        Filters.and(Filters.eq("policyId", policyId),
                                Filters.eq("policyVersion", versionNumber)),
                        Filters.eq("policyVersionId", policyVersionId)))
                .sort(Sorts.ascending("evaluatedOn"))
                .map(EvaluationResultDocumentMapper::fromDocument)
                .into(new ArrayList<>());
    }

    public List<EvaluationResult> findResultsByApplicantId(String applicantId) {
        return findByApplicantId(applicantId).stream()
                .map(EvaluationResultDocumentMapper::fromDocument)
                .toList();
    }

    @Override
    public List<EvaluationResult> findAll() {
        return collection.find()
                .sort(Sorts.ascending("evaluatedOn"))
                .map(EvaluationResultDocumentMapper::fromDocument)
                .into(new ArrayList<>());
    }

    @Override
    public long countDecisions() {
        return collection.countDocuments();
    }

    @Override
    public long countByPolicyAndVersion(String policyId, int versionNumber) {
        String policyVersionId = policyId + "_v" + versionNumber;
        return collection.countDocuments(Filters.or(
                Filters.and(Filters.eq("policyId", policyId),
                        Filters.eq("policyVersion", versionNumber)),
                Filters.eq("policyVersionId", policyVersionId)));
    }

}