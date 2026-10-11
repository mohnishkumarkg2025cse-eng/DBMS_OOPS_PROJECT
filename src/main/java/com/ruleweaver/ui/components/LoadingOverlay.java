package com.ruleweaver.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Non-blocking loading overlay with progress bar and status text.
 */
public class LoadingOverlay extends StackPane {

    private final Label messageLabel = new Label("Processing...");
    private final ProgressBar progressBar = new ProgressBar();
    private final Button cancelButton = new Button("Cancel");

    public LoadingOverlay(String initialMessage) {
        getStyleClass().add("modal-backdrop");
        setVisible(false);
        setMouseTransparent(true);
        setPickOnBounds(false);
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);

        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("loading-overlay-card");

        messageLabel.setText(initialMessage != null ? initialMessage : "Loading...");
        messageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -color-text-primary;");

        progressBar.setPrefWidth(220);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        cancelButton.getStyleClass().add("btn-secondary");
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
        box.getChildren().addAll(progressBar, messageLabel, cancelButton);
        getChildren().add(box);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void show(String message) {
        show(message, null);
    }

    public void show(String message, Runnable onCancel) {
        if (message != null) setMessage(message);
        if (onCancel == null) {
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        }
        cancelButton.setOnAction(onCancel == null ? null : event -> onCancel.run());
        cancelButton.setVisible(onCancel != null);
        cancelButton.setManaged(onCancel != null);
        setMouseTransparent(onCancel == null);
        setVisible(true);
    }

    public void setProgress(double progress) {
        progressBar.setProgress(progress);
    }

    public void hide() {
        setVisible(false);
        setMouseTransparent(true);
        cancelButton.setOnAction(null);
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
    }
}
