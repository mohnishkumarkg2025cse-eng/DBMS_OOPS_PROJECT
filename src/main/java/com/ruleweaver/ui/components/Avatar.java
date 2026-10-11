package com.ruleweaver.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Circular applicant avatar with deterministic color tinting and name initials.
 */
public class Avatar extends StackPane {

    private static final String[] PALETTE = {
            "#89b4fa", // Blue
            "#a6e3a1", // Green
            "#cba6f7", // Mauve
            "#f9e2af", // Yellow
            "#f5c2e7", // Pink
            "#74c7ec", // Sapphire
            "#eba0ac"  // Maroon
    };

    public Avatar(String name) {
        setAlignment(Pos.CENTER);
        getStyleClass().add("avatar-circle");

        String initials = computeInitials(name);
        String color = pickColor(name);

        setStyle("-fx-background-color: " + color + "33; -fx-border-color: " + color + "66; -fx-border-width: 1px;");

        Label label = new Label(initials);
        label.getStyleClass().add("avatar-text");
        label.setStyle("-fx-text-fill: " + color + ";");

        getChildren().add(label);
    }

    private String computeInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String pickColor(String name) {
        if (name == null) return PALETTE[0];
        int hash = Math.abs(name.hashCode());
        return PALETTE[hash % PALETTE.length];
    }
}
