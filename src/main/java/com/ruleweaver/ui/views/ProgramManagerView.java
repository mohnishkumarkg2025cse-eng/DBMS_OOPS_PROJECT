package com.ruleweaver.ui.views;

import com.ruleweaver.model.FieldDefinition;
import com.ruleweaver.model.FieldOperator;
import com.ruleweaver.model.FieldType;
import com.ruleweaver.model.FieldValidationRules;
import com.ruleweaver.model.ProgramConfiguration;
import com.ruleweaver.model.ProgramTemplate;
import com.ruleweaver.service.ProgramService;
import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.components.Icons;
import com.ruleweaver.ui.components.SearchBar;
import com.ruleweaver.ui.components.Toast;
import com.ruleweaver.ui.shell.Sidebar;
import com.ruleweaver.ui.util.Theme;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class ProgramManagerView implements BaseView {
    private final StackPane root = new StackPane();
    private final ListView<ProgramConfiguration> programs = new ListView<>();
    private final ListView<FieldDefinition> fields = new ListView<>();
    private final Label selectedLabel = new Label("Select a program to inspect its applicant schema.");
    private final Button selectButton = new Button("Select Program");
    private final ObservableList<ProgramConfiguration> allPrograms = FXCollections.observableArrayList();
    private final FilteredList<ProgramConfiguration> filteredPrograms = new FilteredList<>(allPrograms);
    private final SortedList<ProgramConfiguration> sortedPrograms = new SortedList<>(filteredPrograms);
    private final SearchBar search = new SearchBar("Search programs...");
    private final ComboBox<ProgramListCriteria.Status> statusFilter = new ComboBox<>();
    private final ComboBox<String> templateFilter = new ComboBox<>();
    private final ComboBox<String> sortChoice = new ComboBox<>();
    private final HBox filterChips = new HBox(8);
    private final Label resultCount = new Label("Loading programs...");
    private final Label programCount = new Label("Loading…");
    private final Label applicantCount = new Label("Loading…");
    private final Label activeCount = new Label("Loading…");
    private final Label programTabCount = new Label("—");
    private final Label applicantTabCount = new Label("—");
    private final Button programsTab = new Button("Programs");
    private final Button applicantsTab = new Button("Applicants");
    private final Consumer<Sidebar.Destination> navigate;
    private ProgramTemplate createTemplate = ProgramTemplate.CUSTOM;

    public ProgramManagerView() {
        this(null);
    }

    public ProgramManagerView(Consumer<Sidebar.Destination> navigate) {
        this.navigate = navigate;
        buildUi();
    }

    private void buildUi() {
        root.getStyleClass().add("page-root");
        VBox page = new VBox(14);
        page.getStyleClass().add("program-manager-page");
        page.setPadding(new Insets(20, 24, 20, 24));
        Label title = new Label("Programs & Applicants");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("A clear view of configured programs and the people associated with them");
        subtitle.getStyleClass().add("page-subtitle");

        ComboBox<ProgramTemplate> templates = new ComboBox<>();
        templates.getItems().setAll(ProgramTemplate.values());
        templates.setValue(createTemplate);
        templates.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ProgramTemplate template) {
                return template == null ? "" : template.getDisplayName();
            }
            @Override public ProgramTemplate fromString(String value) { return null; }
        });
        templates.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) createTemplate = newValue;
        });
        Button create = new Button("Create Program");
        create.getStyleClass().add("btn-primary");
        create.setOnAction(event -> createProgramFromTemplate());
        Button custom = new Button("Custom");
        custom.getStyleClass().add("btn-secondary");
        custom.setTooltip(new Tooltip("Create a custom program schema"));
        custom.setOnAction(event -> createCustomProgram());
        HBox headerActions = new HBox(10, templates, create, custom);
        headerActions.setAlignment(Pos.CENTER_RIGHT);

        VBox heading = new VBox(7, title, subtitle);
        Label context = new Label("WORKSPACE  /  PROGRAMS");
        context.getStyleClass().add("program-context-chip");
        heading.getChildren().add(context);
        HBox header = new HBox(20, heading, headerActions);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(heading, Priority.ALWAYS);
        header.setMinHeight(82);

        HBox kpis = new HBox(16,
                createKpi("TOTAL PROGRAMS", programCount, "Configured program records; built-in templates are excluded.",
                        "program-kpi-blue", () -> clearFilters()),
                createKpi("TOTAL APPLICANTS", applicantCount,
                        "Applicant records, whether or not they have a stored evaluation.",
                        "program-kpi-teal", this::openApplicants),
                createKpi("ACTIVE PROGRAMS", activeCount,
                        "Configured programs not marked archived; built-in templates are excluded.",
                        "program-kpi-amber", this::showActivePrograms));
        kpis.getStyleClass().add("program-kpi-strip");
        for (javafx.scene.Node card : kpis.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }

        HBox tabs = createTabs();
        VBox toolbar = createToolbar();

        programs.setFixedCellSize(48);
        programs.setPlaceholder(new Label("No programs match the current filters."));
        programs.setMinHeight(180);
        programs.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ProgramConfiguration item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                Label name = new Label(item.getName());
                name.getStyleClass().add("program-list-name");
                name.setMaxWidth(Double.MAX_VALUE);
                name.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
                Label metadata = new Label(item.getTemplate().getDisplayName()
                        + " · " + item.getFields().size() + " fields");
                metadata.getStyleClass().add("program-list-metadata");
                Label status = new Label(item.isArchived() ? "Archived"
                        : item.getId().startsWith("template-") ? "Template" : "Active");
                status.getStyleClass().addAll("program-status-pill",
                        item.isArchived() ? "program-status-archived"
                                : item.getId().startsWith("template-") ? "program-status-template"
                                : "program-status-active");
                VBox details = new VBox(2, name, metadata);
                HBox.setHgrow(details, Priority.ALWAYS);
                HBox row = new HBox(10, details, status);
                row.setAlignment(Pos.CENTER_LEFT);
                setText(null);
                setGraphic(row);
                Tooltip tooltip = new Tooltip(item.getName() + " · " + item.getTemplate().getDisplayName()
                        + " · " + item.getFields().size() + " fields");
                Theme.apply(tooltip);
                setTooltip(tooltip);
            }
        });
        programs.getSelectionModel().selectedItemProperty().addListener((observable, old, selected) ->
                renderFields(selected));
        programs.setItems(sortedPrograms);
        sortedPrograms.setComparator(ProgramListCriteria.byName(false));

        Button rename = new Button("Rename");
        rename.setOnAction(event -> {
            ProgramConfiguration current = selected();
            if (current == null) return;
            TextInputDialog dialog = new TextInputDialog(current.getName());
            dialog.setTitle("Rename Program");
            dialog.setContentText("Program name:");
            dialog.showAndWait().ifPresent(name -> run(
                    service -> service.rename(current.getId(), name), updated -> refresh(updated.getId())));
        });
        Button duplicate = new Button("Duplicate");
        duplicate.setOnAction(event -> {
            ProgramConfiguration current = selected();
            if (current == null) return;
            TextInputDialog dialog = new TextInputDialog(current.getName() + " Copy");
            dialog.setTitle("Duplicate Program");
            dialog.setContentText("Copy name:");
            dialog.showAndWait().ifPresent(name -> run(
                    service -> service.duplicate(current.getId(), name), copy -> refresh(copy.getId())));
        });
        Button archive = new Button("Archive");
        archive.setOnAction(event -> {
            ProgramConfiguration current = selected();
            if (current == null) return;
            run(service -> service.archive(current.getId()), archived -> {
                AppContext.getInstance().selectedProgramProperty().set(null);
                refresh(null);
                loadKpis();
            });
        });
        selectButton.getStyleClass().add("btn-primary");
        selectButton.setOnAction(event -> {
            ProgramConfiguration current = selected();
            if (current == null) return;
            run(service -> service.selectProgram(current.getId()), chosen -> {
                AppContext.getInstance().selectedProgramProperty().set(chosen);
                AppContext.getInstance().showToast(Toast.success(
                        "Program Selected", chosen.getName() + " is selected for new program-linked policies.", null));
            });
        });

        Button addField = new Button("Add Field");
        addField.setOnAction(event -> addField());
        Button editField = new Button("Edit Field");
        editField.setOnAction(event -> editField());
        programs.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            boolean editable = selected != null && !selected.isArchived()
                    && !selected.getId().startsWith("template-");
            rename.setDisable(!editable);
            duplicate.setDisable(!editable);
            archive.setDisable(!editable);
            selectButton.setDisable(selected == null || selected.isArchived()
                    || selected.getId().startsWith("template-"));
            addField.setDisable(!editable);
            editField.setDisable(!editable);
        });
        rename.setDisable(true);
        duplicate.setDisable(true);
        archive.setDisable(true);
        addField.setDisable(true);
        editField.setDisable(true);
        HBox actions = new HBox(8, rename, duplicate, archive, selectButton, addField, editField);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(8, new Label("PROGRAMS"), buildProgramListPane());
        left.getStyleClass().addAll("card", "program-list-card");
        left.setPrefWidth(390);
        VBox.setVgrow(programs, Priority.ALWAYS);
        VBox detail = new VBox(14, selectedLabel, actions, new Label("CONFIGURED FIELDS"), fields);
        detail.getStyleClass().addAll("card", "program-details-card");
        HBox.setHgrow(detail, Priority.ALWAYS);
        fields.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(FieldDefinition item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel() + "  (" + item.getKey() + " · "
                        + item.getType() + (item.isRequired() ? " · required" : "") + ")");
            }
        });
        HBox content = new HBox(16, left, detail);
        VBox.setVgrow(content, Priority.ALWAYS);
        page.getChildren().addAll(header, kpis, tabs, toolbar, content);
        root.getChildren().setAll(page);
        search.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        templateFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        sortChoice.valueProperty().addListener((observable, oldValue, newValue) -> {
            sortedPrograms.setComparator(ProgramListCriteria.byName("Name Z–A".equals(newValue)));
        });
        filteredPrograms.addListener((javafx.collections.ListChangeListener<ProgramConfiguration>) change ->
                updateResultCount());
        loadKpis();
        refresh(null);
    }

    private void createProgramFromTemplate() {
        ProgramTemplate template = createTemplate;
        TextInputDialog dialog = new TextInputDialog(template.getDisplayName() + " Program");
        dialog.setTitle("Create Program");
        dialog.setHeaderText("Create an editable program from " + template.getDisplayName());
        dialog.setContentText("Program name:");
        dialog.showAndWait().ifPresent(name -> run(
                service -> service.createFromTemplate(template, name),
                created -> {
                    refresh(created.getId());
                    loadKpis();
                    AppContext.getInstance().showToast(Toast.success(
                            "Program Created", "Created " + created.getName() + ".", null));
                }));
    }

    private void createCustomProgram() {
        TextInputDialog dialog = new TextInputDialog("Custom Program");
        dialog.setTitle("Create Custom Program");
        dialog.setHeaderText("Start with name and email fields");
        dialog.setContentText("Program name:");
        dialog.showAndWait().ifPresent(name -> run(service -> service.createProgram(
                "custom-" + UUID.randomUUID(), name, ProgramTemplate.CUSTOM,
                ProgramTemplate.CUSTOM.defaultFields()), created -> {
            refresh(created.getId());
            loadKpis();
        }));
    }

    private VBox createKpi(String title, Label value, String definition, String accent, Runnable action) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("program-kpi-card", accent);
        card.setAccessibleRole(AccessibleRole.BUTTON);
        card.setFocusTraversable(true);
        Label heading = new Label(title);
        heading.getStyleClass().add("program-kpi-label");
        value.getStyleClass().add("program-kpi-value");
        value.setMaxWidth(Double.MAX_VALUE);
        value.setAlignment(Pos.CENTER_RIGHT);
        String iconName = "TOTAL APPLICANTS".equals(title) ? Icons.APPLICANTS : Icons.POLICIES;
        javafx.scene.Node icon = Icons.create(iconName, 0.8, (String) null);
        HBox iconBox = new HBox(icon);
        iconBox.setAlignment(Pos.CENTER);
        iconBox.getStyleClass().addAll("program-kpi-icon", accent);
        VBox labels = new VBox(6, heading, value);
        HBox top = new HBox(12, iconBox, labels);
        top.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(labels, Priority.ALWAYS);
        Label context = new Label(definition);
        context.getStyleClass().add("program-kpi-context");
        context.setWrapText(true);
        Region line = new Region();
        line.getStyleClass().add("program-kpi-accent-line");
        card.getChildren().addAll(line, top, context);
        Tooltip tooltip = new Tooltip(definition);
        Theme.apply(tooltip);
        Tooltip.install(card, tooltip);
        card.setOnMouseClicked(event -> action.run());
        card.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                action.run();
                event.consume();
            }
        });
        return card;
    }

    private HBox createTabs() {
        HBox track = new HBox(4);
        track.getStyleClass().add("program-tabs");
        programsTab.getStyleClass().add("program-tab-active");
        applicantsTab.getStyleClass().add("program-tab");
        programTabCount.getStyleClass().add("program-tab-count");
        applicantTabCount.getStyleClass().add("program-tab-count");
        programsTab.setGraphic(programTabCount);
        applicantsTab.setGraphic(applicantTabCount);
        programsTab.setOnAction(event -> selectProgramsTab());
        applicantsTab.setOnAction(event -> openApplicants());
        track.getChildren().addAll(programsTab, applicantsTab);
        return track;
    }

    private VBox createToolbar() {
        search.setMinWidth(220);
        search.setPrefWidth(300);
        search.setMaxWidth(380);
        statusFilter.getItems().setAll(ProgramListCriteria.Status.values());
        statusFilter.setValue(ProgramListCriteria.Status.ALL);
        statusFilter.setPrefWidth(150);
        templateFilter.getItems().setAll("All types");
        for (ProgramTemplate template : ProgramTemplate.values()) {
            templateFilter.getItems().add(template.getDisplayName());
        }
        templateFilter.setValue("All types");
        templateFilter.setPrefWidth(160);
        sortChoice.getItems().setAll("Name A–Z", "Name Z–A");
        sortChoice.setValue("Name A–Z");
        sortChoice.setPrefWidth(125);
        resultCount.getStyleClass().add("program-result-count");
        Button clear = new Button("Clear filters");
        clear.getStyleClass().add("link-action");
        clear.setOnAction(event -> clearFilters());
        FlowPane controls = new FlowPane(10, 8, search, statusFilter, templateFilter, sortChoice, resultCount, clear);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setMaxWidth(Double.MAX_VALUE);
        filterChips.getStyleClass().add("program-filter-chips");
        filterChips.setAlignment(Pos.CENTER_LEFT);
        VBox toolbar = new VBox(8, controls, filterChips);
        toolbar.getStyleClass().add("program-toolbar");
        return toolbar;
    }

    private void applyFilters() {
        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase(java.util.Locale.ROOT);
        ProgramListCriteria.Status status = statusFilter.getValue() == null
                ? ProgramListCriteria.Status.ALL : statusFilter.getValue();
        String type = templateFilter.getValue();
        filteredPrograms.setPredicate(program ->
                ProgramListCriteria.matches(program, query, status, type));
        renderFilterChips(query, status, type);
        updateResultCount();
    }

    private void renderFilterChips(String query, ProgramListCriteria.Status status, String type) {
        filterChips.getChildren().clear();
        if (!query.isEmpty()) addFilterChip("Search: " + query, search::clear);
        if (status != ProgramListCriteria.Status.ALL) {
            addFilterChip(status.label(), () -> statusFilter.setValue(ProgramListCriteria.Status.ALL));
        }
        if (type != null && !"All types".equals(type)) {
            addFilterChip(type, () -> templateFilter.setValue("All types"));
        }
    }

    private ListView<ProgramConfiguration> buildProgramListPane() {
        return programs;
    }

    private void addFilterChip(String text, Runnable clear) {
        Button chip = new Button(text + "  ×");
        chip.getStyleClass().add("program-filter-chip");
        chip.setOnAction(event -> clear.run());
        filterChips.getChildren().add(chip);
    }

    private void updateResultCount() {
        resultCount.setText(filteredPrograms.size() + " program records");
        if (filteredPrograms.isEmpty()) {
            programListPlaceholder(allPrograms.isEmpty());
        }
    }

    private void programListPlaceholder(boolean noPrograms) {
        programs.setPlaceholder(new Label(noPrograms
                ? "No programs are available yet."
                : "No programs match the current search and filters."));
    }

    private void clearFilters() {
        search.clear();
        statusFilter.setValue(ProgramListCriteria.Status.ALL);
        templateFilter.setValue("All types");
    }

    private void setStatusFilter(ProgramListCriteria.Status status) {
        statusFilter.setValue(status);
    }

    private void showActivePrograms() {
        clearFilters();
        setStatusFilter(ProgramListCriteria.Status.ACTIVE);
    }

    private void openApplicants() {
        if (navigate != null) navigate.accept(Sidebar.Destination.APPLICANTS);
    }

    private void selectProgramsTab() {
        programsTab.getStyleClass().remove("program-tab");
        if (!programsTab.getStyleClass().contains("program-tab-active")) {
            programsTab.getStyleClass().add("program-tab-active");
        }
        applicantsTab.getStyleClass().remove("program-tab-active");
        if (!applicantsTab.getStyleClass().contains("program-tab")) {
            applicantsTab.getStyleClass().add("program-tab");
        }
    }

    private void loadKpis() {
        AppContext context = AppContext.getInstance();
        boolean databaseOnline = context.mongoOnlineProperty().get();
        Task<KpiCounts> task = new Task<>() {
            @Override protected KpiCounts call() {
                if (!databaseOnline) {
                    return new KpiCounts(null, null, null);
                }
                long programsTotal = context.getProgramService().countPrograms();
                long activeTotal = context.getProgramService().countActivePrograms();
                Long applicantsTotal = context.getApplicantRepository() == null
                        ? null : context.getApplicantRepository().count();
                return new KpiCounts(programsTotal, applicantsTotal, activeTotal);
            }
        };
        task.setOnSucceeded(event -> {
            KpiCounts counts = task.getValue();
            showCount(programCount, counts.programs());
            showCount(applicantCount, counts.applicants());
            showCount(activeCount, counts.activePrograms());
            programTabCount.setText(counts.programs() == null ? "—" : Long.toString(counts.programs()));
            applicantTabCount.setText(counts.applicants() == null ? "—"
                    : Long.toString(counts.applicants()));
        });
        task.setOnFailed(event -> {
            Throwable failure = task.getException();
            if (failure != null) {
                AppContext.logFailure("Could not load program and applicant KPIs", failure);
                programCount.setText("Unavailable");
                applicantCount.setText("Unavailable");
                activeCount.setText("Unavailable");
                programTabCount.setText("—");
                applicantTabCount.setText("—");
                context.showToast(Toast.error("Summary Unavailable",
                        failure.getMessage() == null ? "Could not load summary counts."
                                : com.ruleweaver.config.MongoConfig.sanitize(failure.getMessage()), null));
            }
        });
        context.getExecutor().execute(task);
    }

    private static void showCount(Label label, Long count) {
        if (count == null) {
            label.setText("Unavailable");
            return;
        }
        AppContext context = AppContext.getInstance();
        if (context.reducedMotionProperty().get() || count <= 0) {
            label.setText(Long.toString(count));
            return;
        }
        DoubleProperty animated = new SimpleDoubleProperty(0);
        animated.addListener((observable, oldValue, newValue) ->
                label.setText(Long.toString(Math.round(newValue.doubleValue()))));
        Timeline countUp = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(animated, 0)),
                new KeyFrame(Duration.millis(600),
                        new KeyValue(animated, count.doubleValue(), Interpolator.EASE_OUT)));
        countUp.play();
    }

    private record KpiCounts(Long programs, Long applicants, Long activePrograms) {
    }

    private void addField() {
        ProgramConfiguration current = selected();
        if (current == null || current.isArchived()) return;
        TextInputDialog keyDialog = new TextInputDialog();
        keyDialog.setTitle("Add Applicant Field");
        keyDialog.setHeaderText("Define a dynamic field key");
        keyDialog.setContentText("Field key (letters, numbers, underscores):");
        keyDialog.showAndWait().ifPresent(key -> {
            TextInputDialog labelDialog = new TextInputDialog(key);
            labelDialog.setTitle("Add Applicant Field");
            labelDialog.setContentText("Display label:");
            labelDialog.showAndWait().ifPresent(label -> showFieldType(current, key.trim(), label.trim()));
        });
    }

    private void showFieldType(ProgramConfiguration current, String key, String label) {
        showFieldEditor(current, new FieldDefinition(key, label, FieldType.TEXT, false,
                FieldValidationRules.none(), List.of(), null), false);
    }

    private void editField() {
        ProgramConfiguration current = selected();
        FieldDefinition field = fields.getSelectionModel().getSelectedItem();
        if (current != null && !current.isArchived() && field != null) {
            showFieldEditor(current, field, true);
        }
    }

    private void showFieldEditor(ProgramConfiguration current, FieldDefinition field, boolean editing) {
        ComboBox<FieldType> typeChoice = new ComboBox<>();
        typeChoice.getItems().setAll(FieldType.values());
        typeChoice.setValue(field.getType());
        typeChoice.setDisable(editing);
        CheckBox required = new CheckBox("Required");
        required.setSelected(field.isRequired());
        TextField labelInput = new TextField(field.getLabel());
        TextField permitted = new TextField();
        permitted.setPromptText("Permitted values, comma-separated (optional)");
        permitted.setText(String.join(", ", field.getPermittedValues()));
        TextField minimum = new TextField();
        minimum.setPromptText("Minimum number (optional)");
        TextField maximum = new TextField();
        maximum.setPromptText("Maximum number (optional)");
        TextField minimumLength = new TextField();
        minimumLength.setPromptText("Minimum text length (optional)");
        TextField maximumLength = new TextField();
        maximumLength.setPromptText("Maximum text length (optional)");
        TextField pattern = new TextField();
        pattern.setPromptText("Text pattern (regular expression, optional)");
        FieldValidationRules rules = field.getValidationRules();
        minimum.setText(valueOrEmpty(rules.minimum()));
        maximum.setText(valueOrEmpty(rules.maximum()));
        minimumLength.setText(valueOrEmpty(rules.minimumLength()));
        maximumLength.setText(valueOrEmpty(rules.maximumLength()));
        pattern.setText(valueOrEmpty(rules.pattern()));
        VBox operatorChoices = new VBox(4);
        List<CheckBox> operatorOptions = new ArrayList<>();
        Runnable refreshOperators = () -> {
            operatorOptions.clear();
            operatorChoices.getChildren().clear();
            for (FieldOperator operator : operatorsFor(typeChoice.getValue())) {
                CheckBox choice = new CheckBox(operator.getSymbol());
                choice.setSelected(editing
                        ? field.getSupportedOperators().contains(operator) : true);
                operatorOptions.add(choice);
                operatorChoices.getChildren().add(choice);
            }
        };
        typeChoice.valueProperty().addListener((observable, oldType, newType) -> refreshOperators.run());
        refreshOperators.run();
        javafx.scene.control.Dialog<FieldDefinition> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle(editing ? "Edit Field" : "Configure Field");
        dialog.getDialogPane().getButtonTypes().addAll(
                javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        dialog.getDialogPane().setContent(new VBox(10,
                new Label("Field key: " + field.getKey()), labelInput, typeChoice, required, permitted,
                minimum, maximum, minimumLength, maximumLength, pattern,
                new Label("Supported rule operators"), operatorChoices));
        dialog.setResultConverter(button -> {
            if (button != javafx.scene.control.ButtonType.OK) return null;
            FieldType type = typeChoice.getValue();
            List<String> permittedValues = Arrays.stream(permitted.getText().split(","))
                    .map(String::trim).filter(value -> !value.isEmpty()).toList();
            if (type == FieldType.NUMBER || type == FieldType.BOOLEAN) permittedValues = List.of();
            FieldValidationRules configuredRules = new FieldValidationRules(
                    type == FieldType.NUMBER ? parseDouble(minimum.getText()) : null,
                    type == FieldType.NUMBER ? parseDouble(maximum.getText()) : null,
                    type == FieldType.TEXT ? parseInteger(minimumLength.getText()) : null,
                    type == FieldType.TEXT ? parseInteger(maximumLength.getText()) : null,
                    type == FieldType.TEXT && !pattern.getText().isBlank() ? pattern.getText().trim() : null);
            EnumSet<FieldOperator> supportedOperators = EnumSet.noneOf(FieldOperator.class);
            for (int index = 0; index < operatorOptions.size(); index++) {
                if (operatorOptions.get(index).isSelected()) {
                    supportedOperators.add(operatorsFor(type).get(index));
                }
            }
            return new FieldDefinition(field.getKey(), labelInput.getText(), type, required.isSelected(),
                    configuredRules, permittedValues, supportedOperators);
        });
        dialog.showAndWait().ifPresent(definition -> run(service -> {
            List<FieldDefinition> updated = new ArrayList<>(current.getFields());
            if (editing) {
                int index = updated.indexOf(field);
                updated.set(index, definition);
            } else {
                updated.add(definition);
            }
            return service.editFields(current.getId(), updated);
        }, saved -> refresh(saved.getId())));
    }

    private void renderFields(ProgramConfiguration program) {
        fields.getItems().clear();
        if (program == null) {
            selectedLabel.setText("Select a program to inspect its applicant schema.");
            selectButton.setDisable(true);
            return;
        }
        selectedLabel.setText(program.getName() + " · " + program.getTemplate().getDisplayName());
        selectButton.setDisable(program.isArchived());
        fields.getItems().setAll(program.getFields());
    }

    private ProgramConfiguration selected() {
        return programs.getSelectionModel().getSelectedItem();
    }

    private void refresh(String selectId) {
        run(service -> service.listPrograms(), result -> {
            allPrograms.setAll(result);
            applyFilters();
            if (selectId != null) {
                result.stream().filter(program -> program.getId().equals(selectId)).findFirst()
                        .ifPresent(programs.getSelectionModel()::select);
            }
        });
    }

    private <T> void run(java.util.function.Function<ProgramService, T> operation, Consumer<T> success) {
        AppContext context = AppContext.getInstance();
        Task<T> task = new Task<>() {
            @Override protected T call() { return operation.apply(context.getProgramService()); }
        };
        task.setOnSucceeded(event -> success.accept(task.getValue()));
        task.setOnFailed(event -> {
            Throwable failure = task.getException();
            if (failure != null) {
                AppContext.logFailure("Program operation failed", failure);
                context.showToast(Toast.error("Program Operation Failed",
                        failure.getMessage() == null ? "Program operation failed."
                                : com.ruleweaver.config.MongoConfig.sanitize(failure.getMessage()), null));
            }
        });
        context.getExecutor().execute(task);
    }

    private static String valueOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private static Double parseDouble(String value) {
        return value.isBlank() ? null : Double.valueOf(value.trim());
    }

    private static Integer parseInteger(String value) {
        return value.isBlank() ? null : Integer.valueOf(value.trim());
    }

    private static List<FieldOperator> operatorsFor(FieldType type) {
        return switch (type) {
            case NUMBER -> List.of(FieldOperator.EQUALS, FieldOperator.NOT_EQUALS,
                    FieldOperator.GREATER_THAN, FieldOperator.GREATER_THAN_OR_EQUAL,
                    FieldOperator.LESS_THAN, FieldOperator.LESS_THAN_OR_EQUAL);
            case TEXT, BOOLEAN -> List.of(FieldOperator.EQUALS, FieldOperator.NOT_EQUALS);
            case MULTI_SELECT -> List.of(FieldOperator.CONTAINS, FieldOperator.NOT_CONTAINS,
                    FieldOperator.CONTAINS_ANY, FieldOperator.CONTAINS_ALL);
        };
    }

    @Override public StackPane getRoot() { return root; }

    @Override
    public void onShow() {
        loadKpis();
        refresh(AppContext.getInstance().selectedProgramProperty().get() == null
                ? null : AppContext.getInstance().selectedProgramProperty().get().getId());
    }
}
