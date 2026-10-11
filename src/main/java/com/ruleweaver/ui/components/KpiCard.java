package com.ruleweaver.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 * Metric KPI Card with icon, title, animated value count-up, and subtext badge.
 */
public class KpiCard extends VBox {

    private final Label valueLabel = new Label("0");
    private final Label titleLabel = new Label();
    private final Label subtextLabel = new Label();

    public KpiCard(String title, String initialValue, String subtext, String iconSvg, String accentColorHex) {
        getStyleClass().addAll("card", "card-hoverable");
        setSpacing(12);
        Motion.attachHoverLift(this);

        // Header with title and icon badge
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        titleLabel.setText(title);
        titleLabel.getStyleClass().add("kpi-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        HBox iconBadge = new HBox();
        iconBadge.setAlignment(Pos.CENTER);
        iconBadge.setStyle("-fx-background-color: " + (accentColorHex != null ? accentColorHex + "22" : "rgba(255,255,255,0.06)") + "; "
                + "-fx-background-radius: 10px; -fx-padding: 8px;");
        if (iconSvg != null) {
            SVGPath icon = Icons.create(iconSvg, 0.8, accentColorHex != null ? accentColorHex : "#89b4fa");
            iconBadge.getChildren().add(icon);
        }

        header.getChildren().addAll(titleLabel, iconBadge);

        // Value
        valueLabel.setText(initialValue);
        valueLabel.getStyleClass().add("kpi-value");

        // Subtext / Trend badge
        subtextLabel.setText(subtext);
        subtextLabel.getStyleClass().add("kpi-subtext");

        getChildren().addAll(header, valueLabel, subtextLabel);
    }

    public void setValue(int number) {
        Motion.countUp(valueLabel, number);
    }

    public void setValuePercent(double percent) {
        Motion.countUpPercent(valueLabel, percent);
    }

    public void setValueText(String text) {
        valueLabel.setText(text);
    }

    public void setSubtext(String subtext) {
        subtextLabel.setText(subtext);
    }
}
