package com.ruleweaver.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Circular applicant avatar with deterministic color tinting and name initials.
 */
public class Avatar extends StackPane {

    private static final int PALETTE_SIZE = 7;

    public Avatar(String name) {
        setAlignment(Pos.CENTER);
        getStyleClass().add("avatar-circle");

        String initials = computeInitials(name);
        getStyleClass().add("avatar-c" + pickColorIndex(name));

        Label label = new Label(initials);
        label.getStyleClass().add("avatar-text");

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

    private int pickColorIndex(String name) {
        if (name == null) return 0;
        int hash = Math.abs(name.hashCode());
        return hash % PALETTE_SIZE;
    }
}
