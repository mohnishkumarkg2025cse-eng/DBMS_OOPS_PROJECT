package com.ruleweaver.ui.shell;

import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.components.Icons;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Modern 240px Sidebar with glowing active navigation indicators and MongoDB status chip.
 */
public class Sidebar extends VBox {

    public enum Destination {
        DASHBOARD("Dashboard", Icons.DASHBOARD),
        APPLICANTS("Applicants", Icons.APPLICANTS),
        PROGRAMS("Programs", Icons.POLICIES),
        POLICIES("Policies", Icons.POLICIES),
        EVALUATION("Evaluation", Icons.EVALUATION),
        COMPARISON("Time Machine", Icons.COMPARISON);

        private final String title;
        private final String iconSvg;

        Destination(String title, String iconSvg) {
            this.title = title;
            this.iconSvg = iconSvg;
        }

        public String getTitle() { return title; }
        public String getIconSvg() { return iconSvg; }
    }

    private final Map<Destination, HBox> navItemMap = new HashMap<>();
    private Destination currentDestination = Destination.DASHBOARD;
    private final Consumer<Destination> onNavigate;

    public Sidebar(Consumer<Destination> onNavigate, Runnable onSeedData, Runnable onClearDemoData) {
        this.onNavigate = onNavigate;
        getStyleClass().add("sidebar");

        // 1. Logo Block
        VBox logoBlock = createLogoBlock();

        // 2. Navigation items
        VBox navItemsBox = new VBox(6);
        for (Destination dest : Destination.values()) {
            HBox item = createNavItem(dest);
            navItemMap.put(dest, item);
            navItemsBox.getChildren().add(item);
        }
        VBox.setVgrow(navItemsBox, Priority.ALWAYS);

        // 3. Footer with Mongo status & Demo data seeder
        VBox footerBox = createFooter(onSeedData, onClearDemoData);

        getChildren().addAll(logoBlock, navItemsBox, footerBox);
        setActive(Destination.DASHBOARD);
    }

    private VBox createLogoBlock() {
        VBox box = new VBox(4);
        box.getStyleClass().add("sidebar-logo-block");

        HBox titleRow = new HBox(4);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("RuleWeaver ");
        title.getStyleClass().add("sidebar-logo-title");

        Label accentX = new Label("X");
        accentX.getStyleClass().add("sidebar-logo-accent");

        titleRow.getChildren().addAll(title, accentX);

        Label subtitle = new Label("EXPLAINABLE DECISION ENGINE");
        subtitle.getStyleClass().add("sidebar-logo-subtitle");

        box.getChildren().addAll(titleRow, subtitle);
        return box;
    }

    private HBox createNavItem(Destination dest) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("nav-item");

        SVGPath icon = Icons.create(dest.getIconSvg(), 0.8, "#9aa0c3");
        icon.getStyleClass().add("nav-icon");

        Label label = new Label(dest.getTitle());
        label.getStyleClass().add("nav-label");

        item.getChildren().addAll(icon, label);

        item.setOnMouseClicked(e -> {
            setActive(dest);
            if (onNavigate != null) {
                onNavigate.accept(dest);
            }
        });

        return item;
    }

    public void setActive(Destination dest) {
        this.currentDestination = dest;
        for (Map.Entry<Destination, HBox> entry : navItemMap.entrySet()) {
            HBox item = entry.getValue();
            boolean isActive = entry.getKey() == dest;

            item.getStyleClass().removeAll("nav-item-active");
            if (isActive) {
                item.getStyleClass().add("nav-item-active");
            }

            // Update label and icon styles
            SVGPath icon = (SVGPath) item.getChildren().get(0);
            Label label = (Label) item.getChildren().get(1);

            icon.getStyleClass().removeAll("nav-icon-active");
            label.getStyleClass().removeAll("nav-label-active");

            if (isActive) {
                icon.getStyleClass().add("nav-icon-active");
                label.getStyleClass().add("nav-label-active");
            }
        }
    }

    private VBox createFooter(Runnable onSeedData, Runnable onClearDemoData) {
        VBox footer = new VBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-padding: 14px 0 0 0;");

        // Seed Sample Data Button
        if (onSeedData != null) {
            Button seedBtn = new Button("Seed Demo Data");
            seedBtn.getStyleClass().addAll("btn-secondary");
            seedBtn.setMaxWidth(Double.MAX_VALUE);
            SVGPath sparkles = Icons.create(Icons.SPARKLES, 0.65, "#89b4fa");
            seedBtn.setGraphic(sparkles);
            seedBtn.setOnAction(e -> {
                onSeedData.run();
            });
            footer.getChildren().add(seedBtn);
        }

        if (onClearDemoData != null) {
            Button clearBtn = new Button("Clear Demo Data");
            clearBtn.getStyleClass().add("btn-secondary");
            clearBtn.setMaxWidth(Double.MAX_VALUE);
            clearBtn.setOnAction(e -> onClearDemoData.run());
            footer.getChildren().add(clearBtn);
        }

        // MongoDB Atlas Status Chip
        HBox mongoChip = new HBox(8);
        mongoChip.setAlignment(Pos.CENTER_LEFT);
        mongoChip.getStyleClass().add("mongo-status-chip");

        Circle dot = new Circle(4);
        Label statusLabel = new Label("MongoDB Atlas: Checking...");
        statusLabel.getStyleClass().add("mongo-status-text");

        AppContext.getInstance().mongoOnlineProperty().addListener((obs, oldVal, online) -> {
            javafx.application.Platform.runLater(() -> {
                if (online) {
                    dot.getStyleClass().removeAll("status-dot-offline");
                    dot.getStyleClass().add("status-dot-online");
                    statusLabel.setText("Atlas: Connected");
                    statusLabel.setStyle("-fx-text-fill: -color-success;");
                } else {
                    dot.getStyleClass().removeAll("status-dot-online");
                    dot.getStyleClass().add("status-dot-offline");
                    statusLabel.setText("Atlas: Offline");
                    statusLabel.setStyle("-fx-text-fill: -color-danger;");
                }
            });
        });

        mongoChip.getChildren().addAll(dot, statusLabel);
        footer.getChildren().add(mongoChip);

        return footer;
    }
}
