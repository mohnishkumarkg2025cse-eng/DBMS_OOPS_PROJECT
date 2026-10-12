package com.ruleweaver.ui.components;

import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Integrated Search input bar with SVG search icon and clear button.
 */
public class SearchBar extends HBox {

    private final TextField textField = new TextField();
    private final Button clearBtn = new Button();

    public SearchBar(String promptText) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        getStyleClass().add("search-bar");

        FontIcon searchIcon = Icons.create(Icons.SEARCH, 0.7, "#9aa0c3");

        textField.setPromptText(promptText);
        textField.getStyleClass().add("search-bar-field");
        HBox.setHgrow(textField, Priority.ALWAYS);

        FontIcon closeIcon = Icons.create(Icons.CLOSE, 0.6, "#9aa0c3");
        clearBtn.setGraphic(closeIcon);
        clearBtn.getStyleClass().add("btn-icon");
        clearBtn.visibleProperty().bind(textField.textProperty().isNotEmpty());
        clearBtn.setOnAction(e -> textField.clear());

        getChildren().addAll(searchIcon, textField, clearBtn);

        // Highlight parent border when textfield is focused
        textField.focusedProperty().addListener((obs, oldVal, focused) -> {
            getStyleClass().remove("search-bar-focused");
            if (focused) getStyleClass().add("search-bar-focused");
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
