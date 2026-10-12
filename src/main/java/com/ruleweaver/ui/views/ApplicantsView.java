package com.ruleweaver.ui.views;

import com.ruleweaver.model.Applicant;
import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.adapter.ApplicantDocumentMapper;
import com.ruleweaver.ui.components.*;
import com.ruleweaver.ui.util.Theme;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Screen 1: Candidate Directory with MongoDB CRUD, live multi-field filtering,
 * avatar badges, and validated modal dialogues.
 */
public class ApplicantsView implements BaseView {

    private final StackPane rootPane = new StackPane();
    private final TableView<Applicant> tableView = new TableView<>();
    private final SearchBar searchBar = new SearchBar("Search candidates by name or skill...");
    private final Label rowCountLabel = new Label("0 applicants");
    private final LoadingOverlay loadingOverlay = new LoadingOverlay("Loading applicants from MongoDB...");
    private final Map<KeyCombination, Runnable> previousAccelerators = new LinkedHashMap<>();
    private Scene acceleratorScene;

    private FilteredList<Applicant> filteredList;

    public ApplicantsView() {
        buildUI();
    }

    private void buildUI() {
        VBox contentBox = new VBox(24);
        contentBox.getStyleClass().add("applicants-page");
        contentBox.setMinWidth(0);
        contentBox.setMaxWidth(Double.MAX_VALUE);

        VBox toolbar = createToolbar();

        setupTable();

        VBox tableCard = new VBox(16);
        tableCard.getStyleClass().addAll("card", "applicants-card");
        HBox cardHeader = new HBox(16);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        VBox cardTitle = new VBox(4);
        Label recordsTitle = new Label("Applicant records");
        recordsTitle.getStyleClass().add("applicants-section-title");
        Label recordsSubtitle = new Label("Profiles available for evaluation");
        recordsSubtitle.getStyleClass().add("applicants-section-subtitle");
        cardTitle.getChildren().addAll(recordsTitle, recordsSubtitle);
        HBox.setHgrow(cardTitle, Priority.ALWAYS);
        rowCountLabel.getStyleClass().add("applicants-count");
        cardHeader.getChildren().addAll(cardTitle, rowCountLabel);

        VBox.setVgrow(tableView, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        tableCard.getChildren().addAll(cardHeader, tableView, createTableFooter());

        contentBox.getChildren().addAll(toolbar, tableCard);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        rootPane.getStyleClass().add("page-root");
        rootPane.setMinSize(0, 0);
        rootPane.getChildren().addAll(contentBox, loadingOverlay);
    }

    @Override
    public void onShow() {
        registerShortcuts();
        AppContext.getInstance().loadApplicantsAsync(this::updateRowCount, error -> {
            AppContext.getInstance().showToast(Toast.error(
                    "Applicant Load Failed",
                    error.getMessage() == null ? "Could not load applicants."
                            : com.ruleweaver.config.MongoConfig.sanitize(error.getMessage()),
                    null
            ));
        });
    }

    @Override
    public void onHide() {
        unregisterShortcuts();
    }

    private void registerShortcuts() {
        unregisterShortcuts();
        acceleratorScene = rootPane.getScene();
        if (acceleratorScene == null) {
            return;
        }

        KeyCombination newApplicant = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
        KeyCombination deleteApplicant = new KeyCodeCombination(KeyCode.DELETE);
        previousAccelerators.put(newApplicant, acceleratorScene.getAccelerators().get(newApplicant));
        previousAccelerators.put(deleteApplicant, acceleratorScene.getAccelerators().get(deleteApplicant));
        acceleratorScene.getAccelerators().put(newApplicant, () -> {
            if (!hasModalOpen()) {
                showAddEditModal(null);
            }
        });
        acceleratorScene.getAccelerators().put(deleteApplicant, () -> {
            Applicant selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null && !hasModalOpen()) {
                confirmDelete(selected);
            }
        });
    }

    private void unregisterShortcuts() {
        if (acceleratorScene != null) {
            for (Map.Entry<KeyCombination, Runnable> entry : previousAccelerators.entrySet()) {
                if (entry.getValue() == null) {
                    acceleratorScene.getAccelerators().remove(entry.getKey());
                } else {
                    acceleratorScene.getAccelerators().put(entry.getKey(), entry.getValue());
                }
            }
        }
        previousAccelerators.clear();
        acceleratorScene = null;
    }

