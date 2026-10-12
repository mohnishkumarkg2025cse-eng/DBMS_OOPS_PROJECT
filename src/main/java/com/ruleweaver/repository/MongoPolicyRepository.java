package com.ruleweaver.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Sorts;
import com.ruleweaver.model.Policy;
import com.ruleweaver.model.PolicyVersion;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB-backed policy repository. Policy versions are stored separately and treated
 * as snapshots; saving a policy does not remove existing version documents.
 */
public class MongoPolicyRepository implements PolicyRepository {
    private final MongoCollection<Document> policies;
    private final MongoCollection<Document> versions;

    public MongoPolicyRepository(MongoDatabase database) {
        policies = database.getCollection("policies");
        versions = database.getCollection(PolicyVersionRepository.COLLECTION_NAME);
        versions.createIndex(Indexes.ascending("policyId", "version"), new IndexOptions().unique(true));
    }

    @Override
    public Policy save(Policy policy) {
        if (policy == null || policy.getId() == null || policy.getId().isBlank()) {
            throw new IllegalArgumentException("Policy and policy ID are required.");
        }
        Document document = new Document("_id", policy.getId())
                .append("name", policy.getName())
                .append("description", policy.getDescription())
                .append("category", policy.getCategory())
                .append("programId", policy.getProgramId())
                .append("activeVersionNumber", policy.getPersistedActiveVersionNumber())
                .append("archived", policy.isArchived());
        policies.replaceOne(Filters.eq("_id", policy.getId()), document, new ReplaceOptions().upsert(true));
        return policy;
    }

    @Override
    public Optional<Policy> findById(String id) {
        Document document = policies.find(Filters.eq("_id", id)).first();
        if (document == null) {
            return Optional.empty();
        }
        Policy policy = toPolicy(document);
        List<PolicyVersion> policyVersions = versions.find(Filters.eq("policyId", id))
                .sort(Sorts.ascending("version"))
                .map(PolicyVersionDocumentMapper::fromDocument)
                .into(new ArrayList<>());
        policy.setVersions(policyVersions);
        return Optional.of(policy);
    }

    @Override
    public List<Policy> findAll() {
        List<Policy> result = new ArrayList<>();
        for (Document document : policies.find()) {
            findById(document.getString("_id")).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public boolean deleteById(String id) {
        return policies.updateOne(Filters.eq("_id", id),
                new Document("$set", new Document("archived", true))).getMatchedCount() > 0;
    }

    @Override
    public Optional<PolicyVersion> findVersion(String policyId, int versionNumber) {
        Document document = versions.find(Filters.and(
                Filters.eq("policyId", policyId),
                Filters.eq("version", versionNumber))).first();
        return Optional.ofNullable(document).map(this::toPolicyVersion);
    }

    @Override
    public Optional<PolicyVersion> findLatestVersion(String policyId) {
        Document document = versions.find(Filters.eq("policyId", policyId))
                .sort(Sorts.descending("version"))
                .first();
        return Optional.ofNullable(document).map(this::toPolicyVersion);
    }

    @Override
    public void saveVersion(String policyId, PolicyVersion version) {
        if (policyId == null || version == null) {
            throw new IllegalArgumentException("Policy ID and policy version are required.");
        }
        if (version.getVersionNumber() <= 0) {
            throw new IllegalArgumentException("Version number must be greater than zero.");
        }
        if (findVersion(policyId, version.getVersionNumber()).isPresent()) {
            throw new IllegalStateException("Policy version " + version.getVersionNumber()
                    + " already exists for policy " + policyId + "; versions are immutable.");
        }
        version.setPolicyId(policyId);
        Document policyDocument = policies.find(Filters.eq("_id", policyId)).first();
        if (policyDocument != null && version.getProgramId() == null) {
            version.setProgramId(policyDocument.getString("programId"));
        }
        String id = version.getId() == null || version.getId().isBlank()
                ? policyId + "_v" + version.getVersionNumber()
                : version.getId();
        version.setId(id);
        Document document = PolicyVersionDocumentMapper.toDocument(version);
        try {
            versions.insertOne(document);
        } catch (com.mongodb.MongoWriteException e) {
            if (e.getError().getCategory() == com.mongodb.ErrorCategory.DUPLICATE_KEY) {
                throw new IllegalStateException("Policy version " + version.getVersionNumber()
                        + " already exists for policy " + policyId + "; versions are immutable.", e);
            }
            throw e;
        }
    }

    private Policy toPolicy(Document document) {
        Policy policy = new Policy(
                document.getString("_id"),
                document.getString("name"),
                document.getString("description"));
        policy.setCategory(document.getString("category"));
        policy.setProgramId(document.getString("programId"));
        Integer activeVersion = document.getInteger("activeVersionNumber");
        policy.setActiveVersionNumber(activeVersion == null ? 0 : activeVersion);
        policy.setArchived(Boolean.TRUE.equals(document.getBoolean("archived")));
        return policy;
    }

    private PolicyVersion toPolicyVersion(Document document) {
        PolicyVersion version = PolicyVersionDocumentMapper.fromDocument(document);
        if (version.getProgramId() == null) {
            Document policy = policies.find(Filters.eq("_id", version.getPolicyId())).first();
            if (policy != null) version.setProgramId(policy.getString("programId"));
        }
        return version;
    }

}
