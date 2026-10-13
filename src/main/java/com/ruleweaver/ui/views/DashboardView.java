package com.ruleweaver.ui.views;

import com.ruleweaver.config.MongoConfig;
import com.ruleweaver.model.*;
import com.ruleweaver.service.WhatIfSimulator;
import com.ruleweaver.service.DashboardAnalytics;
import com.ruleweaver.service.PipelineCalculator;
import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.adapter.ApplicantDocumentMapper;
import com.ruleweaver.ui.components.*;
import com.ruleweaver.ui.shell.Sidebar;
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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Screen 4: Executive Overview Dashboard with live KPI cards, animated charts,
 * rule failure distribution, and policy impact summaries.
 */
public class DashboardView implements BaseView {

    private static final double SIDEBAR_WIDTH = 240;
    private static final int POLICY_HEALTH_ROW_LIMIT = 6;
    private static final int PIPELINE_ATTENTION_LIMIT = 8;
    private final StackPane rootPane = new StackPane();
    private final ScrollPane scrollPane = new ScrollPane();
    private final VBox contentBox = new VBox(24);
    private final GridPane contentGrid = new GridPane();

    // Insight Center KPI cards
    private KpiCard programsCard;
    private KpiCard applicantsCard;
    private KpiCard decisionsCard;
    private KpiCard activePoliciesCard;
    private final StackPane kpiSlot = new StackPane();
    private GridPane kpiGrid;
    private VBox kpiContent;
    private VBox pieWidget;
    private VBox barWidget;
    private VBox recentWidget;
    private VBox attentionWidget;

    // Charts
    private final CategoryAxis approvalTrendXAxis = new CategoryAxis();
    private final NumberAxis approvalTrendYAxis = new NumberAxis();
    private final LineChart<String, Number> approvalTrendChart =
            new LineChart<>(approvalTrendXAxis, approvalTrendYAxis);
    private CategoryAxis barYAxis;
    private NumberAxis barXAxis;
    private BarChart<Number, String> barChart;

    private final StackPane chartCard = new StackPane();
    private final StackPane barCard = new StackPane();
    private final StackPane recentEvaluationsSlot = new StackPane();
    private final StackPane needsAttentionSlot = new StackPane();
    private final StackPane policyHealthRowsSlot = new StackPane();
    private final StackPane pipelineSummarySlot = new StackPane();
    private final StackPane pipelineAttentionSlot = new StackPane();
    private final StackPane whatIfResultsSlot = new StackPane();
    private final TableView<EvaluationResult> recentEvaluations = new TableView<>();
    private final VBox needsAttentionList = new VBox(8);
    private final Label ruleSampleSummaryLabel = new Label();
    private final DateTimeFormatter updatedFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a");
    private final Map<StackPane, List<Timeline>> shimmerAnimations = new HashMap<>();
    private final Map<StackPane, List<Region>> skeletonRegions = new HashMap<>();
    private final ComboBox<ProgramConfiguration> whatIfProgramChoice = new ComboBox<>();
    private final ComboBox<Policy> whatIfPolicyChoice = new ComboBox<>();
    private final ComboBox<PolicyVersion> whatIfVersionChoice = new ComboBox<>();
    private final ComboBox<Policy> policyChoice = new ComboBox<>();
    private final ComboBox<ProgramConfiguration> programFilter = new ComboBox<>();
    private final ComboBox<PolicyVersion> versionFilter = new ComboBox<>();
    private final ComboBox<DashboardAnalytics.RunOption> runFilter = new ComboBox<>();
    private final ComboBox<DashboardAnalytics.DecisionFilter> decisionFilter = new ComboBox<>();
    private final VBox whatIfThresholdEditors = new VBox(8);
    private final VBox whatIfFieldEditors = new VBox(8);
    private final List<ThresholdEditor> thresholdEditors = new ArrayList<>();
    private final List<BooleanRuleEditor> booleanRuleEditors = new ArrayList<>();
    private final List<MultiSelectRuleEditor> multiSelectRuleEditors = new ArrayList<>();
    private final Label whatIfSummary = new Label("Select a program, policy, and base version to begin.");
    private final Label whatIfDelta = new Label("No simulation");
    private final ProgressBar whatIfProgressBar = new ProgressBar();
    private final Label selectedPolicyChip = new Label("No policy selected");
    private final Label lastUpdatedLabel = new Label("Not updated yet");
    private final Button runWhatIfButton = new Button("Run What-if");
    private final Button cancelWhatIfButton = new Button("Cancel Simulation");
    private final Button pipelineEvaluateButton = new Button("Evaluate");
    private final Button saveWhatIfButton = new Button("Save as New Version");
    private final Button refreshButton = new Button();
    private final Button evaluateAllButton = new Button("Evaluate All");
    private final Button cancelEvaluationButton = new Button("Cancel");
    private final TableView<WhatIfSimulator.ApplicantFlip> whatIfFlips = new TableView<>();
    private final WhatIfSimulator whatIfSimulator;
    private final DashboardAnalytics dashboardAnalytics = new DashboardAnalytics();
    private final BiConsumer<Policy, PolicyVersion> openNewVersionDraft;
    private final Consumer<Sidebar.Destination> openDestination;
    private final Runnable openAddApplicant;
    private final PipelineCalculator pipelineCalculator = new PipelineCalculator();
    private final Map<PipelineCacheKey, PipelineCache> pipelineCache = new ConcurrentHashMap<>();
    private VBox qualityWidget;
    private VBox policyHealthWidget;
    private VBox pipelineWidget;
    private GridPane pipelineStageGrid;
    private VBox quickAccessWidget;
    private final List<Label> policyHealthMetricValues = new ArrayList<>();
    private final List<Node> pipelineStageNodes = new ArrayList<>();
    private final List<Label> pipelineStageArrows = new ArrayList<>();
    private final List<Label> pipelineStageValues = new ArrayList<>();
    private final List<Label> pipelineStageDescriptions = new ArrayList<>();
    private final List<ProgressBar> pipelineStageMeters = new ArrayList<>();
    private WhatIfSimulator.SimulationResult lastSimulation;
    private Task<List<EvaluationResult>> activeEvaluationTask;
    private Task<DashboardSnapshot> activeDashboardTask;
    private Task<PipelineCalculator.PipelineSummary> activePipelineSummaryTask;
    private Task<List<PipelineCalculator.AttentionApplicant>> activePipelineAttentionTask;
    private Task<WhatIfSimulator.SimulationResult> activeWhatIfTask;
    private boolean syncingPolicyChoice;
    private boolean syncingFilters;
    private boolean configuringImpactLab;
    private boolean hasAnimatedEntrance;
    private int responsiveColumnCount;
    private long refreshGeneration;
    private long whatIfGeneration;
    private long pipelineGeneration;
    private PipelineCacheKey pendingPipelineKey;
    private PipelineCalculator.PipelineSummary pendingPipelineSummary;
    private List<PipelineCalculator.AttentionApplicant> pendingPipelineAttention;

    public DashboardView() {
        this((policy, version) -> { });
    }

    public DashboardView(BiConsumer<Policy, PolicyVersion> openNewVersionDraft) {
        this(openNewVersionDraft, destination -> { });
    }

    public DashboardView(BiConsumer<Policy, PolicyVersion> openNewVersionDraft,
                         Consumer<Sidebar.Destination> openDestination) {
        this(openNewVersionDraft, openDestination, () -> { });
    }

    public DashboardView(BiConsumer<Policy, PolicyVersion> openNewVersionDraft,
                         Consumer<Sidebar.Destination> openDestination,
                         Runnable openAddApplicant) {
        this.openNewVersionDraft = openNewVersionDraft;
        this.openDestination = openDestination;
        this.openAddApplicant = openAddApplicant;
        this.whatIfSimulator = new WhatIfSimulator(AppContext.getInstance().getDecisionEngine());
        buildUI();
    }

