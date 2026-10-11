package com.ruleweaver.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 * Placeholder component for empty lists, search misses, or un-evaluated datasets.
 */
public class EmptyState extends VBox {

    public EmptyState(String iconSvg, String title, String description, String actionButtonText, Runnable onAction) {
        setAlignment(Pos.CENTER);
        setSpacing(14);
        setStyle("-fx-padding: 48px 24px 48px 24px;");

        if (iconSvg != null) {
            SVGPath icon = Icons.create(iconSvg, 1.4, "#5c607f");
            getChildren().add(icon);
        }

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -color-text-secondary;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-muted; -fx-text-alignment: center;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(360);

        getChildren().addAll(titleLabel, descLabel);

        if (actionButtonText != null && onAction != null) {
            Button actionBtn = new Button(actionButtonText);
            actionBtn.getStyleClass().add("btn-primary");
            actionBtn.setOnAction(e -> onAction.run());
            getChildren().add(actionBtn);
        }
    }
}
