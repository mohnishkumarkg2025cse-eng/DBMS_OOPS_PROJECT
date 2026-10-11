package com.ruleweaver.ui.views;

import com.ruleweaver.config.MongoConfig;
import com.ruleweaver.model.*;
import com.ruleweaver.service.WhatIfSimulator;
import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.adapter.ApplicantDocumentMapper;
import com.ruleweaver.ui.components.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Screen 4: Executive Overview Dashboard with live KPI cards, animated charts,
 * rule failure distribution, and policy impact summaries.
 */
public class DashboardView implements BaseView {

    private static final double SIDEBAR_WIDTH = 240;
    private final StackPane rootPane = new StackPane();
    private final ScrollPane scrollPane = new ScrollPane();
    private final VBox contentBox = new VBox(24);
    private final GridPane contentGrid = new GridPane();

    // KPI Cards
    private KpiCard totalCard;
    private KpiCard eligibleCard;
    private KpiCard rejectedCard;
    private KpiCard passRateCard;
    private KpiCard impactCard;
    private final StackPane kpiSlot = new StackPane();
    private GridPane kpiGrid;
    private VBox kpiContent;
    private VBox pieWidget;
    private VBox barWidget;
    private VBox recentWidget;
    private VBox attentionWidget;

    // Charts
    private final PieChart pieChart = new PieChart();
    private CategoryAxis barYAxis;
    private NumberAxis barXAxis;
    private BarChart<Number, String> barChart;

    private final StackPane chartCard = new StackPane();
    private final StackPane barCard = new StackPane();
    private final StackPane recentEvaluationsSlot = new StackPane();
    private final StackPane needsAttentionSlot = new StackPane();
    private final StackPane whatIfResultsSlot = new StackPane();
    private final StackPane donutCenter = new StackPane();
    private StackPane pieVisual;
    private final Circle donutHole = new Circle(52);
    private final Label donutCenterValue = new Label("—");
    private final Label donutCenterLabel = new Label("PASS RATE");
    private final TableView<EvaluationResult> recentEvaluations = new TableView<>();
    private final VBox needsAttentionList = new VBox(8);
    private final DateTimeFormatter updatedFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a");
    private final Map<StackPane, List<Timeline>> shimmerAnimations = new HashMap<>();
    private final Map<StackPane, List<Region>> skeletonRegions = new HashMap<>();
    private final ComboBox<PolicyVersion> whatIfVersionChoice = new ComboBox<>();
    private final ComboBox<WhatIfSimulator.NumericRule> whatIfRuleChoice = new ComboBox<>();
    private final ComboBox<Policy> policyChoice = new ComboBox<>();
    private final Slider whatIfThreshold = new Slider();
    private final Label whatIfThresholdValue = new Label();
    private final Label whatIfSummary = new Label("Choose a version and numeric rule to begin.");
    private final Label whatIfDelta = new Label("No simulation");
    private final Label selectedPolicyChip = new Label("No policy selected");
    private final Label versionChip = new Label("—");
    private final Label lastUpdatedLabel = new Label("Not updated yet");
    private final Button runWhatIfButton = new Button("Run What-if");
    private final Button saveWhatIfButton = new Button("Save as New Version");
    private final Button refreshButton = new Button();
    private final Button evaluateAllButton = new Button("Evaluate All");
    private final Button cancelEvaluationButton = new Button("Cancel");
    private final TableView<WhatIfSimulator.ApplicantFlip> whatIfFlips = new TableView<>();
    private final WhatIfSimulator whatIfSimulator;
    private final BiConsumer<Policy, PolicyVersion> openNewVersionDraft;
    private WhatIfSimulator.SimulationResult lastSimulation;
    private Task<List<EvaluationResult>> activeEvaluationTask;
    private boolean syncingPolicyChoice;
    private boolean hasAnimatedEntrance;
    private int responsiveColumnCount;
    private long refreshGeneration;

    public DashboardView() {
        this((policy, version) -> { });
    }

    public DashboardView(BiConsumer<Policy, PolicyVersion> openNewVersionDraft) {
        this.openNewVersionDraft = openNewVersionDraft;
        this.whatIfSimulator = new WhatIfSimulator(AppContext.getInstance().getDecisionEngine());
        buildUI();
    }

