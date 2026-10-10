package com.ruleweaver.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
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

    // Find applicant by ID (supports string or ObjectId)
    public Document findById(String id) {
        try {
            return collection.find(Filters.eq("_id", new ObjectId(id))).first();
        } catch (IllegalArgumentException e) {
            return collection.find(Filters.eq("_id", id)).first();
        }
    }

    // Retrieve all applicants (for JavaFX Dashboard)
    public List<Document> findAll() {
        return collection.find().into(new ArrayList<>());
    }

    // Update applicant fields
    public void update(String id, Document updatedFields) {
        Document fieldsToUpdate = new Document(updatedFields);
        fieldsToUpdate.remove("_id");

        try {
            collection.updateOne(
                    Filters.eq("_id", new ObjectId(id)),
                    new Document("$set", fieldsToUpdate));
        } catch (IllegalArgumentException e) {
            collection.updateOne(
                    Filters.eq("_id", id),
                    new Document("$set", fieldsToUpdate));
        }
    }

    // Delete applicant
    public void delete(String id) {
        try {
            collection.deleteOne(Filters.eq("_id", new ObjectId(id)));
        } catch (IllegalArgumentException e) {
            collection.deleteOne(Filters.eq("_id", id));
        }
    }
}