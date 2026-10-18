package com.ruleweaver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class JdbcSqlQueryTest {

    public static void main(String[] args) {

        String url = System.getenv("MONGODB_JDBC_URL");
        String username = System.getenv("MONGODB_JDBC_USER");
        String password = System.getenv("MONGODB_JDBC_PASSWORD");

        if (url == null || username == null || password == null) {
            System.err.println("JDBC environment variables are missing.");
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("database", "VirtualDatabase");
        properties.setProperty("user", username);
        properties.setProperty("password", password);

        String sql = "SELECT * FROM applicants LIMIT 5";

        try (
            Connection connection =
                DriverManager.getConnection(url, properties);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql)
        ) {
            System.out.println("SQL query executed successfully!");

            int columnCount =
                resultSet.getMetaData().getColumnCount();

            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(
                        resultSet.getMetaData().getColumnLabel(i)
                        + "=" + resultSet.getString(i) + " | "
                    );
                }
                System.out.println();
            }

        } catch (Exception e) {
            System.err.println("SQL query failed:");
            System.err.println(e.getMessage());
        }
    }
}