package com.ruleweaver.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Compact pill chip tag for displaying applicant skills.
 */
public class SkillChip extends HBox {

    public SkillChip(String skillName) {
        setAlignment(Pos.CENTER);
        getStyleClass().add("skill-chip");

        Label label = new Label(skillName != null ? skillName : "");
        label.getStyleClass().add("skill-chip-text");

        getChildren().add(label);
    }
}
