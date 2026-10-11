package com.ruleweaver.ui.components;

import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.shape.SVGPath;

/**
 * Integrated Search input bar with SVG search icon and clear button.
 */
public class SearchBar extends HBox {

    private final TextField textField = new TextField();
    private final Button clearBtn = new Button();

    public SearchBar(String promptText) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setStyle("-fx-background-color: -color-input-bg; -fx-background-radius: 10px; "
                + "-fx-border-color: -color-border; -fx-border-radius: 10px; -fx-border-width: 1px; "
                + "-fx-padding: 2px 10px 2px 12px;");

        SVGPath searchIcon = Icons.create(Icons.SEARCH, 0.7, "#9aa0c3");

        textField.setPromptText(promptText);
        textField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 8px 4px 8px 4px;");
        HBox.setHgrow(textField, Priority.ALWAYS);

        SVGPath closeIcon = Icons.create(Icons.CLOSE, 0.6, "#9aa0c3");
        clearBtn.setGraphic(closeIcon);
        clearBtn.getStyleClass().add("btn-icon");
        clearBtn.visibleProperty().bind(textField.textProperty().isNotEmpty());
        clearBtn.setOnAction(e -> textField.clear());

        getChildren().addAll(searchIcon, textField, clearBtn);

        // Highlight parent border when textfield is focused
        textField.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                setStyle("-fx-background-color: -color-input-focus; -fx-background-radius: 10px; "
                        + "-fx-border-color: -color-accent; -fx-border-radius: 10px; -fx-border-width: 1px; "
                        + "-fx-padding: 2px 10px 2px 12px; -fx-effect: dropshadow(three-pass-box, -color-accent-glow, 8, 0, 0, 0);");
            } else {
                setStyle("-fx-background-color: -color-input-bg; -fx-background-radius: 10px; "
                        + "-fx-border-color: -color-border; -fx-border-radius: 10px; -fx-border-width: 1px; "
                        + "-fx-padding: 2px 10px 2px 12px;");
            }
        });
    }

    public StringProperty textProperty() {
        return textField.textProperty();
    }

    public String getText() {
        return textField.getText();
    }

    public void clear() {
        textField.clear();
    }
}
