
package com.ruleweaver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class JdbcListTables {

    public static void main(String[] args) {

        String url = System.getenv("MONGODB_JDBC_URL");
        String username = System.getenv("MONGODB_JDBC_USER");
        String password = System.getenv("MONGODB_JDBC_PASSWORD");

        if (url == null || username == null || password == null) {
            System.out.println("JDBC environment variables are missing.");
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("database", "VirtualDatabase");
        properties.setProperty("user", username);
        properties.setProperty("password", password);

        try (Connection connection =
                     DriverManager.getConnection(url, properties);
             Statement statement = connection.createStatement()) {

            System.out.println("JDBC connection successful!");

         String sql = "SELECT * FROM applicants LIMIT 5";
            System.out.println("Executing: " + sql);

            try (ResultSet resultSet = statement.executeQuery(sql)) {
int columnCount = resultSet.getMetaData().getColumnCount();
System.out.println("Column count: " + columnCount);
System.out.println("Result set type: " + resultSet.getClass().getName());
System.out.println("Metadata type: " +
        resultSet.getMetaData().getClass().getName());
int rowCount = 0;

System.out.println("Number of columns: " + columnCount);

System.out.println("Column names:");

for (int i = 1; i <= columnCount; i++) {
    System.out.print(
            resultSet.getMetaData().getColumnName(i) + "\t"
    );
}

System.out.println("\nQuery results:");

while (resultSet.next()) {
    rowCount++;

    for (int i = 1; i <= columnCount; i++) {
        System.out.print(resultSet.getObject(i) + "\t");
    }

    System.out.println();
}

System.out.println("Rows returned: " + rowCount);


            }

        } catch (Exception e) {
            System.out.println("SQL query failed:");
            e.printStackTrace();
        }
    }
}

