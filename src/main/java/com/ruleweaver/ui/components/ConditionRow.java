package com.ruleweaver.ui.components;

import com.ruleweaver.engine.Condition;
import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.engine.Operator;
import com.ruleweaver.engine.SkillCondition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Dynamic row editor for configuring a NumericCondition or SkillCondition.
 */
public class ConditionRow extends VBox {

    public enum Type {
        NUMERIC("Numeric Condition"),
        SKILL("Skill Requirement");

        private final String label;
        Type(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private final ComboBox<Type> typeBox = new ComboBox<>();
    private final ComboBox<String> fieldBox = new ComboBox<>();
    private final ComboBox<Operator> operatorBox = new ComboBox<>();
    private final TextField thresholdField = new TextField();
    private final TextField skillField = new TextField();
    private final StackPane dynamicInputs = new StackPane();
    private final Button removeBtn = new Button();
    private final Button moveUpBtn = new Button();
    private final Button moveDownBtn = new Button();
    private final Label errorLabel = new Label();
    private final HBox rowControls = new HBox(8);
    private final HBox numericContainer = new HBox(8);
    private final HBox skillContainer = new HBox(8);
    private Runnable validationListener = () -> {};

    public ConditionRow(Consumer<ConditionRow> onRemove) {
        setSpacing(6);
        setFillWidth(true);
        getStyleClass().add("condition-builder-row");

        rowControls.setAlignment(Pos.CENTER_LEFT);
        rowControls.setSpacing(8);

        typeBox.getItems().addAll(Type.NUMERIC, Type.SKILL);
        typeBox.setValue(Type.NUMERIC);
        typeBox.setPrefWidth(160);

        fieldBox.getItems().addAll("gpa", "interviewScore");
        fieldBox.setValue("gpa");
        fieldBox.setPrefWidth(140);

        operatorBox.getItems().addAll(Operator.values());
        operatorBox.setValue(Operator.GREATER_THAN_OR_EQUAL);
        operatorBox.setPrefWidth(120);

        thresholdField.setPromptText("Threshold (e.g. 8.0)");
        thresholdField.setPrefWidth(140);

        numericContainer.setAlignment(Pos.CENTER_LEFT);
        numericContainer.getChildren().addAll(fieldBox, operatorBox, thresholdField);
        HBox.setHgrow(thresholdField, Priority.ALWAYS);

        skillField.setPromptText("Enter required skill (e.g. Java, Docker)");
        skillContainer.setAlignment(Pos.CENTER_LEFT);
        skillContainer.getChildren().add(skillField);
        HBox.setHgrow(skillField, Priority.ALWAYS);

        dynamicInputs.getChildren().addAll(numericContainer, skillContainer);
        HBox.setHgrow(dynamicInputs, Priority.ALWAYS);

        moveUpBtn.setGraphic(Icons.create(Icons.ARROW_UP, 0.65, "#cdd6f4"));
        moveUpBtn.getStyleClass().addAll("btn-icon");
        moveUpBtn.setOnAction(e -> reorder(-1));

        moveDownBtn.setGraphic(Icons.create(Icons.ARROW_DOWN, 0.65, "#cdd6f4"));
        moveDownBtn.getStyleClass().addAll("btn-icon");
        moveDownBtn.setOnAction(e -> reorder(1));

        removeBtn.setGraphic(Icons.create(Icons.TRASH, 0.7, "#f38ba8"));
        removeBtn.getStyleClass().addAll("btn-icon", "btn-danger");
        removeBtn.setOnAction(e -> {
            if (onRemove != null) onRemove.accept(this);
        });

        rowControls.getChildren().addAll(moveUpBtn, moveDownBtn, removeBtn);

        HBox line = new HBox(10);
        line.setAlignment(Pos.CENTER_LEFT);
        line.setFillHeight(true);
        line.getChildren().addAll(typeBox, dynamicInputs, rowControls);
        HBox.setHgrow(dynamicInputs, Priority.ALWAYS);

        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-text-fill: #f38ba8; -fx-font-size: 11px; -fx-font-weight: 600;");

        getChildren().addAll(line, errorLabel);

        typeBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateVisibility(newVal);
            notifyValidationListener();
        });
        thresholdField.textProperty().addListener((obs, oldVal, newVal) -> notifyValidationListener());
        skillField.textProperty().addListener((obs, oldVal, newVal) -> notifyValidationListener());
        fieldBox.valueProperty().addListener((obs, oldVal, newVal) -> notifyValidationListener());
        operatorBox.valueProperty().addListener((obs, oldVal, newVal) -> notifyValidationListener());
        updateVisibility(Type.NUMERIC);
        refreshValidationState();
    }

    public void setValidationChangeListener(Runnable validationListener) {
        this.validationListener = validationListener == null ? () -> {} : validationListener;
    }

