package com.ruleweaver.util;

import com.mongodb.MongoWriteException;
import com.mongodb.ErrorCategory;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.ruleweaver.model.ProgramConfiguration;
import com.ruleweaver.model.ProgramSchemaRevision;
import com.ruleweaver.repository.EvaluationRepository;
import com.ruleweaver.repository.MongoProgramSchemaRevisionRepository;
import com.ruleweaver.repository.PolicyVersionRepository;
import com.ruleweaver.repository.ProgramDocumentMapper;
import com.ruleweaver.repository.ProgramSchemaRevisionDocumentMapper;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DatabaseMigrationRunner {
    private static final String RUNS_COLLECTION = "migration_runs";
    private static final String MIGRATION_ID = "program-schema-revision-v1";

    private final MongoDatabase database;

    public DatabaseMigrationRunner(MongoDatabase database) {
        this.database = Objects.requireNonNull(database, "Mongo database is required.");
    }

    public DatabaseMigrationReport preview() {
        Analysis analysis = analyze();
        return report(DatabaseMigrationReport.Mode.DRY_RUN, DatabaseMigrationReport.Status.PREVIEWED,
                analysis, countCollections(), analysis.revisionsToPrepare().size());
    }

    public DatabaseMigrationReport apply() {
        Analysis analysis = analyze();
        if (!analysis.compatibilityIssues().isEmpty()) {
            throw new IllegalStateException("Migration preflight failed: "
                    + String.join("; ", analysis.compatibilityIssues()));
        }

        MongoCollection<Document> runs = database.getCollection(RUNS_COLLECTION);
        Document completed = runs.find(Filters.eq("_id", MIGRATION_ID)).first();
        if (completed != null && "COMPLETED".equals(completed.getString("status"))
                && analysis.revisionsToPrepare().isEmpty()) {
            DatabaseMigrationReport.CollectionCounts counts = countCollections();
            return report(DatabaseMigrationReport.Mode.APPLY, DatabaseMigrationReport.Status.ALREADY_APPLIED,
                    analysis, counts, 0);
        }

        DatabaseMigrationReport.CollectionCounts before = countCollections();
        ensureIndexes(database);
        int revisionsPrepared = 0;
        for (Document programDocument : analysis.revisionsToPrepare()) {
            ensureRevision(programDocument);
            revisionsPrepared++;
        }
        DatabaseMigrationReport.CollectionCounts after = countCollections();
        DatabaseMigrationReport report = report(DatabaseMigrationReport.Mode.APPLY,
                DatabaseMigrationReport.Status.APPLIED, analysis, before, after, revisionsPrepared);
        runs.replaceOne(Filters.eq("_id", MIGRATION_ID),
                new Document("_id", MIGRATION_ID)
                        .append("status", "COMPLETED")
                        .append("completedAt", LocalDateTime.now().toString())
                        .append("report", report.toDocument()),
                new ReplaceOptions().upsert(true));
        return report;
    }

    public static void ensureIndexes(MongoDatabase database) {
        database.getCollection("applicants").createIndex(Indexes.ascending("programId", "status"));
        database.getCollection("policies").createIndex(Indexes.ascending("programId", "archived"));
        database.getCollection(PolicyVersionRepository.COLLECTION_NAME).createIndex(
                Indexes.ascending("policyId", "version"), new IndexOptions().unique(true));
        database.getCollection(PolicyVersionRepository.COLLECTION_NAME).createIndex(
                Indexes.ascending("programId", "policyId", "version"));
        database.getCollection(EvaluationRepository.COLLECTION_NAME).createIndex(
                Indexes.ascending("programId", "policyId", "policyVersion", "evaluatedOn"));
        database.getCollection(EvaluationRepository.COLLECTION_NAME).createIndex(
                Indexes.ascending("evaluationRunId"));
        database.getCollection(EvaluationRepository.COLLECTION_NAME).createIndex(
                Indexes.ascending("policyVersionId"));
        database.getCollection(EvaluationRepository.COLLECTION_NAME).createIndex(
                Indexes.ascending("applicantId", "evaluatedOn"));
        database.getCollection(MongoProgramSchemaRevisionRepository.COLLECTION_NAME).createIndex(
                Indexes.ascending("programId", "revision"), new IndexOptions().unique(true));
    }

    private Analysis analyze() {
        DatabaseMigrationReport.CollectionCounts counts = countCollections();
        List<Document> programs = database.getCollection("programs").find().into(new ArrayList<>());
        List<Document> revisionsToPrepare = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        for (Document document : programs) {
            Object rawRevision = document.get("schemaRevision");
            if (rawRevision == null || isZeroRevision(rawRevision)) {
                ProgramConfiguration configuration;
                try {
                    configuration = ProgramDocumentMapper.fromDocument(document);
                } catch (RuntimeException exception) {
                    issues.add("Program " + document.get("_id") + " has an invalid schema definition: "
                            + exception.getMessage());
                    continue;
                }
                Document existing = database.getCollection(MongoProgramSchemaRevisionRepository.COLLECTION_NAME)
                        .find(Filters.and(Filters.eq("programId", configuration.getId()),
                                Filters.eq("revision", 1))).first();
                if (existing != null && !sameSchema(existing, configuration, 1)) {
                    issues.add("Program " + configuration.getId()
                            + " already has a conflicting schema revision 1; no data will be changed.");
                } else {
                    revisionsToPrepare.add(document);
                }
            } else if (!(rawRevision instanceof Number number) || !isWholeNumber(number)) {
                issues.add("Program " + document.get("_id") + " has an invalid schemaRevision.");
            } else {
                int revision = ((Number) rawRevision).intValue();
                if (revision < 0) {
                    issues.add("Program " + document.get("_id") + " has a negative schemaRevision.");
                    continue;
                }
                if (revision > 0 && database.getCollection(MongoProgramSchemaRevisionRepository.COLLECTION_NAME)
                        .find(Filters.and(Filters.eq("programId", document.get("_id").toString()),
                                Filters.eq("revision", revision))).first() == null) {
                    issues.add("Program " + document.get("_id") + " references missing schema revision " + revision
                            + "; no data will be changed.");
                }
            }
        }
        return new Analysis(counts, revisionsToPrepare, issues,
                countMissing("applicants", "programId"),
                countMissing("policies", "programId"),
                countMissing("evaluations", "programId"));
    }

    private void ensureRevision(Document programDocument) {
        ProgramConfiguration configuration = ProgramDocumentMapper.fromDocument(programDocument);
        ProgramSchemaRevision revision = new ProgramSchemaRevision(
                configuration.getId() + ":r1",
                configuration.getId(),
                1,
                configuration.getTemplate(),
                configuration.getFields(),
                LocalDateTime.now());
        MongoCollection<Document> revisions = database.getCollection(
                MongoProgramSchemaRevisionRepository.COLLECTION_NAME);
        Document revisionDocument = ProgramSchemaRevisionDocumentMapper.toDocument(revision);
        try {
            revisions.insertOne(revisionDocument);
        } catch (MongoWriteException exception) {
            if (exception.getError().getCategory() != ErrorCategory.DUPLICATE_KEY) throw exception;
            Document existing = revisions.find(Filters.and(
                    Filters.eq("programId", configuration.getId()), Filters.eq("revision", 1))).first();
            if (existing == null || !sameSchema(existing, configuration, 1)) {
                throw new IllegalStateException("Conflicting schema revision 1 for program "
                        + configuration.getId() + ".", exception);
            }
        }
        database.getCollection("programs").updateOne(
                Filters.and(Filters.eq("_id", configuration.getId()),
                        Filters.or(Filters.exists("schemaRevision", false), Filters.eq("schemaRevision", 0)),
                        Filters.eq("template", configuration.getTemplate().name()),
                        Filters.eq("fields", programDocument.get("fields"))),
                new Document("$set", new Document("schemaRevision", 1)));
        Document savedProgram = database.getCollection("programs")
                .find(Filters.eq("_id", configuration.getId())).first();
        if (savedProgram == null || !Integer.valueOf(1).equals(savedProgram.getInteger("schemaRevision"))
                || !configuration.getTemplate().name().equals(savedProgram.getString("template"))
                || !sameFields(configuration,
                        ProgramDocumentMapper.fromDocument(savedProgram))) {
            throw new IllegalStateException("Could not set current schema revision for program "
                    + configuration.getId() + ".");
        }
    }

    private static boolean sameSchema(Document revision, ProgramConfiguration configuration, int number) {
        return configuration.getId().equals(revision.getString("programId"))
                && number == revision.getInteger("revision", 0)
                && configuration.getTemplate().name().equals(revision.getString("template"))
                && ProgramDocumentMapper.toDocument(configuration).getList("fields", Document.class, List.of())
                        .equals(revision.getList("fields", Document.class, List.of()));
    }

    private static boolean sameFields(ProgramConfiguration first, ProgramConfiguration second) {
        return ProgramDocumentMapper.toDocument(first).getList("fields", Document.class, List.of())
                .equals(ProgramDocumentMapper.toDocument(second).getList("fields", Document.class, List.of()));
    }

    private static boolean isZeroRevision(Object revision) {
        return revision instanceof Number number && number.doubleValue() == 0.0d;
    }

    private static boolean isWholeNumber(Number revision) {
        return Double.isFinite(revision.doubleValue())
                && revision.doubleValue() == Math.rint(revision.doubleValue())
                && revision.doubleValue() <= Integer.MAX_VALUE
                && revision.doubleValue() >= Integer.MIN_VALUE;
    }

    private DatabaseMigrationReport report(DatabaseMigrationReport.Mode mode,
                                           DatabaseMigrationReport.Status status,
                                           Analysis analysis,
                                           DatabaseMigrationReport.CollectionCounts after,
                                           int prepared) {
        return report(mode, status, analysis, analysis.counts(), after, prepared);
    }

    private DatabaseMigrationReport report(DatabaseMigrationReport.Mode mode,
                                           DatabaseMigrationReport.Status status,
                                           Analysis analysis,
                                           DatabaseMigrationReport.CollectionCounts before,
                                           DatabaseMigrationReport.CollectionCounts after,
                                           int prepared) {
        return new DatabaseMigrationReport(mode, status, before, after,
                analysis.legacyUnassignedApplicants(),
                analysis.legacyUnassignedPolicies(),
                analysis.legacyEvaluations(),
                prepared,
                analysis.compatibilityIssues());
    }

    private DatabaseMigrationReport.CollectionCounts countCollections() {
        return new DatabaseMigrationReport.CollectionCounts(
                database.getCollection("applicants").countDocuments(),
                database.getCollection("policies").countDocuments(),
                database.getCollection(PolicyVersionRepository.COLLECTION_NAME).countDocuments(),
                database.getCollection(EvaluationRepository.COLLECTION_NAME).countDocuments(),
                database.getCollection("programs").countDocuments(),
                database.getCollection(MongoProgramSchemaRevisionRepository.COLLECTION_NAME).countDocuments());
    }

    private long countMissing(String collection, String field) {
        return database.getCollection(collection).countDocuments(
                Filters.or(Filters.exists(field, false), Filters.eq(field, null)));
    }

    private record Analysis(
            DatabaseMigrationReport.CollectionCounts counts,
            List<Document> revisionsToPrepare,
            List<String> compatibilityIssues,
            long legacyUnassignedApplicants,
            long legacyUnassignedPolicies,
            long legacyEvaluations) {
    }
}
