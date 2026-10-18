
package com.ruleweaver.sync;

import com.mongodb.client.MongoDatabase;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SyncWorkerRunner implements AutoCloseable {

    private final MySqlToMongoSyncWorker worker;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "ruleweaver-mysql-sync");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicBoolean started = new AtomicBoolean(false);

    public SyncWorkerRunner(MongoDatabase database) {
        this.worker = new MySqlToMongoSyncWorker(database);
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        scheduler.scheduleWithFixedDelay(() -> {
            try {
                int processed = worker.runOnce();

                if (processed > 0) {
                    System.out.println(
                            "RuleWeaver sync: processed "
                                    + processed + " MySQL event(s).");
                }
            } catch (Exception e) {
                System.err.println(
                        "RuleWeaver sync worker failed: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
