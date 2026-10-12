package com.ruleweaver.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Metric KPI Card with icon, title, animated value count-up, and subtext badge.
 */
public class KpiCard extends VBox {

    private final Label valueLabel = new Label("0");
    private final Label titleLabel = new Label();
    private final Label subtextLabel = new Label();
    private final Label deltaLabel = new Label("—");

    public KpiCard(String title, String initialValue, String subtext, String iconSvg, String accentColorHex) {
        getStyleClass().addAll("kpi-card", "card-hoverable");
        setSpacing(12);
        Motion.attachHoverLift(this);
        javafx.scene.layout.Region accentLine = new javafx.scene.layout.Region();
        accentLine.getStyleClass().add("kpi-accent-line");

        // Header with title and icon badge
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        titleLabel.setText(title);
        titleLabel.getStyleClass().add("kpi-title");
        titleLabel.setWrapText(true);
        titleLabel.setMinWidth(0);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        HBox iconBadge = new HBox();
        iconBadge.setAlignment(Pos.CENTER);
        iconBadge.getStyleClass().addAll("kpi-icon-box", iconStyleClass(accentColorHex, "kpi-icon-"));
        if (iconSvg != null) {
            FontIcon icon = Icons.create(iconSvg, 0.8, accentColorHex != null ? accentColorHex : "#89b4fa");
            icon.getStyleClass().add(iconStyleClass(accentColorHex, "kpi-glyph-"));
            iconBadge.getChildren().add(icon);
        }

        header.getChildren().addAll(titleLabel, iconBadge);

        // Value
        valueLabel.setText(initialValue);
        valueLabel.getStyleClass().add("kpi-value");
        valueLabel.setWrapText(true);
        valueLabel.setMinWidth(0);

        // Subtext / Trend badge
        subtextLabel.setText(subtext);
        subtextLabel.getStyleClass().add("kpi-subtext");
        subtextLabel.setWrapText(true);
        deltaLabel.setWrapText(true);
        deltaLabel.getStyleClass().addAll("delta-flat");

        getChildren().addAll(accentLine, header, valueLabel, deltaLabel, subtextLabel);
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

    public void setDeltaText(String text) {
        deltaLabel.setText(text);
    }

    public void setDeltaStyle(String styleClass) {
        deltaLabel.getStyleClass().removeAll("delta-up", "delta-down", "delta-flat");
        deltaLabel.getStyleClass().add(styleClass);
    }

    private static String iconStyleClass(String color, String prefix) {
        if (color == null) return prefix + "accent";
        return switch (color.toLowerCase()) {
            case "#a6e3a1" -> prefix + "success";
            case "#f38ba8" -> prefix + "danger";
            case "#f9e2af" -> prefix + "warning";
            case "#74c7ec" -> prefix + "info";
            default -> prefix + "accent";
        };
    }
}
