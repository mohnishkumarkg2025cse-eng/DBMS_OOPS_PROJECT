package com.ruleweaver.ui.views;

import com.ruleweaver.model.*;
import com.ruleweaver.config.MongoConfig;
import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.components.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.concurrent.Task;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Screen 3: Algorithmic Evaluation Engine & Transparent Explanation Inspector.
 */
public class EvaluationView implements BaseView {

    private final StackPane rootPane = new StackPane();
    private final ComboBox<Policy> policyCombo = new ComboBox<>();
    private final ComboBox<PolicyVersion> versionCombo = new ComboBox<>();
    private final TableView<EvaluationResult> resultsTable = new TableView<>();
    private final LoadingOverlay loadingOverlay = new LoadingOverlay("Executing Algorithmic Decision Engine...");

    private final ObservableList<EvaluationResult> evaluationList = FXCollections.observableArrayList();
    private FilteredList<EvaluationResult> filteredList;

    private String currentFilter = "ALL";
    private final Label countLabel = new Label("0 evaluations");

    public EvaluationView() {
        buildUI();
    }

    private void buildUI() {
        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(28, 32, 28, 32));

        // 1. Header Toolbar
        HBox toolbar = createToolbar();

        // 2. Control & Filter Bar
        HBox filterBar = createFilterBar();

        // 3. Results Table
        setupTable();

        VBox tableCard = new VBox(12);
        tableCard.getStyleClass().add("card");
        VBox.setVgrow(resultsTable, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        HBox footer = new HBox(countLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        countLabel.getStyleClass().add("caption-label");

        tableCard.getChildren().addAll(resultsTable, footer);

        contentBox.getChildren().addAll(toolbar, filterBar, tableCard);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        rootPane.getChildren().addAll(contentBox, loadingOverlay);

        setupListeners();
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(16);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Algorithmic Decision Engine");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Evaluate candidate pool against governance policies with complete explainability");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // "Evaluate All" Async action button
        Button evalBtn = new Button("Evaluate Candidate Pool");
        evalBtn.getStyleClass().add("btn-primary");
        evalBtn.setGraphic(Icons.create(Icons.PLAY, 0.7, "#11111b"));
        evalBtn.setOnAction(e -> runEvaluationAsync());

        toolbar.getChildren().addAll(titleBox, evalBtn);
        return toolbar;
    }

    private HBox createFilterBar() {
        HBox bar = new HBox(14);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: -color-card; -fx-background-radius: 14px; "
                + "-fx-padding: 12px 18px 12px 18px; -fx-border-color: -color-border-subtle; -fx-border-radius: 14px; -fx-border-width: 1px;");

        Label pLbl = new Label("Policy:");
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

        Label vLbl = new Label("Version:");
        vLbl.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text-secondary;");

