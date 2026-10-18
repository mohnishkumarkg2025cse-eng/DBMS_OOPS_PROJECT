
package com.ruleweaver.sync;

import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.ruleweaver.model.Applicant;
import com.ruleweaver.repository.ApplicantMySqlRepository;
import com.ruleweaver.ui.adapter.ApplicantDocumentMapper;

import org.bson.Document;
import org.bson.BsonValue;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MongoToMySqlChangeStreamListener
        implements AutoCloseable {

    private final MongoCollection<Document> applicants;
    private final ApplicantMySqlRepository mysqlRepository;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread listenerThread;

    public MongoToMySqlChangeStreamListener(MongoDatabase database) {
        this.applicants = database.getCollection("applicants");
        this.mysqlRepository = new ApplicantMySqlRepository();
    }

    public synchronized void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        listenerThread = new Thread(
                this::listen,
                "ruleweaver-mongo-change-stream");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listen() {
        while (running.get()) {
            try {
                ChangeStreamIterable<Document> stream = applicants.watch()
                        .fullDocument(FullDocument.UPDATE_LOOKUP);

                for (ChangeStreamDocument<Document> change : stream) {
                    if (!running.get()) {
                        break;
                    }

                    processChange(change);
                }
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println(
                            "MongoDB change stream failed: " + e.getMessage());

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    private void processChange(ChangeStreamDocument<Document> change) {
        String operation = change.getOperationType().getValue();

        if ("delete".equals(operation)) {
            BsonValue idValue = change.getDocumentKey() == null
                    ? null
                    : change.getDocumentKey().get("_id");

            if (idValue != null) {
                String id = idValue.isObjectId()
                        ? idValue.asObjectId().getValue().toHexString()
                        : idValue.toString();

                try {
                    mysqlRepository.markDeletedByMongoId(id);
                } catch (SQLException e) {
                    System.err.println(
                            "Could not sync deleted applicant " + id + ": "
                                    + e.getMessage());
                }
            }
            return;
        }

        Document document = change.getFullDocument();

        if (document == null) {
            return;
        }

        // Ignore updates written by the MySQL -> MongoDB worker.
        if ("mysql".equals(document.getString("_sync_origin"))) {
            return;
        }

        if (Boolean.TRUE.equals(document.getBoolean("_sync_deleted"))) {
            String id = document.get("_id").toString();
            try {
                mysqlRepository.markDeletedByMongoId(id);
            } catch (SQLException e) {
                System.err.println(
                        "Could not sync applicant tombstone: " + e.getMessage());
            }
            return;
        }

        Applicant applicant = ApplicantDocumentMapper.toApplicant(document);

        try {
            mysqlRepository.upsert(applicant);
        } catch (SQLException e) {
            System.err.println(
                    "Could not sync applicant " + applicant.getId() + ": "
                            + e.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        running.set(false);

        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
    }
}
