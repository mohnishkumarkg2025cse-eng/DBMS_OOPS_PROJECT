package com.ruleweaver.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class ApplicantRepository {
    private final MongoCollection<Document> collection;

    // Updated constructor to accept MongoDatabase injection
    public ApplicantRepository(MongoDatabase database) {
        this.collection = database.getCollection("applicants");
    }

    // Save a new applicant
    public void save(Document applicant) {
        collection.insertOne(applicant);
    }

    public void upsert(Document applicant) {
        Object id = applicant.get("_id");
        if (id == null) {
            throw new IllegalArgumentException("Applicant upsert requires an _id.");
        }
        collection.replaceOne(Filters.eq("_id", id), applicant, new ReplaceOptions().upsert(true));
    }

    // Find applicant by ID (supports string or ObjectId)
    public Document findById(String id) {
        return collection.find(Filters.eq("_id", mongoId(id))).first();
    }

    // Retrieve all applicants (for JavaFX Dashboard)
    public List<Document> findAll() {
        return collection.find(Filters.ne("_sync_deleted", true))
                .into(new ArrayList<>());
    }

    public long count() {
        return collection.countDocuments();
    }

    // Update applicant fields
    public void update(String id, Document updatedFields) {
        Document fieldsToUpdate = new Document(updatedFields);
        fieldsToUpdate.remove("_id");

        UpdateResult result = collection.updateOne(
                Filters.eq("_id", mongoId(id)),
                new Document("$set", fieldsToUpdate));
        requireUpdatedApplicant(result, id);
    }

    // Delete applicant
    public void delete(String id) {
        DeleteResult result = collection.deleteOne(Filters.eq("_id", mongoId(id)));
        requireDeletedApplicant(result, id);
    }

    private static Object mongoId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Applicant ID is required.");
        }
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException e) {
            return id;
        }
    }

    private static void requireUpdatedApplicant(UpdateResult result, String id) {
        if (result.getMatchedCount() == 0) {
            throw new IllegalArgumentException("Applicant not found for update: " + id);
        }
    }

    private static void requireDeletedApplicant(DeleteResult result, String id) {
        if (result.getDeletedCount() == 0) {
            throw new IllegalArgumentException("Applicant not found for deletion: " + id);
        }
    }
}