    public void setErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            errorLabel.setText("");
            setStyle("");
            return;
        }
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        setStyle("-fx-padding: 0 0 0 2; -fx-border-color: rgba(243, 139, 168, 0.35); -fx-border-width: 0 0 0 2; -fx-background-color: rgba(243, 139, 168, 0.05);");
    }

    private void notifyValidationListener() {
        validationListener.run();
    }

    public void refreshValidationState() {
        setErrorMessage(getValidationMessage());
    }

    public String getValidationMessage() {
        if (typeBox.getValue() == Type.NUMERIC) {
            String field = fieldBox.getValue();
            Operator op = operatorBox.getValue();
            String threshText = thresholdField.getText();

            if (field == null || field.trim().isEmpty()) {
                return "Field selection is required.";
            }
            if (op == null) {
                return "Operator selection is required.";
            }
            if (threshText == null || threshText.trim().isEmpty()) {
                return "Threshold numeric value is required.";
            }

            try {
                double thresh = Double.parseDouble(threshText.trim());
                if ("gpa".equals(field) && (thresh < 0.0 || thresh > 10.0)) {
                    return "GPA must be between 0 and 10.";
                }
                if ("interviewScore".equals(field) && (thresh < 0.0 || thresh > 100.0)) {
                    return "Interview score must be between 0 and 100.";
                }
                return null;
            } catch (NumberFormatException e) {
                return "Invalid threshold number: '" + threshText + "'";
            }
        }

        String skill = skillField.getText();
        if (skill == null || skill.trim().isEmpty()) {
            return "Skill name is required.";
        }
        return null;
    }

    public boolean isValid() {
        return getValidationMessage() == null;
    }

    private void updateVisibility(Type type) {
        if (type == Type.NUMERIC) {
            numericContainer.setVisible(true);
            numericContainer.setManaged(true);
            skillContainer.setVisible(false);
            skillContainer.setManaged(false);
        } else {
            numericContainer.setVisible(false);
            numericContainer.setManaged(false);
            skillContainer.setVisible(true);
            skillContainer.setManaged(true);
        }
        refreshValidationState();
    }

    public void populateFrom(Condition condition) {
        if (condition instanceof NumericCondition nc) {
            typeBox.setValue(Type.NUMERIC);
            fieldBox.setValue(nc.getFieldName());
            operatorBox.setValue(nc.getOperator());
            thresholdField.setText(String.valueOf(nc.getThreshold()));
        } else if (condition instanceof SkillCondition sc) {
            typeBox.setValue(Type.SKILL);
            skillField.setText(sc.getRequiredSkill());
        }
        refreshValidationState();
    }

    public Condition buildCondition() {
        if (typeBox.getValue() == Type.NUMERIC) {
            String field = fieldBox.getValue();
            Operator op = operatorBox.getValue();
            String threshText = thresholdField.getText();

            if (field == null || field.trim().isEmpty()) {
                Motion.shake(fieldBox);
                throw new IllegalArgumentException("Field selection is required.");
            }
            if (op == null) {
                Motion.shake(operatorBox);
                throw new IllegalArgumentException("Operator selection is required.");
            }
            if (threshText == null || threshText.trim().isEmpty()) {
                Motion.shake(thresholdField);
                throw new IllegalArgumentException("Threshold numeric value is required.");
            }

            try {
                double thresh = Double.parseDouble(threshText.trim());
                if ("gpa".equals(field) && (thresh < 0.0 || thresh > 10.0)) {
                    Motion.shake(thresholdField);
                    throw new IllegalArgumentException("GPA must be between 0 and 10.");
                }
                if ("interviewScore".equals(field) && (thresh < 0.0 || thresh > 100.0)) {
                    Motion.shake(thresholdField);
                    throw new IllegalArgumentException("Interview score must be between 0 and 100.");
                }
                return new NumericCondition(field, op, thresh);
            } catch (NumberFormatException e) {
                Motion.shake(thresholdField);
                throw new IllegalArgumentException("Invalid threshold number: '" + threshText + "'");
            }
        }

        String skill = skillField.getText();
        if (skill == null || skill.trim().isEmpty()) {
            Motion.shake(skillField);
            throw new IllegalArgumentException("Skill name is required.");
        }
        return new SkillCondition(skill.trim());
    }

    public void reorder(int delta) {
        if (delta == 0) {
            return;
        }
        var parent = getParent();
        if (!(parent instanceof javafx.scene.layout.VBox container)) {
            return;
        }
        int index = container.getChildren().indexOf(this);
        int nextIndex = index + delta;
        if (index < 0 || nextIndex < 0 || nextIndex >= container.getChildren().size()) {
            return;
        }
        container.getChildren().remove(index);
        container.getChildren().add(nextIndex, this);
        notifyValidationListener();
    }
}
