package com.ruleweaver.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoConfig {
    private static final String CONNECTION_STRING = "mongodb+srv://mohnishkumarkg_db_user:RuleWeaver2026@cluster0.wxxzxjx.mongodb.net/?appName=Cluster0";
    private static final String DATABASE_NAME = "ruleweaver_db";
    private static MongoClient mongoClient = null;

    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
            mongoClient = MongoClients.create(CONNECTION_STRING);
        }
        return mongoClient.getDatabase(DATABASE_NAME);
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }
}