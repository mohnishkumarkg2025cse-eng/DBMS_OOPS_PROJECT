
package com.ruleweaver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class MySqlConnectionTest {

    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/ruleweaver_sql";

        String user = System.getenv("MYSQL_USER");
        String password = System.getenv("MYSQL_PASSWORD");

        if (user == null || password == null) {
            System.err.println(
                    "Set MYSQL_USER and MYSQL_PASSWORD first.");
            return;
        }

        try (
                Connection connection = DriverManager.getConnection(url, user, password);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT VERSION(), DATABASE()")) {
            if (result.next()) {
                System.out.println(
                        "MySQL connection successful!");
                System.out.println(
                        "Server version: " + result.getString(1));
                System.out.println(
                        "Database: " + result.getString(2));
            }
        } catch (Exception e) {
            System.err.println("MySQL connection failed:");
            e.printStackTrace();
        }
    }
}
