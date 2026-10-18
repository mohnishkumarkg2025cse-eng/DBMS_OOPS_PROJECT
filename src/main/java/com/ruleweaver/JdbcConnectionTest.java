
package com.ruleweaver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.util.Properties;

public class JdbcConnectionTest {

    public static void main(String[] args) {
        System.out.println("JDBC TEST STARTED");
        String url = System.getenv("MONGODB_JDBC_URL");
        String username = System.getenv("MONGODB_JDBC_USER");
        String password = System.getenv("MONGODB_JDBC_PASSWORD");

        if (url == null || url.isBlank()) {
            System.err.println(
                "MONGODB_JDBC_URL environment variable is not set."
            );
            return;
        }

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            System.err.println(
                "JDBC username or password environment variable is missing."
            );
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("database", "ruleweaver_db");
        properties.setProperty("user", username);
        properties.setProperty("password", password);

        try (Connection connection =
                 DriverManager.getConnection(url, properties)) {

            DatabaseMetaData metadata = connection.getMetaData();

            System.out.println("JDBC connection successful!");
            System.out.println(
                "Database: " + metadata.getDatabaseProductName()
            );
            System.out.println(
                "JDBC Driver: " + metadata.getDriverName()
            );

        } catch (Exception e) {
            System.err.println("JDBC connection failed.");
            System.err.println(e.getMessage());
        }
    }
}