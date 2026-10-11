package com.ruleweaver.ui.views;

import com.ruleweaver.engine.Condition;
import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.engine.SkillCondition;
import com.ruleweaver.model.Policy;
import com.ruleweaver.model.PolicyVersion;
import com.ruleweaver.config.MongoConfig;
import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.components.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Screen 2: Policy & Version Manager (Master-Detail).
 * Features dynamic rule creation, snapshot versioning, and immutable policy inspection.
 */
public class PoliciesView implements BaseView {

    private final StackPane rootPane = new StackPane();
    private final ListView<Policy> policyListView = new ListView<>();
    private final ComboBox<PolicyVersion> versionComboBox = new ComboBox<>();
    private final TableView<PolicyVersion> versionHistoryTable = new TableView<>();
    private final Map<Integer, Long> evaluationCounts = new HashMap<>();
    private final VBox conditionsContainer = new VBox(10);

    private final Label policyTitleLabel = new Label("Select a Policy");
    private final Label policyDescLabel = new Label("Choose a policy from the list to view version history.");
    private final Label policyIdBadge = new Label("");
    private final Button newVersionBtn = new Button("New Version");
    private final Button setActiveBtn = new Button("Set Active");
    private Policy displayedPolicy;

    public PoliciesView() {
        buildUI();
    }

    private void buildUI() {
        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(28, 32, 28, 32));

        // 1. Toolbar Header
        HBox toolbar = createToolbar();

        // 2. Master-Detail Layout (SplitPane / HBox)
        HBox masterDetail = new HBox(20);
        VBox.setVgrow(masterDetail, Priority.ALWAYS);

        // Master Column (Policies List)
        VBox masterCol = createMasterColumn();
        masterCol.setPrefWidth(320);
        masterCol.setMinWidth(280);

        // Detail Column (Version & Conditions)
        VBox detailCol = createDetailColumn();
        HBox.setHgrow(detailCol, Priority.ALWAYS);

        masterDetail.getChildren().addAll(masterCol, detailCol);

        contentBox.getChildren().addAll(toolbar, masterDetail);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        rootPane.getChildren().add(contentBox);

