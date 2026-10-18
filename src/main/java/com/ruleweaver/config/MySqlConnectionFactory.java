
package com.ruleweaver.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruleweaver.config.MySqlConnectionFactory;
import com.ruleweaver.model.Applicant;

public final class MySqlConnectionFactory {

    private static final String URL = "jdbc:mysql://localhost:3306/ruleweaver_sql?serverTimezone=UTC";

    private MySqlConnectionFactory() {
        // Utility class
    }

    public static Connection getConnection() throws SQLException {
        String username = System.getenv("MYSQL_USER");
        String password = System.getenv("MYSQL_PASSWORD");

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new SQLException(
                    "MySQL credentials are missing. Set MYSQL_USER "
                            + "and MYSQL_PASSWORD in the application environment.");
        }

        return DriverManager.getConnection(URL, username, password);
    }
}
