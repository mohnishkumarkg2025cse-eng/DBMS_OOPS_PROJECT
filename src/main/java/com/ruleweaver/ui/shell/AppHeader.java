package com.ruleweaver.ui.shell;

import atlantafx.base.theme.Styles;
import com.ruleweaver.model.Policy;
import com.ruleweaver.model.PolicyVersion;
import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.components.Icons;
import com.ruleweaver.ui.util.Theme;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Top Application Header displaying current route information and quick controls.
 */
public class AppHeader extends HBox {

    private final Label titleLabel = new Label("Dashboard");
    private final Label subtitleLabel = new Label("System health and overview");
    private final Label policyBadge = new Label("Active Policy: None");

    public AppHeader(Runnable onRefresh) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        getStyleClass().add("app-header");

        VBox titleBlock = new VBox(2);
        titleLabel.getStyleClass().add("app-header-title");
        subtitleLabel.getStyleClass().addAll(Styles.TEXT_SMALL, "app-header-subtitle");
        titleBlock.getChildren().addAll(titleLabel, subtitleLabel);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        // Active Policy / Version indicator chip
        HBox policyChip = new HBox(6);
        policyChip.setAlignment(Pos.CENTER);
        policyChip.getStyleClass().add("app-header-policy-chip");

        FontIcon policyIcon = Icons.create(Icons.POLICIES, 0.65, "#89b4fa");
        policyBadge.getStyleClass().addAll(Styles.TEXT_SMALL, "app-header-policy-label");
        policyChip.getChildren().addAll(policyIcon, policyBadge);

        // Listen for active policy / version updates
        AppContext ctx = AppContext.getInstance();
        ctx.selectedPolicyProperty().addListener((obs, oldVal, newVal) -> updatePolicyBadge(newVal, ctx.selectedVersionProperty().get()));
        ctx.selectedVersionProperty().addListener((obs, oldVal, newVal) -> updatePolicyBadge(ctx.selectedPolicyProperty().get(), newVal));

        // Reduced motion toggle button
        Button motionBtn = new Button();
        FontIcon motionIcon = Icons.create(Icons.SPARKLES, 0.7, "#9aa0c3");
        motionIcon.getStyleClass().remove("icon-fill-secondary");
        motionIcon.getStyleClass().add(ctx.reducedMotionProperty().get()
                ? "motion-toggle-muted" : "motion-toggle-active");
        motionBtn.setGraphic(motionIcon);
        motionBtn.getStyleClass().addAll(Styles.BUTTON_ICON, "btn-icon");
        Tooltip motionTooltip = new Tooltip("Toggle Reduced Motion");
        Theme.apply(motionTooltip);
        motionBtn.setTooltip(motionTooltip);
        motionBtn.setOnAction(e -> {
            boolean current = ctx.reducedMotionProperty().get();
            ctx.reducedMotionProperty().set(!current);
            FontIcon updatedMotionIcon = (FontIcon) motionBtn.getGraphic();
            updatedMotionIcon.getStyleClass().removeAll("motion-toggle-active", "motion-toggle-muted");
            updatedMotionIcon.getStyleClass().add(!current ? "motion-toggle-muted" : "motion-toggle-active");
        });

        // Global Refresh Button
        Button refreshBtn = new Button();
        refreshBtn.setGraphic(Icons.create(Icons.REFRESH, 0.7, "#9aa0c3"));
        refreshBtn.getStyleClass().addAll(Styles.BUTTON_ICON, "btn-icon");
        Tooltip refreshTooltip = new Tooltip("Refresh Current Data");
        Theme.apply(refreshTooltip);
        refreshBtn.setTooltip(refreshTooltip);
        refreshBtn.setOnAction(e -> {
            if (onRefresh != null) onRefresh.run();
        });

        getChildren().addAll(titleBlock, policyChip, motionBtn, refreshBtn);
    }

    private void updatePolicyBadge(Policy policy, PolicyVersion version) {
        javafx.application.Platform.runLater(() -> {
            if (policy == null) {
                policyBadge.setText("No Policy Selected");
            } else {
                String ver = version != null ? " (v" + version.getVersionNumber() + ")" : "";
                policyBadge.setText(policy.getName() + ver);
            }
        });
    }

    public void setTitle(String title, String subtitle) {
        titleLabel.setText(title);
        subtitleLabel.setText(subtitle);
    }
}
