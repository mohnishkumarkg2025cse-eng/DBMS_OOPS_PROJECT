package com.ruleweaver.ui.views;

import com.ruleweaver.model.*;
import com.ruleweaver.config.MongoConfig;
import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.components.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen 4: Executive Overview Dashboard with live KPI cards, animated charts,
 * rule failure distribution, and policy impact summaries.
 */
public class DashboardView implements BaseView {

    private final StackPane rootPane = new StackPane();
    private final ScrollPane scrollPane = new ScrollPane();
    private final VBox contentBox = new VBox(24);

    // KPI Cards
    private KpiCard totalCard;
    private KpiCard eligibleCard;
    private KpiCard rejectedCard;
    private KpiCard passRateCard;
    private KpiCard impactCard;

    // Charts
    private final PieChart pieChart = new PieChart();
    private CategoryAxis barXAxis;
    private NumberAxis barYAxis;
    private BarChart<String, Number> barChart;

    private final StackPane chartCard = new StackPane();
    private final StackPane barCard = new StackPane();

    public DashboardView() {
        buildUI();
    }

    private void buildUI() {
        contentBox.setPadding(new Insets(28, 32, 28, 32));

        // 1. Header Toolbar
        HBox header = createHeader();

        // 2. KPI Cards Row
        HBox kpiRow = createKpiRow();

        // 3. Analytics Charts Row
        HBox chartsRow = createChartsRow();

        contentBox.getChildren().addAll(header, kpiRow, chartsRow);

        scrollPane.setContent(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        rootPane.getChildren().add(scrollPane);
        AppContext ctx = AppContext.getInstance();
        ctx.resultsChangedProperty().addListener((observable, oldValue, newValue) -> refreshDashboard());
        ctx.selectedPolicyProperty().addListener((observable, oldValue, newValue) -> refreshDashboard());
        ctx.selectedVersionProperty().addListener((observable, oldValue, newValue) -> refreshDashboard());
    }

    private HBox createHeader() {
        HBox toolbar = new HBox(16);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("System Dashboard");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Real-time decision distribution, candidate velocity, and version metrics");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        toolbar.getChildren().add(titleBox);
        return toolbar;
    }

    private HBox createKpiRow() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        totalCard = new KpiCard("TOTAL APPLICANTS", "0", "Registered candidates", Icons.APPLICANTS, "#89b4fa");
        eligibleCard = new KpiCard("ELIGIBLE (PASS)", "0", "Criteria satisfied", Icons.CHECK, "#a6e3a1");
        rejectedCard = new KpiCard("REJECTED (FAIL)", "0", "Disqualified candidates", Icons.CLOSE, "#f38ba8");
        passRateCard = new KpiCard("QUALIFICATION RATE", "0.0%", "Pass / Total ratio", Icons.SPARKLES, "#cba6f7");
        impactCard = new KpiCard("VERSION IMPACT", "0", "Flips vs previous version", Icons.COMPARISON, "#f9e2af");

        HBox.setHgrow(totalCard, Priority.ALWAYS);
        HBox.setHgrow(eligibleCard, Priority.ALWAYS);
        HBox.setHgrow(rejectedCard, Priority.ALWAYS);
        HBox.setHgrow(passRateCard, Priority.ALWAYS);
        HBox.setHgrow(impactCard, Priority.ALWAYS);

        row.getChildren().addAll(totalCard, eligibleCard, rejectedCard, passRateCard, impactCard);
        return row;
    }

    private HBox createChartsRow() {
        HBox row = new HBox(20);
        row.setAlignment(Pos.TOP_LEFT);

        // Chart 1: Pie Chart (Decision Outcomes)
        VBox pieBox = new VBox(12);
        pieBox.getStyleClass().add("card");
        pieBox.setPrefWidth(420);
        pieBox.setMinWidth(360);

        Label pieTitle = new Label("DECISION OUTCOME RATIO");
        pieTitle.getStyleClass().add("caption-label");

        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(true);
        pieChart.setStyle("-fx-background-color: transparent;");

        chartCard.getChildren().add(pieChart);
        pieBox.getChildren().addAll(pieTitle, chartCard);
        VBox.setVgrow(chartCard, Priority.ALWAYS);

        // Chart 2: Bar Chart (Condition Pass/Fail Distribution)
        VBox barBox = new VBox(12);
        barBox.getStyleClass().add("card");
        HBox.setHgrow(barBox, Priority.ALWAYS);

        Label barTitle = new Label("RULE COMPLIANCE BY CRITERION");
        barTitle.getStyleClass().add("caption-label");

        barXAxis = new CategoryAxis();
        barXAxis.setLabel("Evaluation Criterion");

        barYAxis = new NumberAxis();
        barYAxis.setLabel("Applicants");

        barChart = new BarChart<>(barXAxis, barYAxis);
        barChart.setStyle("-fx-background-color: transparent;");
        barChart.setLegendVisible(true);

        barCard.getChildren().add(barChart);
        barBox.getChildren().addAll(barTitle, barCard);
        VBox.setVgrow(barCard, Priority.ALWAYS);

        row.getChildren().addAll(pieBox, barBox);
        return row;
    }

