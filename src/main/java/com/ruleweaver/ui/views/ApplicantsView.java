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
import javafx.geometry.Insets;
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
import java.util.Arrays;
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
        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(28, 32, 28, 32));

        // 1. Header Toolbar
        HBox toolbar = createToolbar();

        // 2. TableView Configuration
        setupTable();

        VBox tableCard = new VBox(12);
        tableCard.getStyleClass().add("card");
        VBox.setVgrow(tableView, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        tableCard.getChildren().addAll(tableView, createTableFooter());

        contentBox.getChildren().addAll(toolbar, tableCard);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

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

    private HBox createToolbar() {
        HBox toolbar = new HBox(16);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Candidate Directory");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage applicant pool and evaluate eligibility credentials");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Live Search Bar
        searchBar.setPrefWidth(320);

        // Add Applicant Button
        Button addBtn = new Button("New Candidate");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setGraphic(Icons.create(Icons.PLUS, 0.7, "#11111b"));
        addBtn.setOnAction(e -> {
            showAddEditModal(null);
        });

        toolbar.getChildren().addAll(titleBox, searchBar, addBtn);
        return toolbar;
    }

    private void setupTable() {
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPlaceholder(new EmptyState(
                Icons.APPLICANTS,
                "No Candidates Found",
                "No applicant matches your search query or the candidate database is empty.",
                "Add Candidate",
                () -> showAddEditModal(null)
        ));

        // Column 1: Avatar + Name + Email
        TableColumn<Applicant, Applicant> nameCol = new TableColumn<>("CANDIDATE");
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
                    nameLbl.setStyle("-fx-font-weight: 700; -fx-text-fill: -color-text-primary;");

                    Label emailLbl = new Label(applicant.getEmail() != null && !applicant.getEmail().isEmpty() 
                            ? applicant.getEmail() 
                            : "ID: " + (applicant.getId() != null ? applicant.getId() : "N/A"));
                    emailLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

                    textBlock.getChildren().addAll(nameLbl, emailLbl);
                    box.getChildren().addAll(avatar, textBlock);
                    setGraphic(box);
                }
            }
        });
        nameCol.setPrefWidth(240);

        // Column 2: GPA
        TableColumn<Applicant, String> gpaCol = new TableColumn<>("GPA");
        gpaCol.setCellValueFactory(param -> new SimpleStringProperty(String.format("%.2f", param.getValue().getGpa())));
        gpaCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label(val);
                    lbl.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text-primary;");
                    setGraphic(lbl);
                }
            }
        });
        gpaCol.setPrefWidth(90);

        // Column 3: Interview Score
        TableColumn<Applicant, String> scoreCol = new TableColumn<>("INTERVIEW SCORE");
        scoreCol.setCellValueFactory(param -> new SimpleStringProperty(String.valueOf(param.getValue().getInterviewScore())));
        scoreCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label(val + " / 100");
                    lbl.setStyle("-fx-font-weight: 600; -fx-text-fill: -color-text-primary;");
                    setGraphic(lbl);
                }
            }
        });
        scoreCol.setPrefWidth(130);

        // Column 4: Skills Pill Chips
        TableColumn<Applicant, List<String>> skillsCol = new TableColumn<>("COMPETENCIES & SKILLS");
        skillsCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue().getSkills()));
        skillsCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(List<String> skills, boolean empty) {
                super.updateItem(skills, empty);
                if (empty || skills == null || skills.isEmpty()) {
                    setGraphic(new Label("No skills recorded"));
                } else {
                    HBox chips = new HBox(6);
                    chips.setAlignment(Pos.CENTER_LEFT);
                    for (String s : skills) {
                        chips.getChildren().add(new SkillChip(s));
                    }
                    setGraphic(chips);
                }
            }
        });
        skillsCol.setPrefWidth(280);

        // Column 5: Action Buttons (Edit / Delete)
        TableColumn<Applicant, Applicant> actionCol = new TableColumn<>("ACTIONS");
        actionCol.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox actions = new HBox(6, editBtn, deleteBtn);

            {
                actions.setAlignment(Pos.CENTER_LEFT);
                editBtn.setGraphic(Icons.create(Icons.EDIT, 0.65, "#89b4fa"));
                editBtn.getStyleClass().add("btn-icon");
                Tooltip editTooltip = new Tooltip("Edit Candidate");
                Theme.apply(editTooltip);
                editBtn.setTooltip(editTooltip);

                deleteBtn.setGraphic(Icons.create(Icons.TRASH, 0.65, "#f38ba8"));
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
        actionCol.setPrefWidth(100);

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
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-padding: 4px 0 0 0;");

        rowCountLabel.getStyleClass().add("caption-label");
        HBox.setHgrow(rowCountLabel, Priority.ALWAYS);

        Label hint = new Label("Keyboard Shortcuts: Ctrl+N New Candidate | Del Delete Selected");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-muted;");

        footer.getChildren().addAll(rowCountLabel, hint);
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
        dialogBox.getStyleClass().add("modal-window");
        dialogBox.setPrefWidth(460);
        dialogBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(isEdit ? "Edit Candidate" : "New Candidate Profile");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -color-text-primary;");

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

        VBox form = new VBox(12,
                new VBox(4, new Label("Full Name *"), nameInput, nameError),
                new VBox(4, new Label("Email"), emailInput),
                new VBox(4, new Label("GPA (scale 0-10) *"), gpaInput, gpaError),
                new VBox(4, new Label("Interview Score (0-100) *"), scoreInput, scoreError),
                new VBox(4, new Label("Competencies & Skills"), skillsInput)
        );

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setStyle("-fx-padding: 8px 0 0 0;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> {
            rootPane.getChildren().remove(modalBackdrop);
        });

        Button saveBtn = new Button(isEdit ? "Update Candidate" : "Save Candidate");
        saveBtn.getStyleClass().add("btn-primary");
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
