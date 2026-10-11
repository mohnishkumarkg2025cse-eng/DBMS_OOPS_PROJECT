package com.ruleweaver.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 * Placeholder component for empty lists, search misses, or un-evaluated datasets.
 */
public class EmptyState extends VBox {

    public EmptyState(String iconSvg, String title, String description, String actionButtonText, Runnable onAction) {
        setAlignment(Pos.CENTER);
        getStyleClass().add("empty-state");

        if (iconSvg != null) {
            SVGPath icon = Icons.create(iconSvg, 1.0, "#5c607f");
            icon.getStyleClass().add("empty-icon");
            StackPane iconWrap = new StackPane(icon);
            iconWrap.getStyleClass().add("empty-icon-wrap");
            getChildren().add(iconWrap);
        }

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-title");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("empty-text");
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