    private void buildUI() {
        rootPane.getStyleClass().add("page-root");
        contentBox.getStyleClass().addAll("dash-page", "page-root");
        contentBox.setMinWidth(0);
        contentBox.setMaxWidth(Double.MAX_VALUE);
        contentGrid.setHgap(20);
        contentGrid.setVgap(20);
        contentGrid.setMinWidth(0);
        contentGrid.setMaxWidth(Double.MAX_VALUE);
        rootPane.setMinSize(0, 0);

        HBox header = createHeader();
        createKpiRow();
        createChartsRow();
        VBox recentCard = createRecentEvaluationsCard();
        VBox attentionCard = createNeedsAttentionCard();
        VBox whatIfCard = createWhatIfCard();
        layoutDashboard(header, recentCard, attentionCard, whatIfCard, 0);

        scrollPane.setContent(contentBox);
        scrollPane.getStyleClass().add("page-root");
        scrollPane.setMinWidth(0);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        rootPane.getChildren().add(scrollPane);
        AppContext ctx = AppContext.getInstance();
        ctx.resultsChangedProperty().addListener((observable, oldValue, newValue) -> refreshDashboard());
        ctx.selectedPolicyProperty().addListener((observable, oldValue, newValue) -> {
            syncHeaderPolicy(newValue);
            configureWhatIfVersions(newValue);
            refreshDashboard();
        });
        ctx.selectedVersionProperty().addListener((observable, oldValue, newValue) -> {
            updateVersionChip(newValue);
            refreshDashboard();
        });
        ctx.reducedMotionProperty().addListener((observable, oldValue, newValue) -> {
            pieChart.setAnimated(!newValue);
            if (barChart != null) barChart.setAnimated(!newValue);
            updateShimmerAnimations(newValue);
        });
        rootPane.widthProperty().addListener((observable, oldWidth, width) ->
                layoutDashboard(header, recentCard, attentionCard, whatIfCard, width.doubleValue()));
        scrollPane.viewportBoundsProperty().addListener((observable, oldBounds, bounds) ->
                layoutDashboard(header, recentCard, attentionCard, whatIfCard, bounds.getWidth()));
        syncHeaderPolicy(ctx.selectedPolicyProperty().get());
        updateVersionChip(ctx.selectedVersionProperty().get());
        whatIfVersionChoice.valueProperty().addListener((observable, oldValue, newValue) ->
                configureWhatIfRules(newValue));
        whatIfRuleChoice.valueProperty().addListener((observable, oldValue, newValue) ->
                configureWhatIfThreshold(newValue));
        policyChoice.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (syncingPolicyChoice) return;
            ctx.selectedPolicyProperty().set(newValue);
            PolicyVersion selected = newValue == null ? null
                    : newValue.getVersion(newValue.getActiveVersionNumber())
                            .orElseGet(() -> newValue.getLatestVersion().orElse(null));
            ctx.selectedVersionProperty().set(selected);
        });
        refreshButton.setOnAction(event -> refreshDashboard());
        evaluateAllButton.setOnAction(event -> evaluateAll());
        cancelEvaluationButton.setOnAction(event -> {
            if (activeEvaluationTask != null) activeEvaluationTask.cancel(true);
        });
        syncHeaderPolicy(ctx.selectedPolicyProperty().get());
        updateVersionChip(ctx.selectedVersionProperty().get());
        configureWhatIfVersions(ctx.selectedPolicyProperty().get());
    }

    private VBox createWhatIfCard() {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card", "whatif-card");
        Label title = new Label("WHAT-IF SIMULATOR");
        title.getStyleClass().add("caption-label");

        whatIfVersionChoice.setPromptText("Select version");
        whatIfVersionChoice.setPrefWidth(180);
        whatIfVersionChoice.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(PolicyVersion version, boolean empty) {
                super.updateItem(version, empty);
                setText(empty || version == null ? null : "Version " + version.getVersionNumber());
            }
        });
        whatIfVersionChoice.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(PolicyVersion version, boolean empty) {
                super.updateItem(version, empty);
                setText(empty || version == null ? "Select version" : "Version " + version.getVersionNumber());
            }
        });
        whatIfRuleChoice.setPromptText("Select numeric rule");
        whatIfRuleChoice.setPrefWidth(250);
        whatIfThreshold.setShowTickMarks(true);
        whatIfThreshold.setShowTickLabels(true);
        whatIfThreshold.setPrefWidth(280);
        whatIfThreshold.valueProperty().addListener((observable, oldValue, newValue) ->
                whatIfThresholdValue.setText(String.format("%.2f", newValue.doubleValue())));
        whatIfThresholdValue.getStyleClass().add("whatif-number");

        Label versionLabel = new Label("Version");
        versionLabel.getStyleClass().add("policy-inline-label");
        Label ruleLabel = new Label("Numeric rule");
        ruleLabel.getStyleClass().add("policy-inline-label");
        HBox selectors = new HBox(10,
                versionLabel, whatIfVersionChoice, ruleLabel, whatIfRuleChoice);
        selectors.setAlignment(Pos.CENTER_LEFT);
        Label thresholdLabel = new Label("Test threshold");
        thresholdLabel.getStyleClass().add("policy-inline-label");
        HBox thresholdRow = new HBox(10, thresholdLabel, whatIfThreshold, whatIfThresholdValue);
        thresholdRow.setAlignment(Pos.CENTER_LEFT);

        runWhatIfButton.getStyleClass().add("btn-primary");
        runWhatIfButton.setOnAction(event -> runWhatIfSimulation());
        saveWhatIfButton.getStyleClass().add("btn-secondary");
        saveWhatIfButton.setDisable(true);
        saveWhatIfButton.setOnAction(event -> {
            Policy policy = AppContext.getInstance().selectedPolicyProperty().get();
            if (policy != null && lastSimulation != null) {
                openNewVersionDraft.accept(policy, lastSimulation.simulatedVersion());
            }
        });
        HBox actions = new HBox(10, runWhatIfButton, saveWhatIfButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        whatIfSummary.setWrapText(true);
        whatIfSummary.getStyleClass().add("page-subtitle");
        whatIfDelta.getStyleClass().add("flip-chip-gain");
        TableColumn<WhatIfSimulator.ApplicantFlip, String> applicantColumn =
                new TableColumn<>("Applicant");
        applicantColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().applicantName()));
        TableColumn<WhatIfSimulator.ApplicantFlip, String> failingRuleColumn =
                new TableColumn<>("Failing rule");
        failingRuleColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().failingRule()));
        whatIfFlips.getColumns().setAll(applicantColumn, failingRuleColumn);
        whatIfFlips.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        whatIfFlips.setPrefHeight(180);
        VBox.setVgrow(whatIfResultsSlot, Priority.ALWAYS);

        whatIfResultsSlot.getStyleClass().add("widget-slot");
        whatIfResultsSlot.getChildren().setAll(whatIfFlips);
        card.getChildren().addAll(title, selectors, thresholdRow, actions, whatIfSummary,
                whatIfDelta, whatIfResultsSlot);
        return card;
    }

    private void configureWhatIfVersions(Policy policy) {
        whatIfVersionChoice.getItems().clear();
        whatIfRuleChoice.getItems().clear();
        lastSimulation = null;
        saveWhatIfButton.setDisable(true);
        whatIfFlips.getItems().clear();
        if (policy == null) {
            whatIfVersionChoice.setValue(null);
            whatIfSummary.setText("Select a policy to simulate its versions.");
            return;
        }
        whatIfVersionChoice.getItems().setAll(policy.getVersions());
        PolicyVersion selected = AppContext.getInstance().selectedVersionProperty().get();
        if (selected == null || !selected.getPolicyId().equals(policy.getId())) {
            selected = policy.getVersion(policy.getActiveVersionNumber())
                    .orElseGet(() -> policy.getLatestVersion().orElse(null));
        }
        whatIfVersionChoice.setValue(selected);
    }

    private void configureWhatIfRules(PolicyVersion version) {
        List<WhatIfSimulator.NumericRule> rules = whatIfSimulator.findNumericRules(version);
        whatIfRuleChoice.getItems().setAll(rules);
        whatIfRuleChoice.setValue(rules.isEmpty() ? null : rules.get(0));
        if (rules.isEmpty()) {
            whatIfSummary.setText("This version has no numeric rules to adjust.");
        } else {
            whatIfSummary.setText("The simulation compares against saved results and does not write data.");
        }
        lastSimulation = null;
        saveWhatIfButton.setDisable(true);
        whatIfFlips.getItems().clear();
    }

    private void configureWhatIfThreshold(WhatIfSimulator.NumericRule rule) {
        if (rule == null) {
            whatIfThreshold.setDisable(true);
            return;
        }
        String field = rule.fieldName().toLowerCase().replace("_", "");
        double min = 0;
        double max;
        if (field.equals("gpa")) {
            max = 10;
        } else if (field.equals("interview") || field.equals("interviewscore")) {
            max = 100;
        } else {
            min = Math.min(0, rule.threshold());
            max = Math.max(100, rule.threshold() + Math.max(10, Math.abs(rule.threshold()) * 0.5));
        }
        max = Math.max(max, rule.threshold());
        whatIfThreshold.setMin(min);
        whatIfThreshold.setMax(max);
        whatIfThreshold.setMajorTickUnit(field.equals("gpa") ? 1 : 10);
        whatIfThreshold.setMinorTickCount(4);
        whatIfThreshold.setBlockIncrement(field.equals("gpa") ? 0.1 : 1);
        whatIfThreshold.setValue(rule.threshold());
        whatIfThreshold.setDisable(false);
        whatIfThresholdValue.setText(String.format("%.2f", rule.threshold()));
    }

    private void runWhatIfSimulation() {
        AppContext ctx = AppContext.getInstance();
        Policy policy = ctx.selectedPolicyProperty().get();
        PolicyVersion version = whatIfVersionChoice.getValue();
        WhatIfSimulator.NumericRule rule = whatIfRuleChoice.getValue();
        if (policy == null || version == null || rule == null) {
            ctx.showToast(Toast.error("What-if Simulation Unavailable",
                    "Select a policy version and numeric rule first.", null));
            return;
        }

        List<Applicant> cachedApplicants = new ArrayList<>(ctx.getApplicants());
        double threshold = whatIfThreshold.getValue();
        runWhatIfButton.setDisable(true);
        saveWhatIfButton.setDisable(true);
        whatIfSummary.setText("Running what-if simulation...");
        showSkeleton(whatIfResultsSlot, 2);

        Task<WhatIfSimulator.SimulationResult> task = new Task<>() {
            @Override
            protected WhatIfSimulator.SimulationResult call() {
                List<Applicant> applicants = ctx.getApplicantRepository() == null
                        ? cachedApplicants
                        : ctx.getApplicantRepository().findAll().stream()
                                .map(ApplicantDocumentMapper::toApplicant)
                                .toList();
                List<EvaluationResult> storedResults = ctx.getEvaluationResultRepository()
                        .findByPolicyAndVersion(policy.getId(), version.getVersionNumber());
                return whatIfSimulator.simulate(version, rule, threshold, applicants, storedResults);
            }
        };
        task.setOnSucceeded(event -> {
            WhatIfSimulator.SimulationResult result = task.getValue();
            lastSimulation = result;
            whatIfSummary.setText(String.format(
                    "Simulated passes: %d / %d | Real passes: %d | Delta: %+d",
                    result.passCount(), result.comparedCount(),
                    result.baselinePassCount(), result.delta()));
            whatIfDelta.setText(String.format("Pass change: %+d", result.delta()));
            whatIfDelta.getStyleClass().removeAll("flip-chip-gain", "flip-chip-loss");
            whatIfDelta.getStyleClass().add(result.delta() < 0 ? "flip-chip-loss" : "flip-chip-gain");
            whatIfFlips.getItems().setAll(result.flips());
            stopShimmer(whatIfResultsSlot);
            whatIfResultsSlot.getChildren().setAll(whatIfFlips);
            saveWhatIfButton.setDisable(false);
            runWhatIfButton.setDisable(false);
            ctx.showToast(Toast.success("What-if Complete",
                    "Compared " + result.comparedCount() + " applicants; no results were saved.", null));
        });
        task.setOnFailed(event -> {
            runWhatIfButton.setDisable(false);
            Throwable failure = task.getException();
            if (failure != null) {
                AppContext.logFailure("What-if simulation failed", failure);
                stopShimmer(whatIfResultsSlot);
                whatIfResultsSlot.getChildren().setAll(createErrorState(
                        "Simulation failed", "Could not complete the simulation.",
                        "Retry", this::runWhatIfSimulation));
                ctx.showToast(Toast.errorWithAction(
                        "What-if Simulation Failed",
                        failure.getMessage() == null ? "The simulation could not be completed."
                                : MongoConfig.sanitize(failure.getMessage()),
                        "Retry", this::runWhatIfSimulation));
            }
        });
        task.setOnCancelled(event -> {
            runWhatIfButton.setDisable(false);
            whatIfSummary.setText("What-if simulation cancelled.");
            stopShimmer(whatIfResultsSlot);
            whatIfResultsSlot.getChildren().setAll(whatIfFlips);
        });
        ctx.getExecutor().execute(task);
    }

    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(3);
        titleBox.setMinWidth(0);
        Label title = new Label("System Dashboard");
        title.setMinWidth(0);
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Decision outcomes, criteria compliance, and version impact");
        subtitle.setMinWidth(0);
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        policyChoice.setPromptText("Select policy");
        policyChoice.setPrefWidth(190);
        policyChoice.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Policy policy, boolean empty) {
                super.updateItem(policy, empty);
                setText(empty || policy == null ? null : policy.getName());
            }
        });
        policyChoice.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Policy policy, boolean empty) {
                super.updateItem(policy, empty);
                setText(empty || policy == null ? "Select policy" : policy.getName());
            }
        });
        policyChoice.getItems().setAll(AppContext.getInstance().getPolicies());
        selectedPolicyChip.getStyleClass().addAll("policy-chip", "policy-chip-text");
        versionChip.getStyleClass().add("version-chip");
        lastUpdatedLabel.getStyleClass().add("last-updated");
        refreshButton.getStyleClass().add("btn-icon");
        refreshButton.setGraphic(Icons.create(Icons.REFRESH, 0.8, "#9aa0c3"));
        refreshButton.setTooltip(new Tooltip("Refresh dashboard"));
        evaluateAllButton.getStyleClass().add("btn-primary");
        cancelEvaluationButton.getStyleClass().add("btn-secondary");
        cancelEvaluationButton.setVisible(false);
        cancelEvaluationButton.setManaged(false);

        FlowPane toolbar = new FlowPane(8, 8, policyChoice, selectedPolicyChip, versionChip,
                lastUpdatedLabel, refreshButton, evaluateAllButton, cancelEvaluationButton);
        toolbar.getStyleClass().add("dash-toolbar-controls");
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        toolbar.setMaxWidth(Double.MAX_VALUE);
        toolbar.setMinWidth(0);
        HBox.setHgrow(toolbar, Priority.ALWAYS);
        header.getStyleClass().add("dash-toolbar");
        header.getChildren().addAll(titleBox, toolbar);
        return header;
    }

    private void createKpiRow() {
        totalCard = new KpiCard("TOTAL APPLICANTS", "0", "Registered candidates", Icons.APPLICANTS, "#89b4fa");
        eligibleCard = new KpiCard("ELIGIBLE", "0", "Criteria satisfied", Icons.CHECK, "#a6e3a1");
        rejectedCard = new KpiCard("REJECTED", "0", "One or more criteria failed", Icons.CLOSE, "#f38ba8");
        passRateCard = new KpiCard("PASS RATE", "0.0%", "Eligible / evaluated", Icons.SPARKLES, "#cba6f7");
        impactCard = new KpiCard("VERSION IMPACT", "0", "Changed decisions", Icons.COMPARISON, "#f9e2af");
        kpiGrid = new GridPane();
        kpiGrid.setHgap(16);
        kpiGrid.setVgap(16);
        kpiContent = new VBox(12, kpiGrid);
        kpiSlot.getStyleClass().add("widget-slot");
        kpiSlot.getChildren().setAll(kpiContent);
        layoutKpiCards(0);
    }

    private void layoutKpiCards(double width) {
        double screenWidth = width + SIDEBAR_WIDTH;
        int columns = screenWidth >= 1300 ? 5 : 3;
        if (screenWidth < 900) {
            columns = 2;
        }
        kpiGrid.getChildren().clear();
        List<KpiCard> cards = List.of(totalCard, eligibleCard, rejectedCard, passRateCard, impactCard);
        for (int i = 0; i < cards.size(); i++) {
            int row = i / columns;
            int column = i % columns;
            kpiGrid.add(cards.get(i), column, row);
            cards.get(i).setMinWidth(0);
            cards.get(i).setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(cards.get(i), Priority.ALWAYS);
            GridPane.setFillWidth(cards.get(i), true);
        }
        kpiGrid.getColumnConstraints().clear();
        for (int i = 0; i < columns; i++) {
            ColumnConstraints constraint = new ColumnConstraints();
            constraint.setPercentWidth(100.0 / columns);
            constraint.setHgrow(Priority.ALWAYS);
            kpiGrid.getColumnConstraints().add(constraint);
        }
    }

    private void createChartsRow() {
        pieWidget = new VBox(12);
        pieWidget.getStyleClass().add("card");
        Label pieTitle = new Label("DECISION OUTCOME");
        pieTitle.getStyleClass().add("caption-label");
        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(false);
        pieChart.setAnimated(!AppContext.getInstance().reducedMotionProperty().get());
        pieChart.setPrefHeight(300);
        pieChart.setMinHeight(250);
        donutHole.getStyleClass().add("donut-hole");
        VBox centerLabels = new VBox(2, donutCenterValue, donutCenterLabel);
        centerLabels.setAlignment(Pos.CENTER);
        donutCenterValue.getStyleClass().add("donut-center-value");
        donutCenterLabel.getStyleClass().add("donut-center-label");
        donutCenter.setMouseTransparent(true);
        donutCenter.getChildren().setAll(donutHole, centerLabels);
        pieVisual = new StackPane(pieChart, donutCenter);
        pieVisual.setMinHeight(280);
        chartCard.getStyleClass().add("widget-slot");
        chartCard.setMinHeight(280);
        chartCard.getChildren().setAll(pieVisual);
        pieWidget.getChildren().addAll(pieTitle, chartCard);
        VBox.setVgrow(chartCard, Priority.ALWAYS);

        barWidget = new VBox(12);
        barWidget.getStyleClass().add("card");
        Label barTitle = new Label("CRITERIA COMPLIANCE");
        barTitle.getStyleClass().add("caption-label");
        barXAxis = new NumberAxis();
        barXAxis.setLabel("Applicants");
        barYAxis = new CategoryAxis();
        barYAxis.setLabel("Evaluation criterion");
        barChart = new BarChart<>(barXAxis, barYAxis);
        barChart.setLegendVisible(true);
        barChart.setAnimated(!AppContext.getInstance().reducedMotionProperty().get());
        barChart.setPrefHeight(300);
        barChart.setMinHeight(250);
        barCard.getStyleClass().add("widget-slot");
        barCard.setMinHeight(280);
        barCard.getChildren().setAll(barChart);
        barWidget.getChildren().addAll(barTitle, barCard);
        VBox.setVgrow(barCard, Priority.ALWAYS);
    }

    private VBox createRecentEvaluationsCard() {
        recentWidget = new VBox(12);
        recentWidget.getStyleClass().add("card");
        Label title = new Label("RECENT EVALUATIONS");
        title.getStyleClass().add("caption-label");
        configureRecentTable();
        recentEvaluations.setPrefHeight(300);
        recentEvaluations.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        recentEvaluationsSlot.getStyleClass().add("widget-slot");
        recentWidget.getChildren().addAll(title, recentEvaluationsSlot);
        VBox.setVgrow(recentEvaluationsSlot, Priority.ALWAYS);
        return recentWidget;
    }

    private void configureRecentTable() {
        TableColumn<EvaluationResult, EvaluationResult> applicantColumn = new TableColumn<>("Applicant");
        applicantColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        applicantColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(EvaluationResult result, boolean empty) {
                super.updateItem(result, empty);
                if (empty || result == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                String name = Objects.requireNonNullElse(result.getApplicantName(), "Unknown applicant");
                Label nameLabel = new Label(name);
                nameLabel.getStyleClass().add("insight-title");
                HBox row = new HBox(8, new Avatar(name), nameLabel);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
            }
        });
        TableColumn<EvaluationResult, EvaluationResult> decisionColumn = new TableColumn<>("Decision");
        decisionColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        decisionColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(EvaluationResult result, boolean empty) {
                super.updateItem(result, empty);
                if (empty || result == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                setGraphic(result.isOverallDecision()
                        ? StatusBadge.pass("ELIGIBLE") : StatusBadge.fail("REJECTED"));
            }
        });
        TableColumn<EvaluationResult, String> timeColumn = new TableColumn<>("Evaluated");
        timeColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatEvaluationTime(
                cell.getValue().getEvaluatedOn())));
        recentEvaluations.getColumns().setAll(applicantColumn, decisionColumn, timeColumn);
    }

    private VBox createNeedsAttentionCard() {
        attentionWidget = new VBox(12);
        attentionWidget.getStyleClass().add("card");
        Label title = new Label("NEEDS ATTENTION");
        title.getStyleClass().add("caption-label");
        needsAttentionList.setFillWidth(true);
        needsAttentionSlot.getStyleClass().add("widget-slot");
        needsAttentionSlot.getChildren().setAll(needsAttentionList);
        attentionWidget.getChildren().addAll(title, needsAttentionSlot);
        VBox.setVgrow(needsAttentionSlot, Priority.ALWAYS);
        return attentionWidget;
    }

    private void layoutDashboard(HBox header, VBox recentCard, VBox attentionCard,
                                 VBox whatIfCard, double width) {
        double screenWidth = width + SIDEBAR_WIDTH;
        int columns = screenWidth >= 1300 ? 3 : 2;
        if (screenWidth < 900) {
            columns = 1;
        }
        layoutKpiCards(width);
        if (responsiveColumnCount == columns && !contentGrid.getChildren().isEmpty()) return;
        responsiveColumnCount = columns;
        contentGrid.getChildren().clear();
        contentGrid.getColumnConstraints().clear();
        for (int i = 0; i < columns; i++) {
            ColumnConstraints constraint = new ColumnConstraints();
            constraint.setPercentWidth(100.0 / columns);
            constraint.setHgrow(Priority.ALWAYS);
            constraint.setFillWidth(true);
            contentGrid.getColumnConstraints().add(constraint);
        }
        contentGrid.add(header, 0, 0, columns, 1);
        contentGrid.add(kpiSlot, 0, 1, columns, 1);
        if (columns == 3) {
            contentGrid.add(pieWidget, 0, 2);
            contentGrid.add(barWidget, 1, 2, 2, 1);
            contentGrid.add(recentCard, 0, 3, 2, 1);
            contentGrid.add(attentionCard, 2, 3);
        } else if (columns == 2) {
            contentGrid.add(pieWidget, 0, 2);
            contentGrid.add(barWidget, 1, 2);
            contentGrid.add(recentCard, 0, 3);
            contentGrid.add(attentionCard, 1, 3);
        } else {
            contentGrid.add(pieWidget, 0, 2);
            contentGrid.add(barWidget, 0, 3);
            contentGrid.add(recentCard, 0, 4);
            contentGrid.add(attentionCard, 0, 5);
        }
        contentGrid.add(whatIfCard, 0, columns == 1 ? 6 : 4, columns, 1);
        for (Node node : contentGrid.getChildren()) {
            if (node instanceof Region region) {
                region.setMinWidth(0);
                region.setMaxWidth(Double.MAX_VALUE);
                GridPane.setHgrow(region, Priority.ALWAYS);
                GridPane.setFillWidth(region, true);
            }
        }
        contentBox.getChildren().setAll(contentGrid);
    }

    private void syncHeaderPolicy(Policy policy) {
        syncingPolicyChoice = true;
        policyChoice.getItems().setAll(AppContext.getInstance().getPolicies());
        policyChoice.setValue(policy);
        syncingPolicyChoice = false;
        selectedPolicyChip.setText(policy == null ? "No policy selected" : policy.getName());
        selectedPolicyChip.getStyleClass().remove("policy-chip-muted");
        if (policy == null) selectedPolicyChip.getStyleClass().add("policy-chip-muted");
    }

    private void updateVersionChip(PolicyVersion version) {
        versionChip.setText(version == null ? "No version" : "v" + version.getVersionNumber());
    }

    public void refreshDashboard() {
        AppContext ctx = AppContext.getInstance();
        Policy policy = ctx.selectedPolicyProperty().get();
        PolicyVersion version = ctx.selectedVersionProperty().get();
        long generation = ++refreshGeneration;
        List<Applicant> cachedApplicants = new ArrayList<>(ctx.getApplicants());
        showSkeleton(kpiSlot, 5);
        showSkeleton(chartCard, 1);
        showSkeleton(barCard, 1);
        showSkeleton(recentEvaluationsSlot, 3);
        showSkeleton(needsAttentionSlot, 3);

        Task<DashboardData> kpiTask = new Task<>() {
            @Override
            protected DashboardData call() {
                List<Applicant> applicants = ctx.getApplicantRepository() == null ? cachedApplicants
                        : ctx.getApplicantRepository().findAll().stream()
                                .map(ApplicantDocumentMapper::toApplicant).toList();
                List<EvaluationResult> results = loadResults(ctx, policy, version);
                List<EvaluationResult> previous = version != null && version.getVersionNumber() > 1
                        ? loadResults(ctx, policy, version.getVersionNumber() - 1) : List.of();
                return new DashboardData(applicants, results, previous);
            }
        };
        loadWidget(kpiSlot, generation, kpiTask, data -> renderKpis(data, policy, version),
                "Dashboard KPI load failed", 5, this::refreshDashboard);

        Task<List<EvaluationResult>> chartTask = resultsTask(ctx, policy, version);
        loadWidget(chartCard, generation, chartTask, this::renderPieChart,
                "Dashboard outcome chart load failed", 1, this::refreshDashboard);

        Task<List<EvaluationResult>> complianceTask = resultsTask(ctx, policy, version);
        loadWidget(barCard, generation, complianceTask, this::renderComplianceChart,
                "Dashboard criteria chart load failed", 1, this::refreshDashboard);

        Task<List<EvaluationResult>> recentTask = resultsTask(ctx, policy, version);
        loadWidget(recentEvaluationsSlot, generation, recentTask, this::renderRecentEvaluations,
                "Dashboard recent evaluations load failed", 3, this::refreshDashboard);

        Task<List<EvaluationResult>> attentionTask = resultsTask(ctx, policy, version);
        loadWidget(needsAttentionSlot, generation, attentionTask, this::renderNeedsAttention,
                "Dashboard needs-attention load failed", 3, this::refreshDashboard);
    }

    private Task<List<EvaluationResult>> resultsTask(AppContext ctx, Policy policy, PolicyVersion version) {
        return new Task<>() {
            @Override
            protected List<EvaluationResult> call() {
                return loadResults(ctx, policy, version);
            }
        };
    }

    private List<EvaluationResult> loadResults(AppContext ctx, Policy policy, PolicyVersion version) {
        return version == null || policy == null || ctx.getEvaluationResultRepository() == null
                ? List.of()
                : ctx.getEvaluationResultRepository().findByPolicyAndVersion(
                        policy.getId(), version.getVersionNumber());
    }

    private List<EvaluationResult> loadResults(AppContext ctx, Policy policy, int versionNumber) {
        return policy == null || ctx.getEvaluationResultRepository() == null
                ? List.of()
                : ctx.getEvaluationResultRepository().findByPolicyAndVersion(policy.getId(), versionNumber);
    }

    private <T> void loadWidget(StackPane slot, long generation, Task<T> task,
                                java.util.function.Consumer<T> success, String failureContext,
                                int skeletonCount, Runnable retry) {
        task.setOnSucceeded(event -> {
            if (generation != refreshGeneration) return;
            stopShimmer(slot);
            success.accept(task.getValue());
            lastUpdatedLabel.setText("Updated " + updatedFormatter.format(LocalDateTime.now()));
        });
        task.setOnCancelled(event -> {
            if (generation == refreshGeneration) stopShimmer(slot);
        });
        task.setOnFailed(event -> {
            if (generation != refreshGeneration) return;
            stopShimmer(slot);
            Throwable failure = task.getException();
            if (failure != null) {
                AppContext.logFailure(failureContext, failure);
                String message = failure.getMessage() == null || failure.getMessage().isBlank()
                        ? "Dashboard data could not be loaded."
                        : MongoConfig.sanitize(failure.getMessage());
                slot.getChildren().setAll(createErrorState("Unable to load this widget", message,
                        "Retry", retry));
                AppContext.getInstance().showToast(
                        Toast.errorWithAction("Dashboard Data Failed", message, "Retry", retry));
            }
        });
        AppContext.getInstance().getExecutor().execute(task);
    }

    private void renderKpis(DashboardData data, Policy policy, PolicyVersion version) {
        kpiSlot.getChildren().setAll(kpiContent);
        List<EvaluationResult> results = data.results();
        int eligible = (int) results.stream().filter(EvaluationResult::isOverallDecision).count();
        int rejected = results.size() - eligible;
        totalCard.setValue(data.applicants().size());
        totalCard.setDeltaText("Current");
        totalCard.setDeltaStyle("delta-flat");
        eligibleCard.setValue(eligible);
        rejectedCard.setValue(rejected);
        if (results.isEmpty()) {
            passRateCard.setValueText("N/A");
            impactCard.setValueText("0");
            eligibleCard.setDeltaText("No results");
            rejectedCard.setDeltaText("No results");
            passRateCard.setDeltaText("No results");
            impactCard.setDeltaText("No results");
            eligibleCard.setDeltaStyle("delta-flat");
            rejectedCard.setDeltaStyle("delta-flat");
            passRateCard.setDeltaStyle("delta-flat");
            impactCard.setDeltaStyle("delta-flat");
            kpiContent.getChildren().setAll(kpiGrid, new EmptyState(
                    Icons.EVALUATION, "No evaluations yet",
                    "Evaluate applicants to populate policy-specific dashboard metrics.",
                    "Evaluate All", this::evaluateAll));
        } else {
            kpiContent.getChildren().setAll(kpiGrid);
            passRateCard.setValuePercent(eligible * 100.0 / results.size());
            if (policy != null && version != null && version.getVersionNumber() > 1) {
                ComparisonReport report = AppContext.getInstance().getComparisonService()
                        .compareEvaluations(data.previousResults(), results);
                int changed = report.getChangedApplicants().size();
                impactCard.setValue(changed);
                impactCard.setSubtext("vs v" + (version.getVersionNumber() - 1));
                if (data.previousResults().isEmpty()) {
                    eligibleCard.setDeltaText("No prior data");
                    rejectedCard.setDeltaText("No prior data");
                    passRateCard.setDeltaText("No prior data");
                    eligibleCard.setDeltaStyle("delta-flat");
                    rejectedCard.setDeltaStyle("delta-flat");
                    passRateCard.setDeltaStyle("delta-flat");
                } else {
                    eligibleCard.setDeltaText(String.format("%+d vs prev",
                            report.getGainedEligibilityCount() - report.getLostEligibilityCount()));
                    rejectedCard.setDeltaText(String.format("%+d vs prev",
                            report.getLostEligibilityCount() - report.getGainedEligibilityCount()));
                    double previousRate = data.previousResults().stream()
                            .filter(EvaluationResult::isOverallDecision).count()
                            * 100.0 / data.previousResults().size();
                    double change = eligible * 100.0 / results.size() - previousRate;
                    passRateCard.setDeltaText(String.format("%+.1f pp", change));
                    eligibleCard.setDeltaStyle(change >= 0 ? "delta-up" : "delta-down");
                    rejectedCard.setDeltaStyle(change <= 0 ? "delta-up" : "delta-down");
                    passRateCard.setDeltaStyle(change >= 0 ? "delta-up" : "delta-down");
                }
                impactCard.setDeltaText(String.format("%d lost · %d gained",
                        report.getLostEligibilityCount(), report.getGainedEligibilityCount()));
                impactCard.setDeltaStyle(report.getLostEligibilityCount() > report.getGainedEligibilityCount()
                        ? "delta-down" : report.getGainedEligibilityCount() > report.getLostEligibilityCount()
                                ? "delta-up" : "delta-flat");
            } else {
                eligibleCard.setDeltaText("Baseline");
                rejectedCard.setDeltaText("Baseline");
                passRateCard.setDeltaText("Baseline");
                eligibleCard.setDeltaStyle("delta-flat");
                rejectedCard.setDeltaStyle("delta-flat");
                passRateCard.setDeltaStyle("delta-flat");
                impactCard.setValueText("Baseline");
                impactCard.setSubtext("Version 1 initial baseline");
                impactCard.setDeltaText("Initial");
                impactCard.setDeltaStyle("delta-flat");
            }
        }
        if (!hasAnimatedEntrance) {
            Motion.staggeredFadeUp(List.of(totalCard, eligibleCard, rejectedCard, passRateCard, impactCard));
            hasAnimatedEntrance = true;
        }
    }

    private void renderPieChart(List<EvaluationResult> results) {
        if (results.isEmpty()) {
            chartCard.getChildren().setAll(new EmptyState(Icons.EVALUATION, "No evaluations yet",
                    "Evaluate applicants to see the eligible and rejected distribution.",
                    "Evaluate All", this::evaluateAll));
            return;
        }
        if (chartCard.getChildren().isEmpty() || chartCard.getChildren().get(0) != pieVisual) {
            chartCard.getChildren().setAll(pieVisual);
        }
        int passed = (int) results.stream().filter(EvaluationResult::isOverallDecision).count();
        int failed = results.size() - passed;
        double passRate = passed * 100.0 / results.size();
        pieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Eligible (" + passed + ")", passed),
                new PieChart.Data("Rejected (" + failed + ")", failed)));
        donutCenterValue.setText(String.format("%.0f%%", passRate));
        for (PieChart.Data slice : pieChart.getData()) {
            String tooltipText = slice.getName() + ": " + (int) slice.getPieValue() + " applicants";
            slice.nodeProperty().addListener((observable, oldNode, node) -> {
                if (node != null) Tooltip.install(node, new Tooltip(tooltipText));
            });
            if (slice.getNode() != null) Tooltip.install(slice.getNode(), new Tooltip(tooltipText));
        }
    }

    private void renderComplianceChart(List<EvaluationResult> results) {
        if (results.isEmpty()) {
            barCard.getChildren().setAll(new EmptyState(Icons.POLICIES, "No criteria data",
                    "Criteria compliance appears after applicants have been evaluated.",
                    "Evaluate All", this::evaluateAll));
            return;
        }
        if (barCard.getChildren().isEmpty() || barCard.getChildren().get(0) != barChart) {
            barCard.getChildren().setAll(barChart);
        }
        Map<String, Integer> passed = new HashMap<>();
        Map<String, Integer> failed = new HashMap<>();
        for (EvaluationResult result : results) {
            for (ConditionResult condition : result.getConditionResults()) {
                String name = Objects.requireNonNullElse(condition.getConditionName(), "Unnamed criterion");
                (condition.isPassed() ? passed : failed).merge(name, 1, Integer::sum);
            }
        }
        if (passed.isEmpty() && failed.isEmpty()) {
            barCard.getChildren().setAll(new EmptyState(Icons.POLICIES, "No criteria data",
                    "These evaluations do not contain condition-level results.",
                    "Evaluate All", this::evaluateAll));
            return;
        }
        List<String> names = new ArrayList<>();
        names.addAll(passed.keySet());
        failed.keySet().stream().filter(name -> !passed.containsKey(name)).forEach(names::add);
        names.sort(String.CASE_INSENSITIVE_ORDER);
        XYChart.Series<Number, String> passSeries = new XYChart.Series<>();
        passSeries.setName("Passed");
        XYChart.Series<Number, String> failSeries = new XYChart.Series<>();
        failSeries.setName("Failed");
        for (String name : names) {
            passSeries.getData().add(new XYChart.Data<>(passed.getOrDefault(name, 0), name));
            failSeries.getData().add(new XYChart.Data<>(failed.getOrDefault(name, 0), name));
        }
        barChart.getData().setAll(passSeries, failSeries);
        for (XYChart.Series<Number, String> series : barChart.getData()) {
            for (XYChart.Data<Number, String> datum : series.getData()) {
                String tooltipText = series.getName() + ": " + datum.getYValue()
                        + " — " + datum.getXValue();
                datum.nodeProperty().addListener((observable, oldNode, node) -> {
                    if (node != null) Tooltip.install(node, new Tooltip(tooltipText));
                });
                if (datum.getNode() != null) Tooltip.install(datum.getNode(), new Tooltip(tooltipText));
            }
        }
    }

    private void renderRecentEvaluations(List<EvaluationResult> results) {
        List<EvaluationResult> recent = results.stream()
                .sorted(Comparator.comparing(EvaluationResult::getEvaluatedOn,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .toList();
        if (recent.isEmpty()) {
            recentEvaluationsSlot.getChildren().setAll(new EmptyState(Icons.EVALUATION,
                    "No recent evaluations", "Run an evaluation to see recent decisions.",
                    "Evaluate All", this::evaluateAll));
            return;
        }
        recentEvaluations.getItems().setAll(recent);
        recentEvaluationsSlot.getChildren().setAll(recentEvaluations);
    }

    private void renderNeedsAttention(List<EvaluationResult> results) {
        if (results.isEmpty()) {
            needsAttentionSlot.getChildren().setAll(new EmptyState(Icons.CHECK,
                    "Nothing to flag yet", "Evaluate applicants to identify criteria that need attention.",
                    "Evaluate All", this::evaluateAll));
            return;
        }
        Map<String, int[]> counts = new HashMap<>();
        for (EvaluationResult result : results) {
            for (ConditionResult condition : result.getConditionResults()) {
                String name = Objects.requireNonNullElse(condition.getConditionName(), "Unnamed criterion");
                int[] count = counts.computeIfAbsent(name, ignored -> new int[2]);
                count[1]++;
                if (!condition.isPassed()) count[0]++;
            }
        }
        List<Map.Entry<String, int[]>> attention = counts.entrySet().stream()
                .filter(entry -> entry.getValue()[0] > 0)
                .sorted(Map.Entry.<String, int[]>comparingByValue(
                        Comparator.comparingInt((int[] count) -> count[0]).reversed()))
                .limit(6)
                .collect(Collectors.toList());
        if (attention.isEmpty()) {
            needsAttentionSlot.getChildren().setAll(new EmptyState(Icons.CHECK,
                    "No failing criteria", "All recorded criteria passed for this version.",
                    null, null));
            return;
        }
        needsAttentionList.getChildren().clear();
        for (Map.Entry<String, int[]> entry : attention) {
            Label name = new Label(entry.getKey());
            name.getStyleClass().add("insight-title");
            Label meta = new Label(entry.getValue()[0] + " failed of " + entry.getValue()[1]);
            meta.getStyleClass().add("margin-chip");
            Hyperlink action = new Hyperlink("Re-evaluate");
            action.getStyleClass().add("link-action");
            action.setOnAction(event -> evaluateAll());
            HBox.setHgrow(name, Priority.ALWAYS);
            HBox row = new HBox(10, name, meta, action);
            row.getStyleClass().add("insight-row");
            row.setAlignment(Pos.CENTER_LEFT);
            needsAttentionList.getChildren().add(row);
        }
        needsAttentionSlot.getChildren().setAll(needsAttentionList);
    }

    private void showSkeleton(StackPane slot, int blocks) {
        stopShimmer(slot);
        HBox skeleton = new HBox(12);
        skeleton.setAlignment(Pos.CENTER_LEFT);
        List<Region> regions = new ArrayList<>();
        for (int i = 0; i < blocks; i++) {
            Region region = new Region();
            region.getStyleClass().addAll("skeleton", blocks == 1 ? "skeleton-block" : "skeleton-line");
            HBox.setHgrow(region, Priority.ALWAYS);
            region.setMaxWidth(Double.MAX_VALUE);
            regions.add(region);
            skeleton.getChildren().add(region);
        }
        skeleton.setMinHeight(blocks == 1 ? 160 : 100);
        slot.getChildren().setAll(skeleton);
        skeletonRegions.put(slot, regions);
        if (!AppContext.getInstance().reducedMotionProperty().get()) startShimmer(slot, regions);
    }

    private void startShimmer(StackPane slot, List<Region> regions) {
        if (shimmerAnimations.containsKey(slot)) return;
        List<Timeline> timelines = new ArrayList<>();
        for (Region region : regions) {
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(region.opacityProperty(), 0.45)),
                    new KeyFrame(Duration.millis(900), new javafx.animation.KeyValue(
                            region.opacityProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH)));
            timeline.setAutoReverse(true);
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();
            timelines.add(timeline);
        }
        shimmerAnimations.put(slot, timelines);
    }

    private void stopShimmer(StackPane slot) {
        List<Timeline> timelines = shimmerAnimations.remove(slot);
        if (timelines != null) timelines.forEach(Timeline::stop);
        List<Region> regions = skeletonRegions.remove(slot);
        if (regions != null) regions.forEach(region -> region.setOpacity(1));
    }

    private VBox createErrorState(String title, String message, String actionText, Runnable retry) {
        VBox state = new VBox(10);
        state.getStyleClass().add("error-state");
        state.setAlignment(Pos.CENTER);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-title");
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("empty-text");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(360);
        Button retryButton = new Button(actionText);
        retryButton.getStyleClass().add("btn-secondary");
        retryButton.setOnAction(event -> retry.run());
        state.getChildren().addAll(titleLabel, messageLabel, retryButton);
        return state;
    }

    private void updateShimmerAnimations(boolean reducedMotion) {
        if (reducedMotion) {
            shimmerAnimations.values().forEach(timelines -> timelines.forEach(Timeline::stop));
            shimmerAnimations.clear();
            skeletonRegions.values().forEach(regions -> regions.forEach(region -> region.setOpacity(1)));
        } else {
            skeletonRegions.forEach(this::startShimmer);
        }
    }

    private void evaluateAll() {
        AppContext ctx = AppContext.getInstance();
        Policy policy = ctx.selectedPolicyProperty().get();
        PolicyVersion version = ctx.selectedVersionProperty().get();
        if (policy == null || version == null) {
            ctx.showToast(Toast.error("Evaluation Unavailable", "Select a policy and version first.", null));
            return;
        }
        if (activeEvaluationTask != null
                && activeEvaluationTask.getState() == javafx.concurrent.Worker.State.RUNNING) return;
        List<Applicant> cached = new ArrayList<>(ctx.getApplicants());
        Task<List<EvaluationResult>> task = new Task<>() {
            @Override
            protected List<EvaluationResult> call() {
                List<Applicant> applicants = ctx.getApplicantRepository() == null ? cached
                        : ctx.getApplicantRepository().findAll().stream()
                                .map(ApplicantDocumentMapper::toApplicant).toList();
                updateMessage("Evaluating 0/" + applicants.size());
                return ctx.getPolicyService().evaluateApplicants(policy.getId(), version.getVersionNumber(),
                        applicants, count -> updateMessage("Evaluating " + count + "/" + applicants.size()),
                        this::isCancelled);
            }
        };
        activeEvaluationTask = task;
        evaluateAllButton.setDisable(true);
        cancelEvaluationButton.setVisible(true);
        cancelEvaluationButton.setManaged(true);
        evaluateAllButton.textProperty().bind(task.messageProperty());
        task.setOnSucceeded(event -> {
            unbindEvaluationButton(task);
            ctx.markResultsChanged();
            ctx.showToast(Toast.success("Evaluation Complete",
                    "Stored results for " + task.getValue().size() + " applicants.", null));
        });
        task.setOnFailed(event -> {
            unbindEvaluationButton(task);
            Throwable failure = task.getException();
            if (failure != null) {
                AppContext.logFailure("Dashboard evaluation failed", failure);
                ctx.showToast(Toast.errorWithAction("Evaluation Failed",
                        failure.getMessage() == null ? "Could not evaluate applicants."
                                : MongoConfig.sanitize(failure.getMessage()),
                        "Retry", this::evaluateAll));
            }
        });
        task.setOnCancelled(event -> {
            unbindEvaluationButton(task);
            ctx.showToast(Toast.info("Evaluation Cancelled", "No further applicants were evaluated.", null));
        });
        ctx.getExecutor().execute(task);
    }

    private void unbindEvaluationButton(Task<?> task) {
        if (evaluateAllButton.textProperty().isBound()) evaluateAllButton.textProperty().unbind();
        evaluateAllButton.setText("Evaluate All");
        evaluateAllButton.setDisable(false);
        cancelEvaluationButton.setVisible(false);
        cancelEvaluationButton.setManaged(false);
        if (activeEvaluationTask == task) activeEvaluationTask = null;
    }

    private String formatEvaluationTime(LocalDateTime evaluatedOn) {
        return evaluatedOn == null ? "Unknown" : updatedFormatter.format(evaluatedOn);
    }

    private record DashboardData(List<Applicant> applicants, List<EvaluationResult> results,
                                 List<EvaluationResult> previousResults) { }

    @Override
    public Node getRoot() {
        return rootPane;
    }

    @Override
    public void onShow() {
        configureWhatIfVersions(AppContext.getInstance().selectedPolicyProperty().get());
        refreshDashboard();
    }
}
