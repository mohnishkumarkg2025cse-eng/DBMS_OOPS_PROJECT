package com.ruleweaver.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.ruleweaver.model.ProgramSchemaRevision;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MongoProgramSchemaRevisionRepository implements ProgramSchemaRevisionRepository {
    public static final String COLLECTION_NAME = "program_schema_revisions";
    private final MongoCollection<Document> collection;

    public MongoProgramSchemaRevisionRepository(MongoDatabase database) {
        collection = database.getCollection(COLLECTION_NAME);
        collection.createIndex(Indexes.compoundIndex(
                Indexes.ascending("programId"), Indexes.ascending("revision")), new IndexOptions().unique(true));
    }

    @Override
    public ProgramSchemaRevision save(ProgramSchemaRevision revision) {
        collection.insertOne(ProgramSchemaRevisionDocumentMapper.toDocument(revision));
        return revision;
    }

    @Override
    public Optional<ProgramSchemaRevision> findByProgramIdAndRevision(String programId, int revision) {
        Document document = collection.find(Filters.and(
                Filters.eq("programId", programId), Filters.eq("revision", revision))).first();
        return Optional.ofNullable(document).map(ProgramSchemaRevisionDocumentMapper::fromDocument);
    }

    @Override
    public List<ProgramSchemaRevision> findByProgramId(String programId) {
        return collection.find(Filters.eq("programId", programId))
                .sort(Sorts.ascending("revision"))
                .map(ProgramSchemaRevisionDocumentMapper::fromDocument)
                .into(new ArrayList<>());
    }
}