        // Bindings & Selection Handlers
        setupSelectionHandlers();
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(16);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Policy & Version Manager");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Configure governance criteria, version snapshots, and threshold rules");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button newPolicyBtn = new Button("New Policy");
        newPolicyBtn.getStyleClass().add("btn-primary");
        newPolicyBtn.setGraphic(Icons.create(Icons.PLUS, 0.7, "#11111b"));
        newPolicyBtn.setOnAction(e -> showPolicyModal(null));

        toolbar.getChildren().addAll(titleBox, newPolicyBtn);
        return toolbar;
    }

    private VBox createMasterColumn() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label masterTitle = new Label("DEFINED POLICIES");
        masterTitle.getStyleClass().add("caption-label");

        policyListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Policy policy, boolean empty) {
                super.updateItem(policy, empty);
                if (empty || policy == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox box = new VBox(4);
                    box.setPadding(new Insets(6, 4, 6, 4));
                    box.setMaxWidth(Double.MAX_VALUE);

                    HBox titleRow = new HBox(8);
                    titleRow.setAlignment(Pos.CENTER_LEFT);
                    titleRow.setMaxWidth(Double.MAX_VALUE);

                    Label nameLbl = new Label(policy.getName());
                    nameLbl.setStyle("-fx-font-weight: 700; -fx-text-fill: -color-text-primary; -fx-font-size: 13px;");
                    nameLbl.setTextOverrun(OverrunStyle.ELLIPSIS);
                    nameLbl.setWrapText(false);
                    nameLbl.setMinWidth(0);
                    nameLbl.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(nameLbl, Priority.ALWAYS);

                    StatusBadge versionBadge = StatusBadge.info("v" + policy.getVersions().size());
                    titleRow.getChildren().addAll(nameLbl, versionBadge);

                    Label idLbl = new Label("ID: " + policy.getId());
                    idLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

                    box.getChildren().addAll(titleRow, idLbl);
                    setGraphic(box);
                }
            }
        });
        policyListView.getStyleClass().add("policy-list");
        VBox.setVgrow(policyListView, Priority.ALWAYS);

        card.getChildren().addAll(masterTitle, policyListView);
        return card;
    }

    private VBox createDetailColumn() {
        VBox card = new VBox(18);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        // Header with Policy info & Actions
        HBox detailHeader = new HBox(12);
        detailHeader.setAlignment(Pos.CENTER_LEFT);

        VBox infoBlock = new VBox(3);
        policyTitleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: -color-text-primary;");
        policyDescLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-secondary;");
        infoBlock.getChildren().addAll(policyTitleLabel, policyDescLabel);
        HBox.setHgrow(infoBlock, Priority.ALWAYS);

        policyIdBadge.getStyleClass().add("caption-label");
        detailHeader.getChildren().addAll(infoBlock, policyIdBadge);

        // Version Selector & Action Row
        HBox versionControlBar = new HBox(12);
        versionControlBar.setAlignment(Pos.CENTER_LEFT);
        versionControlBar.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12px; "
                + "-fx-padding: 10px 14px 10px 14px; -fx-border-color: -color-border-subtle; -fx-border-radius: 12px; -fx-border-width: 1px;");

        Label versionPrompt = new Label("Policy Version:");
        versionPrompt.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text-secondary;");

        versionComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PolicyVersion pv, boolean empty) {
                super.updateItem(pv, empty);
                if (empty || pv == null) {
                    setText(null);
                } else {
                    setText("Version " + pv.getVersionNumber() + " (" + pv.getConditions().size() + " rules)");
                }
            }
        });
        versionComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PolicyVersion pv, boolean empty) {
                super.updateItem(pv, empty);
                if (empty || pv == null) {
                    setText("Select Version");
                } else {
                    setText("Version " + pv.getVersionNumber() + " (" + pv.getConditions().size() + " rules)");
                }
            }
        });
        versionComboBox.setPrefWidth(200);

        setActiveBtn.getStyleClass().add("btn-secondary");
        setActiveBtn.setGraphic(Icons.create(Icons.CHECK, 0.65, "#a6e3a1"));
        setActiveBtn.setOnAction(e -> {
            PolicyVersion selected = versionComboBox.getValue();
            if (selected != null) {
                setActiveVersion(selected);
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        newVersionBtn.getStyleClass().add("btn-primary");
        newVersionBtn.setGraphic(Icons.create(Icons.PLUS, 0.7, "#11111b"));
        newVersionBtn.setOnAction(e -> {
            Policy selected = policyListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showNewVersionModal(selected);
            }
        });

        versionControlBar.getChildren().addAll(versionPrompt, versionComboBox, setActiveBtn, spacer, newVersionBtn);
        VBox historySection = createVersionHistory();

        // Conditions Section
        Label conditionsHeader = new Label("ACTIVE EVALUATION RULES");
        conditionsHeader.getStyleClass().add("caption-label");

        ScrollPane scroll = new ScrollPane(conditionsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        card.getChildren().addAll(detailHeader, versionControlBar, historySection, conditionsHeader, scroll);
        return card;
    }

    private VBox createVersionHistory() {
        VBox section = new VBox(8);
        Label title = new Label("VERSION HISTORY");
        title.getStyleClass().add("caption-label");
        versionHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        versionHistoryTable.setPrefHeight(170);

        TableColumn<PolicyVersion, String> number = new TableColumn<>("Version");
        number.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().getVersionNumber())));
        TableColumn<PolicyVersion, String> date = new TableColumn<>("Date");
        date.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedAt() == null
                ? "Unknown" : cell.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        TableColumn<PolicyVersion, String> note = new TableColumn<>("Note");
        note.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getDescription() == null ? "" : cell.getValue().getDescription()));
        TableColumn<PolicyVersion, String> rules = new TableColumn<>("Rules");
        rules.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().getConditions().size())));
        TableColumn<PolicyVersion, String> evaluations = new TableColumn<>("Evaluations");
        evaluations.setCellValueFactory(cell -> new SimpleStringProperty(
                String.valueOf(evaluationCounts.getOrDefault(cell.getValue().getVersionNumber(), 0L))));
        TableColumn<PolicyVersion, PolicyVersion> active = new TableColumn<>("Status");
        active.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue()));
        active.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(PolicyVersion version, boolean empty) {
                super.updateItem(version, empty);
                setGraphic(empty || version == null ? null
                        : version.getVersionNumber() == currentPolicyActiveVersion()
                        ? StatusBadge.pass("ACTIVE") : null);
            }
        });
        versionHistoryTable.getColumns().setAll(List.of(number, date, note, rules, evaluations, active));
        section.getChildren().addAll(title, versionHistoryTable);
        return section;
    }

    private int currentPolicyActiveVersion() {
        return displayedPolicy == null ? 0 : displayedPolicy.getActiveVersionNumber();
    }

    private void setupSelectionHandlers() {
        AppContext ctx = AppContext.getInstance();
        policyListView.setItems(ctx.getPolicies());

        policyListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                ctx.selectedPolicyProperty().set(selected);
                displayPolicy(selected);
            }
        });

        versionComboBox.valueProperty().addListener((obs, oldVal, selectedVer) -> {
            if (selectedVer != null) {
                ctx.selectedVersionProperty().set(selectedVer);
                renderConditions(selectedVer);
            }
        });
    }

    private void displayPolicy(Policy policy) {
        displayedPolicy = policy;
        policyTitleLabel.setText(policy.getName());
        policyDescLabel.setText(policy.getDescription() != null && !policy.getDescription().isEmpty() 
                ? policy.getDescription() 
                : "No description specified for this policy.");
        policyIdBadge.setText("ID: " + policy.getId());

        List<PolicyVersion> versions = policy.getVersions();
        versionComboBox.getItems().setAll(versions);
        evaluationCounts.clear();
        versionHistoryTable.setItems(FXCollections.observableArrayList(versions));
        loadEvaluationCounts(policy);

        if (!versions.isEmpty()) {
            PolicyVersion active = policy.getVersion(policy.getActiveVersionNumber()).orElse(versions.get(0));
            versionComboBox.setValue(active);
            renderConditions(active);
        } else {
            conditionsContainer.getChildren().clear();
        }
    }

    private void loadEvaluationCounts(Policy policy) {
        AppContext ctx = AppContext.getInstance();
        Task<Map<Integer, Long>> task = new Task<>() {
            @Override
            protected Map<Integer, Long> call() {
                Map<Integer, Long> counts = new HashMap<>();
                for (PolicyVersion version : policy.getVersions()) {
                    counts.put(version.getVersionNumber(), ctx.getEvaluationResultRepository()
                            .countByPolicyAndVersion(policy.getId(), version.getVersionNumber()));
                }
                return counts;
            }
        };
        task.setOnSucceeded(event -> {
            if (policyListView.getSelectionModel().getSelectedItem() == policy) {
                evaluationCounts.putAll(task.getValue());
                versionHistoryTable.refresh();
            }
        });
        task.setOnFailed(event -> {
            Throwable failure = task.getException();
            if (failure != null) {
                AppContext.logFailure("Could not load policy evaluation counts", failure);
                ctx.showToast(Toast.errorWithAction(
                        "Version History Failed",
                        failure.getMessage() == null ? "Could not load evaluation counts."
                                : MongoConfig.sanitize(failure.getMessage()),
                        "Retry", () -> loadEvaluationCounts(policy)));
            }
        });
        ctx.getExecutor().execute(task);
    }

    private void setActiveVersion(PolicyVersion version) {
        AppContext ctx = AppContext.getInstance();
        Task<Policy> task = new Task<>() {
            @Override
            protected Policy call() {
                return ctx.getPolicyService().setActiveVersion(version.getPolicyId(), version.getVersionNumber());
            }
        };
        task.setOnSucceeded(event -> {
            Policy refreshed = task.getValue();
            ctx.activeVersionProperty().set(version);
            ctx.loadPoliciesAsync(() -> {
                Policy selected = ctx.getPolicies().stream()
                        .filter(policy -> policy.getId().equals(refreshed.getId()))
                        .findFirst().orElse(refreshed);
                policyListView.getSelectionModel().select(selected);
                ctx.selectedPolicyProperty().set(selected);
                displayPolicy(selected);
            });
            ctx.showToast(Toast.success("Active Version Set",
                    "Policy '" + refreshed.getName() + "' version " + version.getVersionNumber() + " is now active.", null));
        });
        task.setOnFailed(event -> {
            Throwable failure = task.getException();
            if (failure != null) {
                AppContext.logFailure("Could not set active policy version", failure);
                ctx.showToast(Toast.errorWithAction("Could Not Set Active Version",
                        failure.getMessage() == null ? "The active version could not be saved."
                                : MongoConfig.sanitize(failure.getMessage()),
                        "Retry", () -> setActiveVersion(version)));
            }
        });
        ctx.getExecutor().execute(task);
    }

    private void renderConditions(PolicyVersion version) {
        conditionsContainer.getChildren().clear();

        List<Condition> conditions = version.getConditions();
        if (conditions.isEmpty()) {
            conditionsContainer.getChildren().add(new EmptyState(
                    Icons.POLICIES,
                    "No Conditions Defined",
                    "This policy version contains 0 criteria. All applicants vacuously pass.",
                    null,
                    null
            ));
            return;
        }

        for (int i = 0; i < conditions.size(); i++) {
            Condition c = conditions.get(i);
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12px; "
                    + "-fx-padding: 14px 18px 14px 18px; -fx-border-color: -color-border-subtle; -fx-border-radius: 12px; -fx-border-width: 1px;");

            Label num = new Label(String.valueOf(i + 1));
            num.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: -color-accent;");

            VBox ruleInfo = new VBox(2);
            Label name = new Label(c.getMessage());
            name.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -color-text-primary;");

            Label type = new Label("Type: " + c.getType());
            type.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

            ruleInfo.getChildren().addAll(name, type);
            HBox.setHgrow(ruleInfo, Priority.ALWAYS);

            StatusBadge badge = c instanceof NumericCondition 
                    ? StatusBadge.info("Numeric") 
                    : (c instanceof SkillCondition ? StatusBadge.pass("Skill") : StatusBadge.warning("Composite"));

            row.getChildren().addAll(num, ruleInfo, badge);
            conditionsContainer.getChildren().add(row);
        }
    }

    private void showPolicyModal(Policy existing) {
        StackPane modalBackdrop = new StackPane();
        modalBackdrop.getStyleClass().add("modal-backdrop");

        VBox box = new VBox(16);
        box.getStyleClass().add("modal-window");
        box.setPrefWidth(540);

        Label title = new Label("Define New Business Policy");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -color-text-primary;");

        TextField nameInput = new TextField();
        nameInput.setPromptText("Policy Name (e.g. Merit Scholarship Eligibility)");

        TextField descInput = new TextField();
        descInput.setPromptText("Description / Business Intent");

        VBox rowsContainer = new VBox(10);
        Button addRuleBtn = new Button("Add Condition");
        addRuleBtn.getStyleClass().add("btn-secondary");
        addRuleBtn.setGraphic(Icons.create(Icons.PLUS, 0.65, "#89b4fa"));
        addRuleBtn.setOnAction(e -> {
            ConditionRow newRow = new ConditionRow(r -> rowsContainer.getChildren().remove(r));
            rowsContainer.getChildren().add(newRow);
        });

        // Add 1 default row
        ConditionRow firstRow = new ConditionRow(r -> rowsContainer.getChildren().remove(r));
        rowsContainer.getChildren().add(firstRow);

        ScrollPane rowsScroll = new ScrollPane(rowsContainer);
        rowsScroll.setFitToWidth(true);
        rowsScroll.setMaxHeight(260);
        rowsScroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> rootPane.getChildren().remove(modalBackdrop));

        Button saveBtn = new Button("Create Policy");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            String name = nameInput.getText().trim();
            if (name.isEmpty()) {
                Motion.shake(nameInput);
                return;
            }

            List<Condition> conditions = new ArrayList<>();
            try {
                for (Node child : rowsContainer.getChildren()) {
                    if (child instanceof ConditionRow cr) {
                        conditions.add(cr.buildCondition());
                    }
                }
            } catch (IllegalArgumentException ex) {
                AppContext.getInstance().showToast(Toast.error("Condition Error", ex.getMessage(), null));
                return;
            }

            if (conditions.isEmpty()) {
                AppContext.getInstance().showToast(Toast.error("Validation Error", "At least one condition rule must be defined.", null));
                return;
            }

            String policyId = "POL-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            AppContext.getInstance().runAsync(() -> {
                Policy policy = AppContext.getInstance().getPolicyService().createPolicy(
                        policyId,
                        name,
                        descInput.getText().trim(),
                        conditions
                );
                Platform.runLater(() -> {
                    rootPane.getChildren().remove(modalBackdrop);
                    AppContext.getInstance().showToast(Toast.success(
                            "Policy Created",
                            "Policy '" + policy.getName() + "' created with " + conditions.size() + " rules.",
                            null
                    ));
                    AppContext.getInstance().loadPoliciesAsync(() -> {
                        policyListView.getSelectionModel().select(policy);
                    });
                });
            });
        });

        btnRow.getChildren().addAll(cancelBtn, saveBtn);

        box.getChildren().addAll(
                title,
                new VBox(4, new Label("Policy Name *"), nameInput),
                new VBox(4, new Label("Description"), descInput),
                new HBox(8, new Label("Initial Conditions"), addRuleBtn),
                rowsScroll,
                btnRow
        );

        modalBackdrop.getChildren().add(box);
        rootPane.getChildren().add(modalBackdrop);
        Motion.modalScaleIn(box);
    }

    private void showNewVersionModal(Policy policy) {
        StackPane modalBackdrop = new StackPane();
        modalBackdrop.getStyleClass().add("modal-backdrop");

        VBox box = new VBox(16);
        box.getStyleClass().add("modal-window");
        box.setPrefWidth(540);

        int nextVerNum = policy.getVersions().size() + 1;
        Label title = new Label("Create Version " + nextVerNum + " for '" + policy.getName() + "'");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -color-text-primary;");

        TextField descInput = new TextField("Tightened criteria for version " + nextVerNum);
        descInput.setPromptText("Version change rationale / notes");

        VBox rowsContainer = new VBox(10);
        Button addRuleBtn = new Button("Add Condition");
        addRuleBtn.getStyleClass().add("btn-secondary");
        addRuleBtn.setGraphic(Icons.create(Icons.PLUS, 0.65, "#89b4fa"));
        addRuleBtn.setOnAction(e -> {
            ConditionRow newRow = new ConditionRow(r -> rowsContainer.getChildren().remove(r));
            rowsContainer.getChildren().add(newRow);
        });

        // Pre-populate with existing conditions of latest version
        policy.getLatestVersion().ifPresent(latest -> {
            for (Condition c : latest.getConditions()) {
                ConditionRow row = new ConditionRow(r -> rowsContainer.getChildren().remove(r));
                row.populateFrom(c);
                rowsContainer.getChildren().add(row);
            }
        });

        ScrollPane rowsScroll = new ScrollPane(rowsContainer);
        rowsScroll.setFitToWidth(true);
        rowsScroll.setMaxHeight(260);
        rowsScroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> rootPane.getChildren().remove(modalBackdrop));

        Button saveBtn = new Button("Save Version " + nextVerNum);
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            List<Condition> conditions = new ArrayList<>();
            try {
                for (Node child : rowsContainer.getChildren()) {
                    if (child instanceof ConditionRow cr) {
                        conditions.add(cr.buildCondition());
                    }
                }
            } catch (IllegalArgumentException ex) {
                AppContext.getInstance().showToast(Toast.error("Condition Error", ex.getMessage(), null));
                return;
            }

            if (conditions.isEmpty()) {
                AppContext.getInstance().showToast(Toast.error("Validation Error", "At least one condition rule must be defined.", null));
                return;
            }

            AppContext.getInstance().runAsync(() -> {
                PolicyVersion newVersion = AppContext.getInstance().getPolicyService().createNewVersion(
                        policy.getId(),
                        descInput.getText().trim(),
                        conditions
                );
                Platform.runLater(() -> {
                    rootPane.getChildren().remove(modalBackdrop);
                    AppContext.getInstance().showToast(Toast.success(
                            "New Version Created",
                            "Created Version " + newVersion.getVersionNumber() + " with " + conditions.size() + " rules.",
                            null
                    ));
                    displayPolicy(policy);
                    versionComboBox.setValue(newVersion);
                });
            });
        });

        btnRow.getChildren().addAll(cancelBtn, saveBtn);

        box.getChildren().addAll(
                title,
                new VBox(4, new Label("Version Rationale / Notes"), descInput),
                new HBox(8, new Label("Version Criteria (Cloned from v" + (nextVerNum - 1) + ")"), addRuleBtn),
                rowsScroll,
                btnRow
        );

        modalBackdrop.getChildren().add(box);
        rootPane.getChildren().add(modalBackdrop);
        Motion.modalScaleIn(box);
    }

    @Override
    public Node getRoot() {
        return rootPane;
    }

    @Override
    public void onShow() {
        AppContext ctx = AppContext.getInstance();
        ctx.loadPoliciesAsync(() -> {
            Policy sel = ctx.selectedPolicyProperty().get();
            if (sel != null) {
                policyListView.getSelectionModel().select(sel);
            }
        });
    }
}