        versionCombo.setPrefWidth(180);
        versionCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PolicyVersion pv, boolean empty) {
                super.updateItem(pv, empty);
                setText(empty || pv == null ? null : "Version " + pv.getVersionNumber() + " (" + pv.getConditions().size() + " rules)");
            }
        });
        versionCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PolicyVersion pv, boolean empty) {
                super.updateItem(pv, empty);
                setText(empty || pv == null ? "Select Version" : "Version " + pv.getVersionNumber());
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Filter Toggle Buttons: All / Pass / Fail
        HBox toggleGroup = new HBox(4);
        toggleGroup.setAlignment(Pos.CENTER);
        toggleGroup.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 10px; -fx-padding: 3px;");

        Button allBtn = new Button("All");
        allBtn.getStyleClass().add("btn-secondary");

        Button passBtn = new Button("Passed");
        passBtn.getStyleClass().add("btn-secondary");

        Button failBtn = new Button("Rejected");
        failBtn.getStyleClass().add("btn-secondary");

        allBtn.setOnAction(e -> applyFilter("ALL", allBtn, passBtn, failBtn));
        passBtn.setOnAction(e -> applyFilter("PASS", passBtn, allBtn, failBtn));
        failBtn.setOnAction(e -> applyFilter("FAIL", failBtn, allBtn, passBtn));

        toggleGroup.getChildren().addAll(allBtn, passBtn, failBtn);

        bar.getChildren().addAll(pLbl, policyCombo, vLbl, versionCombo, spacer, toggleGroup);
        return bar;
    }

    private void applyFilter(String filter, Button active, Button... inactives) {
        this.currentFilter = filter;
        active.setStyle("-fx-background-color: -color-accent; -fx-text-fill: #11111b; -fx-font-weight: 700;");
        for (Button b : inactives) {
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-text-secondary;");
        }

        if (filteredList != null) {
            filteredList.setPredicate(res -> {
                if ("PASS".equals(filter)) return res.isOverallDecision();
                if ("FAIL".equals(filter)) return !res.isOverallDecision();
                return true;
            });
            updateCountLabel();
        }
    }

    private void setupTable() {
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        resultsTable.setPlaceholder(new EmptyState(
                Icons.EVALUATION,
                "No Evaluation Outcomes",
                "Select a policy and click 'Evaluate Candidate Pool' to execute rule assessments.",
                "Evaluate Now",
                this::runEvaluationAsync
        ));

        // Column 1: Candidate
        TableColumn<EvaluationResult, EvaluationResult> candCol = new TableColumn<>("CANDIDATE");
        candCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        candCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(EvaluationResult res, boolean empty) {
                super.updateItem(res, empty);
                if (empty || res == null) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(12);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Avatar avatar = new Avatar(res.getApplicantName());
                    VBox textBlock = new VBox(2);
                    Label nameLbl = new Label(res.getApplicantName());
                    nameLbl.setStyle("-fx-font-weight: 700; -fx-text-fill: -color-text-primary;");
                    Label idLbl = new Label("ID: " + res.getApplicantId());
                    idLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");
                    textBlock.getChildren().addAll(nameLbl, idLbl);
                    box.getChildren().addAll(avatar, textBlock);
                    setGraphic(box);
                }
            }
        });
        candCol.setPrefWidth(220);

        // Column 2: Status Verdict
        TableColumn<EvaluationResult, EvaluationResult> statusCol = new TableColumn<>("DECISION VERDICT");
        statusCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(EvaluationResult res, boolean empty) {
                super.updateItem(res, empty);
                if (empty || res == null) {
                    setGraphic(null);
                } else {
                    StatusBadge badge = res.isOverallDecision() 
                            ? StatusBadge.pass("ELIGIBLE (PASS)") 
                            : StatusBadge.fail("REJECTED (" + res.getFailedConditions().size() + " failed)");
                    setGraphic(badge);
                }
            }
        });
        statusCol.setPrefWidth(170);

        // Column 3: Rules Satisfied Breakdown
        TableColumn<EvaluationResult, String> breakdownCol = new TableColumn<>("RULE COMPLIANCE");
        breakdownCol.setCellValueFactory(param -> {
            int passed = param.getValue().getPassedConditions().size();
            int total = param.getValue().getConditionResults().size();
            return new SimpleStringProperty(passed + " of " + total + " criteria met");
        });
        breakdownCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String str, boolean empty) {
                super.updateItem(str, empty);
                if (empty || str == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(str);
                    lbl.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text-secondary;");
                    setGraphic(lbl);
                }
            }
        });
        breakdownCol.setPrefWidth(180);

        // Column 4: Timestamp
        TableColumn<EvaluationResult, String> timeCol = new TableColumn<>("EVALUATED AT");
        timeCol.setCellValueFactory(param -> {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            return new SimpleStringProperty(param.getValue().getEvaluatedOn() == null
                    ? "Unknown" : param.getValue().getEvaluatedOn().format(dtf));
        });
        timeCol.setPrefWidth(160);

        // Column 5: Explain Audit Trail
        TableColumn<EvaluationResult, EvaluationResult> explainCol = new TableColumn<>("AUDIT EXPLANATION");
        explainCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        explainCol.setCellFactory(col -> new TableCell<>() {
            private final Button inspectBtn = new Button("View Explanation");
            {
                inspectBtn.getStyleClass().add("btn-secondary");
                inspectBtn.setGraphic(Icons.create(Icons.EYE, 0.65, "#89b4fa"));
            }
            @Override
            protected void updateItem(EvaluationResult res, boolean empty) {
                super.updateItem(res, empty);
                if (empty || res == null) {
                    setGraphic(null);
                } else {
                    inspectBtn.setOnAction(e -> showExplanationModal(res));
                    setGraphic(inspectBtn);
                }
            }
        });
        explainCol.setPrefWidth(160);

        resultsTable.getColumns().addAll(List.of(candCol, statusCol, breakdownCol, timeCol, explainCol));

        // Wire Filtered & Sorted Lists
        filteredList = new FilteredList<>(evaluationList, p -> true);
        SortedList<EvaluationResult> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(resultsTable.comparatorProperty());
        resultsTable.setItems(sortedList);
    }

    private void setupListeners() {
        AppContext ctx = AppContext.getInstance();
        policyCombo.setItems(ctx.getPolicies());

        policyCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ctx.selectedPolicyProperty().set(newVal);
                versionCombo.getItems().setAll(newVal.getVersions());
                int activeNumber = newVal.getActiveVersionNumber();
                versionCombo.setValue(newVal.getVersion(activeNumber)
                        .orElseGet(() -> newVal.getLatestVersion().orElse(null)));
            } else {
                versionCombo.getItems().clear();
                evaluationList.clear();
            }
        });
        versionCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            ctx.selectedVersionProperty().set(newVal);
            loadStoredResults();
        });

        // Mirror AppContext selections
        ctx.selectedPolicyProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                if (newVal != null && policyCombo.getValue() != newVal) {
                    policyCombo.setValue(newVal);
                }
            });
        });
    }

    private void runEvaluationAsync() {
        Policy selectedPolicy = policyCombo.getValue();
        PolicyVersion selectedVersion = versionCombo.getValue();

        if (selectedPolicy == null || selectedVersion == null) {
            AppContext.getInstance().showToast(Toast.error("Selection Required", "Please select both a policy and version to evaluate.", null));
            return;
        }

        AppContext ctx = AppContext.getInstance();
        List<Applicant> pool = List.copyOf(ctx.getApplicants());
        Task<List<EvaluationResult>> task = new Task<>() {
            @Override
            protected List<EvaluationResult> call() {
                updateProgress(0, Math.max(pool.size(), 1));
                updateMessage("Evaluating 0/" + pool.size());
                return ctx.getPolicyService().evaluateApplicants(
                        selectedPolicy.getId(),
                        selectedVersion.getVersionNumber(),
                        pool,
                        completed -> {
                            updateProgress(completed, Math.max(pool.size(), 1));
                            updateMessage("Evaluating " + completed + "/" + pool.size());
                        },
                        this::isCancelled);
            }
        };
        task.messageProperty().addListener((observable, oldMessage, message) ->
                loadingOverlay.setMessage(message));
        task.progressProperty().addListener((observable, oldProgress, progress) ->
                loadingOverlay.setProgress(progress.doubleValue()));
        loadingOverlay.show("Evaluating 0/" + pool.size(), task::cancel);
        task.setOnSucceeded(event -> {
            loadingOverlay.hide();
            List<EvaluationResult> results = task.getValue();
            evaluationList.setAll(results);
            ctx.getEvaluationResults().setAll(results);
            ctx.markResultsChanged();
            updateCountLabel();

            long passed = results.stream().filter(EvaluationResult::isOverallDecision).count();
            ctx.showToast(Toast.success(
                    "Evaluation Complete",
                    "Evaluated " + results.size() + " applicants: " + passed + " Eligible, "
                            + (results.size() - passed) + " Rejected.",
                    null
            ));
        });
        task.setOnCancelled(event -> {
            loadingOverlay.hide();
            ctx.showToast(Toast.info("Evaluation Cancelled", "No partial results were saved.", null));
        });
        task.setOnFailed(event -> {
            loadingOverlay.hide();
            Throwable failure = task.getException();
            if (failure != null) {
                AppContext.logFailure("Evaluation failed", failure);
            }
            String message = failure == null || failure.getMessage() == null
                    ? "Evaluation failed." : MongoConfig.sanitize(failure.getMessage());
            ctx.showToast(Toast.errorWithAction("Evaluation Failed", message, "Retry", this::runEvaluationAsync));
        });
        ctx.getExecutor().execute(task);
    }

    private void loadStoredResults() {
        Policy policy = policyCombo.getValue();
        PolicyVersion version = versionCombo.getValue();
        AppContext ctx = AppContext.getInstance();
        if (policy == null || version == null) {
            evaluationList.clear();
            ctx.getEvaluationResults().clear();
            updateCountLabel();
            return;
        }
        ctx.loadEvaluationResultsAsync(policy.getId(), version.getVersionNumber(), results -> {
            evaluationList.setAll(results);
            updateCountLabel();
        }, failure -> {
            AppContext.logFailure("Could not load selected evaluation history", failure);
            ctx.showToast(Toast.errorWithAction(
                    "Evaluation History Failed",
                    failure.getMessage() == null ? "Could not load stored results."
                            : MongoConfig.sanitize(failure.getMessage()),
                    "Retry", this::loadStoredResults));
        });
    }

    private void showExplanationModal(EvaluationResult result) {
        StackPane modalBackdrop = new StackPane();
        modalBackdrop.getStyleClass().add("modal-backdrop");

        VBox box = new VBox(18);
        box.getStyleClass().add("modal-window");
        box.setPrefWidth(540);
        box.setMaxHeight(600);

        // Header with Applicant Info
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Avatar avatar = new Avatar(result.getApplicantName());

        VBox titleBlock = new VBox(2);
        Label nameLbl = new Label(result.getApplicantName());
        nameLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -color-text-primary;");
        Label policyInfo = new Label("Policy: " + result.getPolicyName() + " (v" + result.getPolicyVersionNumber() + ")");
        policyInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-secondary;");
        titleBlock.getChildren().addAll(nameLbl, policyInfo);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(Icons.create(Icons.CLOSE, 0.7, "#9aa0c3"));
        closeBtn.getStyleClass().add("btn-icon");
        closeBtn.setOnAction(e -> rootPane.getChildren().remove(modalBackdrop));

        header.getChildren().addAll(avatar, titleBlock, closeBtn);

        // Verdict Banner
        HBox verdictBanner = new HBox(10);
        verdictBanner.setAlignment(Pos.CENTER_LEFT);
        verdictBanner.setStyle("-fx-background-radius: 12px; -fx-padding: 12px 16px 12px 16px; "
                + (result.isOverallDecision() 
                        ? "-fx-background-color: rgba(166,227,161,0.12); -fx-border-color: rgba(166,227,161,0.3); -fx-border-radius: 12px; -fx-border-width: 1px;"
                        : "-fx-background-color: rgba(243,139,168,0.12); -fx-border-color: rgba(243,139,168,0.3); -fx-border-radius: 12px; -fx-border-width: 1px;"));

        SVGPath verdictIcon = Icons.create(result.isOverallDecision() ? Icons.CHECK : Icons.CLOSE, 0.9,
                result.isOverallDecision() ? "#a6e3a1" : "#f38ba8");

        VBox verdictTexts = new VBox(2);
        Label verdictTitle = new Label(result.isOverallDecision() ? "VERDICT: ELIGIBLE (ACCEPTED)" : "VERDICT: NOT ELIGIBLE (REJECTED)");
        verdictTitle.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: " 
                + (result.isOverallDecision() ? "#a6e3a1;" : "#f38ba8;"));

        int total = result.getConditionResults().size();
        int failed = result.getFailedConditions().size();
        Label verdictSub = new Label(result.isOverallDecision() 
                ? "All " + total + " criteria rules satisfied." 
                : failed + " of " + total + " criteria failed.");
        verdictSub.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-secondary;");

        verdictTexts.getChildren().addAll(verdictTitle, verdictSub);
        verdictBanner.getChildren().addAll(verdictIcon, verdictTexts);

        // Condition-by-condition breakdown list
        Label breakdownTitle = new Label("GRANULAR CRITERIA TRACE");
        breakdownTitle.getStyleClass().add("caption-label");

        VBox conditionCards = new VBox(10);
        for (ConditionResult cr : result.getConditionResults()) {
            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 10px; "
                    + "-fx-padding: 12px 14px 12px 14px; -fx-border-color: -color-border-subtle; -fx-border-radius: 10px; -fx-border-width: 1px;");

            HBox topRow = new HBox(8);
            topRow.setAlignment(Pos.CENTER_LEFT);

            SVGPath icon = Icons.create(cr.isPassed() ? Icons.CHECK : Icons.CLOSE, 0.75,
                    cr.isPassed() ? "#a6e3a1" : "#f38ba8");

            Label ruleName = new Label(cr.getConditionName());
            ruleName.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: -color-text-primary;");
            HBox.setHgrow(ruleName, Priority.ALWAYS);

            StatusBadge badge = cr.isPassed() ? StatusBadge.pass("PASS") : StatusBadge.fail("FAIL");
            topRow.getChildren().addAll(icon, ruleName, badge);

            Label explanation = new Label(cr.getExplanation());
            explanation.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-text-secondary;");

            // Mini progress bar for numeric actual vs required where applicable
            if (cr.getActualValue() instanceof Number numVal) {
                ProgressBar miniBar = new ProgressBar();
                miniBar.setPrefWidth(Double.MAX_VALUE);
                miniBar.setPrefHeight(4);
                // Heuristic progress estimation
                double actual = numVal.doubleValue();
                miniBar.setProgress(Math.min(1.0, Math.max(0.0, actual / 10.0))); // scaled to 0-10
                if (cr.isPassed()) {
                    miniBar.getStyleClass().add("progress-bar-success");
                } else {
                    miniBar.getStyleClass().add("progress-bar-danger");
                }
                card.getChildren().addAll(topRow, explanation, miniBar);
            } else {
                card.getChildren().addAll(topRow, explanation);
            }

            conditionCards.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(conditionCards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button doneBtn = new Button("Done");
        doneBtn.getStyleClass().add("btn-secondary");
        doneBtn.setOnAction(e -> rootPane.getChildren().remove(modalBackdrop));
        HBox bottom = new HBox(doneBtn);
        bottom.setAlignment(Pos.CENTER_RIGHT);

        box.getChildren().addAll(header, verdictBanner, breakdownTitle, scroll, bottom);
        modalBackdrop.getChildren().add(box);

        rootPane.getChildren().add(modalBackdrop);
        Motion.modalScaleIn(box);
    }

    private void updateCountLabel() {
        int count = filteredList != null ? filteredList.size() : 0;
        countLabel.setText(count + " evaluations displayed (" + currentFilter + ")");
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
        } else {
            loadStoredResults();
        }
    }
}