    private boolean hasModalOpen() {
        return rootPane.getChildren().stream()
                .anyMatch(node -> node.getStyleClass().contains("modal-backdrop"));
    }

    private VBox createToolbar() {
        VBox toolbar = new VBox(16);
        toolbar.getStyleClass().add("applicants-toolbar");
        toolbar.setMinWidth(0);
        Label title = new Label("Applicants");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage applicant profiles and eligibility credentials");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("page-subtitle");

        searchBar.setPrefWidth(360);
        searchBar.setMinWidth(240);
        searchBar.setMaxWidth(480);
        HBox.setHgrow(searchBar, Priority.ALWAYS);

        Button addBtn = new Button("New Candidate");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setMinWidth(152);
        var addIcon = Icons.create(Icons.PLUS, 0.7, (String) null);
        addIcon.getStyleClass().add("applicant-primary-icon");
        addBtn.setGraphic(addIcon);
        addBtn.setOnAction(e -> {
            showAddEditModal(null);
        });

        FlowPane controls = new FlowPane(16, 8, searchBar, addBtn);
        controls.getStyleClass().add("applicants-toolbar-controls");
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setMaxWidth(Double.MAX_VALUE);
        controls.setMinWidth(0);
        toolbar.getChildren().addAll(title, subtitle, controls);
        return toolbar;
    }

