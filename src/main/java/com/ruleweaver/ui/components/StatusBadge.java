package com.ruleweaver.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

/**
 * Pill status badge with glowing indicator dot and semantic tinting.
 */
public class StatusBadge extends HBox {

    public enum Type {
        PASS,
        FAIL,
        INFO,
        WARNING
    }

    public StatusBadge(String text, Type type) {
        setAlignment(Pos.CENTER);
        setSpacing(6);
        getStyleClass().add("badge-pill");

        Circle dot = new Circle(3.5);
        Label label = new Label(text);

        switch (type) {
            case PASS -> {
                getStyleClass().add("badge-pass");
                label.getStyleClass().add("badge-pass-text");
                dot.getStyleClass().add("badge-dot-pass");
            }
            case FAIL -> {
                getStyleClass().add("badge-fail");
                label.getStyleClass().add("badge-fail-text");
                dot.getStyleClass().add("badge-dot-fail");
            }
            case WARNING -> {
                getStyleClass().add("badge-warning");
                label.getStyleClass().add("badge-warning-text");
                dot.getStyleClass().add("badge-dot-warning");
            }
            case INFO -> {
                getStyleClass().add("badge-info");
                label.getStyleClass().add("badge-info-text");
                dot.getStyleClass().add("badge-dot-info");
            }
        }

        getChildren().addAll(dot, label);
    }

    public static StatusBadge pass(String text) {
        return new StatusBadge(text, Type.PASS);
    }

    public static StatusBadge fail(String text) {
        return new StatusBadge(text, Type.FAIL);
    }

    public static StatusBadge info(String text) {
        return new StatusBadge(text, Type.INFO);
    }

    public static StatusBadge warning(String text) {
        return new StatusBadge(text, Type.WARNING);
    }
}