    public void refreshDashboard() {
        AppContext ctx = AppContext.getInstance();
        Policy policy = ctx.selectedPolicyProperty().get();
        PolicyVersion version = ctx.selectedVersionProperty().get();
        if (policy == null || version == null) {
            renderDashboard(ctx.getApplicants(), List.of(), policy, version);
            return;
        }
        ctx.loadEvaluationResultsAsync(policy.getId(), version.getVersionNumber(),
                results -> renderDashboard(ctx.getApplicants(), results, policy, version),
                failure -> {
                    AppContext.logFailure("Could not load dashboard evaluation results", failure);
                    ctx.showToast(Toast.errorWithAction(
                            "Dashboard Data Failed",
                            failure.getMessage() == null ? "Could not load stored results."
                                    : MongoConfig.sanitize(failure.getMessage()),
                            "Retry", this::refreshDashboard));
                });
    }

    private void renderDashboard(List<Applicant> applicants, List<EvaluationResult> results,
                                 Policy policy, PolicyVersion version) {
        AppContext ctx = AppContext.getInstance();

        totalCard.setValue(applicants.size());

        if (results.isEmpty()) {
            eligibleCard.setValue(0);
            rejectedCard.setValue(0);
            passRateCard.setValueText("N/A");
            impactCard.setValueText("0");

            chartCard.getChildren().setAll(new EmptyState(
                    Icons.EVALUATION,
                    "No Evaluations Yet",
                    "Evaluate candidates to generate pie charts and decision distributions.",
                    null,
                    null
            ));

            barCard.getChildren().setAll(new EmptyState(
                    Icons.POLICIES,
                    "No Compliance Data",
                    "Criteria compliance will render once an evaluation is executed.",
                    null,
                    null
            ));
            return;
        }

        // Restore chart nodes if replaced by empty state
        if (!chartCard.getChildren().contains(pieChart)) {
            chartCard.getChildren().setAll(pieChart);
        }
        if (!barCard.getChildren().contains(barChart)) {
            barCard.getChildren().setAll(barChart);
        }

        int total = results.size();
        int eligible = (int) results.stream().filter(EvaluationResult::isOverallDecision).count();
        int rejected = total - eligible;
        double passRate = total > 0 ? (eligible * 100.0 / total) : 0.0;

        eligibleCard.setValue(eligible);
        rejectedCard.setValue(rejected);
        passRateCard.setValuePercent(passRate);

        // Update Pie Chart
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Eligible (" + eligible + ")", eligible),
                new PieChart.Data("Rejected (" + rejected + ")", rejected)
        );
        pieChart.setData(pieData);

        // Compute Bar Chart Data (Rule Breakdown)
        Map<String, Integer> passCounts = new HashMap<>();
        Map<String, Integer> failCounts = new HashMap<>();

        for (EvaluationResult res : results) {
            for (ConditionResult cr : res.getConditionResults()) {
                String name = cr.getConditionName();
                if (cr.isPassed()) {
                    passCounts.put(name, passCounts.getOrDefault(name, 0) + 1);
                } else {
                    failCounts.put(name, failCounts.getOrDefault(name, 0) + 1);
                }
            }
        }

        XYChart.Series<String, Number> passSeries = new XYChart.Series<>();
        passSeries.setName("Passed");

        XYChart.Series<String, Number> failSeries = new XYChart.Series<>();
        failSeries.setName("Failed");

        for (String ruleName : passCounts.keySet()) {
            passSeries.getData().add(new XYChart.Data<>(ruleName, passCounts.get(ruleName)));
            failSeries.getData().add(new XYChart.Data<>(ruleName, failCounts.getOrDefault(ruleName, 0)));
        }

        barChart.getData().setAll(List.of(passSeries, failSeries));

        // Compute Shift vs Previous Version if version > 1
        if (policy != null && version != null && version.getVersionNumber() > 1) {
            int prevVerNum = version.getVersionNumber() - 1;
            ctx.loadEvaluationResultsAsync(policy.getId(), prevVerNum, previousResults -> {
                ComparisonReport report = ctx.getComparisonService().compareEvaluations(previousResults, results);
                int shift = report.getChangedApplicants().size();
                impactCard.setValue(shift);
                impactCard.setSubtext("vs v" + prevVerNum + " (" + report.getLostEligibilityCount()
                        + " lost, " + report.getGainedEligibilityCount() + " gained)");
            }, failure -> {
                AppContext.logFailure("Could not load previous version results for dashboard", failure);
                ctx.showToast(Toast.errorWithAction(
                        "Version Impact Failed",
                        failure.getMessage() == null ? "Could not load prior evaluation results."
                                : MongoConfig.sanitize(failure.getMessage()),
                        "Retry", this::refreshDashboard));
            });
        } else {
            impactCard.setValueText("Baseline");
            impactCard.setSubtext("Version 1 initial baseline");
        }

        // Animate entrance of cards
        Motion.staggeredFadeUp(List.of(totalCard, eligibleCard, rejectedCard, passRateCard, impactCard));
    }

    @Override
    public Node getRoot() {
        return rootPane;
    }

    @Override
    public void onShow() {
        refreshDashboard();
    }
}
