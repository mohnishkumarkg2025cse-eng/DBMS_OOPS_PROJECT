package com.ruleweaver.ui.components;

import com.ruleweaver.engine.Condition;
import com.ruleweaver.engine.LogicalOperator;
import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.FieldDefinition;
import com.ruleweaver.model.FieldOperator;
import com.ruleweaver.model.FieldRegistry;
import com.ruleweaver.model.FieldType;
import com.ruleweaver.model.FieldValidationRules;
import com.ruleweaver.model.PolicyVersion;
import com.ruleweaver.model.ProgramConfiguration;
import com.ruleweaver.service.PolicyDraftImpactService;
import com.ruleweaver.service.PolicyRuleDraft;
import com.ruleweaver.ui.AppContext;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.util.HashSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Visual editor for in-memory policy rule drafts.
 */
public final class PolicyRuleBuilder extends VBox {
    private final PolicyRuleDraft draft;
    private final VBox editor = new VBox(8);
    private final Label naturalPreview = new Label();
    private final Label treePreview = new Label();
    private final Label impactPreview = new Label("Impact preview will run when valid rules and applicants are available.");
    private final Label validation = new Label();
    private final TabPane previews = new TabPane();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));
    private final PauseTransition textPreviewDebounce = new PauseTransition(Duration.millis(120));
    private final AtomicLong previewGeneration = new AtomicLong();
    private final Set<PolicyRuleDraft.Group> collapsedGroups = new HashSet<>();
    private FieldRegistry registry;
    private List<FieldDefinition> definitions;
    private Consumer<Boolean> validityListener = ignored -> { };
    private Consumer<List<String>> validationListener = ignored -> { };
    private boolean valid;
    private PolicyVersion previewBase;
    private Future<?> impactTask;

    public PolicyRuleBuilder(ProgramConfiguration program, List<Condition> initialConditions) {
        this(program == null ? legacyDefinitions() : program.getFields(),
                PolicyRuleDraft.fromConditions(initialConditions));
    }

    private PolicyRuleBuilder(List<FieldDefinition> definitions, PolicyRuleDraft draft) {
        super(10);
        this.definitions = List.copyOf(definitions);
        this.registry = new FieldRegistry(this.definitions);
        this.draft = draft;
        getStyleClass().add("policy-rule-builder");
        setFillWidth(true);

        editor.setPadding(new Insets(8));
        editor.setStyle("-fx-background-color: -color-surface; -fx-background-radius: 8;");
        ScrollPane editorScroll = new ScrollPane(editor);
        editorScroll.setFitToWidth(true);
        editorScroll.setPrefViewportHeight(260);
        editorScroll.setMaxHeight(340);

        naturalPreview.setWrapText(true);
        naturalPreview.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -color-accent;");
        treePreview.setWrapText(true);
        treePreview.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        impactPreview.setWrapText(true);

        previews.getTabs().addAll(
                new Tab("Natural language", naturalPreview),
                new Tab("Rule tree", treePreview),
                new Tab("Draft impact", impactPreview));
        previews.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        validation.setWrapText(true);
        validation.getStyleClass().add("policy-form-error");

        getChildren().addAll(createToolbar(), editorScroll, validation, previews);
        debounce.setOnFinished(event -> refreshDraftImpact(previewGeneration.get()));
        textPreviewDebounce.setOnFinished(event -> updateTextPreviews());
        render();
    }

    public void setProgram(ProgramConfiguration program) {
        definitions = program == null ? legacyDefinitions() : program.getFields();
        registry = new FieldRegistry(definitions);
        render();
    }

    public void setValidityListener(Consumer<Boolean> listener) {
        validityListener = listener == null ? ignored -> { } : listener;
        validityListener.accept(valid);
    }

    public void setValidationListener(Consumer<List<String>> listener) {
        validationListener = listener == null ? ignored -> { } : listener;
    }

    public void setImpactPreviewBase(PolicyVersion baseVersion) {
        previewBase = baseVersion;
        scheduleImpactPreview();
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getValidationErrors() {
        return draft.validate(registry);
    }

    public List<Condition> getConditions() {
        return draft.buildConditions(registry);
    }

    public void refresh() {
        render();
    }

    private Node createToolbar() {
        Button addCondition = new Button("Add condition");
        addCondition.getStyleClass().add("btn-secondary");
        addCondition.setOnAction(event -> {
            FieldDefinition first = definitions.isEmpty() ? null : definitions.get(0);
            draft.root().children().add(new PolicyRuleDraft.Rule(
                    first == null ? "" : first.getKey(),
                    first == null || first.getSupportedOperators().isEmpty()
                            ? null : first.getSupportedOperators().iterator().next(), ""));
            render();
        });
        Button addGroup = new Button("Add AND/OR group");
        addGroup.getStyleClass().add("btn-secondary");
        addGroup.setOnAction(event -> {
            PolicyRuleDraft.Group group = new PolicyRuleDraft.Group(LogicalOperator.AND);
            if (!definitions.isEmpty()) {
                FieldDefinition first = definitions.get(0);
                group.children().add(new PolicyRuleDraft.Rule(first.getKey(),
                        first.getSupportedOperators().iterator().next(), ""));
            }
            draft.root().children().add(group);
            render();
        });
        HBox toolbar = new HBox(8, addCondition, addGroup);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private void render() {
        editor.getChildren().clear();
        Label rootTitle = new Label("All top-level conditions must pass (AND)");
        rootTitle.setStyle("-fx-font-weight: 700;");
        editor.getChildren().add(rootTitle);
        editor.getChildren().add(renderGroup(draft.root(), 0, true));

        List<String> errors = draft.validate(registry);
        valid = errors.isEmpty();
        validation.setText(valid ? "" : String.join("\n", errors));
        validation.setVisible(!valid);
        validation.setManaged(!valid);
        updateTextPreviews();
        validityListener.accept(valid);
        validationListener.accept(errors);
        scheduleImpactPreview();
    }

    private Node renderGroup(PolicyRuleDraft.Group group, int depth, boolean root) {
        VBox groupPanel = new VBox(6);
        groupPanel.setPadding(new Insets(4));
        if (!root) {
            HBox heading = new HBox(8);
            heading.setAlignment(Pos.CENTER_LEFT);
            heading.setPadding(new Insets(6, 4, 4, depth * 18));
            ComboBox<LogicalOperator> logical = new ComboBox<>();
            logical.getItems().setAll(LogicalOperator.values());
            logical.setValue(group.operator());
            logical.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    group.setOperator(newValue);
                    render();
                }
            });
            Button collapse = new Button("−");
            collapse.getStyleClass().add("btn-icon");
            Button addChild = new Button("Add condition");
            addChild.getStyleClass().add("btn-secondary");
            Button addNested = new Button("Add group");
            addNested.getStyleClass().add("btn-secondary");
            Button duplicate = new Button("Duplicate");
            duplicate.getStyleClass().add("btn-secondary");
            Button remove = new Button("Remove group");
            remove.getStyleClass().add("btn-secondary");
            heading.getChildren().addAll(new Label("Group"), logical, addChild, addNested, duplicate, remove, collapse);
            groupPanel.getChildren().add(heading);

            VBox children = new VBox(7);
            children.setPadding(new Insets(0, 0, 0, depth * 18 + 12));
            for (Object child : List.copyOf(group.children())) {
                if (child instanceof PolicyRuleDraft.Group nested) {
                    children.getChildren().add(renderGroup(nested, depth + 1, false));
                } else {
                    children.getChildren().add(renderRule((PolicyRuleDraft.Rule) child, group, depth + 1));
                }
            }
            if (group.children().isEmpty()) {
                Label empty = new Label("This logical group is empty. Add a condition before saving.");
                empty.getStyleClass().add("condition-error-text");
                children.getChildren().add(empty);
            }
            groupPanel.getChildren().add(children);
            boolean expanded = !collapsedGroups.contains(group);
            children.setVisible(expanded);
            children.setManaged(expanded);
            collapse.setText(expanded ? "−" : "+");
            collapse.setOnAction(event -> {
                if (collapsedGroups.remove(group)) {
                    children.setVisible(true);
                    children.setManaged(true);
                    collapse.setText("−");
                } else {
                    collapsedGroups.add(group);
                    children.setVisible(false);
                    children.setManaged(false);
                    collapse.setText("+");
                }
            });
            addChild.setOnAction(event -> addRuleTo(group));
            addNested.setOnAction(event -> {
                PolicyRuleDraft.Group nested = new PolicyRuleDraft.Group(LogicalOperator.AND);
                group.children().add(nested);
                render();
            });
            duplicate.setOnAction(event -> {
                group.children().add(copyGroup(group));
                render();
            });
            remove.setOnAction(event -> removeNode(group));
            return groupPanel;
        }

        for (Object child : List.copyOf(group.children())) {
            if (child instanceof PolicyRuleDraft.Group nested) {
                groupPanel.getChildren().add(renderGroup(nested, depth + 1, false));
            } else {
                groupPanel.getChildren().add(renderRule((PolicyRuleDraft.Rule) child, group, depth + 1));
            }
        }
        return groupPanel;
    }

    private Node renderRule(PolicyRuleDraft.Rule rule, PolicyRuleDraft.Group parent, int depth) {
        HBox row = new HBox(7);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 4, 4, depth * 18));
        row.getStyleClass().add("policy-condition-row");
        ComboBox<FieldDefinition> field = new ComboBox<>();
        field.getItems().setAll(definitions);
        field.setConverter(new StringConverter<>() {
            @Override public String toString(FieldDefinition value) {
                return value == null ? "" : value.getLabel();
            }
            @Override public FieldDefinition fromString(String value) { return null; }
        });
        field.setValue(registry.find(rule.fieldKey()).orElse(null));
        field.setPrefWidth(150);

        ComboBox<FieldOperator> operator = new ComboBox<>();
        TextField value = new TextField(rule.value());
        value.setPromptText("Value");
        value.setPrefWidth(130);
        HBox.setHgrow(value, Priority.ALWAYS);
        Node valueEditor = value;
        FieldDefinition selected = field.getValue();
        if (selected != null) {
            operator.getItems().setAll(selected.getSupportedOperators());
            operator.setValue(selected.getSupportedOperators().contains(rule.operator())
                    ? rule.operator() : null);
            if (selected.getType() == FieldType.BOOLEAN) {
                ComboBox<String> booleanValue = new ComboBox<>();
                booleanValue.getItems().setAll("true", "false");
                booleanValue.setEditable(true);
                booleanValue.setValue(rule.value().isBlank() ? null : rule.value());
                valueEditor = booleanValue;
                booleanValue.valueProperty().addListener((observable, oldValue, newValue) -> {
                    rule.setValue(newValue == null ? "" : newValue);
                    updateDraftState();
                });
            } else if (selected.getType() == FieldType.MULTI_SELECT
                    && !selected.getPermittedValues().isEmpty()) {
                ListView<String> choices = new ListView<>();
                choices.getItems().setAll(selected.getPermittedValues());
                choices.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                choices.setPrefHeight(64);
                if (!rule.value().isBlank()) {
                    for (String item : rule.value().split(",")) {
                        choices.getSelectionModel().select(item.trim());
                    }
                }
                boolean multiple = rule.operator() == FieldOperator.CONTAINS_ANY
                        || rule.operator() == FieldOperator.CONTAINS_ALL;
                choices.getSelectionModel().setSelectionMode(
                        multiple ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
                valueEditor = choices;
                choices.getSelectionModel().getSelectedItems().addListener(
                        (javafx.collections.ListChangeListener<String>) change -> {
                            List<String> selectedValues = choices.getSelectionModel().getSelectedItems();
                            rule.setValue(multiple ? String.join(", ", selectedValues)
                                    : selectedValues.stream().findFirst().orElse(""));
                            updateDraftState();
                        });
            } else if (selected.getType() == FieldType.TEXT
                    && !selected.getPermittedValues().isEmpty()) {
                ComboBox<String> permitted = new ComboBox<>();
                permitted.getItems().setAll(selected.getPermittedValues());
                permitted.setEditable(true);
                permitted.setValue(rule.value().isBlank() ? null : rule.value());
                valueEditor = permitted;
                permitted.valueProperty().addListener((observable, oldValue, newValue) -> {
                    rule.setValue(newValue == null ? "" : newValue);
                    updateDraftState();
                });
            }
        }

        Node actualValueEditor = valueEditor;
        field.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                rule.setFieldKey(newValue.getKey());
                rule.setOperator(newValue.getSupportedOperators().stream().findFirst().orElse(null));
                rule.setValue("");
                render();
            }
        });
        operator.valueProperty().addListener((observable, oldValue, newValue) -> {
            rule.setOperator(newValue);
            render();
        });
        if (actualValueEditor == value) {
            value.textProperty().addListener((observable, oldValue, newValue) -> {
                rule.setValue(newValue);
                updateDraftState();
            });
        }

        Button duplicate = new Button("Duplicate");
        duplicate.getStyleClass().add("btn-secondary");
        duplicate.setOnAction(event -> {
            parent.children().add(new PolicyRuleDraft.Rule(rule.fieldKey(), rule.operator(), rule.value()));
            render();
        });
        Button up = new Button("↑");
        up.setOnAction(event -> move(parent, rule, -1));
        Button down = new Button("↓");
        down.setOnAction(event -> move(parent, rule, 1));
        Button remove = new Button("×");
        remove.getStyleClass().add("btn-danger");
        remove.setOnAction(event -> {
            parent.children().remove(rule);
            render();
        });
        row.getChildren().addAll(field, operator, valueEditor, duplicate, up, down, remove);
        return row;
    }

    private void addRuleTo(PolicyRuleDraft.Group group) {
        FieldDefinition first = definitions.isEmpty() ? null : definitions.get(0);
        group.children().add(new PolicyRuleDraft.Rule(first == null ? "" : first.getKey(),
                first == null ? null : first.getSupportedOperators().stream().findFirst().orElse(null), ""));
        render();
    }

    private void removeNode(Object target) {
        if (removeFrom(draft.root(), target)) render();
    }

    private static boolean removeFrom(PolicyRuleDraft.Group group, Object target) {
        if (group.children().remove(target)) return true;
        for (Object child : group.children()) {
            if (child instanceof PolicyRuleDraft.Group nested && removeFrom(nested, target)) return true;
        }
        return false;
    }

    private void move(PolicyRuleDraft.Group parent, Object node, int offset) {
        int index = parent.children().indexOf(node);
        int target = index + offset;
        if (target < 0 || target >= parent.children().size()) return;
        parent.children().remove(index);
        parent.children().add(target, node);
        render();
    }

    private static PolicyRuleDraft.Group copyGroup(PolicyRuleDraft.Group source) {
        PolicyRuleDraft.Group copy = new PolicyRuleDraft.Group(source.operator());
        for (Object child : source.children()) {
            copy.children().add(child instanceof PolicyRuleDraft.Group nested
                    ? copyGroup(nested)
                    : new PolicyRuleDraft.Rule(((PolicyRuleDraft.Rule) child).fieldKey(),
                    ((PolicyRuleDraft.Rule) child).operator(), ((PolicyRuleDraft.Rule) child).value()));
        }
        return copy;
    }

    private void updateDraftState() {
        List<String> errors = draft.validate(registry);
        valid = errors.isEmpty();
        validation.setText(valid ? "" : String.join("\n", errors));
        validation.setVisible(!valid);
        validation.setManaged(!valid);
        textPreviewDebounce.playFromStart();
        validityListener.accept(valid);
        validationListener.accept(errors);
        scheduleImpactPreview();
    }

    private void updateTextPreviews() {
        naturalPreview.setText(draft.naturalLanguage(registry));
        treePreview.setText(draft.readOnlyTree(registry));
    }

    private void scheduleImpactPreview() {
        long generation = previewGeneration.incrementAndGet();
        debounce.stop();
        if (impactTask != null) impactTask.cancel(true);
        if (valid && previewBase != null) debounce.playFromStart();
        else impactPreview.setText(valid
                ? "Choose or create rules to calculate an impact preview."
                : "Resolve validation errors to calculate a draft impact preview.");
    }

    private void refreshDraftImpact(long generation) {
        if (!valid || previewBase == null) return;
        final List<Condition> conditions;
        try {
            conditions = getConditions();
        } catch (IllegalArgumentException exception) {
            impactPreview.setText(exception.getMessage());
            return;
        }
        PolicyVersion baselineVersion = previewBase;
        List<Applicant> applicants = List.copyOf(AppContext.getInstance().getApplicants());
        impactPreview.setText("Calculating draft impact for " + applicants.size() + " applicants…");
        impactTask = AppContext.getInstance().getExecutor().submit(() -> {
            try {
                PolicyDraftImpactService.ImpactSummary summary = new PolicyDraftImpactService(
                        AppContext.getInstance().getDecisionEngine())
                        .compare(baselineVersion, conditions, applicants);
                Platform.runLater(() -> {
                    if (previewGeneration.get() == generation) {
                        int change = summary.eligibleChange();
                        impactPreview.setText("Draft impact: " + summary.draftEligible() + " eligible of "
                                + summary.sampleSize() + " applicants (" + (change >= 0 ? "+" : "") + change
                                + " vs. baseline " + summary.baselineEligible()
                                + "). Unsaved preview; no records are written.");
                    }
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    if (previewGeneration.get() == generation) {
                        String message = exception.getMessage();
                        impactPreview.setText("Could not calculate draft impact: "
                                + (message == null || message.isBlank()
                                ? exception.getClass().getSimpleName() : message));
                    }
                });
            }
        });
    }

    private static List<FieldDefinition> legacyDefinitions() {
        Set<FieldOperator> numeric = EnumSet.of(FieldOperator.EQUALS, FieldOperator.NOT_EQUALS,
                FieldOperator.GREATER_THAN, FieldOperator.GREATER_THAN_OR_EQUAL,
                FieldOperator.LESS_THAN, FieldOperator.LESS_THAN_OR_EQUAL);
        return List.of(
                new FieldDefinition("gpa", "GPA", FieldType.NUMBER, false,
                        new FieldValidationRules(0.0, 10.0, null, null, null), List.of(), numeric),
                new FieldDefinition("interviewScore", "Interview score", FieldType.NUMBER, false,
                        new FieldValidationRules(0.0, 100.0, null, null, null), List.of(), numeric),
                new FieldDefinition("skills", "Skills", FieldType.MULTI_SELECT, false,
                        FieldValidationRules.none(), List.of(), EnumSet.of(FieldOperator.CONTAINS,
                        FieldOperator.NOT_CONTAINS, FieldOperator.CONTAINS_ANY, FieldOperator.CONTAINS_ALL)));
    }
}
