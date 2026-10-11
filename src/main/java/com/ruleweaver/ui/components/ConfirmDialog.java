package com.ruleweaver.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 * Styled custom confirmation modal dialog replacing default OS alerts.
 */
public class ConfirmDialog extends StackPane {

    public ConfirmDialog(String title, String message, String confirmBtnText, boolean isDestructive,
                         Runnable onConfirm, Runnable onCancel) {
        getStyleClass().add("modal-backdrop");
        setPickOnBounds(true);
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);

        VBox box = new VBox(16);
        box.getStyleClass().add("modal-window");
        box.setMaxWidth(420);
        box.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        SVGPath icon = Icons.create(isDestructive ? Icons.TRASH : Icons.INFO, 0.9, isDestructive ? "#f38ba8" : "#89b4fa");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -color-text-primary;");
        header.getChildren().addAll(icon, titleLabel);

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-secondary;");
        msgLabel.setWrapText(true);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setStyle("-fx-padding: 8px 0 0 0;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> {
            if (onCancel != null) onCancel.run();
        });

        Button confirmBtn = new Button(confirmBtnText);
        confirmBtn.getStyleClass().add(isDestructive ? "btn-danger" : "btn-primary");
        confirmBtn.setOnAction(e -> {
            if (onConfirm != null) onConfirm.run();
        });

        actions.getChildren().addAll(cancelBtn, confirmBtn);
        box.getChildren().addAll(header, msgLabel, actions);

        getChildren().add(box);
        Motion.modalScaleIn(box);
    }
}
