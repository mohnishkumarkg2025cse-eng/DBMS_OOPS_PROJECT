package com.ruleweaver.ui.views;

import com.ruleweaver.model.*;
import com.ruleweaver.config.MongoConfig;
import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.components.*;
import com.ruleweaver.ui.util.Theme;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * Screen 5: 'The Decision Time Machine' Policy Comparison Inspector.
 * Computes decision shifts across version transitions and pinpoints root-cause rule changes.
 */
public class ComparisonView implements BaseView {

    private final StackPane rootPane = new StackPane();
    private final ComboBox<Policy> policyCombo = new ComboBox<>();
    private final ComboBox<PolicyVersion> versionACombo = new ComboBox<>();
    private final ComboBox<PolicyVersion> versionBCombo = new ComboBox<>();

    private final TableView<ApplicantDiff> diffTable = new TableView<>();
    private final ObservableList<ApplicantDiff> diffList = FXCollections.observableArrayList();

    private final Label changedCountLabel = new Label("0");
    private final Label lostCountLabel = new Label("0");
    private final Label gainedCountLabel = new Label("0");

    private final LoadingOverlay loadingOverlay = new LoadingOverlay("Comparing policy versions in the Time Machine...");

    public ComparisonView() {
        buildUI();
    }

    private void buildUI() {
        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(28, 32, 28, 32));

        // 1. Toolbar Header
        HBox toolbar = createToolbar();

        // 2. Version Comparison Control Bar
        HBox controlBar = createControlBar();

        // 3. Impact Summary Cards Row
        HBox summaryRow = createSummaryCards();

        // 4. Differential Table Card
        VBox tableCard = createTableCard();
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        contentBox.getChildren().addAll(toolbar, controlBar, summaryRow, tableCard);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        rootPane.getChildren().addAll(contentBox, loadingOverlay);

        setupListeners();
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(16);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("The Decision Time Machine");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Simulate policy shifts and inspect candidate decision flips between version epochs");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button compareBtn = new Button("Run Comparison");
        compareBtn.getStyleClass().add("btn-primary");
        compareBtn.setGraphic(Icons.create(Icons.COMPARISON, 0.7, "#11111b"));
        compareBtn.setOnAction(e -> runComparisonAsync());

