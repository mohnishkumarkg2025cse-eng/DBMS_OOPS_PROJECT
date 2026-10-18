package com.ruleweaver.ui.views;

import com.ruleweaver.engine.Condition;
import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.engine.SkillCondition;
import com.ruleweaver.model.Policy;
import com.ruleweaver.model.PolicyVersion;
import com.ruleweaver.model.ProgramConfiguration;
import com.ruleweaver.config.MongoConfig;
import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.components.*;
import javafx.application.Platform;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javafx.util.StringConverter;

/**
 * Screen 2: Policy & Version Manager (Master-Detail).
 * Features dynamic rule creation, snapshot versioning, and immutable policy
 * inspection.
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
    private final Runnable openWhatIfSimulator;
    private Policy displayedPolicy;

    public PoliciesView() {
        this(() -> {
        });
    }

    public PoliciesView(Runnable openWhatIfSimulator) {
        this.openWhatIfSimulator = openWhatIfSimulator;
        buildUI();
    }

    private void buildUI() {
        rootPane.getStyleClass().add("page-root");
        VBox contentBox = new VBox(24);
        contentBox.getStyleClass().add("policies-page");
        contentBox.setMinWidth(0);
        contentBox.setMaxWidth(Double.MAX_VALUE);

        VBox toolbar = createToolbar();

        GridPane masterDetail = new GridPane();
        masterDetail.setHgap(24);
        masterDetail.setVgap(16);
        masterDetail.setMinWidth(0);
        VBox.setVgrow(masterDetail, Priority.ALWAYS);

        VBox masterCol = createMasterColumn();
        masterCol.setPrefWidth(320);
        masterCol.setMinWidth(220);
        masterCol.setMaxWidth(Double.MAX_VALUE);

        VBox detailCol = createDetailColumn();
        detailCol.setMinWidth(0);
        detailCol.setMaxWidth(Double.MAX_VALUE);
        layoutMasterDetail(masterDetail, masterCol, detailCol, 0);

        contentBox.getChildren().addAll(toolbar, masterDetail);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        ScrollPane pageScroll = new ScrollPane(contentBox);
        pageScroll.getStyleClass().add("page-root");
        pageScroll.setFitToWidth(true);
        pageScroll.setFitToHeight(false);
        pageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        rootPane.setMinSize(0, 0);
        rootPane.getChildren().add(pageScroll);
        rootPane.widthProperty().addListener((observable, oldWidth, width) -> layoutMasterDetail(masterDetail,
                masterCol, detailCol, width.doubleValue()));

        setupSelectionHandlers();
    }

    private void layoutMasterDetail(GridPane layout, VBox master, VBox detail, double width) {
        boolean stacked = width > 0 && width < 1040;
        boolean currentlyStacked = GridPane.getRowIndex(detail) != null
                && GridPane.getRowIndex(detail) == 1;
        if (stacked == currentlyStacked && layout.getChildren().containsAll(List.of(master, detail))) {
            return;
        }

        layout.getChildren().clear();
        layout.getColumnConstraints().clear();
        layout.getRowConstraints().clear();
        if (stacked) {
            master.setPrefHeight(240);
            master.setMaxHeight(300);
            GridPane.setConstraints(master, 0, 0);
            GridPane.setConstraints(detail, 0, 1);

            ColumnConstraints column = new ColumnConstraints();
            column.setHgrow(Priority.ALWAYS);
            column.setFillWidth(true);
            layout.getColumnConstraints().add(column);
            RowConstraints masterRow = new RowConstraints();
            masterRow.setVgrow(Priority.NEVER);
            RowConstraints detailRow = new RowConstraints();
            detailRow.setVgrow(Priority.ALWAYS);
            layout.getRowConstraints().addAll(masterRow, detailRow);
        } else {
            master.setPrefHeight(Region.USE_COMPUTED_SIZE);
            master.setMaxHeight(Double.MAX_VALUE);
            GridPane.setConstraints(master, 0, 0);
            GridPane.setConstraints(detail, 1, 0);

            ColumnConstraints masterColumn = new ColumnConstraints();
            masterColumn.setPercentWidth(32);
            masterColumn.setHgrow(Priority.ALWAYS);
            masterColumn.setFillWidth(true);
            ColumnConstraints detailColumn = new ColumnConstraints();
            detailColumn.setPercentWidth(68);
            detailColumn.setHgrow(Priority.ALWAYS);
            detailColumn.setFillWidth(true);
            layout.getColumnConstraints().addAll(masterColumn, detailColumn);
            RowConstraints row = new RowConstraints();
            row.setVgrow(Priority.ALWAYS);
            layout.getRowConstraints().add(row);
        }
        layout.getChildren().addAll(master, detail);
    }

    private VBox createToolbar() {
        VBox toolbar = new VBox(16);
        toolbar.getStyleClass().add("policies-toolbar");
        toolbar.setMinWidth(0);
        Label title = new Label("Policies");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage governance criteria, policy versions, and evaluation rules");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("page-subtitle");

        Button newPolicyBtn = new Button("New Policy");
        newPolicyBtn.getStyleClass().add("btn-primary");
        newPolicyBtn.setMinWidth(136);
        var addIcon = Icons.create(Icons.PLUS, 0.7, (String) null);
        addIcon.getStyleClass().add("policy-primary-icon");
        newPolicyBtn.setGraphic(addIcon);
        newPolicyBtn.setOnAction(e -> showPolicyModal(null));

        FlowPane actions = new FlowPane(16, 8, newPolicyBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setMaxWidth(Double.MAX_VALUE);
        toolbar.getChildren().addAll(title, subtitle, actions);
        return toolbar;
    }

    private VBox createMasterColumn() {
        VBox card = new VBox(16);
        card.getStyleClass().addAll("card", "policies-card", "policy-master-card");

        Label masterTitle = new Label("Defined policies");
        masterTitle.getStyleClass().add("policy-section-title");

        policyListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Policy policy, boolean empty) {
                super.updateItem(policy, empty);
                if (empty || policy == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox box = new VBox(8);
                    box.getStyleClass().add("policy-list-item");
                    box.setMaxWidth(Double.MAX_VALUE);

                    HBox titleRow = new HBox(8);
                    titleRow.setAlignment(Pos.CENTER_LEFT);
                    titleRow.setMaxWidth(Double.MAX_VALUE);

                    Label nameLbl = new Label(policy.getName());
                    nameLbl.getStyleClass().add("policy-row-name");
                    nameLbl.setWrapText(true);
                    nameLbl.setMinWidth(0);
                    nameLbl.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(nameLbl, Priority.ALWAYS);

                    StatusBadge versionBadge = StatusBadge.info("v" + policy.getVersions().size());
                    versionBadge.setMinWidth(Region.USE_PREF_SIZE);
                    titleRow.getChildren().addAll(nameLbl, versionBadge);

                    Label idLbl = new Label("ID: " + policy.getId());
                    idLbl.getStyleClass().add("policy-row-id");
                    idLbl.setWrapText(true);

                    box.getChildren().addAll(titleRow, idLbl);
                    setGraphic(box);
                }
            }
        });
        policyListView.getStyleClass().addAll("policy-list", "policies-list");
        policyListView.setPlaceholder(new EmptyState(
                Icons.POLICIES,
                "No policies yet",
                "Create a policy to define versioned evaluation criteria.",
                null,
                null));
        VBox.setVgrow(policyListView, Priority.ALWAYS);

        card.getChildren().addAll(masterTitle, policyListView);
        return card;
    }

    private VBox createDetailColumn() {
        VBox card = new VBox(18);
        card.getStyleClass().addAll("card", "policy-detail");
        VBox.setVgrow(card, Priority.ALWAYS);

        // Header with Policy info & Actions
        HBox detailHeader = new HBox(12);
        detailHeader.setAlignment(Pos.CENTER_LEFT);

        VBox infoBlock = new VBox(3);
        policyTitleLabel.getStyleClass().add("policy-detail-title");
        policyDescLabel.getStyleClass().add("policy-detail-description");
        infoBlock.getChildren().addAll(policyTitleLabel, policyDescLabel);
        HBox.setHgrow(infoBlock, Priority.ALWAYS);

        policyIdBadge.getStyleClass().add("caption-label");
        detailHeader.getChildren().addAll(infoBlock, policyIdBadge);

        // Version Selector & Action Row
        FlowPane versionControlBar = new FlowPane(12, 10);
        versionControlBar.setAlignment(Pos.CENTER_LEFT);
        versionControlBar.setMinWidth(0);
        versionControlBar.getStyleClass().add("policy-version-control-bar");

        Label versionPrompt = new Label("Policy Version:");
        versionPrompt.getStyleClass().add("policy-inline-label");

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
        versionComboBox.setMinWidth(Region.USE_PREF_SIZE);

        setActiveBtn.getStyleClass().add("btn-secondary");
        setActiveBtn.setMinWidth(Region.USE_PREF_SIZE);
        var checkIcon = Icons.create(Icons.CHECK, 0.65, (String) null);
        checkIcon.getStyleClass().add("policy-success-icon");
        setActiveBtn.setGraphic(checkIcon);
        setActiveBtn.setOnAction(e -> {
            PolicyVersion selected = versionComboBox.getValue();
            if (selected != null) {
                setActiveVersion(selected);
            }
        });

        newVersionBtn.getStyleClass().add("btn-primary");
        newVersionBtn.setMinWidth(Region.USE_PREF_SIZE);
        var versionIcon = Icons.create(Icons.PLUS, 0.7, (String) null);
        versionIcon.getStyleClass().add("policy-primary-icon");
        newVersionBtn.setGraphic(versionIcon);
        newVersionBtn.setOnAction(e -> {
            Policy selected = policyListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showNewVersionModal(selected);
            }
        });

        Button whatIfButton = new Button("What-if Simulator");
        whatIfButton.getStyleClass().add("btn-secondary");
        whatIfButton.setMinWidth(Region.USE_PREF_SIZE);
        whatIfButton.setOnAction(event -> {
            if (policyListView.getSelectionModel().getSelectedItem() == null) {
                AppContext.getInstance().showToast(Toast.error(
                        "Select a Policy", "Choose a policy before opening the simulator.", null));
                return;
            }
            openWhatIfSimulator.run();
        });

        versionControlBar.getChildren().addAll(
                versionPrompt, versionComboBox, setActiveBtn, whatIfButton, newVersionBtn);
        VBox historySection = createVersionHistory();

        // Conditions Section
        Label conditionsHeader = new Label("Active evaluation rules");
        conditionsHeader.getStyleClass().add("policy-section-title");

        ScrollPane scroll = new ScrollPane(conditionsContainer);
        scroll.getStyleClass().add("page-root");
        scroll.setMinWidth(0);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        card.getChildren().addAll(detailHeader, versionControlBar, historySection, conditionsHeader, scroll);
        return card;
    }

    private VBox createVersionHistory() {
        VBox section = new VBox(12);
        Label title = new Label("Version history");
        title.getStyleClass().add("policy-section-title");
        versionHistoryTable.getStyleClass().add("policy-version-table");
        versionHistoryTable.setPlaceholder(new Label("No versions available"));
        versionHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        versionHistoryTable.setPrefHeight(170);

        TableColumn<PolicyVersion, String> number = new TableColumn<>("Version");
        number.setCellValueFactory(
                cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getVersionNumber())));
        TableColumn<PolicyVersion, String> date = new TableColumn<>("Date");
        date.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCreatedAt() == null
                ? "Unknown"
                : cell.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        TableColumn<PolicyVersion, String> note = new TableColumn<>("Note");
        note.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getDescription() == null ? "" : cell.getValue().getDescription()));
        TableColumn<PolicyVersion, String> rules = new TableColumn<>("Rules");
        rules.setCellValueFactory(
                cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getConditions().size())));
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
                                ? StatusBadge.pass("ACTIVE")
                                : null);
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
                    "Policy '" + refreshed.getName() + "' version " + version.getVersionNumber() + " is now active.",
                    null));
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
        Label preview = new Label(ConditionRow.toPreviewSentence(conditions));
        preview.setWrapText(true);
        preview.getStyleClass().add("policy-rule-preview");
        conditionsContainer.getChildren().add(preview);

        FlowPane diffChips = new FlowPane(8, 8);
        List<Condition> previous = version.getVersionNumber() <= 1 || displayedPolicy == null
                ? List.of()
                : displayedPolicy.getVersion(version.getVersionNumber() - 1)
                        .map(PolicyVersion::getConditions)
                        .orElse(List.of());
        renderDiffChips(diffChips, previous, conditions, version.getVersionNumber());
        if (!diffChips.getChildren().isEmpty()) {
            conditionsContainer.getChildren().add(diffChips);
        }

        if (conditions.isEmpty()) {
            conditionsContainer.getChildren().add(new EmptyState(
                    Icons.POLICIES,
                    "No Conditions Defined",
                    "This policy version contains 0 criteria. All applicants vacuously pass.",
                    null,
                    null));
            return;
        }

        for (int i = 0; i < conditions.size(); i++) {
            Condition c = conditions.get(i);
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("policy-condition-row");

            Label num = new Label(String.valueOf(i + 1));
            num.getStyleClass().add("policy-condition-number");

            VBox ruleInfo = new VBox(2);
            Label name = new Label(c.getMessage());
            name.getStyleClass().add("policy-condition-name");
            name.setWrapText(true);

            Label type = new Label("Type: " + c.getType());
            type.getStyleClass().add("policy-condition-type");

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
        box.setPrefWidth(900);

        Label title = new Label("Define New Business Policy");
        title.getStyleClass().add("policy-modal-title");

        TextField nameInput = new TextField();
        nameInput.setPromptText("Policy Name (e.g. Merit Scholarship Eligibility)");

        TextField descInput = new TextField();
        descInput.setPromptText("Description / Business Intent");

        ComboBox<ProgramConfiguration> programChoice = new ComboBox<>();
        programChoice.setPromptText("Legacy policy (no program schema)");
        programChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProgramConfiguration program) {
                return program == null ? "" : program.getName();
            }

            @Override
            public ProgramConfiguration fromString(String value) {
                return null;
            }
        });
        AppContext context = AppContext.getInstance();
        PolicyRuleBuilder ruleBuilder = new PolicyRuleBuilder(null, List.of());
        context.getExecutor().execute(() -> {
            List<ProgramConfiguration> available = context.getProgramService().listActivePrograms();
            Platform.runLater(() -> {
                programChoice.getItems().setAll(available);
                ProgramConfiguration selected = context.selectedProgramProperty().get();
                if (selected != null) {
                    available.stream().filter(program -> program.getId().equals(selected.getId()))
                            .findFirst().ifPresent(programChoice::setValue);
                }
                if (programChoice.getValue() == null)
                    ruleBuilder.setProgram(null);
            });
        });

        Button saveBtn = new Button("Create Policy");
        saveBtn.getStyleClass().add("btn-primary");
        ruleBuilder.setValidityListener(valid -> saveBtn.setDisable(
                !valid || nameInput.getText() == null || nameInput.getText().trim().isEmpty()));
        nameInput.textProperty().addListener((obs, oldVal, newVal) -> ruleBuilder.refresh());
        programChoice.valueProperty().addListener((obs, oldValue, newValue) -> ruleBuilder.setProgram(newValue));
        ruleBuilder.setImpactPreviewBase(new PolicyVersion("draft-preview", "draft-preview", 1,
                "Unsaved draft", List.of()));

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> rootPane.getChildren().remove(modalBackdrop));

        saveBtn.setOnAction(e -> {
            String name = nameInput.getText().trim();
            if (name.isEmpty()) {
                Motion.shake(nameInput);
                return;
            }

            if (!ruleBuilder.isValid()) {
                return;
            }
            List<Condition> conditions = ruleBuilder.getConditions();

            String policyId = "POL-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            String description = descInput.getText().trim();
            String programId = programChoice.getValue() == null ? null : programChoice.getValue().getId();
            AppContext.getInstance().getExecutor().execute(() -> {
                Policy policy = AppContext.getInstance().getPolicyService().createPolicy(
                        policyId,
                        name,
                        description,
                        programId,
                        conditions);
                Platform.runLater(() -> {
                    rootPane.getChildren().remove(modalBackdrop);
                    AppContext.getInstance().showToast(Toast.success(
                            "Policy Created",
                            "Policy '" + policy.getName() + "' created with " + conditions.size() + " rules.",
                            null));
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
                new VBox(4, new Label("Program schema"), programChoice),
                new Label("Initial conditions"),
                ruleBuilder,
                btnRow);

        modalBackdrop.getChildren().add(box);
        rootPane.getChildren().add(modalBackdrop);
        Motion.modalScaleIn(box);
    }

    private void showNewVersionModal(Policy policy) {
        PolicyVersion latest = policy.getLatestVersion().orElse(null);
        showNewVersionModal(
                policy,
                latest == null ? List.of() : latest.getConditions(),
                latest == null ? 0 : latest.getVersionNumber(),
                "Tightened criteria for version " + (policy.getVersions().size() + 1));
    }

    public void showNewVersionDraft(Policy policy, PolicyVersion simulatedVersion) {
        if (policy == null || simulatedVersion == null
                || !policy.getId().equals(simulatedVersion.getPolicyId())) {
            throw new IllegalArgumentException("A matching policy and simulated version are required.");
        }
        Policy availablePolicy = AppContext.getInstance().getPolicies().stream()
                .filter(candidate -> candidate.getId().equals(policy.getId()))
                .findFirst()
                .orElse(policy);
        policyListView.getSelectionModel().select(availablePolicy);
        AppContext.getInstance().selectedPolicyProperty().set(availablePolicy);
        displayPolicy(availablePolicy);
        showNewVersionModal(
                availablePolicy,
                simulatedVersion.getConditions(),
                simulatedVersion.getVersionNumber(),
                "What-if draft based on version " + simulatedVersion.getVersionNumber());
    }

    private void showNewVersionModal(Policy policy, List<Condition> initialConditions,
            int sourceVersionNumber, String initialNote) {
        StackPane modalBackdrop = new StackPane();
        modalBackdrop.getStyleClass().add("modal-backdrop");

        VBox box = new VBox(16);
        box.getStyleClass().add("modal-window");
        box.setPrefWidth(900);

        int nextVerNum = policy.getVersions().size() + 1;
        Label title = new Label("Create Version " + nextVerNum + " for '" + policy.getName() + "'");
        title.getStyleClass().add("policy-modal-title");

        TextField descInput = new TextField(initialNote);
        descInput.setPromptText("Version change rationale / notes");

        PolicyRuleBuilder ruleBuilder = new PolicyRuleBuilder(null, initialConditions);
        List<Condition> previousRules = policy.getLatestVersion()
                .map(PolicyVersion::getConditions)
                .orElse(List.of());
        FlowPane diffChips = new FlowPane(8, 8);
        Button saveBtn = new Button("Save Version " + nextVerNum);
        saveBtn.getStyleClass().add("btn-primary");
        ruleBuilder.setValidityListener(valid -> saveBtn.setDisable(!valid));
        ruleBuilder.setValidationListener(
                errors -> refreshVersionDiff(diffChips, previousRules, ruleBuilder, nextVerNum, errors));
        refreshVersionDiff(diffChips, previousRules, ruleBuilder, nextVerNum, ruleBuilder.getValidationErrors());
        PolicyVersion impactBase = policy.getVersion(sourceVersionNumber)
                .orElseGet(() -> policy.getLatestVersion()
                        .orElseGet(() -> new PolicyVersion(policy.getId() + "_preview", policy.getId(), 1,
                                "Draft impact baseline", List.of())));
        ruleBuilder.setImpactPreviewBase(impactBase);
        AppContext context = AppContext.getInstance();
        if (policy.getProgramId() != null) {
            context.getExecutor().execute(() -> {
                ProgramConfiguration program = context.getProgramService().findProgram(policy.getProgramId());
                Platform.runLater(() -> ruleBuilder.setProgram(program));
            });
        }

        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> rootPane.getChildren().remove(modalBackdrop));

        saveBtn.setOnAction(e -> {
            if (!ruleBuilder.isValid()) {
                return;
            }
            List<Condition> conditions = ruleBuilder.getConditions();

            AppContext.getInstance().getExecutor().execute(() -> {
                PolicyVersion newVersion = AppContext.getInstance().getPolicyService().createNewVersion(
                        policy.getId(),
                        descInput.getText().trim(),
                        conditions);
                Platform.runLater(() -> {
                    rootPane.getChildren().remove(modalBackdrop);
                    AppContext.getInstance().showToast(Toast.success(
                            "New Version Created",
                            "Created Version " + newVersion.getVersionNumber() + " with " + conditions.size()
                                    + " rules.",
                            null));
                    displayPolicy(policy);
                    versionComboBox.setValue(newVersion);
                });
            });
        });

        btnRow.getChildren().addAll(cancelBtn, saveBtn);

        box.getChildren().addAll(
                title,
                new VBox(4, new Label("Version Rationale / Notes"), descInput),
                new Label("Version criteria (drafted from v" + sourceVersionNumber + ")"),
                ruleBuilder,
                diffChips,
                btnRow);

        modalBackdrop.getChildren().add(box);
        rootPane.getChildren().add(modalBackdrop);
        Motion.modalScaleIn(box);
    }

    private void renderDiffChips(FlowPane chips, List<Condition> previousRules, List<Condition> currentRules,
            int currentVersionNumber) {
        chips.getChildren().clear();
        if (previousRules == null) {
            chips.setVisible(false);
            chips.setManaged(false);
            return;
        }
        chips.setVisible(true);
        chips.setManaged(true);
        if (currentVersionNumber <= 1) {
            chips.getChildren().add(StatusBadge.info("Initial version"));
            return;
        }

        Map<String, String> previous = clausesBySignature(previousRules);
        Map<String, String> current = clausesBySignature(currentRules);
        for (Map.Entry<String, String> entry : current.entrySet()) {
            if (!previous.containsKey(entry.getKey())) {
                chips.getChildren().add(StatusBadge.pass("+ " + entry.getValue()));
            }
        }
        for (Map.Entry<String, String> entry : previous.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                chips.getChildren().add(StatusBadge.fail("− " + entry.getValue()));
            }
        }
        if (chips.getChildren().isEmpty()) {
            String vs = currentVersionNumber > 1 ? " vs v" + (currentVersionNumber - 1) : " vs previous version";
            chips.getChildren().add(StatusBadge.info("No rule changes" + vs));
        }
    }

    private Map<String, String> clausesBySignature(List<Condition> conditions) {
        Map<String, String> clauses = new LinkedHashMap<>();
        if (conditions == null) {
            return clauses;
        }
        for (Condition condition : conditions) {
            String signature = ConditionRow.signatureOf(condition);
            if (signature != null) {
                clauses.putIfAbsent(signature, ConditionRow.toPreviewClause(condition));
            }
        }
        return clauses;
    }

    private void refreshVersionDiff(FlowPane chips, List<Condition> previousRules,
            PolicyRuleBuilder ruleBuilder, int versionNumber,
            List<String> validationErrors) {
        chips.getChildren().clear();
        if (versionNumber <= 1) {
            chips.getChildren().add(StatusBadge.info("Initial version"));
            return;
        }
        if (validationErrors != null && !validationErrors.isEmpty()) {
            chips.getChildren().add(StatusBadge.warning("Complete valid rules to view the change summary."));
            return;
        }
        List<Condition> currentRules = ruleBuilder.getConditions();
        Map<String, String> previous = clausesBySignature(previousRules);
        Map<String, String> current = clausesBySignature(currentRules);
        for (Map.Entry<String, String> entry : current.entrySet()) {
            if (!previous.containsKey(entry.getKey())) {
                chips.getChildren().add(StatusBadge.pass("+ " + entry.getValue()));
            }
        }
        for (Map.Entry<String, String> entry : previous.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                chips.getChildren().add(StatusBadge.fail("− " + entry.getValue()));
            }
        }
        if (chips.getChildren().isEmpty()) {
            chips.getChildren().add(StatusBadge.info("No rule changes vs previous version"));
        }
    }

    @Override
    public Node getRoot() {
        return rootPane;
    }

    @Override
    public void onShow() {
        AppContext ctx = AppContext.getInstance();
        ctx.loadPoliciesAsync(() -> {
            Policy selected = ctx.selectedPolicyProperty().get();
            if (selected != null) {
                Policy matching = ctx.getPolicies().stream()
                        .filter(policy -> java.util.Objects.equals(policy.getId(), selected.getId()))
                        .findFirst().orElse(null);
                if (matching != null) {
                    policyListView.getSelectionModel().select(matching);
                }
            }
        });
    }
}
