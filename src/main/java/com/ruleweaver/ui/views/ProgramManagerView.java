package com.ruleweaver.ui.views;

import com.ruleweaver.model.FieldDefinition;
import com.ruleweaver.model.FieldOperator;
import com.ruleweaver.model.FieldType;
import com.ruleweaver.model.FieldValidationRules;
import com.ruleweaver.model.ProgramConfiguration;
import com.ruleweaver.model.ProgramTemplate;
import com.ruleweaver.service.ProgramService;
import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.components.Toast;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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

    public ProgramManagerView() {
        buildUi();
    }

    private void buildUi() {
        root.getStyleClass().add("page-root");
        VBox page = new VBox(18);
        page.setPadding(new Insets(28, 32, 28, 32));
        Label title = new Label("Program & Field Registry");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage reusable program templates and applicant field schemas");
        subtitle.getStyleClass().add("page-subtitle");

        programs.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ProgramConfiguration item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName()
                        + (item.isArchived() ? " (Archived)" : "")
                        + "  ·  " + item.getTemplate().getDisplayName());
            }
        });
        programs.getSelectionModel().selectedItemProperty().addListener((observable, old, selected) ->
                renderFields(selected));

        ComboBox<ProgramTemplate> templates = new ComboBox<>();
        templates.getItems().setAll(ProgramTemplate.values());
        templates.setValue(ProgramTemplate.CUSTOM);
        Button create = new Button("Create from Template");
        create.getStyleClass().add("btn-primary");
        create.setOnAction(event -> {
            ProgramTemplate template = templates.getValue();
            TextInputDialog dialog = new TextInputDialog(template.getDisplayName() + " Program");
            dialog.setTitle("Create Program");
            dialog.setHeaderText("Create an editable program from " + template.getDisplayName());
            dialog.setContentText("Program name:");
            dialog.showAndWait().ifPresent(name -> run(
                    service -> service.createFromTemplate(template, name),
                    created -> {
                        refresh(created.getId());
                        AppContext.getInstance().showToast(Toast.success(
                                "Program Created", "Created " + created.getName() + ".", null));
                    }));
        });

        Button custom = new Button("New Custom Program");
        custom.getStyleClass().add("btn-secondary");
        custom.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog("Custom Program");
            dialog.setTitle("Create Custom Program");
            dialog.setHeaderText("Start with name and email fields");
            dialog.setContentText("Program name:");
            dialog.showAndWait().ifPresent(name -> run(service -> service.createProgram(
                    "custom-" + UUID.randomUUID(), name, ProgramTemplate.CUSTOM,
                    ProgramTemplate.CUSTOM.defaultFields()), created -> refresh(created.getId())));
        });

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
        HBox actions = new HBox(8, rename, duplicate, archive, selectButton, addField, editField);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox templateActions = new HBox(10, templates, create, custom);
        templateActions.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(12, new Label("PROGRAMS"), programs);
        left.getStyleClass().add("card");
        left.setPrefWidth(390);
        VBox.setVgrow(programs, Priority.ALWAYS);
        VBox detail = new VBox(14, selectedLabel, actions, new Label("CONFIGURED FIELDS"), fields);
        detail.getStyleClass().add("card");
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
        page.getChildren().addAll(title, subtitle, templateActions, content);
        root.getChildren().setAll(page);
        refresh(null);
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
            programs.getItems().setAll(result);
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
                        failure.getMessage() == null ? "Program operation failed." : failure.getMessage(), null));
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
        refresh(AppContext.getInstance().selectedProgramProperty().get() == null
                ? null : AppContext.getInstance().selectedProgramProperty().get().getId());
    }
}