        toolbar.getChildren().addAll(titleBox, compareBtn);
        return toolbar;
    }

    private HBox createControlBar() {
        HBox bar = new HBox(14);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: -color-card; -fx-background-radius: 14px; "
                + "-fx-padding: 12px 18px 12px 18px; -fx-border-color: -color-border-subtle; -fx-border-radius: 14px; -fx-border-width: 1px;");

        Label pLbl = new Label("Target Policy:");
        pLbl.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text-secondary;");

        policyCombo.setPrefWidth(220);
        policyCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Policy p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getName());
            }
        });
        policyCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Policy p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "Select Policy" : p.getName());
            }
        });

        Label vaLbl = new Label("Baseline (Version A):");
        vaLbl.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text-secondary;");

        versionACombo.setPrefWidth(160);
        setupVersionCellFactory(versionACombo);

        // Swap Button
        Button swapBtn = new Button();
        swapBtn.setGraphic(Icons.create(Icons.SWAP, 0.75, "#89b4fa"));
        swapBtn.getStyleClass().add("btn-icon");
        Tooltip swapTooltip = new Tooltip("Swap Baseline & Target Versions");
        Theme.apply(swapTooltip);
        swapBtn.setTooltip(swapTooltip);
        swapBtn.setOnAction(e -> {
            PolicyVersion temp = versionACombo.getValue();
            versionACombo.setValue(versionBCombo.getValue());
            versionBCombo.setValue(temp);
            runComparisonAsync();
        });

        Label vbLbl = new Label("Target (Version B):");
        vbLbl.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text-secondary;");

        versionBCombo.setPrefWidth(160);
        setupVersionCellFactory(versionBCombo);

        bar.getChildren().addAll(pLbl, policyCombo, vaLbl, versionACombo, swapBtn, vbLbl, versionBCombo);
        return bar;
    }

    private void setupVersionCellFactory(ComboBox<PolicyVersion> combo) {
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PolicyVersion pv, boolean empty) {
                super.updateItem(pv, empty);
                setText(empty || pv == null ? null : "Version " + pv.getVersionNumber());
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PolicyVersion pv, boolean empty) {
                super.updateItem(pv, empty);
                setText(empty || pv == null ? "Version" : "Version " + pv.getVersionNumber());
            }
        });
    }

    private HBox createSummaryCards() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        HBox card1 = createSummaryMetric("DECISIONS CHANGED", changedCountLabel, "comparison-metric-accent");
        HBox card2 = createSummaryMetric("LOST ELIGIBILITY (Downgraded)", lostCountLabel, "comparison-metric-danger");
        HBox card3 = createSummaryMetric("GAINED ELIGIBILITY (Upgraded)", gainedCountLabel, "comparison-metric-success");

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);

        row.getChildren().addAll(card1, card2, card3);
        return row;
    }

    private HBox createSummaryMetric(String title, Label countLabel, String valueStyleClass) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("card", "card-hoverable");
        Motion.attachHoverLift(card);

        VBox text = new VBox(2);
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("caption-label");

        countLabel.getStyleClass().addAll("comparison-metric-value", valueStyleClass);

        text.getChildren().addAll(titleLbl, countLabel);
        card.getChildren().add(text);
        return card;
    }

    private VBox createTableCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        diffTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        diffTable.setPlaceholder(new EmptyState(
                Icons.COMPARISON,
                "No Version Comparison Executed",
                "Select Version A and Version B and click 'Run Comparison' to calculate differences.",
                "Compare Baseline vs Target",
                this::runComparisonAsync
        ));

        // Row factory for highlight tints
        diffTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ApplicantDiff item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-changed-lost", "row-changed-gained");
                if (!empty && item != null && item.hasDecisionChanged()) {
                    if (item.isV1Decision() && !item.isV2Decision()) {
                        getStyleClass().add("row-changed-lost");
                    } else {
                        getStyleClass().add("row-changed-gained");
                    }
                }
            }
        });

        // Column 1: Candidate
        TableColumn<ApplicantDiff, ApplicantDiff> candCol = new TableColumn<>("CANDIDATE");
        candCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        candCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ApplicantDiff diff, boolean empty) {
                super.updateItem(diff, empty);
                if (empty || diff == null) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(12);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Avatar avatar = new Avatar(diff.getApplicantName());
                    VBox textBlock = new VBox(2);
                    Label nameLbl = new Label(diff.getApplicantName());
                    nameLbl.setStyle("-fx-font-weight: 700; -fx-text-fill: -color-text-primary;");
                    Label idLbl = new Label("ID: " + diff.getApplicantId());
                    idLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
                    textBlock.getChildren().addAll(nameLbl, idLbl);
                    box.getChildren().addAll(avatar, textBlock);
                    setGraphic(box);
                }
            }
        });
        candCol.setPrefWidth(220);

        // Column 2: Transition Badges (v1 -> v2)
        TableColumn<ApplicantDiff, ApplicantDiff> shiftCol = new TableColumn<>("DECISION TRANSITION");
        shiftCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        shiftCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ApplicantDiff diff, boolean empty) {
                super.updateItem(diff, empty);
                if (empty || diff == null) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);

                    StatusBadge v1Badge = diff.isV1Decision() ? StatusBadge.pass("vA: PASS") : StatusBadge.fail("vA: FAIL");
                    FontIcon arrow = Icons.create(Icons.ARROW_RIGHT, 0.6, "#9aa0c3");
                    StatusBadge v2Badge = diff.isV2Decision() ? StatusBadge.pass("vB: PASS") : StatusBadge.fail("vB: FAIL");

                    box.getChildren().addAll(v1Badge, arrow, v2Badge);
                    setGraphic(box);
                }
            }
        });
        shiftCol.setPrefWidth(240);

        // Column 3: Transition State Badge
        TableColumn<ApplicantDiff, String> stateCol = new TableColumn<>("CHANGE IMPACT");
        stateCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getChangeType()));
        stateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setGraphic(null);
                } else {
                    if (val.contains("LOST")) {
                        setGraphic(StatusBadge.fail("LOST ELIGIBILITY"));
                    } else if (val.contains("GAINED")) {
                        setGraphic(StatusBadge.pass("GAINED ELIGIBILITY"));
                    } else {
                        Label lbl = new Label("Unchanged");
                        lbl.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px;");
                        setGraphic(lbl);
                    }
                }
            }
        });
        stateCol.setPrefWidth(180);

        // Column 4: Differentiating Reason
        TableColumn<ApplicantDiff, ApplicantDiff> reasonCol = new TableColumn<>("ROOT CAUSE DIFFERENTIATING FACTOR");
        reasonCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        reasonCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ApplicantDiff diff, boolean empty) {
                super.updateItem(diff, empty);
                if (empty || diff == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (!diff.hasDecisionChanged()) {
                        Label noneLbl = new Label("Consistent across criteria");
                        noneLbl.setStyle("-fx-text-fill: -color-text-muted; -fx-font-size: 12px;");
                        setGraphic(noneLbl);
                    } else {
                        VBox reasonsBox = new VBox(3);
                        for (String r : diff.getDifferentiatingReasons()) {
                            Label rLbl = new Label("• " + r);
                            rLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -color-text-primary;");
                            reasonsBox.getChildren().add(rLbl);
                        }
                        setGraphic(reasonsBox);
                    }
                }
            }
        });
        reasonCol.setPrefWidth(320);

        diffTable.getColumns().addAll(List.of(candCol, shiftCol, stateCol, reasonCol));
        diffTable.setItems(diffList);
        VBox.setVgrow(diffTable, Priority.ALWAYS);

        card.getChildren().add(diffTable);
        return card;
    }

    private void setupListeners() {
        AppContext ctx = AppContext.getInstance();
        policyCombo.setItems(ctx.getPolicies());

        policyCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                List<PolicyVersion> versions = newVal.getVersions();
                versionACombo.getItems().setAll(versions);
                versionBCombo.getItems().setAll(versions);

                if (versions.size() >= 2) {
                    versionACombo.setValue(versions.get(0));
                    versionBCombo.setValue(versions.get(1));
                } else if (!versions.isEmpty()) {
                    versionACombo.setValue(versions.get(0));
                    versionBCombo.setValue(versions.get(0));
                }
            }
        });
        ctx.resultsChangedProperty().addListener((observable, oldValue, newValue) -> {
            if (policyCombo.getValue() != null && versionACombo.getValue() != null
                    && versionBCombo.getValue() != null) {
                runComparisonAsync();
            }
        });
    }

    private void runComparisonAsync() {
        Policy policy = policyCombo.getValue();
        PolicyVersion vA = versionACombo.getValue();
        PolicyVersion vB = versionBCombo.getValue();

        if (policy == null || vA == null || vB == null) {
            AppContext.getInstance().showToast(Toast.error("Selection Required", "Please select a policy and two versions to compare.", null));
            return;
        }

        AppContext ctx = AppContext.getInstance();
        loadingOverlay.show("Simulating Decision Time Machine between v" + vA.getVersionNumber() + " and v" + vB.getVersionNumber() + "...");
        ctx.loadEvaluationResultsAsync(policy.getId(), vA.getVersionNumber(), firstResults ->
                ctx.loadEvaluationResultsAsync(policy.getId(), vB.getVersionNumber(), secondResults -> {
                    loadingOverlay.hide();
                    if (firstResults.isEmpty() || secondResults.isEmpty()) {
                        ctx.showToast(Toast.errorWithAction(
                                "Stored Results Required",
                                "Evaluate both selected versions before comparing their saved results.",
                                "Retry", this::runComparisonAsync));
                        return;
                    }
                    ComparisonReport report = ctx.getComparisonService()
                            .compareEvaluations(firstResults, secondResults);
                    ObservableList<ApplicantDiff> allDiffs = FXCollections.observableArrayList();
                    allDiffs.addAll(report.getChangedApplicants());
                    allDiffs.addAll(report.getUnchangedApplicants());
                    diffList.setAll(allDiffs);

                    int changed = report.getChangedApplicants().size();
                    changedCountLabel.setText(String.valueOf(changed));
                    lostCountLabel.setText(String.valueOf(report.getLostEligibilityCount()));
                    gainedCountLabel.setText(String.valueOf(report.getGainedEligibilityCount()));
                    ctx.showToast(Toast.success(
                            "Comparison Complete",
                            "Compared saved results for v" + vA.getVersionNumber() + " vs v"
                                    + vB.getVersionNumber() + ": " + changed + " candidate decision shifts detected.",
                            null));
                }, failure -> {
                    loadingOverlay.hide();
                    AppContext.logFailure("Could not load target comparison results", failure);
                    ctx.showToast(Toast.errorWithAction("Comparison Error",
                            failure.getMessage() == null ? "Could not load stored results."
                                    : MongoConfig.sanitize(failure.getMessage()),
                            "Retry", this::runComparisonAsync));
                }), failure -> {
            loadingOverlay.hide();
            AppContext.logFailure("Could not load baseline comparison results", failure);
            ctx.showToast(Toast.errorWithAction("Comparison Error",
                    failure.getMessage() == null ? "Could not load stored results."
                            : MongoConfig.sanitize(failure.getMessage()),
                    "Retry", this::runComparisonAsync));
        });
    }

    @Override
    public Node getRoot() {
        return rootPane;
    }

    @Override
    public void onShow() {
        AppContext ctx = AppContext.getInstance();
        Policy sel = ctx.selectedPolicyProperty().get();
        if (sel != null && policyCombo.getValue() != sel) {
            policyCombo.setValue(sel);
        }
    }
}