    private void buildUI() {
        rootPane.getStyleClass().add("page-root");
        contentBox.getStyleClass().addAll("dash-page", "insight-center-page", "page-root");
        contentBox.setMinWidth(0);
        contentBox.setMaxWidth(Double.MAX_VALUE);
        contentGrid.setHgap(24);
        contentGrid.setVgap(24);
        contentGrid.setMinWidth(0);
        contentGrid.setMaxWidth(Double.MAX_VALUE);
        rootPane.setMinSize(0, 0);

        VBox header = createHeader();
        FlowPane filters = createFiltersRow();
        createKpiRow();
        createChartsRow();
        policyHealthWidget = createPolicyHealthCard();
        pipelineWidget = createDecisionPipelineCard();
        VBox recentCard = createRecentEvaluationsCard();
        qualityWidget = createDataQualityCard();
        quickAccessWidget = createQuickAccessCard();
        VBox attentionCard = createNeedsAttentionCard();
        VBox whatIfCard = createWhatIfCard();
        layoutDashboard(header, filters, recentCard, attentionCard, whatIfCard, 0);

        scrollPane.setContent(contentBox);
        scrollPane.getStyleClass().add("page-root");
        scrollPane.setMinWidth(0);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        rootPane.getChildren().add(scrollPane);
        AppContext ctx = AppContext.getInstance();
        ctx.resultsChangedProperty().addListener((observable, oldValue, newValue) -> {
            pipelineCache.clear();
            refreshDashboard();
            refreshPipeline();
        });
        ctx.getApplicants().addListener((javafx.collections.ListChangeListener<Applicant>) change -> {
            pipelineCache.clear();
            refreshPipeline();
        });
        ctx.selectedPolicyProperty().addListener((observable, oldValue, newValue) -> {
            syncHeaderPolicy(newValue);
            refreshDashboard();
            refreshPipeline();
        });
        ctx.selectedVersionProperty().addListener((observable, oldValue, newValue) -> {
            syncVersionFilter(newValue);
            refreshDashboard();
            refreshPipeline();
        });
        ctx.reducedMotionProperty().addListener((observable, oldValue, newValue) -> {
            approvalTrendChart.setAnimated(!newValue);
            if (barChart != null) barChart.setAnimated(!newValue);
            updateShimmerAnimations(newValue);
        });
        ctx.mongoOnlineProperty().addListener((observable, oldValue, connected) -> refreshPipeline());
        rootPane.widthProperty().addListener((observable, oldWidth, width) ->
                layoutDashboard(header, filters, recentCard, attentionCard, whatIfCard, width.doubleValue()));
        scrollPane.viewportBoundsProperty().addListener((observable, oldBounds, bounds) ->
                layoutDashboard(header, filters, recentCard, attentionCard, whatIfCard, bounds.getWidth()));
        syncHeaderPolicy(ctx.selectedPolicyProperty().get());
        whatIfProgramChoice.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (configuringImpactLab) return;
            configureImpactLabPolicies();
            invalidateImpactSimulation();
        });
        whatIfPolicyChoice.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (configuringImpactLab) return;
            if (newValue != null && newValue.getProgramId() != null
                    && !Objects.equals(whatIfProgramChoice.getValue() == null
                            ? null : whatIfProgramChoice.getValue().getId(), newValue.getProgramId())) {
                ProgramConfiguration matchingProgram = whatIfProgramChoice.getItems().stream()
                        .filter(program -> Objects.equals(program.getId(), newValue.getProgramId()))
                        .findFirst().orElse(null);
                if (matchingProgram != null) {
                    configuringImpactLab = true;
                    whatIfProgramChoice.setValue(matchingProgram);
                    configuringImpactLab = false;
                    configureImpactLabPolicies();
                    return;
                }
            }
            configureWhatIfVersions(newValue);
            invalidateImpactSimulation();
        });
        whatIfVersionChoice.valueProperty().addListener((observable, oldValue, newValue) ->
                configureWhatIfRules(newValue));
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
        syncVersionFilter(ctx.selectedVersionProperty().get());
    }

    private VBox createWhatIfCard() {
        VBox card = new VBox(16);
        card.getStyleClass().addAll("card", "dashboard-card", "whatif-card");
        Label title = new Label("Impact Lab — what-if simulator");
        title.getStyleClass().add("dashboard-section-title");

        configureImpactLabSelector(whatIfProgramChoice, "All programs", ProgramConfiguration::getName);
        configureImpactLabSelector(whatIfPolicyChoice, "Select policy", Policy::getName);
        whatIfProgramChoice.setPrefWidth(190);
        whatIfPolicyChoice.setPrefWidth(220);
        whatIfVersionChoice.setPromptText("Select version");
        whatIfVersionChoice.setPrefWidth(180);
        whatIfVersionChoice.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PolicyVersion version, boolean empty) {
                super.updateItem(version, empty);
                setText(empty || version == null ? null : "Version " + version.getVersionNumber());
            }
        });
        whatIfVersionChoice.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PolicyVersion version, boolean empty) {
                super.updateItem(version, empty);
                setText(empty || version == null ? "Select version" : "Version " + version.getVersionNumber());
            }
        });
        Label programLabel = new Label("Program");
        Label policyLabel = new Label("Policy");
        Label versionLabel = new Label("Base version");
        for (Label label : List.of(programLabel, policyLabel, versionLabel)) {
            label.getStyleClass().add("policy-inline-label");
        }
        FlowPane selectors = new FlowPane(10, 8, programLabel, whatIfProgramChoice,
                policyLabel, whatIfPolicyChoice, versionLabel, whatIfVersionChoice);
        selectors.setAlignment(Pos.CENTER_LEFT);

        Label numericTitle = new Label("NUMERIC THRESHOLDS (UP TO 3)");
        numericTitle.getStyleClass().add("caption-label");
        Label numericHelp = new Label("Choose up to three numeric rules. Adjust with the slider or enter an exact value.");
        numericHelp.getStyleClass().add("page-subtitle");
        whatIfThresholdEditors.setFillWidth(true);
        for (int index = 0; index < 3; index++) {
            ThresholdEditor editor = createThresholdEditor(index);
            thresholdEditors.add(editor);
            whatIfThresholdEditors.getChildren().add(editor.row());
        }
        VBox numericSection = new VBox(6, numericTitle, numericHelp, whatIfThresholdEditors);

        Label fieldTitle = new Label("BOOLEAN AND MULTI-SELECT RULES");
        fieldTitle.getStyleClass().add("caption-label");
        whatIfFieldEditors.setFillWidth(true);
        VBox fieldSection = new VBox(6, fieldTitle, whatIfFieldEditors);

        runWhatIfButton.setText("Run Simulation");
        runWhatIfButton.getStyleClass().add("btn-primary");
        runWhatIfButton.setOnAction(event -> runWhatIfSimulation());
        cancelWhatIfButton.getStyleClass().add("btn-secondary");
        cancelWhatIfButton.setDisable(true);
        cancelWhatIfButton.setOnAction(event -> {
            if (activeWhatIfTask != null) activeWhatIfTask.cancel(true);
        });
        saveWhatIfButton.getStyleClass().add("btn-secondary");
        saveWhatIfButton.setDisable(true);
        saveWhatIfButton.setOnAction(event -> {
            Policy policy = whatIfPolicyChoice.getValue();
            if (policy != null && lastSimulation != null) {
                openNewVersionDraft.accept(policy, lastSimulation.simulatedVersion());
            }
        });
        HBox actions = new HBox(10, runWhatIfButton, cancelWhatIfButton, saveWhatIfButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        whatIfSummary.setWrapText(true);
        whatIfSummary.getStyleClass().add("page-subtitle");
        whatIfDelta.getStyleClass().add("flip-chip-gain");
        whatIfProgressBar.setMaxWidth(Double.MAX_VALUE);
        whatIfProgressBar.setVisible(false);
        whatIfProgressBar.setManaged(false);
        TableColumn<WhatIfSimulator.ApplicantFlip, String> applicantColumn =
                new TableColumn<>("Applicant");
        applicantColumn.setCellValueFactory(cell -> {
            WhatIfSimulator.ApplicantFlip flip = cell.getValue();
            String name = Objects.requireNonNullElse(flip.applicantName(), "Unnamed applicant");
            return new SimpleStringProperty(flip.applicantId() == null
                    ? name : name + " (" + flip.applicantId() + ")");
        });
        TableColumn<WhatIfSimulator.ApplicantFlip, String> decisionColumn =
                new TableColumn<>("Decision change");
        decisionColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                (cell.getValue().baselineEligible() ? "Eligible" : "Rejected") + " → "
                        + (cell.getValue().simulatedEligible() ? "Eligible" : "Rejected")));
        TableColumn<WhatIfSimulator.ApplicantFlip, String> responsibleConditionColumn =
                new TableColumn<>("Responsible condition(s)");
        responsibleConditionColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().failingRule()));
        whatIfFlips.getColumns().setAll(applicantColumn, decisionColumn, responsibleConditionColumn);
        whatIfFlips.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        whatIfFlips.setPrefHeight(180);
        whatIfFlips.setPlaceholder(new Label("No applicants changed decisions."));
        VBox.setVgrow(whatIfResultsSlot, Priority.ALWAYS);

        whatIfResultsSlot.getStyleClass().add("widget-slot");
        whatIfResultsSlot.getChildren().setAll(whatIfFlips);
        card.getChildren().addAll(title, selectors, numericSection, fieldSection, actions,
                whatIfProgressBar, whatIfSummary, whatIfDelta, whatIfResultsSlot);
        return card;
    }

    private <T> void configureImpactLabSelector(ComboBox<T> combo, String prompt,
                                                Function<T, String> itemLabel) {
        combo.setPromptText(prompt);
        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : itemLabel.apply(item));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? prompt : itemLabel.apply(item));
            }
        });
    }

    private ThresholdEditor createThresholdEditor(int index) {
        Label indexLabel = new Label("Threshold " + (index + 1));
        indexLabel.getStyleClass().add("policy-inline-label");
        ComboBox<WhatIfSimulator.NumericRule> ruleChoice = new ComboBox<>();
        ruleChoice.setPromptText("Choose numeric rule");
        ruleChoice.setPrefWidth(220);
        ruleChoice.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(WhatIfSimulator.NumericRule rule, boolean empty) {
                super.updateItem(rule, empty);
                setText(empty || rule == null ? null : rule.label());
            }
        });
        ruleChoice.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(WhatIfSimulator.NumericRule rule, boolean empty) {
                super.updateItem(rule, empty);
                setText(empty || rule == null ? "Choose numeric rule" : rule.label());
            }
        });
        Slider slider = new Slider();
        slider.setShowTickMarks(true);
        slider.setPrefWidth(220);
        Spinner<Double> numericInput = new Spinner<>();
        numericInput.setEditable(true);
        numericInput.setPrefWidth(100);
        HBox row = new HBox(10, indexLabel, ruleChoice, slider, numericInput);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(slider, Priority.ALWAYS);
        ruleChoice.valueProperty().addListener((observable, oldRule, newRule) -> {
            configureThresholdEditor(new ThresholdEditor(row, ruleChoice, slider, numericInput), newRule);
            if (!configuringImpactLab) invalidateImpactSimulation();
        });
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (configuringImpactLab || numericInput.getValueFactory() == null) return;
            numericInput.getValueFactory().setValue(newValue.doubleValue());
            invalidateImpactSimulation();
        });
        numericInput.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (configuringImpactLab || newValue == null) return;
            slider.setValue(newValue);
            invalidateImpactSimulation();
        });
        numericInput.getEditor().setOnAction(event -> commitThresholdInput(numericInput));
        numericInput.getEditor().focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused) commitThresholdInput(numericInput);
        });
        slider.setDisable(true);
        numericInput.setDisable(true);
        return new ThresholdEditor(row, ruleChoice, slider, numericInput);
    }

    private void commitThresholdInput(Spinner<Double> input) {
        if (input.getValueFactory() == null) return;
        try {
            double value = Double.parseDouble(input.getEditor().getText().trim());
            WhatIfSimulator.NumericRule rule = selectedNumericRule(input);
            if (rule == null || !Double.isFinite(value)
                    || value < rule.minimum() || value > rule.maximum()) {
                throw new NumberFormatException("Threshold outside the allowed range.");
            }
            input.getValueFactory().setValue(value);
            input.getEditor().getStyleClass().remove("dashboard-input-error");
        } catch (NumberFormatException exception) {
            if (!input.getEditor().getStyleClass().contains("dashboard-input-error")) {
                input.getEditor().getStyleClass().add("dashboard-input-error");
            }
            input.getEditor().setText(String.valueOf(minimumThreshold(input)));
            whatIfSummary.setText("Enter a finite threshold within the selected field's permitted range.");
        }
    }

    private static double minimumThreshold(Spinner<Double> input) {
        return input.getValueFactory() == null ? 0 : input.getValueFactory().getValue();
    }

    private WhatIfSimulator.NumericRule selectedNumericRule(Spinner<Double> input) {
        return thresholdEditors.stream()
                .filter(editor -> editor.numericInput() == input)
                .map(editor -> editor.ruleChoice().getValue())
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private void configureThresholdEditor(ThresholdEditor editor, WhatIfSimulator.NumericRule rule) {
        if (rule == null) {
            editor.slider().setDisable(true);
            editor.numericInput().setDisable(true);
            editor.numericInput().setValueFactory(null);
            editor.slider().setValue(0);
            return;
        }
        editor.slider().setMin(rule.minimum());
        editor.slider().setMax(rule.maximum());
        editor.slider().setMajorTickUnit(Math.max(rule.step(), (rule.maximum() - rule.minimum()) / 5.0));
        editor.slider().setMinorTickCount(4);
        editor.slider().setBlockIncrement(rule.step());
        editor.slider().setSnapToTicks(true);
        editor.numericInput().setValueFactory(new javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory(
                rule.minimum(), rule.maximum(), rule.threshold(), rule.step()));
        editor.slider().setValue(rule.threshold());
        editor.slider().setDisable(false);
        editor.numericInput().setDisable(false);
    }

    private void configureWhatIfVersions(Policy policy) {
        whatIfVersionChoice.getItems().clear();
        if (policy == null) {
            whatIfVersionChoice.setValue(null);
            configureWhatIfRules(null);
            return;
        }
        whatIfVersionChoice.getItems().setAll(policy.getVersions());
        PolicyVersion selected = null;
        Policy globalSelected = AppContext.getInstance().selectedPolicyProperty().get();
        if (globalSelected != null && Objects.equals(globalSelected.getId(), policy.getId())) {
            selected = AppContext.getInstance().selectedVersionProperty().get();
        }
        if (selected == null || !selected.getPolicyId().equals(policy.getId())) {
            selected = policy.getVersion(policy.getActiveVersionNumber())
                    .orElseGet(() -> policy.getLatestVersion().orElse(null));
        }
        whatIfVersionChoice.setValue(selected);
    }

    private void configureImpactLabPrograms(List<ProgramConfiguration> programs) {
        String selectedId = whatIfProgramChoice.getValue() == null
                ? null : whatIfProgramChoice.getValue().getId();
        Policy selectedPolicy = AppContext.getInstance().selectedPolicyProperty().get();
        if (selectedId == null && selectedPolicy != null && selectedPolicy.getProgramId() != null) {
            selectedId = selectedPolicy.getProgramId();
        }
        if (selectedId == null && AppContext.getInstance().selectedProgramProperty().get() != null) {
            selectedId = AppContext.getInstance().selectedProgramProperty().get().getId();
        }
        configuringImpactLab = true;
        List<ProgramConfiguration> activePrograms = programs.stream()
                .filter(program -> !program.isArchived())
                .toList();
        whatIfProgramChoice.getItems().setAll(activePrograms);
        String preferredId = selectedId;
        ProgramConfiguration selected = activePrograms.stream()
                .filter(program -> Objects.equals(program.getId(), preferredId))
                .findFirst().orElse(null);
        whatIfProgramChoice.setValue(selected);
        configuringImpactLab = false;
        configureImpactLabPolicies();
    }

    private void configureImpactLabPolicies() {
        String programId = whatIfProgramChoice.getValue() == null
                ? null : whatIfProgramChoice.getValue().getId();
        Policy currentPolicy = whatIfPolicyChoice.getValue();
        if (currentPolicy == null) currentPolicy = AppContext.getInstance().selectedPolicyProperty().get();
        String preferredPolicyId = currentPolicy == null ? null : currentPolicy.getId();
        List<Policy> candidates = AppContext.getInstance().getPolicies().stream()
                .filter(policy -> programId == null || Objects.equals(programId, policy.getProgramId()))
                .toList();
        configuringImpactLab = true;
        whatIfPolicyChoice.getItems().setAll(candidates);
        Policy selectedPolicy = candidates.stream()
                .filter(policy -> Objects.equals(policy.getId(), preferredPolicyId))
                .findFirst().orElse(null);
        whatIfPolicyChoice.setValue(selectedPolicy);
        configuringImpactLab = false;
        configureWhatIfVersions(selectedPolicy);
    }

    private void configureWhatIfRules(PolicyVersion version) {
        configuringImpactLab = true;
        ProgramConfiguration program = whatIfProgramChoice.getValue();
        List<WhatIfSimulator.NumericRule> numericRules = whatIfSimulator.findNumericRules(version, program);
        for (ThresholdEditor editor : thresholdEditors) {
            editor.ruleChoice().getItems().setAll(numericRules);
            editor.ruleChoice().setValue(null);
            configureThresholdEditor(editor, null);
        }
        booleanRuleEditors.clear();
        multiSelectRuleEditors.clear();
        whatIfFieldEditors.getChildren().clear();
        for (WhatIfSimulator.BooleanRule rule : whatIfSimulator.findBooleanRules(version, program)) {
            ChoiceBox<String> choice = new ChoiceBox<>(FXCollections.observableArrayList(
                    "Use base", "True", "False"));
            choice.setValue("Use base");
            whatIfFieldEditors.getChildren().add(new HBox(10,
                    new Label(rule.label()), choice));
            booleanRuleEditors.add(new BooleanRuleEditor(rule, choice));
            choice.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (!configuringImpactLab) invalidateImpactSimulation();
            });
        }
        for (WhatIfSimulator.MultiSelectRule rule : whatIfSimulator.findMultiSelectRules(version, program)) {
            if (rule.openValueSet()) {
                TextField valuesInput = new TextField(String.join(", ", rule.expectedValues()));
                valuesInput.setPromptText("Comma-separated values");
                valuesInput.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!configuringImpactLab) invalidateImpactSimulation();
                });
                whatIfFieldEditors.getChildren().add(new VBox(4,
                        new Label(rule.label() + " (comma-separated values)"), valuesInput));
                multiSelectRuleEditors.add(new MultiSelectRuleEditor(rule, null, valuesInput));
                continue;
            }
            ListView<String> values = new ListView<>(FXCollections.observableArrayList(rule.options()));
            values.getSelectionModel().setSelectionMode(
                    rule.operator() == com.ruleweaver.model.FieldOperator.CONTAINS
                            || rule.operator() == com.ruleweaver.model.FieldOperator.NOT_CONTAINS
                            ? SelectionMode.SINGLE : SelectionMode.MULTIPLE);
            values.getSelectionModel().clearSelection();
            for (String value : rule.expectedValues()) values.getSelectionModel().select(value);
            values.setPrefHeight(Math.min(96, Math.max(42, rule.options().size() * 24.0)));
            VBox ruleBox = new VBox(4, new Label(rule.label()), values);
            whatIfFieldEditors.getChildren().add(ruleBox);
            multiSelectRuleEditors.add(new MultiSelectRuleEditor(rule, values, null));
            values.getSelectionModel().getSelectedItems().addListener(
                    (javafx.collections.ListChangeListener<String>) change -> {
                        if (!configuringImpactLab) invalidateImpactSimulation();
                    });
        }
        if (whatIfFieldEditors.getChildren().isEmpty()) {
            whatIfFieldEditors.getChildren().add(new Label("This version has no editable Boolean or multi-select rules."));
        }
        configuringImpactLab = false;
        invalidateImpactSimulation();
    }

    private List<WhatIfSimulator.RuleAdjustment> selectedAdjustments() {
        List<WhatIfSimulator.RuleAdjustment> adjustments = new ArrayList<>();
        for (ThresholdEditor editor : thresholdEditors) {
            WhatIfSimulator.NumericRule rule = editor.ruleChoice().getValue();
            if (rule != null) {
                adjustments.add(new WhatIfSimulator.RuleAdjustment(
                        rule.path(), editor.numericInput().getValue()));
            }
        }
        for (BooleanRuleEditor editor : booleanRuleEditors) {
            String selection = editor.choice().getValue();
            if ("True".equals(selection) || "False".equals(selection)) {
                adjustments.add(new WhatIfSimulator.RuleAdjustment(editor.rule().path(),
                        Boolean.valueOf(selection.toLowerCase(java.util.Locale.ROOT))));
            }
        }
        for (MultiSelectRuleEditor editor : multiSelectRuleEditors) {
            List<String> selected;
            if (editor.valuesInput() != null) {
                selected = java.util.Arrays.stream(editor.valuesInput().getText().split(","))
                        .map(String::trim).filter(value -> !value.isEmpty()).toList();
            } else {
                selected = List.copyOf(editor.values().getSelectionModel().getSelectedItems());
            }
            if (selected.equals(editor.rule().expectedValues())) continue;
            boolean singleValueOperator = editor.rule().operator() == com.ruleweaver.model.FieldOperator.CONTAINS
                    || editor.rule().operator() == com.ruleweaver.model.FieldOperator.NOT_CONTAINS;
            if (singleValueOperator && selected.size() > 1) {
                throw new IllegalArgumentException("Enter one value for " + editor.rule().label() + ".");
            }
            Object value = singleValueOperator ? selected.stream().findFirst().orElse(null) : selected;
            if (value == null || value instanceof List<?> values && values.isEmpty()) {
                throw new IllegalArgumentException("Select at least one value for " + editor.rule().label() + ".");
            }
            adjustments.add(new WhatIfSimulator.RuleAdjustment(editor.rule().path(), value));
        }
        return List.copyOf(adjustments);
    }

    private void invalidateImpactSimulation() {
        if (configuringImpactLab) return;
        whatIfGeneration++;
        if (activeWhatIfTask != null && !activeWhatIfTask.isDone()) activeWhatIfTask.cancel(true);
        activeWhatIfTask = null;
        lastSimulation = null;
        saveWhatIfButton.setDisable(true);
        cancelWhatIfButton.setDisable(true);
        runWhatIfButton.setDisable(whatIfVersionChoice.getValue() == null);
        whatIfProgressBar.progressProperty().unbind();
        whatIfProgressBar.setProgress(0);
        whatIfProgressBar.setVisible(false);
        whatIfProgressBar.setManaged(false);
        whatIfFlips.getItems().clear();
        stopShimmer(whatIfResultsSlot);
        whatIfResultsSlot.getChildren().setAll(whatIfFlips);
        whatIfDelta.setText("No simulation");
        whatIfSummary.setText(whatIfVersionChoice.getValue() == null
                ? "Select a program, policy, and base version to begin."
                : "Inputs changed. Run the simulation to compare against the selected base version.");
    }

    private void runWhatIfSimulation() {
        AppContext ctx = AppContext.getInstance();
        Policy policy = whatIfPolicyChoice.getValue();
        ProgramConfiguration program = whatIfProgramChoice.getValue();
        PolicyVersion version = whatIfVersionChoice.getValue();
        if (policy == null || version == null
                || !Objects.equals(version.getPolicyId(), policy.getId())
                || program != null && !Objects.equals(program.getId(), policy.getProgramId())) {
            ctx.showToast(Toast.error("What-if Simulation Unavailable",
                    "Select a matching program, policy, and base version first.", null));
            return;
        }

        List<Applicant> cachedApplicants = new ArrayList<>(ctx.getApplicants());
        List<WhatIfSimulator.RuleAdjustment> adjustments;
        try {
            adjustments = selectedAdjustments();
        } catch (IllegalArgumentException exception) {
            ctx.showToast(Toast.error("Impact Lab Input Invalid", exception.getMessage(), null));
            return;
        }
        long generation = ++whatIfGeneration;
        if (activeWhatIfTask != null && !activeWhatIfTask.isDone()) activeWhatIfTask.cancel(true);
        runWhatIfButton.setDisable(true);
        cancelWhatIfButton.setDisable(false);
        saveWhatIfButton.setDisable(true);
        whatIfSummary.setText("Recalculating baseline and simulated decisions with the shared decision engine...");
        showSkeleton(whatIfResultsSlot, 2);
        whatIfProgressBar.setManaged(true);
        whatIfProgressBar.setVisible(true);
        whatIfProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        Task<WhatIfSimulator.SimulationResult> task = new Task<>() {
            @Override
            protected WhatIfSimulator.SimulationResult call() {
                List<Applicant> applicants = ctx.getApplicantRepository() == null
                        ? cachedApplicants
                        : ctx.getApplicantRepository().findAll().stream()
                                .map(ApplicantDocumentMapper::toApplicant)
                                .toList();
                int total = applicants.size();
                updateProgress(0, Math.max(1, total));
                return whatIfSimulator.simulate(version, adjustments, program, applicants,
                        completed -> updateProgress(completed, Math.max(1, total)), this::isCancelled);
            }
        };
        activeWhatIfTask = task;
        whatIfProgressBar.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded(event -> {
            if (generation != whatIfGeneration) return;
            activeWhatIfTask = null;
            WhatIfSimulator.SimulationResult result = task.getValue();
            lastSimulation = result;
            int baselineRejected = result.comparedCount() - result.baselinePassCount();
            int simulatedRejected = result.comparedCount() - result.passCount();
            double baselineRate = result.comparedCount() == 0 ? 0
                    : result.baselinePassCount() * 100.0 / result.comparedCount();
            double simulatedRate = result.comparedCount() == 0 ? 0
                    : result.passCount() * 100.0 / result.comparedCount();
            double rateChange = simulatedRate - baselineRate;
            whatIfSummary.setText(result.comparedCount() == 0
                    ? "No applicants are available for this simulation."
                    : String.format("Cohort: %d applicants | Eligible: %d → %d (%+d) | Rejected: %d → %d (%+d)",
                            result.comparedCount(), result.baselinePassCount(), result.passCount(), result.delta(),
                            baselineRejected, simulatedRejected, simulatedRejected - baselineRejected));
            whatIfDelta.setText(result.comparedCount() == 0
                    ? "Pass rate: N/A"
                    : String.format("Pass rate: %.1f%% → %.1f%% (%+.1f percentage points)",
                            baselineRate, simulatedRate, rateChange));
            whatIfDelta.getStyleClass().removeAll("flip-chip-gain", "flip-chip-loss");
            whatIfDelta.getStyleClass().add(rateChange < 0 ? "flip-chip-loss" : "flip-chip-gain");
            whatIfFlips.getItems().setAll(result.flips());
            stopShimmer(whatIfResultsSlot);
            whatIfResultsSlot.getChildren().setAll(whatIfFlips);
            saveWhatIfButton.setDisable(result.comparedCount() == 0);
            runWhatIfButton.setDisable(false);
            cancelWhatIfButton.setDisable(true);
            whatIfProgressBar.progressProperty().unbind();
            whatIfProgressBar.setVisible(false);
            whatIfProgressBar.setManaged(false);
            ctx.showToast(Toast.success("Impact Simulation Complete",
                    "Compared " + result.comparedCount() + " applicants; no simulation data was saved.", null));
        });
        task.setOnFailed(event -> {
            if (generation != whatIfGeneration) return;
            activeWhatIfTask = null;
            runWhatIfButton.setDisable(false);
            cancelWhatIfButton.setDisable(true);
            Throwable failure = task.getException();
            if (failure != null) {
                AppContext.logFailure("What-if simulation failed", failure);
                String message = failure.getMessage() == null || failure.getMessage().isBlank()
                        ? "The simulation could not be completed."
                        : MongoConfig.sanitize(failure.getMessage());
                whatIfProgressBar.progressProperty().unbind();
                whatIfProgressBar.setVisible(false);
                whatIfProgressBar.setManaged(false);
                stopShimmer(whatIfResultsSlot);
                whatIfResultsSlot.getChildren().setAll(createErrorState(
                        "Simulation failed", message,
                        "Retry", this::runWhatIfSimulation));
                ctx.showToast(Toast.errorWithAction(
                        "What-if Simulation Failed",
                        message,
                        "Retry", this::runWhatIfSimulation));
            }
        });
        task.setOnCancelled(event -> {
            if (activeWhatIfTask == task) activeWhatIfTask = null;
            if (generation != whatIfGeneration) return;
            runWhatIfButton.setDisable(false);
            cancelWhatIfButton.setDisable(true);
            whatIfProgressBar.progressProperty().unbind();
            whatIfProgressBar.setVisible(false);
            whatIfProgressBar.setManaged(false);
            whatIfSummary.setText("Impact Lab simulation cancelled.");
            stopShimmer(whatIfResultsSlot);
            whatIfResultsSlot.getChildren().setAll(whatIfFlips);
        });
        ctx.getExecutor().execute(task);
    }

    private VBox createHeader() {
        VBox header = new VBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(8);
        titleBox.setMinWidth(0);
        Label title = new Label("Insight Center");
        title.setMinWidth(0);
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("A live overview of your programs, applicants, and policy decisions");
        subtitle.setMinWidth(0);
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        policyChoice.setPromptText("All policies");
        policyChoice.setPrefWidth(210);
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
                setText(empty || policy == null ? "All policies" : policy.getName());
            }
        });
        policyChoice.getItems().setAll(AppContext.getInstance().getPolicies());
        selectedPolicyChip.getStyleClass().addAll("policy-chip", "policy-chip-text");
        selectedPolicyChip.setWrapText(true);
        selectedPolicyChip.setMaxWidth(240);
        lastUpdatedLabel.getStyleClass().add("last-updated");
        lastUpdatedLabel.setWrapText(true);
        lastUpdatedLabel.setMaxWidth(180);
        refreshButton.getStyleClass().add("btn-icon");
        refreshButton.setGraphic(Icons.create(Icons.REFRESH, 0.8, "#9aa0c3"));
        refreshButton.setTooltip(new Tooltip("Refresh dashboard"));
        evaluateAllButton.getStyleClass().add("btn-primary");
        evaluateAllButton.setMinWidth(124);
        cancelEvaluationButton.getStyleClass().add("btn-secondary");
        cancelEvaluationButton.setMinWidth(88);
        cancelEvaluationButton.setVisible(false);
        cancelEvaluationButton.setManaged(false);

        FlowPane toolbar = new FlowPane(16, 8, policyChoice, selectedPolicyChip,
                lastUpdatedLabel, refreshButton, evaluateAllButton, cancelEvaluationButton);
        toolbar.getStyleClass().add("dash-toolbar-controls");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setMaxWidth(Double.MAX_VALUE);
        toolbar.setMinWidth(0);
        header.getStyleClass().addAll("dash-toolbar", "dashboard-header");
        header.getChildren().addAll(titleBox, toolbar);
        return header;
    }

    private FlowPane createFiltersRow() {
        configureProgramFilter();
        configureVersionFilter();
        configureRunFilter();
        configureDecisionFilter();
        programFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!syncingFilters) {
                pipelineCache.clear();
                refreshDashboard();
                refreshPipeline();
            }
        });
        versionFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (syncingFilters) return;
            AppContext.getInstance().selectedVersionProperty().set(newValue);
            refreshDashboard();
        });
        runFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!syncingFilters) refreshDashboard();
        });
        decisionFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!syncingFilters) refreshDashboard();
        });

        FlowPane row = new FlowPane(16, 8);
        row.getStyleClass().addAll("card", "dashboard-card", "dashboard-filters");
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
                filterControl("Program", programFilter),
                filterControl("Policy version", versionFilter),
                filterControl("Evaluation run", runFilter),
                filterControl("Decision", decisionFilter));
        return row;
    }

    private void configureProgramFilter() {
        programFilter.setPromptText("All programs");
        programFilter.setPrefWidth(190);
        programFilter.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(ProgramConfiguration item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        programFilter.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(ProgramConfiguration item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All programs" : item.getName());
            }
        });
    }

    private void configureVersionFilter() {
        versionFilter.setPromptText("All versions");
        versionFilter.setPrefWidth(150);
        versionFilter.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(PolicyVersion item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "Version " + item.getVersionNumber());
            }
        });
        versionFilter.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(PolicyVersion item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All versions"
                        : "Version " + item.getVersionNumber());
            }
        });
    }

    private void configureRunFilter() {
        runFilter.setPromptText("All runs");
        runFilter.setPrefWidth(250);
        runFilter.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(DashboardAnalytics.RunOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : runOptionLabel(item));
            }
        });
        runFilter.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(DashboardAnalytics.RunOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All runs" : runOptionLabel(item));
            }
        });
    }

    private void configureDecisionFilter() {
        decisionFilter.getItems().setAll(DashboardAnalytics.DecisionFilter.values());
        decisionFilter.setValue(DashboardAnalytics.DecisionFilter.ALL);
        decisionFilter.setPrefWidth(150);
        decisionFilter.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(DashboardAnalytics.DecisionFilter item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : decisionLabel(item));
            }
        });
        decisionFilter.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(DashboardAnalytics.DecisionFilter item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All decisions" : decisionLabel(item));
            }
        });
    }

    private VBox filterControl(String label, ComboBox<?> control) {
        VBox box = new VBox(8, new Label(label), control);
        box.getStyleClass().add("dashboard-filter");
        box.setMinWidth(176);
        control.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static String decisionLabel(DashboardAnalytics.DecisionFilter decision) {
        return switch (decision) {
            case ALL -> "All decisions";
            case ELIGIBLE -> "Eligible";
            case REJECTED -> "Rejected";
        };
    }

    private static String runOptionLabel(DashboardAnalytics.RunOption run) {
        if (run.isLegacy()) return "Legacy / untracked (" + run.applicantCount() + ")";
        String time = run.latestEvaluation() == null ? "Unknown time"
                : DateTimeFormatter.ofPattern("MMM d, h:mm a").format(run.latestEvaluation());
        String identifier = run.id().length() > 8 ? run.id().substring(0, 8) : run.id();
        return identifier + " · " + time + " · " + run.applicantCount() + " applicants";
    }

    private void createKpiRow() {
        programsCard = new KpiCard("TOTAL PROGRAMS", "—", "Available program records",
                Icons.POLICIES, "#89b4fa");
        applicantsCard = new KpiCard("TOTAL APPLICANTS", "—", "Applicant records",
                Icons.APPLICANTS, "#74c7ec");
        decisionsCard = new KpiCard("DECISIONS MADE", "—", "Distinct evaluated applicants",
                Icons.EVALUATION, "#a6e3a1");
        activePoliciesCard = new KpiCard("ACTIVE POLICIES", "—", "Published policy records",
                Icons.CHECK, "#f9e2af");
        kpiGrid = new GridPane();
        kpiGrid.setHgap(16);
        kpiGrid.setVgap(16);
        kpiContent = new VBox(kpiGrid);
        kpiSlot.getStyleClass().add("widget-slot");
        kpiSlot.getChildren().setAll(kpiContent);
        layoutKpiCards(0);
    }

    private void layoutKpiCards(double width) {
        int columns = width >= 760 ? 4 : width >= 480 ? 2 : 1;
        kpiGrid.getChildren().clear();
        List<KpiCard> cards = List.of(programsCard, applicantsCard, decisionsCard, activePoliciesCard);
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
        pieWidget = new VBox(16);
        pieWidget.getStyleClass().addAll("card", "dashboard-card");
        Label pieTitle = new Label("Approval / Rejection Trends");
        pieTitle.getStyleClass().add("dashboard-section-title");
        Label trendSubtitle = new Label("Saved decisions · Eligible / rejected · Last 30 days");
        trendSubtitle.getStyleClass().add("page-subtitle");
        approvalTrendXAxis.setLabel("Week of");
        approvalTrendYAxis.setLabel("Decisions");
        approvalTrendChart.setLegendVisible(true);
        approvalTrendChart.setCreateSymbols(true);
        approvalTrendChart.setAnimated(!AppContext.getInstance().reducedMotionProperty().get());
        approvalTrendChart.setPrefHeight(300);
        approvalTrendChart.setMinHeight(250);
        chartCard.getStyleClass().add("widget-slot");
        chartCard.setMinHeight(280);
        chartCard.getChildren().setAll(approvalTrendChart);
        pieWidget.getChildren().addAll(pieTitle, trendSubtitle, chartCard);
        VBox.setVgrow(chartCard, Priority.ALWAYS);

        barWidget = new VBox(16);
        barWidget.getStyleClass().addAll("card", "dashboard-card");
        Label barTitle = new Label("Rule-level outcomes");
        barTitle.getStyleClass().add("dashboard-section-title");
        ruleSampleSummaryLabel.getStyleClass().add("page-subtitle");
        ruleSampleSummaryLabel.setWrapText(true);
        barXAxis = new NumberAxis();
        barXAxis.setLabel("Applicants");
        barYAxis = new CategoryAxis();
        barYAxis.setLabel("Evaluation criterion");
        barChart = new BarChart<>(barXAxis, barYAxis);
        barChart.setLegendVisible(true);
        barChart.setAnimated(!AppContext.getInstance().reducedMotionProperty().get());
        barChart.setPrefHeight(320);
        barChart.setMinHeight(280);
        barCard.getStyleClass().add("widget-slot");
        barCard.setMinHeight(280);
        barCard.getChildren().setAll(barChart);
        barWidget.getChildren().addAll(barTitle, ruleSampleSummaryLabel, barCard);
        VBox.setVgrow(barCard, Priority.ALWAYS);
    }

    private VBox createPolicyHealthCard() {
        VBox card = new VBox(16);
        card.getStyleClass().addAll("card", "dashboard-card", "policy-health-card");

        Label title = new Label("Policy Health");
        title.getStyleClass().add("dashboard-section-title");
        Label subtitle = new Label("Lifecycle and version status from saved policy records.");
        subtitle.getStyleClass().add("page-subtitle");
        Button viewAll = new Button("View all policies");
        viewAll.getStyleClass().add("link-action");
        viewAll.setOnAction(event -> navigateTo(Sidebar.Destination.POLICIES));
        HBox titleRow = new HBox(12, title, viewAll);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);

        GridPane summary = new GridPane();
        summary.setHgap(12);
        summary.setVgap(12);
        summary.setMinWidth(0);
        List<VBox> metrics = List.of(
                policyHealthMetric("ACTIVE", "policy-health-active", "Non-archived with a saved version"),
                policyHealthMetric("DRAFT", "policy-health-draft", "Non-archived without a saved version"),
                policyHealthMetric("NEEDS ATTENTION", "policy-health-attention",
                        "Validation status unavailable"),
                policyHealthMetric("VERSIONED", "policy-health-versioned", "Policies with saved versions"));
        for (int index = 0; index < metrics.size(); index++) {
            summary.add(metrics.get(index), index, 0);
            GridPane.setHgrow(metrics.get(index), Priority.ALWAYS);
            GridPane.setFillWidth(metrics.get(index), true);
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            column.setHgrow(Priority.ALWAYS);
            summary.getColumnConstraints().add(column);
        }

        policyHealthRowsSlot.getStyleClass().add("widget-slot");
        policyHealthRowsSlot.setMinHeight(150);
        showSkeleton(policyHealthRowsSlot, 3);
        card.getChildren().addAll(titleRow, subtitle, summary, policyHealthRowsSlot);
        return card;
    }

    private VBox policyHealthMetric(String title, String accentStyle, String description) {
        VBox metric = new VBox(6);
        metric.getStyleClass().add("policy-health-metric");
        Label heading = new Label(title);
        heading.getStyleClass().addAll("policy-health-metric-title", accentStyle);
        Label value = new Label("—");
        value.getStyleClass().addAll("policy-health-metric-value", accentStyle);
        Label detail = new Label(description);
        detail.getStyleClass().add("policy-health-metric-detail");
        detail.setWrapText(true);
        metric.getChildren().addAll(heading, value, detail);
        policyHealthMetricValues.add(value);
        return metric;
    }

    private void renderPolicyHealth(List<Policy> policies) {
        long activeCount = policies.stream()
                .filter(policy -> !policy.isArchived() && !policy.getVersions().isEmpty()).count();
        long draftCount = policies.stream()
                .filter(policy -> !policy.isArchived() && policy.getVersions().isEmpty()).count();
        long versionedCount = policies.stream()
                .filter(policy -> !policy.getVersions().isEmpty()).count();
        policyHealthMetricValues.get(0).setText(Long.toString(activeCount));
        policyHealthMetricValues.get(1).setText(Long.toString(draftCount));
        policyHealthMetricValues.get(2).setText("—");
        policyHealthMetricValues.get(3).setText(Long.toString(versionedCount));

        stopShimmer(policyHealthRowsSlot);
        if (policies.isEmpty()) {
            policyHealthRowsSlot.getChildren().setAll(new EmptyState(
                    Icons.POLICIES, "No policies yet",
                    "Saved policy records will appear here with their lifecycle and version status.",
                    null, null));
            return;
        }

        List<Policy> ordered = policies.stream()
                .sorted(Comparator.comparing(this::policyLastUpdated,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(policy -> Objects.requireNonNullElse(policy.getName(), "")))
                .limit(POLICY_HEALTH_ROW_LIMIT)
                .toList();
        VBox rows = new VBox(6);
        rows.getStyleClass().add("policy-health-rows");
        rows.getChildren().add(policyHealthTableHeader());
        for (Policy policy : ordered) {
            rows.getChildren().add(policyHealthRow(policy));
        }
        policyHealthRowsSlot.getChildren().setAll(rows);
    }

    private GridPane policyHealthTableHeader() {
        GridPane header = new GridPane();
        header.getStyleClass().add("policy-health-table-header");
        addPolicyHealthColumnConstraints(header);
        List<String> columns = List.of(
                "POLICY", "STATUS", "CURRENT VERSION", "LAST UPDATED", "VERSION DETAILS");
        for (int column = 0; column < columns.size(); column++) {
            Label label = new Label(columns.get(column));
            label.getStyleClass().add("policy-health-column-title");
            label.setMinWidth(0);
            header.add(label, column, 0);
        }
        return header;
    }

    private Button policyHealthRow(Policy policy) {
        GridPane row = new GridPane();
        row.getStyleClass().add("policy-health-row-content");
        addPolicyHealthColumnConstraints(row);

        VBox identity = new VBox(3);
        Label name = new Label(Objects.requireNonNullElse(policy.getName(), "Unnamed policy"));
        name.getStyleClass().add("policy-health-name");
        name.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        name.setMaxWidth(Double.MAX_VALUE);
        Label id = new Label(Objects.requireNonNullElse(policy.getId(), "ID unavailable"));
        id.getStyleClass().add("policy-health-id");
        id.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        id.setMaxWidth(Double.MAX_VALUE);
        identity.getChildren().addAll(name, id);
        identity.setMinWidth(0);
        row.add(identity, 0, 0);

        boolean versioned = !policy.getVersions().isEmpty();
        StatusBadge status = policy.isArchived()
                ? StatusBadge.fail("Archived")
                : versioned ? StatusBadge.pass("Active") : StatusBadge.warning("Draft");
        row.add(status, 1, 0);

        int currentVersionNumber = versioned ? policy.getActiveVersionNumber() : 0;
        Label version = new Label(currentVersionNumber > 0 ? "v" + currentVersionNumber : "—");
        version.getStyleClass().add("policy-health-cell");
        row.add(version, 2, 0);

        LocalDateTime lastUpdated = policyLastUpdated(policy);
        Label updated = new Label(lastUpdated == null ? versioned ? "Unavailable" : "—"
                : DateTimeFormatter.ofPattern("MMM d, yyyy").format(lastUpdated));
        updated.getStyleClass().add("policy-health-cell");
        row.add(updated, 3, 0);

        int versionCount = policy.getVersions().size();
        Label details = new Label(versionCount > 0
                ? versionCount + " saved version" + (versionCount == 1 ? "" : "s")
                : "No saved version");
        details.getStyleClass().add("policy-health-cell");
        details.setWrapText(true);
        row.add(details, 4, 0);

        Button button = new Button();
        button.getStyleClass().add("policy-health-row");
        button.setGraphic(row);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinWidth(0);
        button.setAccessibleText("Open policy " + Objects.requireNonNullElse(policy.getName(), ""));
        button.setOnAction(event -> {
            AppContext ctx = AppContext.getInstance();
            ctx.selectedPolicyProperty().set(policy);
            ctx.selectedVersionProperty().set(versioned
                    ? policy.getVersion(currentVersionNumber)
                            .orElseGet(() -> policy.getLatestVersion().orElse(null))
                    : null);
            navigateTo(Sidebar.Destination.POLICIES);
        });
        return button;
    }

    private static void addPolicyHealthColumnConstraints(GridPane grid) {
        double[] widths = {30, 16, 15, 17, 22};
        for (double width : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            column.setHgrow(Priority.ALWAYS);
            column.setFillWidth(true);
            grid.getColumnConstraints().add(column);
        }
    }

    private LocalDateTime policyLastUpdated(Policy policy) {
        return policy.getVersions().stream()
                .map(PolicyVersion::getCreatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private VBox createDecisionPipelineCard() {
        VBox card = new VBox(16);
        card.getStyleClass().addAll("card", "dashboard-card", "decision-pipeline-card");

        Label title = new Label("Decision Pipeline");
        title.getStyleClass().add("dashboard-section-title");
        Label subtitle = new Label("Applicant progress for the selected policy version.");
        subtitle.getStyleClass().add("page-subtitle");
        Label offlineState = new Label("OFFLINE · Locally available records");
        offlineState.getStyleClass().add("pipeline-offline-state");
        offlineState.setTooltip(new Tooltip(
                "MongoDB is disconnected. Values reflect records currently available in local application state."));
        offlineState.setVisible(false);
        offlineState.setManaged(false);
        HBox heading = new HBox(12, title, offlineState);
        heading.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);

        pipelineStageGrid = new GridPane();
        pipelineStageGrid.setHgap(8);
        pipelineStageGrid.setVgap(12);
        pipelineStageGrid.setMinWidth(0);
        createPipelineStages();
        layoutPipelineStages(0);
        pipelineStageGrid.widthProperty().addListener((observable, oldWidth, width) ->
                layoutPipelineStages(width.doubleValue()));
        pipelineSummarySlot.getStyleClass().add("widget-slot");
        showSkeleton(pipelineSummarySlot, 6);

        Label attentionTitle = new Label("Needs Attention");
        attentionTitle.getStyleClass().add("dashboard-section-title");
        Button viewApplicants = new Button("View all");
        viewApplicants.getStyleClass().add("link-action");
        viewApplicants.setTooltip(new Tooltip(
                "Open all applicants. The Applicants screen does not currently support an awaiting-evaluation filter."));
        viewApplicants.setOnAction(event -> navigateTo(Sidebar.Destination.APPLICANTS));
        pipelineEvaluateButton.getStyleClass().add("btn-secondary");
        pipelineEvaluateButton.setTooltip(new Tooltip("Open the existing evaluation workflow for the selected policy."));
        pipelineEvaluateButton.setDisable(true);
        pipelineEvaluateButton.setOnAction(event -> navigateTo(Sidebar.Destination.EVALUATION));
        HBox attentionHeading = new HBox(10, attentionTitle, pipelineEvaluateButton, viewApplicants);
        attentionHeading.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(attentionTitle, Priority.ALWAYS);
        pipelineAttentionSlot.getStyleClass().add("widget-slot");
        pipelineAttentionSlot.setMinHeight(120);
        showSkeleton(pipelineAttentionSlot, 3);

        VBox attentionPanel = new VBox(12, attentionHeading, pipelineAttentionSlot);
        attentionPanel.getStyleClass().add("pipeline-attention-panel");
        card.getChildren().addAll(heading, subtitle, pipelineSummarySlot, attentionPanel);
        return card;
    }

    private void createPipelineStages() {
        String[] labels = {
                "TOTAL APPLICANTS", "AWAITING EVALUATION", "EVALUATIONS COMPLETED",
                "APPROVED", "REJECTED", "FAILED / INCOMPLETE"
        };
        String[] iconNames = {
                Icons.APPLICANTS, Icons.INFO, Icons.EVALUATION, Icons.CHECK, Icons.CLOSE, Icons.REFRESH
        };
        String[] accents = {
                "pipeline-accent-blue", "pipeline-accent-amber", "pipeline-accent-blue",
                "pipeline-accent-teal", "pipeline-accent-red", "pipeline-accent-red"
        };
        String[] tips = {
                "All applicant records in the repository. Open the Applicants screen to view them.",
                "Applicants without a stored result for this selected policy and version. No matching filtered view exists.",
                "Distinct applicants with a saved result for this policy and version. No matching filtered view exists.",
                "Latest known approval outcomes for distinct applicants. No matching filtered view exists.",
                "Latest known rejection outcomes for distinct applicants. No matching filtered view exists.",
                "Unavailable: the evaluation model does not represent failed or incomplete runs."
        };
        for (int index = 0; index < labels.length; index++) {
            VBox content = new VBox(8);
            content.getStyleClass().addAll("pipeline-stage-card", accents[index]);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label arrow = new Label("›");
            arrow.getStyleClass().add("pipeline-stage-arrow");
            arrow.setVisible(false);
            arrow.setManaged(false);
            HBox iconLine = new HBox(7, Icons.create(iconNames[index], 0.65, null), spacer, arrow);
            iconLine.setAlignment(Pos.CENTER_LEFT);
            iconLine.getStyleClass().add("pipeline-stage-icon");
            pipelineStageArrows.add(arrow);
            Label heading = new Label(labels[index]);
            heading.getStyleClass().add("pipeline-stage-title");
            heading.setWrapText(true);
            Label value = new Label("—");
            value.getStyleClass().add("pipeline-stage-value");
            Label description = new Label("Select a policy version");
            description.getStyleClass().add("pipeline-stage-description");
            description.setWrapText(true);
            ProgressBar meter = new ProgressBar(-1);
            meter.getStyleClass().add("pipeline-stage-meter");
            meter.setMaxWidth(Double.MAX_VALUE);
            content.getChildren().addAll(iconLine, heading, value, description, meter);
            content.setMinWidth(0);
            content.setMaxWidth(Double.MAX_VALUE);

            Node stage = content;
            if (index == 0) {
                Button button = new Button();
                button.getStyleClass().add("pipeline-stage-action");
                button.setGraphic(content);
                button.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                button.setTooltip(new Tooltip(tips[index]));
                button.setAccessibleText(labels[index] + ". Open applicant records.");
                button.setOnAction(event -> navigateTo(Sidebar.Destination.APPLICANTS));
                stage = button;
            } else {
                Tooltip.install(content, new Tooltip(tips[index]));
            }
            pipelineStageNodes.add(stage);
            pipelineStageValues.add(value);
            pipelineStageDescriptions.add(description);
            pipelineStageMeters.add(meter);
        }
    }

    private void layoutPipelineStages(double width) {
        int columns = width >= 1180 ? 6 : width >= 620 ? 3 : 2;
        for (int index = 0; index < pipelineStageArrows.size(); index++) {
            boolean showArrow = width >= 1180 && index < pipelineStageArrows.size() - 1;
            pipelineStageArrows.get(index).setVisible(showArrow);
            pipelineStageArrows.get(index).setManaged(showArrow);
        }
        pipelineStageGrid.getChildren().clear();
        pipelineStageGrid.getColumnConstraints().clear();
        for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / columns);
            column.setHgrow(Priority.ALWAYS);
            column.setFillWidth(true);
            pipelineStageGrid.getColumnConstraints().add(column);
        }
        for (int index = 0; index < pipelineStageNodes.size(); index++) {
            Node node = pipelineStageNodes.get(index);
            int column = index % columns;
            int row = index / columns;
            pipelineStageGrid.add(node, column, row);
            if (node instanceof Region region) {
                region.setMinWidth(0);
                region.setMaxWidth(Double.MAX_VALUE);
            }
            GridPane.setHgrow(node, Priority.ALWAYS);
            GridPane.setFillWidth(node, true);
        }
    }

    private void renderPipelineSummary(PipelineCalculator.PipelineSummary summary, Policy policy) {
        stopShimmer(pipelineSummarySlot);
        if (summary.misconfiguredVersion()) {
            VBox warning = new VBox(10);
            warning.getStyleClass().add("pipeline-misconfigured-state");
            StatusBadge badge = StatusBadge.warning("MISCONFIGURED VERSION");
            Label message = new Label(summary.warnings().get(0));
            message.getStyleClass().add("pipeline-warning-text");
            message.setWrapText(true);
            Button openPolicy = new Button("Review policy version");
            openPolicy.getStyleClass().add("btn-secondary");
            openPolicy.setOnAction(event -> navigateTo(Sidebar.Destination.POLICIES));
            warning.getChildren().addAll(badge, message, openPolicy);
            pipelineSummarySlot.getChildren().setAll(warning);
            if (pendingPipelineAttention != null) {
                renderPipelineAttention(pendingPipelineAttention, policy, -2);
            }
            return;
        }
        pipelineSummarySlot.getChildren().setAll(pipelineStageGrid);

        PipelineCalculator.Metric total = summary.totalApplicants();
        PipelineCalculator.Metric awaiting = summary.awaitingEvaluation();
        PipelineCalculator.Metric completed = summary.evaluationsCompleted();
        PipelineCalculator.Metric approved = summary.approved();
        PipelineCalculator.Metric rejected = summary.rejected();
        List<PipelineCalculator.Metric> metrics = List.of(
                total, awaiting, completed, approved, rejected, summary.failedOrIncomplete());
        long totalCount = availableCount(total);
        long completedCount = availableCount(completed);
        long awaitingCount = availableCount(awaiting);
        long approvedCount = availableCount(approved);
        long rejectedCount = availableCount(rejected);
        setPipelineStage(0, total, programFilter.getValue() == null
                ? "Applicants in the repository" : "Applicants in the selected program",
                totalCount, totalCount);
        setPipelineStage(1, awaiting, awaiting.isAvailable()
                ? awaitingCount + " not yet evaluated"
                : "Awaiting count unavailable", awaitingCount, totalCount);
        setPipelineStage(2, completed, completed.isAvailable()
                ? completedCount + " of " + totalCount + " applicants evaluated"
                : completed.explanation(), completedCount, totalCount);
        setPipelineStage(3, approved, approved.isAvailable()
                ? approvedCount + " of " + completedCount + " completed"
                : approved.explanation(), approvedCount, completedCount);
        setPipelineStage(4, rejected, rejected.isAvailable()
                ? rejectedCount + " of " + completedCount + " completed"
                : rejected.explanation(), rejectedCount, completedCount);
        setPipelineStage(5, summary.failedOrIncomplete(), summary.failedOrIncomplete().explanation(), 0, 0);
        for (int index = 0; index < metrics.size(); index++) {
            Tooltip tooltip = index == 0
                    ? new Tooltip("Open all applicant records. No evaluation-status filter is available.")
                    : new Tooltip(metrics.get(index).explanation() == null
                            ? "No filtered destination exists for this stage."
                            : metrics.get(index).explanation());
            if (pipelineStageNodes.get(index) instanceof Button button) {
                button.setTooltip(tooltip);
                button.setAccessibleText("Total applicants: " + total.displayValue()
                        + ". Open applicant records.");
            } else {
                Tooltip.install(pipelineStageNodes.get(index), tooltip);
            }
        }
    }

    private void setPipelineStage(int index, PipelineCalculator.Metric metric, String description,
                                  long numerator, long denominator) {
        pipelineStageValues.get(index).setText(metric.displayValue());
        pipelineStageDescriptions.get(index).setText(description == null ? "" : description);
        ProgressBar meter = pipelineStageMeters.get(index);
        meter.setProgress(metric.isAvailable() && denominator > 0
                ? Math.min(1.0, (double) numerator / denominator)
                : metric.isAvailable() && denominator == 0 && index == 0 ? 1.0 : -1);
        meter.setVisible(metric.isAvailable() && denominator > 0 && index != 0);
        meter.setManaged(meter.isVisible());
    }

    private static long availableCount(PipelineCalculator.Metric metric) {
        return metric.count() == null ? 0 : metric.count();
    }

    private void renderPipelineAttention(List<PipelineCalculator.AttentionApplicant> applicants,
                                        Policy policy, long total) {
        stopShimmer(pipelineAttentionSlot);
        if (total == -2) {
            Label unavailable = new Label(
                    "Attention status is unavailable while this policy version has no evaluation conditions.");
            unavailable.getStyleClass().add("pipeline-current-state");
            pipelineAttentionSlot.getChildren().setAll(unavailable);
            return;
        }
        if (total < 0) {
            return;
        }
        if (total == 0) {
            pipelineAttentionSlot.getChildren().setAll(new EmptyState(
                    Icons.APPLICANTS, "No applicants yet",
                    "Add an applicant to begin this policy's decision pipeline.",
                    "Add applicant", openAddApplicant));
            return;
        }
        if (applicants.isEmpty()) {
            Label current = new Label("No applicants are awaiting evaluation for this policy version.");
            current.getStyleClass().add("pipeline-current-state");
            pipelineAttentionSlot.getChildren().setAll(current);
            return;
        }

        VBox rows = new VBox(6);
        if (applicants.size() == total) {
            Label noEvaluations = new Label("No evaluations yet for this policy version.");
            noEvaluations.getStyleClass().add("pipeline-empty-evaluations");
            rows.getChildren().add(noEvaluations);
        }
        applicants.stream().limit(PIPELINE_ATTENTION_LIMIT).forEach(item ->
                rows.getChildren().add(pipelineAttentionRow(item, policy)));
        if (applicants.size() > PIPELINE_ATTENTION_LIMIT) {
            Button viewAll = new Button("View all (" + applicants.size() + ")");
            viewAll.getStyleClass().add("link-action");
            viewAll.setTooltip(new Tooltip(
                    "Open all applicants; no awaiting-evaluation filter is available in that view."));
            viewAll.setOnAction(event -> navigateTo(Sidebar.Destination.APPLICANTS));
            rows.getChildren().add(viewAll);
        }
        pipelineAttentionSlot.getChildren().setAll(rows);
    }

    private Button pipelineAttentionRow(PipelineCalculator.AttentionApplicant item, Policy policy) {
        HBox row = new HBox(12);
        row.getStyleClass().add("pipeline-attention-row-content");
        row.setAlignment(Pos.CENTER_LEFT);
        VBox identity = new VBox(3);
        Label name = new Label(item.displayName());
        name.getStyleClass().add("pipeline-attention-name");
        Label policyName = new Label("Policy · "
                + Objects.requireNonNullElse(policy.getName(), "Selected policy"));
        policyName.getStyleClass().add("pipeline-attention-detail");
        identity.getChildren().addAll(name, policyName);
        HBox.setHgrow(identity, Priority.ALWAYS);
        row.getChildren().addAll(identity, StatusBadge.warning("AWAITING"));

        Button button = new Button();
        button.getStyleClass().add("pipeline-attention-row");
        button.setGraphic(row);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinWidth(0);
        button.setAccessibleText("Open applicant " + item.displayName()
                + ", awaiting evaluation for " + policyName.getText());
        if (item.applicantId() != null) {
            button.setOnAction(event -> {
                AppContext.getInstance().selectedApplicantProperty().set(
                        new Applicant(item.applicantId(), item.displayName(), 0, 0, List.of()));
                navigateTo(Sidebar.Destination.APPLICANTS);
            });
        } else {
            button.setDisable(true);
            button.setTooltip(new Tooltip("This applicant record has no identifier and cannot be selected."));
        }
        return button;
    }

    private void refreshPipeline() {
        AppContext ctx = AppContext.getInstance();
        Label offlineLabel = (Label) ((HBox) pipelineWidget.getChildren().get(0)).getChildren().get(1);
        boolean offline = !ctx.mongoOnlineProperty().get();
        offlineLabel.setVisible(offline);
        offlineLabel.setManaged(offline);
        Policy policy = policyChoice.getValue();
        if (policy == null) {
            policy = ctx.selectedPolicyProperty().get();
        }
        PolicyVersion version = versionFilter.getValue();
        if (version == null) {
            version = ctx.selectedVersionProperty().get();
        }
        if (policy == null || version == null || version.getVersionNumber() <= 0) {
            pipelineEvaluateButton.setDisable(true);
            renderPipelineSelectionState();
            return;
        }
        pipelineEvaluateButton.setDisable(version.getConditions().isEmpty());
        if (version.getConditions().isEmpty()) {
            pipelineGeneration++;
            cancelPipelineTasks();
            pipelineCache.clear();
            pendingPipelineKey = null;
            PipelineCalculator.PipelineSummary warning = pipelineCalculator.calculate(
                    List.of(), List.of(), policy.getId(), version.getVersionNumber(), version);
            pendingPipelineSummary = warning;
            pendingPipelineAttention = List.of();
            renderPipelineSummary(warning, policy);
            renderPipelineAttention(List.of(), policy, -2);
            return;
        }

        PipelineCacheKey key = new PipelineCacheKey(policy.getId(), version.getVersionNumber(),
                ctx.resultsChangedProperty().get());
        PipelineCache cached = pipelineCache.get(key);
        if (cached != null) {
            renderPipelineSummary(cached.summary(), policy);
            renderPipelineAttention(cached.attention(), policy,
                    availableCount(cached.summary().totalApplicants()));
            return;
        }

        long generation = ++pipelineGeneration;
        if (activePipelineSummaryTask != null && !activePipelineSummaryTask.isDone()) {
            activePipelineSummaryTask.cancel(true);
        }
        if (activePipelineAttentionTask != null && !activePipelineAttentionTask.isDone()) {
            activePipelineAttentionTask.cancel(true);
        }
        PipelineCacheKey taskKey = key;
        String policyId = policy.getId();
        int versionNumber = version.getVersionNumber();
        String programId = programFilter.getValue() == null ? null : programFilter.getValue().getId();
        PolicyVersion selectedVersion = version;
        Policy selectedPolicy = policy;
        List<Applicant> offlineApplicants = List.copyOf(ctx.getApplicants());
        showSkeleton(pipelineSummarySlot, 6);
        showSkeleton(pipelineAttentionSlot, 3);

        Task<PipelineCalculator.PipelineSummary> summaryTask = new Task<>() {
            @Override
            protected PipelineCalculator.PipelineSummary call() {
                PipelineData data = loadPipelineData(ctx, offlineApplicants, offline,
                        policyId, versionNumber, programId);
                return pipelineCalculator.calculate(data.applicants(), data.results(),
                        policyId, versionNumber, selectedVersion);
            }
        };
        Task<List<PipelineCalculator.AttentionApplicant>> attentionTask = new Task<>() {
            @Override
            protected List<PipelineCalculator.AttentionApplicant> call() {
                PipelineData data = loadPipelineData(ctx, offlineApplicants, offline,
                        policyId, versionNumber, programId);
                return pipelineCalculator.calculate(data.applicants(), data.results(),
                        policyId, versionNumber, selectedVersion).needsAttention();
            }
        };
        pendingPipelineKey = taskKey;
        pendingPipelineSummary = null;
        pendingPipelineAttention = null;
        summaryTask.setOnSucceeded(event -> {
            if (generation != pipelineGeneration) return;
            pendingPipelineSummary = summaryTask.getValue();
            renderPipelineSummary(pendingPipelineSummary, selectedPolicy);
            cachePipelineResultIfReady(selectedPolicy);
        });
        attentionTask.setOnSucceeded(event -> {
            if (generation != pipelineGeneration) return;
            pendingPipelineAttention = attentionTask.getValue();
            if (pendingPipelineSummary != null) {
                renderPipelineAttention(pendingPipelineAttention, selectedPolicy,
                        pendingPipelineSummary.misconfiguredVersion() ? -2
                                : availableCount(pendingPipelineSummary.totalApplicants()));
            }
            cachePipelineResultIfReady(selectedPolicy);
        });
        summaryTask.setOnFailed(event -> handlePipelineTaskFailure(
                "Decision pipeline summary load failed", summaryTask.getException(), pipelineSummarySlot,
                generation));
        attentionTask.setOnFailed(event -> handlePipelineTaskFailure(
                "Decision pipeline attention load failed", attentionTask.getException(), pipelineAttentionSlot,
                generation));
        activePipelineSummaryTask = summaryTask;
        activePipelineAttentionTask = attentionTask;
        ctx.getExecutor().execute(summaryTask);
        ctx.getExecutor().execute(attentionTask);
    }

    private PipelineData loadPipelineData(AppContext ctx, List<Applicant> offlineApplicants, boolean offline,
                                          String policyId, int versionNumber, String programId) {
        List<Applicant> applicants = offline || ctx.getApplicantRepository() == null
                ? offlineApplicants
                : ctx.getApplicantRepository().findAll().stream()
                        .map(ApplicantDocumentMapper::toApplicant)
                        .filter(Objects::nonNull)
                        .toList();
        if (programId != null) {
            applicants = applicants.stream()
                    .filter(applicant -> Objects.equals(programId, applicant.getProgramId()))
                    .toList();
        }
        List<EvaluationResult> results = ctx.getEvaluationResultRepository() == null
                ? List.copyOf(ctx.getEvaluationResults())
                : ctx.getEvaluationResultRepository().findByPolicyAndVersion(policyId, versionNumber);
        return new PipelineData(applicants, results);
    }

    private void cachePipelineResultIfReady(Policy policy) {
        if (pendingPipelineKey != null && pendingPipelineSummary != null
                && pendingPipelineAttention != null) {
            if (pendingPipelineKey.resultsVersion() != AppContext.getInstance().resultsChangedProperty().get()) {
                refreshPipeline();
                return;
            }
            pipelineCache.clear();
            pipelineCache.put(pendingPipelineKey,
                    new PipelineCache(pendingPipelineSummary, pendingPipelineAttention));
            renderPipelineAttention(pendingPipelineAttention, policy,
                    pendingPipelineSummary.misconfiguredVersion() ? -2
                            : availableCount(pendingPipelineSummary.totalApplicants()));
        }
    }

    private void handlePipelineTaskFailure(String message, Throwable failure, StackPane slot, long generation) {
        if (generation != pipelineGeneration || failure == null) return;
        AppContext.logFailure(message, failure);
        String detail = failure.getMessage() == null || failure.getMessage().isBlank()
                ? "Pipeline data could not be loaded."
                : MongoConfig.sanitize(failure.getMessage());
        stopShimmer(slot);
        slot.getChildren().setAll(createErrorState("Unable to load decision pipeline", detail,
                "Retry", this::refreshPipeline));
        AppContext.getInstance().showToast(
                Toast.errorWithAction("Decision Pipeline Failed", detail, "Retry", this::refreshPipeline));
    }

    private void renderPipelineSelectionState() {
        pipelineGeneration++;
        pipelineCache.clear();
        pendingPipelineKey = null;
        pendingPipelineSummary = null;
        pendingPipelineAttention = null;
        cancelPipelineTasks();
        stopShimmer(pipelineSummarySlot);
        stopShimmer(pipelineAttentionSlot);
        pipelineSummarySlot.getChildren().setAll(new EmptyState(
                Icons.POLICIES, "Select a policy version",
                "Choose a policy and saved version in the dashboard filters to view applicant progress.",
                "View policies", () -> navigateTo(Sidebar.Destination.POLICIES)));
        pipelineAttentionSlot.getChildren().setAll(new Label("Select a policy version to see applicants needing evaluation."));
    }

    private void cancelPipelineTasks() {
        if (activePipelineSummaryTask != null && !activePipelineSummaryTask.isDone()) {
            activePipelineSummaryTask.cancel(true);
        }
        if (activePipelineAttentionTask != null && !activePipelineAttentionTask.isDone()) {
            activePipelineAttentionTask.cancel(true);
        }
    }

    private VBox createRecentEvaluationsCard() {
        recentWidget = new VBox(16);
        recentWidget.getStyleClass().addAll("card", "dashboard-card");
        Label title = new Label("Recent Activity");
        title.getStyleClass().add("dashboard-section-title");
        configureRecentTable();
        recentEvaluations.setPrefHeight(300);
        recentEvaluations.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        recentEvaluationsSlot.getStyleClass().add("widget-slot");
        Button viewAll = new Button("View all");
        viewAll.getStyleClass().add("link-action");
        viewAll.setOnAction(event -> navigateTo(Sidebar.Destination.EVALUATION));
        HBox titleRow = new HBox(12, title, viewAll);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);
        recentWidget.getChildren().addAll(titleRow, recentEvaluationsSlot);
        VBox.setVgrow(recentEvaluationsSlot, Priority.ALWAYS);
        return recentWidget;
    }

    private VBox createDataQualityCard() {
        VBox card = new VBox(16);
        card.getStyleClass().addAll("card", "dashboard-card");
        Label title = new Label("Data Quality Warnings");
        title.getStyleClass().add("dashboard-section-title");
        EmptyState unavailable = new EmptyState(Icons.INFO, "Checks unavailable",
                "The application does not currently provide validated data-quality findings.",
                null, null);
        unavailable.setMinHeight(160);
        card.getChildren().addAll(title, unavailable);
        return card;
    }

    private VBox createQuickAccessCard() {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card", "dashboard-card");
        Label title = new Label("Quick Access");
        title.getStyleClass().add("dashboard-section-title");
        card.getChildren().addAll(
                title,
                quickAccessButton("View All Programs", Icons.POLICIES, Sidebar.Destination.PROGRAMS),
                quickAccessButton("View All Applicants", Icons.APPLICANTS, Sidebar.Destination.APPLICANTS),
                quickAccessButton("Manage Policies", Icons.POLICIES, Sidebar.Destination.POLICIES),
                quickAccessButton("View Evaluations", Icons.EVALUATION, Sidebar.Destination.EVALUATION));
        return card;
    }

    private Button quickAccessButton(String title, String icon, Sidebar.Destination destination) {
        Button button = new Button(title, Icons.create(icon, 0.7, "#89b4fa"));
        button.getStyleClass().addAll("btn-secondary", "insight-quick-action");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> navigateTo(destination));
        return button;
    }

    private void navigateTo(Sidebar.Destination destination) {
        if (openDestination != null) {
            openDestination.accept(destination);
        }
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
        attentionWidget = new VBox(16);
        attentionWidget.getStyleClass().addAll("card", "dashboard-card");
        Label title = new Label("Decision funnel & rejection reasons");
        title.setWrapText(true);
        title.getStyleClass().add("dashboard-section-title");
        needsAttentionList.setFillWidth(true);
        needsAttentionSlot.getStyleClass().add("widget-slot");
        needsAttentionSlot.getChildren().setAll(needsAttentionList);
        attentionWidget.getChildren().addAll(title, needsAttentionSlot);
        VBox.setVgrow(needsAttentionSlot, Priority.ALWAYS);
        return attentionWidget;
    }

    private void layoutDashboard(VBox header, FlowPane filters, VBox recentCard, VBox attentionCard,
                                 VBox whatIfCard, double width) {
        double screenWidth = width + SIDEBAR_WIDTH;
        int columns = screenWidth >= 900 ? 3 : 1;
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
        contentGrid.add(filters, 0, 1, columns, 1);
        contentGrid.add(kpiSlot, 0, 2, columns, 1);
        contentGrid.add(policyHealthWidget, 0, 3, columns, 1);
        contentGrid.add(pipelineWidget, 0, 4, columns, 1);
        if (columns == 3) {
            contentGrid.add(pieWidget, 0, 5, 2, 1);
            contentGrid.add(recentCard, 2, 5);
            contentGrid.add(qualityWidget, 0, 6, 2, 1);
            contentGrid.add(quickAccessWidget, 2, 6);
            contentGrid.add(barWidget, 0, 7, 2, 1);
            contentGrid.add(attentionCard, 2, 7);
            contentGrid.add(whatIfCard, 0, 8, 3, 1);
        } else {
            contentGrid.add(pieWidget, 0, 5);
            contentGrid.add(recentCard, 0, 6);
            contentGrid.add(qualityWidget, 0, 7);
            contentGrid.add(quickAccessWidget, 0, 8);
            contentGrid.add(barWidget, 0, 9);
            contentGrid.add(attentionCard, 0, 10);
            contentGrid.add(whatIfCard, 0, 11);
        }
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
        selectedPolicyChip.setText(policy == null ? "All policies" : policy.getName());
        selectedPolicyChip.getStyleClass().remove("policy-chip-muted");
        if (policy == null) selectedPolicyChip.getStyleClass().add("policy-chip-muted");
        syncVersionChoices(policy, AppContext.getInstance().selectedVersionProperty().get());
    }

    private void syncVersionChoices(Policy policy, PolicyVersion selected) {
        syncingFilters = true;
        versionFilter.getItems().setAll(policy == null ? List.of() : policy.getVersions());
        PolicyVersion matching = selected == null ? null : versionFilter.getItems().stream()
                .filter(candidate -> candidate.getVersionNumber() == selected.getVersionNumber())
                .findFirst().orElse(null);
        versionFilter.setValue(matching);
        syncingFilters = false;
        evaluateAllButton.setDisable(policy == null || selected == null);
    }

    private void syncVersionFilter(PolicyVersion selected) {
        if (versionFilter.getItems().isEmpty()) return;
        syncingFilters = true;
        PolicyVersion matching = selected == null ? null : versionFilter.getItems().stream()
                .filter(candidate -> candidate.getVersionNumber() == selected.getVersionNumber())
                .findFirst().orElse(null);
        versionFilter.setValue(matching);
        syncingFilters = false;
        evaluateAllButton.setDisable(AppContext.getInstance().selectedPolicyProperty().get() == null
                || selected == null);
    }

    public void refreshDashboard() {
        AppContext ctx = AppContext.getInstance();
        long generation = ++refreshGeneration;
        Task<DashboardSnapshot> previousTask = activeDashboardTask;
        if (previousTask != null && !previousTask.isDone()) previousTask.cancel(true);
        DashboardAnalytics.Filters filters = selectedFilters();
        long cachedApplicantCount = ctx.getApplicants().size();
        LocalDate trendEndDate = LocalDate.now();
        LocalDate trendStartDate = trendEndDate.minusDays(29);
        showSkeleton(kpiSlot, 4);
        showSkeleton(chartCard, 1);
        showSkeleton(barCard, 1);
        showSkeleton(policyHealthRowsSlot, 3);
        showSkeleton(recentEvaluationsSlot, 3);
        showSkeleton(needsAttentionSlot, 3);

        Task<DashboardSnapshot> task = new Task<>() {
            @Override
            protected DashboardSnapshot call() {
                List<EvaluationResult> results = ctx.getEvaluationResultRepository() == null
                        ? List.copyOf(ctx.getEvaluationResults())
                        : ctx.getEvaluationResultRepository().findAll();
                List<ProgramConfiguration> programs = ctx.getProgramService().listPrograms();
                long programCount = ctx.getProgramService().countPrograms();
                long applicantCount = ctx.getApplicantRepository() == null
                        ? cachedApplicantCount : ctx.getApplicantRepository().count();
                long decisionCount = ctx.getEvaluationResultRepository() == null
                        ? results.size() : ctx.getEvaluationResultRepository().countDecisions();
                long activePolicyCount = ctx.getPolicyRepository().countActivePolicies();
                List<Policy> loadedPolicies = ctx.getPolicyRepository().findAll();
                DashboardAnalytics.Analysis analysis = dashboardAnalytics.analyze(results, loadedPolicies, filters);
                DashboardAnalytics.Filters runScope = new DashboardAnalytics.Filters(
                        filters.programId(), filters.policyId(), filters.versionNumber(), null,
                        DashboardAnalytics.DecisionFilter.ALL);
                List<DashboardAnalytics.RunOption> runs =
                        dashboardAnalytics.findRuns(results, loadedPolicies, runScope);
                List<DashboardAnalytics.DailyDecisionCount> dailyDecisionCounts =
                        dashboardAnalytics.dailyDecisionCounts(results, trendStartDate, trendEndDate);
                return new DashboardSnapshot(programs, loadedPolicies, programCount, applicantCount, decisionCount,
                        activePolicyCount, dailyDecisionCounts, runs, analysis);
            }
        };
        activeDashboardTask = task;
        task.setOnSucceeded(event -> {
            if (generation != refreshGeneration) return;
            if (activeDashboardTask == task) activeDashboardTask = null;
            DashboardSnapshot snapshot = task.getValue();
            if (updateDashboardFilters(snapshot)) return;
            DashboardAnalytics.Analysis analysis = snapshot.analysis();
            renderPolicyHealth(snapshot.policies());
            renderKpis(snapshot.programCount(), snapshot.applicantCount(), snapshot.decisionCount(),
                    snapshot.activePolicyCount());
            renderApprovalTrend(snapshot.dailyDecisionCounts());
            renderComplianceChart(analysis);
            renderRecentEvaluations(analysis.evaluations());
            renderNeedsAttention(analysis);
            stopDashboardShimmers();
            lastUpdatedLabel.setText("Updated " + updatedFormatter.format(LocalDateTime.now()));
        });
        task.setOnCancelled(event -> {
            if (activeDashboardTask == task) activeDashboardTask = null;
            if (generation == refreshGeneration) stopDashboardShimmers();
        });
        task.setOnFailed(event -> {
            if (generation != refreshGeneration) return;
            if (activeDashboardTask == task) activeDashboardTask = null;
            Throwable failure = task.getException();
            if (failure != null) {
                AppContext.logFailure("Dashboard analytics load failed", failure);
                String message = failure.getMessage() == null || failure.getMessage().isBlank()
                        ? "Dashboard data could not be loaded."
                        : MongoConfig.sanitize(failure.getMessage());
                stopDashboardShimmers();
                for (StackPane slot : List.of(kpiSlot, chartCard, barCard, policyHealthRowsSlot,
                        recentEvaluationsSlot, needsAttentionSlot)) {
                    slot.getChildren().setAll(createErrorState(
                            "Unable to load dashboard insights", message, "Retry", this::refreshDashboard));
                }
                AppContext.getInstance().showToast(
                        Toast.errorWithAction("Dashboard Data Failed", message, "Retry", this::refreshDashboard));
            }
        });
        ctx.getExecutor().execute(task);
    }

    private DashboardAnalytics.Filters selectedFilters() {
        ProgramConfiguration program = programFilter.getValue();
        Policy policy = policyChoice.getValue();
        PolicyVersion version = versionFilter.getValue();
        DashboardAnalytics.RunOption run = runFilter.getValue();
        return new DashboardAnalytics.Filters(
                program == null ? null : program.getId(),
                policy == null ? null : policy.getId(),
                version == null ? null : version.getVersionNumber(),
                run == null ? null : run.id(),
                decisionFilter.getValue());
    }

    private boolean updateDashboardFilters(DashboardSnapshot snapshot) {
        configureImpactLabPrograms(snapshot.programs());
        syncingFilters = true;
        String selectedProgramId = programFilter.getValue() == null ? null : programFilter.getValue().getId();
        programFilter.getItems().setAll(snapshot.programs());
        ProgramConfiguration selectedProgram = snapshot.programs().stream()
                .filter(program -> Objects.equals(program.getId(), selectedProgramId))
                .findFirst().orElse(null);
        programFilter.setValue(selectedProgram);

        String selectedRunId = runFilter.getValue() == null ? null : runFilter.getValue().id();
        runFilter.getItems().setAll(snapshot.runs());
        DashboardAnalytics.RunOption selectedRun = snapshot.runs().stream()
                .filter(run -> Objects.equals(run.id(), selectedRunId)).findFirst().orElse(null);
        runFilter.setValue(selectedRun);
        boolean lostRunSelection = selectedRunId != null && selectedRun == null;
        syncingFilters = false;
        boolean lostProgramSelection = selectedProgramId != null && selectedProgram == null;
        if (lostRunSelection || lostProgramSelection) {
            refreshDashboard();
            return true;
        }
        return false;
    }

    private void stopDashboardShimmers() {
        for (StackPane slot : List.of(kpiSlot, chartCard, barCard, policyHealthRowsSlot,
                recentEvaluationsSlot, needsAttentionSlot)) {
            stopShimmer(slot);
        }
    }

    private void renderKpis(long programCount, long applicantCount, long decisionCount,
                            long activePolicyCount) {
        kpiSlot.getChildren().setAll(kpiContent);
        programsCard.setValueText(Long.toString(programCount));
        programsCard.setSubtext(programCount == 0
                ? "No configured programs" : "Configured programs · excludes templates");
        applicantsCard.setValueText(Long.toString(applicantCount));
        applicantsCard.setSubtext(applicantCount == 0 ? "No applicant records" : "Applicant records");
        decisionsCard.setValueText(Long.toString(decisionCount));
        decisionsCard.setSubtext(decisionCount == 0 ? "No saved decisions" : "Saved evaluation decisions");
        activePoliciesCard.setValueText(Long.toString(activePolicyCount));
        activePoliciesCard.setSubtext(activePolicyCount == 0
                ? "No active policies" : "Non-archived policies with a saved version");
        if (!hasAnimatedEntrance) {
            Motion.staggeredFadeUp(List.of(programsCard, applicantsCard, decisionsCard, activePoliciesCard));
            hasAnimatedEntrance = true;
        }
    }

    private void renderApprovalTrend(List<DashboardAnalytics.DailyDecisionCount> dailyCounts) {
        if (dailyCounts.isEmpty()) {
            chartCard.getChildren().setAll(new EmptyState(Icons.EVALUATION, "No recent decisions",
                    "Dated, completed approvals or rejections from the last 30 days will appear here. "
                            + "Undated or unrecognized outcomes are omitted.",
                    null, null));
            return;
        }

        XYChart.Series<String, Number> approved = new XYChart.Series<>();
        approved.setName("Eligible");
        XYChart.Series<String, Number> rejected = new XYChart.Series<>();
        rejected.setName("Rejected");
        List<String> dates = new ArrayList<>();
        DateTimeFormatter dateLabel = DateTimeFormatter.ofPattern("MMM d");
        for (DashboardAnalytics.DailyDecisionCount dailyCount : dailyCounts) {
            String label = dateLabel.format(dailyCount.date());
            dates.add(label);
            approved.getData().add(new XYChart.Data<>(label, dailyCount.approved()));
            rejected.getData().add(new XYChart.Data<>(label, dailyCount.rejected()));
        }
        approvalTrendXAxis.setLabel("Date");
        approvalTrendXAxis.setTickLabelRotation(45);
        approvalTrendYAxis.setLabel("Evaluations");
        approvalTrendXAxis.setCategories(FXCollections.observableArrayList(dates));
        approvalTrendChart.getData().setAll(List.of(approved, rejected));
        if (chartCard.getChildren().isEmpty()
                || chartCard.getChildren().get(0) != approvalTrendChart) {
            chartCard.getChildren().setAll(approvalTrendChart);
        }
    }

    private void renderComplianceChart(DashboardAnalytics.Analysis analysis) {
        List<DashboardAnalytics.RuleImpact> impacts = analysis.ruleImpacts();
        List<EvaluationResult> results = analysis.evaluations();
        ruleSampleSummaryLabel.setText("Each bar counts distinct applicants assessed for that rule; denominators vary by recorded rule details.");
        if (results.isEmpty()) {
            barCard.getChildren().setAll(new EmptyState(Icons.POLICIES, "No criteria data",
                    "Criteria compliance appears after applicants have been evaluated.",
                    null, null));
            return;
        }
        if (barCard.getChildren().isEmpty() || barCard.getChildren().get(0) != barChart) {
            barCard.getChildren().setAll(barChart);
        }
        if (impacts.isEmpty()) {
            barCard.getChildren().setAll(new EmptyState(Icons.POLICIES, "No criteria data",
                    "These evaluations do not contain condition-level results.",
                    null, null));
            return;
        }
        XYChart.Series<Number, String> passSeries = new XYChart.Series<>();
        passSeries.setName("Passed");
        XYChart.Series<Number, String> failSeries = new XYChart.Series<>();
        failSeries.setName("Failed");
        Map<String, Integer> denominatorByRule = new HashMap<>();
        for (DashboardAnalytics.RuleImpact impact : impacts) {
            passSeries.getData().add(new XYChart.Data<>(impact.passedApplicants(), impact.ruleName()));
            failSeries.getData().add(new XYChart.Data<>(impact.failedApplicants(), impact.ruleName()));
            denominatorByRule.put(impact.ruleName(), impact.assessedApplicants());
        }
        barChart.getData().setAll(passSeries, failSeries);
        for (XYChart.Series<Number, String> series : barChart.getData()) {
            for (XYChart.Data<Number, String> datum : series.getData()) {
                String tooltipText = series.getName() + ": " + datum.getYValue()
                        + " of " + denominatorByRule.getOrDefault(datum.getYValue(), 0)
                        + " assessed applicants — " + datum.getXValue();
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

    private void renderNeedsAttention(DashboardAnalytics.Analysis analysis) {
        DashboardAnalytics.Funnel funnel = analysis.funnel();
        if (funnel.evaluated() == 0) {
            needsAttentionSlot.getChildren().setAll(new EmptyState(Icons.CHECK,
                    "No decision funnel data", "Evaluated applicant results will appear here.",
                    null, null));
            return;
        }
        needsAttentionList.getChildren().clear();
        needsAttentionList.getChildren().addAll(
                funnelRow("Evaluated applicants", funnel.evaluated(), funnel.evaluated()),
                funnelRow("Eligible", funnel.eligible(), funnel.evaluated()),
                funnelRow("Rejected", funnel.rejected(), funnel.evaluated()),
                funnelRow("Near-miss · one recorded rule failure", funnel.nearMisses(),
                        funnel.rejectedWithRuleDetails()));
        Label reasonsTitle = new Label("COMMON REJECTION REASONS");
        reasonsTitle.getStyleClass().add("caption-label");
        needsAttentionList.getChildren().add(reasonsTitle);
        List<DashboardAnalytics.RuleImpact> rejectionReasons = analysis.rejectionReasons().stream()
                .filter(impact -> impact.failedApplicants() > 0).limit(6).toList();
        if (rejectionReasons.isEmpty()) {
            Label none = new Label("No recorded failed rule details for this sample.");
            none.getStyleClass().add("page-subtitle");
            needsAttentionList.getChildren().add(none);
        } else {
            for (DashboardAnalytics.RuleImpact impact : rejectionReasons) {
                Label rule = new Label(impact.ruleName());
                rule.getStyleClass().add("insight-title");
                rule.setWrapText(true);
                Label count = new Label(impact.failedApplicants() + " / "
                        + impact.assessedApplicants() + " assessed");
                count.getStyleClass().add("margin-chip");
                ProgressBar failureRate = new ProgressBar(
                        impact.assessedApplicants() == 0 ? 0
                                : (double) impact.failedApplicants() / impact.assessedApplicants());
                failureRate.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(failureRate, Priority.ALWAYS);
                HBox row = new HBox(10, rule, failureRate, count);
                row.getStyleClass().add("insight-row");
                row.setAlignment(Pos.CENTER_LEFT);
                needsAttentionList.getChildren().add(row);
            }
        }
        needsAttentionSlot.getChildren().setAll(needsAttentionList);
    }

    private HBox funnelRow(String label, int count, int denominator) {
        Label name = new Label(label);
        name.getStyleClass().add("insight-title");
        name.setWrapText(true);
        Label value = new Label(count + " / " + denominator);
        value.getStyleClass().add("margin-chip");
        ProgressBar share = new ProgressBar(denominator == 0 ? 0 : (double) count / denominator);
        share.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(share, Priority.ALWAYS);
        HBox row = new HBox(10, name, share, value);
        row.getStyleClass().add("insight-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
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
        evaluateAllButton.setDisable(AppContext.getInstance().selectedPolicyProperty().get() == null
                || AppContext.getInstance().selectedVersionProperty().get() == null);
        cancelEvaluationButton.setVisible(false);
        cancelEvaluationButton.setManaged(false);
        if (activeEvaluationTask == task) activeEvaluationTask = null;
    }

    private String formatEvaluationTime(LocalDateTime evaluatedOn) {
        return evaluatedOn == null ? "Unknown" : updatedFormatter.format(evaluatedOn);
    }

    private record ThresholdEditor(HBox row,
                                   ComboBox<WhatIfSimulator.NumericRule> ruleChoice,
                                   Slider slider,
                                   Spinner<Double> numericInput) { }

    private record BooleanRuleEditor(WhatIfSimulator.BooleanRule rule, ChoiceBox<String> choice) { }

    private record MultiSelectRuleEditor(WhatIfSimulator.MultiSelectRule rule,
                                         ListView<String> values,
                                         TextField valuesInput) { }

    private record DashboardSnapshot(List<ProgramConfiguration> programs,
                                     List<Policy> policies,
                                     long programCount,
                                     long applicantCount,
                                     long decisionCount,
                                     long activePolicyCount,
                                     List<DashboardAnalytics.DailyDecisionCount> dailyDecisionCounts,
                                     List<DashboardAnalytics.RunOption> runs,
                                     DashboardAnalytics.Analysis analysis) { }

    private record PipelineData(List<Applicant> applicants, List<EvaluationResult> results) { }

    private record PipelineCacheKey(String policyId, int versionNumber, long resultsVersion) { }

    private record PipelineCache(PipelineCalculator.PipelineSummary summary,
                                 List<PipelineCalculator.AttentionApplicant> attention) {
        private PipelineCache {
            attention = List.copyOf(attention);
        }
    }

    @Override
    public Node getRoot() {
        return rootPane;
    }

    @Override
    public void onShow() {
        refreshDashboard();
        refreshPipeline();
    }
}
