
package com.ruleweaver.repository;

import com.ruleweaver.config.MySqlConnectionFactory;
import com.ruleweaver.model.Applicant;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class ApplicantMySqlRepository {

    public void markDeletedByMongoId(String mongoId) throws SQLException {
        if (mongoId == null || mongoId.isBlank()) {
            throw new IllegalArgumentException("Applicant ID is required.");
        }

        String sql = """
                UPDATE synced_applicants
                SET is_deleted = TRUE
                WHERE mongo_id = ?
                """;

        try (Connection connection = MySqlConnectionFactory.getConnection()) {

            // Prevent MongoDB-originated changes from entering the outbox.
            try (var flag = connection.createStatement()) {
                flag.execute("SET @ruleweaver_sync_origin = 'mongo'");
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, mongoId);
                statement.executeUpdate();
            } finally {
                try (var reset = connection.createStatement()) {
                    reset.execute("SET @ruleweaver_sync_origin = NULL");
                }
            }
        }
    }

    public void upsert(Applicant applicant) throws SQLException {
        String sql = """
                INSERT INTO synced_applicants
                    (mongo_id, program_id, name, email, gpa,
                    interview_score, skills, custom_attributes,
                    version, is_deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, FALSE)
                ON DUPLICATE KEY UPDATE
                    program_id = VALUES(program_id),
                    name = VALUES(name),
                    email = VALUES(email),
                    gpa = VALUES(gpa),
                    interview_score = VALUES(interview_score),
                    skills = VALUES(skills),
                    custom_attributes = VALUES(custom_attributes),
                    is_deleted = FALSE
                """;

        try (Connection connection = MySqlConnectionFactory.getConnection()) {
            // Prevent our Mongo -> MySQL write from entering the outbox.
            try (var flag = connection.createStatement()) {
                flag.execute("SET @ruleweaver_sync_origin = 'mongo'");
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, applicant.getId());
                statement.setString(2, applicant.getProgramId());
                statement.setString(3, applicant.getName());
                statement.setString(4, applicant.getEmail());
                statement.setDouble(5, applicant.getGpa());
                statement.setInt(6, applicant.getInterviewScore());
                statement.setString(7, toJson(applicant.getSkills()));
                statement.setString(8, toJson(applicant.getCustomAttributes()));
                statement.executeUpdate();
            } finally {
                try (var reset = connection.createStatement()) {
                    reset.execute(
                            "SET @ruleweaver_sync_origin = NULL");
                }
            }
        }
    }

    private String toJson(Object value) throws SQLException {
        if (value == null) {
            return "null";
        }

        if (value instanceof String string) {
            return quote(string);
        }

        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }

        if (value instanceof Map<?, ?> map) {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    json.append(',');
                }
                json.append(quote(String.valueOf(entry.getKey())))
                        .append(':')
                        .append(toJson(entry.getValue()));
                first = false;
            }
            return json.append('}').toString();
        }

        if (value instanceof Iterable<?> iterable) {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    json.append(',');
                }
                json.append(toJson(item));
                first = false;
            }
            return json.append(']').toString();
        }

        if (value.getClass().isArray()) {
            StringBuilder json = new StringBuilder("[");
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    json.append(',');
                }
                json.append(toJson(Array.get(value, i)));
            }
            return json.append(']').toString();
        }

        if (value instanceof Enum<?> enumValue) {
            return quote(enumValue.name());
        }

        return quote(String.valueOf(value));
    }

    private String quote(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder escaped = new StringBuilder();
        escaped.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        escaped.append('"');
        return escaped.toString();
    }
}
