package com.ruleweaver.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.ruleweaver.model.ProgramConfiguration;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MongoProgramRepository implements ProgramRepository {
    public static final String COLLECTION_NAME = "programs";
    private final MongoCollection<Document> collection;

    public MongoProgramRepository(MongoDatabase database) {
        collection = database.getCollection(COLLECTION_NAME);
        collection.createIndex(Indexes.ascending("name"), new IndexOptions());
    }

    @Override
    public ProgramConfiguration save(ProgramConfiguration program) {
        Document document = ProgramDocumentMapper.toDocument(program);
        collection.replaceOne(new Document("_id", program.getId()), document, new ReplaceOptions().upsert(true));
        return program.copy();
    }

    @Override
    public Optional<ProgramConfiguration> findById(String id) {
        Document document = collection.find(new Document("_id", id)).first();
        return Optional.ofNullable(document).map(ProgramDocumentMapper::fromDocument);
    }

    @Override
    public List<ProgramConfiguration> findAll() {
        return collection.find().sort(Sorts.ascending("name"))
                .map(ProgramDocumentMapper::fromDocument).into(new ArrayList<>());
    }
}
