package com.ruleweaver.ui.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/**
 * Modern notification toast with auto-dismiss and animated slide/fade.
 */
public class Toast extends HBox {

    public enum Type {
        SUCCESS,
        ERROR,
        INFO
    }

    private final Type type;
    private final Runnable onDismiss;
    private boolean dismissing;

    public Toast(String title, String message, Type type, Runnable onDismiss) {
        this(title, message, type, onDismiss, null, null);
    }

    private Toast(String title, String message, Type type, Runnable onDismiss,
                  String actionText, Runnable action) {
        this.type = type;
        this.onDismiss = onDismiss;

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(14);
        getStyleClass().add("toast");

        String iconSvg = Icons.CHECK;
        String accentColor = "#a6e3a1";

        switch (type) {
            case SUCCESS -> {
                getStyleClass().add("toast-success");
                iconSvg = Icons.CHECK;
                accentColor = "#a6e3a1";
            }
            case ERROR -> {
                getStyleClass().add("toast-error");
                iconSvg = Icons.CLOSE;
                accentColor = "#f38ba8";
            }
            case INFO -> {
                getStyleClass().add("toast-info");
                iconSvg = Icons.SPARKLES;
                accentColor = "#74c7ec";
            }
        }

        SVGPath icon = Icons.create(iconSvg, 0.8, accentColor);

        VBox textBlock = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("toast-title");

        Label msgLabel = new Label(message);
        msgLabel.getStyleClass().add("toast-message");
        msgLabel.setWrapText(true);

        textBlock.getChildren().addAll(titleLabel, msgLabel);
        HBox.setHgrow(textBlock, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(Icons.create(Icons.CLOSE, 0.6, "#9aa0c3"));
        closeBtn.getStyleClass().add("btn-icon");
        closeBtn.setOnAction(e -> dismiss());

        getChildren().add(icon);
        getChildren().add(textBlock);
        if (actionText != null && action != null) {
            Button actionButton = new Button(actionText);
            actionButton.getStyleClass().add("btn-secondary");
            actionButton.setOnAction(event -> {
                dismiss();
                action.run();
            });
            getChildren().add(actionButton);
        }
        getChildren().add(closeBtn);

        // Auto dismiss sequence after 3.5s
        PauseTransition wait = new PauseTransition(Duration.millis(3500));
        wait.setOnFinished(e -> dismiss());
        wait.play();
    }

    public void dismiss() {
        if (dismissing) {
            return;
        }
        dismissing = true;
        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            setVisible(false);
            setMouseTransparent(true);
            if (onDismiss != null) {
                onDismiss.run();
            }
        });
        fadeOut.play();
    }

    public static Toast success(String title, String message, Runnable onDismiss) {
        return new Toast(title, message, Type.SUCCESS, onDismiss);
    }

    public static Toast error(String title, String message, Runnable onDismiss) {
        return new Toast(title, message, Type.ERROR, onDismiss);
    }

    public static Toast errorWithAction(String title, String message, String actionText, Runnable action) {
        return new Toast(title, message, Type.ERROR, null, actionText, action);
    }

    public static Toast info(String title, String message, Runnable onDismiss) {
        return new Toast(title, message, Type.INFO, onDismiss);
    }
}