    private void setupTable() {
        tableView.getStyleClass().add("applicants-table");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setMinHeight(240);
        tableView.setPlaceholder(new EmptyState(
                Icons.APPLICANTS,
                "No applicants found",
                "No applicant matches your search, or there are no profiles yet.",
                "Add applicant",
                () -> showAddEditModal(null)
        ));

        // Column 1: Avatar + Name + Email
        TableColumn<Applicant, Applicant> nameCol = new TableColumn<>("Applicant");
        nameCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Applicant applicant, boolean empty) {
                super.updateItem(applicant, empty);
                if (empty || applicant == null) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(12);
                    box.setAlignment(Pos.CENTER_LEFT);

                    Avatar avatar = new Avatar(applicant.getName());

                    VBox textBlock = new VBox(2);
                    Label nameLbl = new Label(applicant.getName());
                    nameLbl.getStyleClass().add("applicant-name");
                    nameLbl.setWrapText(true);
                    nameLbl.setMaxWidth(Double.MAX_VALUE);

                    Label emailLbl = new Label(applicant.getEmail() != null && !applicant.getEmail().isEmpty() 
                            ? applicant.getEmail() 
                            : "ID: " + (applicant.getId() != null ? applicant.getId() : "N/A"));
                    emailLbl.getStyleClass().add("applicant-secondary");
                    emailLbl.setWrapText(true);
                    emailLbl.setMaxWidth(Double.MAX_VALUE);

                    textBlock.getChildren().addAll(nameLbl, emailLbl);
                    HBox.setHgrow(textBlock, Priority.ALWAYS);
                    box.getChildren().addAll(avatar, textBlock);
                    setGraphic(box);
                }
            }
        });
        nameCol.setMinWidth(180);
        nameCol.setPrefWidth(280);

        // Column 2: GPA
        TableColumn<Applicant, String> gpaCol = new TableColumn<>("GPA");
        gpaCol.setCellValueFactory(param -> new SimpleStringProperty(String.format("%.2f", param.getValue().getGpa())));
        gpaCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setAlignment(Pos.CENTER_LEFT);
                if (empty || val == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label(val);
                    lbl.getStyleClass().add("applicant-metric");
                    setGraphic(lbl);
                }
            }
        });
        gpaCol.setMinWidth(64);
        gpaCol.setPrefWidth(90);

        // Column 3: Interview Score
        TableColumn<Applicant, String> scoreCol = new TableColumn<>("Interview score");
        scoreCol.setCellValueFactory(param -> new SimpleStringProperty(String.valueOf(param.getValue().getInterviewScore())));
        scoreCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                setAlignment(Pos.CENTER_LEFT);
                if (empty || val == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label(val + " / 100");
                    lbl.getStyleClass().add("applicant-metric");
                    setGraphic(lbl);
                }
            }
        });
        scoreCol.setMinWidth(112);
        scoreCol.setPrefWidth(140);

        // Column 4: Skills Pill Chips
        TableColumn<Applicant, List<String>> skillsCol = new TableColumn<>("Skills");
        skillsCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue().getSkills()));
        skillsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(List<String> skills, boolean empty) {
                super.updateItem(skills, empty);
                setAlignment(Pos.CENTER_LEFT);
                if (empty) {
                    setGraphic(null);
                } else if (skills == null || skills.isEmpty()) {
                    Label noSkills = new Label("No skills recorded");
                    noSkills.getStyleClass().add("applicant-secondary");
                    setGraphic(noSkills);
                } else {
                    FlowPane chips = new FlowPane(8, 4);
                    chips.setAlignment(Pos.CENTER_LEFT);
                    chips.setMaxWidth(Double.MAX_VALUE);
                    for (String s : skills) {
                        chips.getChildren().add(new SkillChip(s));
                    }
                    setGraphic(chips);
                }
            }
        });
        skillsCol.setMinWidth(160);
        skillsCol.setPrefWidth(300);

        // Column 5: Action Buttons (Edit / Delete)
        TableColumn<Applicant, Applicant> actionCol = new TableColumn<>("Actions");
        actionCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox actions = new HBox(6, editBtn, deleteBtn);

            {
                actions.setAlignment(Pos.CENTER_LEFT);
                var editIcon = Icons.create(Icons.EDIT, 0.65, (String) null);
                editIcon.getStyleClass().add("applicant-edit-icon");
                editBtn.setGraphic(editIcon);
                editBtn.getStyleClass().add("btn-icon");
                Tooltip editTooltip = new Tooltip("Edit Candidate");
                Theme.apply(editTooltip);
                editBtn.setTooltip(editTooltip);

                var deleteIcon = Icons.create(Icons.TRASH, 0.65, (String) null);
                deleteIcon.getStyleClass().add("applicant-delete-icon");
                deleteBtn.setGraphic(deleteIcon);
                deleteBtn.getStyleClass().addAll("btn-icon", "btn-danger");
                Tooltip deleteTooltip = new Tooltip("Delete Candidate");
                Theme.apply(deleteTooltip);
                deleteBtn.setTooltip(deleteTooltip);
            }

            @Override
            protected void updateItem(Applicant applicant, boolean empty) {
                super.updateItem(applicant, empty);
                if (empty || applicant == null) {
                    setGraphic(null);
                } else {
                    editBtn.setOnAction(e -> {
                        showAddEditModal(applicant);
                    });
                    deleteBtn.setOnAction(e -> {
                        confirmDelete(applicant);
                    });
                    setGraphic(actions);
                }
            }
        });
        actionCol.setMinWidth(88);
        actionCol.setPrefWidth(104);

        tableView.getColumns().addAll(nameCol, gpaCol, scoreCol, skillsCol, actionCol);

        // Hook observable list to FilteredList and SortedList
        AppContext ctx = AppContext.getInstance();
        filteredList = new FilteredList<>(ctx.getApplicants(), p -> true);

        searchBar.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(applicant -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    return true;
                }
                String filter = newVal.trim().toLowerCase();
                boolean matchesName = applicant.getName() != null && applicant.getName().toLowerCase().contains(filter);
                boolean matchesSkill = applicant.getSkills() != null && applicant.getSkills().stream()
                        .anyMatch(s -> s.toLowerCase().contains(filter));
                return matchesName || matchesSkill;
            });
            updateRowCount();
        });

        SortedList<Applicant> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedList);

        updateRowCount();
    }

    private HBox createTableFooter() {
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label hint = new Label("Keyboard Shortcuts: Ctrl+N New Candidate | Del Delete Selected");
        hint.getStyleClass().add("applicants-shortcut-hint");
        hint.setWrapText(true);

        footer.getChildren().add(hint);
        return footer;
    }

    private void updateRowCount() {
        int count = filteredList != null ? filteredList.size() : 0;
        rowCountLabel.setText(count + (count == 1 ? " applicant" : " applicants"));
    }

    private void showAddEditModal(Applicant existing) {
        boolean isEdit = existing != null;
        StackPane modalBackdrop = new StackPane();
        modalBackdrop.getStyleClass().add("modal-backdrop");
        modalBackdrop.setPickOnBounds(true);
        modalBackdrop.setMaxWidth(Double.MAX_VALUE);
        modalBackdrop.setMaxHeight(Double.MAX_VALUE);
        StackPane.setAlignment(modalBackdrop, javafx.geometry.Pos.CENTER);

        VBox dialogBox = new VBox(18);
        dialogBox.getStyleClass().addAll("modal-window", "applicant-form");
        dialogBox.setMinWidth(320);
        dialogBox.setPrefWidth(520);
        dialogBox.setMaxWidth(560);
        dialogBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(isEdit ? "Edit Candidate" : "New Candidate Profile");
        title.getStyleClass().add("applicant-form-title");

        // Form Fields
        TextField nameInput = new TextField(isEdit ? existing.getName() : "");
        nameInput.setPromptText("Full Name (e.g. Alice Smith)");
        Label nameError = new Label();
        nameError.getStyleClass().add("error-text");

        TextField emailInput = new TextField(isEdit ? existing.getEmail() : "");
        emailInput.setPromptText("Email Address (optional)");

        TextField gpaInput = new TextField(isEdit ? String.valueOf(existing.getGpa()) : "");
        gpaInput.setPromptText("GPA (0.00 - 10.00)");
        Label gpaError = new Label();
        gpaError.getStyleClass().add("error-text");

        TextField scoreInput = new TextField(isEdit ? String.valueOf(existing.getInterviewScore()) : "");
        scoreInput.setPromptText("Interview Score (0 - 100)");
        Label scoreError = new Label();
        scoreError.getStyleClass().add("error-text");

        String existingSkills = (isEdit && existing.getSkills() != null) 
                ? String.join(", ", existing.getSkills()) 
                : "";
        TextField skillsInput = new TextField(existingSkills);
        skillsInput.setPromptText("Skills comma-separated (e.g. Java, SQL, Spring)");

        VBox form = new VBox(16,
                formField("Full Name *", nameInput, nameError),
                formField("Email", emailInput, null),
                formField("GPA (scale 0-10) *", gpaInput, gpaError),
                formField("Interview Score (0-100) *", scoreInput, scoreError),
                formField("Competencies & Skills", skillsInput, null)
        );

        FlowPane btnRow = new FlowPane(12, 8);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.getStyleClass().add("applicant-form-actions");
        btnRow.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setMinWidth(112);
        cancelBtn.setOnAction(e -> {
            rootPane.getChildren().remove(modalBackdrop);
        });

        Button saveBtn = new Button(isEdit ? "Update Candidate" : "Save Candidate");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMinWidth(152);
        saveBtn.setOnAction(e -> {
            // Validation
            boolean valid = true;
            nameError.setText("");
            gpaError.setText("");
            scoreError.setText("");

            String name = nameInput.getText().trim();
            if (name.isEmpty()) {
                nameError.setText("Candidate name is required.");
                Motion.shake(nameInput);
                valid = false;
            }

            double gpa = 0.0;
            try {
                gpa = Double.parseDouble(gpaInput.getText().trim());
                if (gpa < 0.0 || gpa > 10.0) {
                    gpaError.setText("GPA must be between 0.0 and 10.0.");
                    Motion.shake(gpaInput);
                    valid = false;
                }
            } catch (Exception ex) {
                gpaError.setText("Valid numeric GPA is required.");
                Motion.shake(gpaInput);
                valid = false;
            }

            int score = 0;
            try {
                score = Integer.parseInt(scoreInput.getText().trim());
                if (score < 0 || score > 100) {
                    scoreError.setText("Score must be between 0 and 100.");
                    Motion.shake(scoreInput);
                    valid = false;
                }
            } catch (Exception ex) {
                scoreError.setText("Valid integer score is required.");
                Motion.shake(scoreInput);
                valid = false;
            }

            if (!valid) return;

            // Parse skills
            List<String> skills = new ArrayList<>();
            if (!skillsInput.getText().trim().isEmpty()) {
                String[] parts = skillsInput.getText().split(",");
                for (String p : parts) {
                    if (!p.trim().isEmpty()) skills.add(p.trim());
                }
            }

            String applicantId = isEdit ? existing.getId() : null;
            Applicant applicant = new Applicant(applicantId, name, gpa, score, skills);
            applicant.setEmail(emailInput.getText().trim());

            saveApplicantAsync(applicant, isEdit, () -> {
                rootPane.getChildren().remove(modalBackdrop);
            });
        });

        btnRow.getChildren().addAll(cancelBtn, saveBtn);
        dialogBox.getChildren().addAll(title, form, btnRow);
        modalBackdrop.getChildren().add(dialogBox);

        rootPane.getChildren().add(modalBackdrop);
        Motion.modalScaleIn(dialogBox);
    }

    private VBox formField(String label, TextField input, Label error) {
        Label fieldLabel = new Label(label);
        fieldLabel.getStyleClass().add("applicant-form-label");
        VBox field = new VBox(8);
        field.getChildren().addAll(fieldLabel, input);
        if (error != null) {
            field.getChildren().add(error);
        }
        input.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private void saveApplicantAsync(Applicant applicant, boolean isEdit, Runnable onSuccess) {
        AppContext ctx = AppContext.getInstance();
        loadingOverlay.show(isEdit ? "Updating candidate in MongoDB Atlas..." : "Saving candidate to MongoDB Atlas...");

        ctx.runAsync(() -> {
            try {
                Document doc = ApplicantDocumentMapper.toDocument(applicant);
                if (isEdit) {
                    ctx.getApplicantRepository().update(applicant.getId(), doc);
                } else {
                    ctx.getApplicantRepository().save(doc);
                }

                Platform.runLater(() -> {
                    loadingOverlay.hide();
                    ctx.showToast(Toast.success(
                            isEdit ? "Candidate Updated" : "Candidate Saved",
                            "Applicant '" + applicant.getName() + "' saved to MongoDB.",
                            null
                    ));
                    if (onSuccess != null) onSuccess.run();
                    ctx.loadApplicantsAsync(this::updateRowCount, null);
                });
            } catch (Exception e) {
                AppContext.logFailure("Applicant save failed", e);
                Platform.runLater(() -> {
                    loadingOverlay.hide();
                    ctx.showToast(Toast.error("Database Save Failed", e.getMessage(), null));
                });
            }
        });
    }

    private void confirmDelete(Applicant applicant) {
        ConfirmDialog dialog = new ConfirmDialog(
                "Delete Candidate Profile",
                "Are you sure you want to permanently delete '" + applicant.getName() + "'? This action will remove the record from MongoDB Atlas.",
                "Delete Candidate",
                true,
                () -> deleteApplicantAsync(applicant),
                () -> rootPane.getChildren().removeIf(node -> node instanceof ConfirmDialog)
        );
        rootPane.getChildren().add(dialog);
    }

    private void deleteApplicantAsync(Applicant applicant) {
        rootPane.getChildren().removeIf(node -> node instanceof ConfirmDialog);
        AppContext ctx = AppContext.getInstance();
        loadingOverlay.show("Deleting candidate from MongoDB Atlas...");

        ctx.runAsync(() -> {
            try {
                ctx.getApplicantRepository().delete(applicant.getId());
                Platform.runLater(() -> {
                    loadingOverlay.hide();
                    ctx.showToast(Toast.info(
                            "Candidate Deleted",
                            "Removed '" + applicant.getName() + "' from MongoDB.",
                            null
                    ));
                    ctx.loadApplicantsAsync(this::updateRowCount, null);
                });
            } catch (Exception e) {
                AppContext.logFailure("Applicant deletion failed", e);
                Platform.runLater(() -> {
                    loadingOverlay.hide();
                    ctx.showToast(Toast.error("Delete Failed", e.getMessage(), null));
                });
            }
        });
    }

    @Override
    public Node getRoot() {
        return rootPane;
    }

}
