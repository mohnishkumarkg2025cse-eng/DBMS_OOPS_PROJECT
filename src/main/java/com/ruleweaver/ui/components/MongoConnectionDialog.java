package com.ruleweaver.ui.components;

import com.ruleweaver.config.MongoConfig;
import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.util.Theme;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Collects and validates a MongoDB URI without blocking the JavaFX thread.
 */
public final class MongoConnectionDialog {

    private MongoConnectionDialog() {
    }

    public static void show(Stage owner, Runnable onConnected, Runnable onCancelled) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Connect to MongoDB");
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        Theme.applyTo(dialog);

        ButtonType connectType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Continue Offline", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(connectType, cancelType);

        Label introduction = new Label(
                "Enter your MongoDB Atlas connection URI. The connection is tested before it is saved locally.");
        introduction.setWrapText(true);
        PasswordField uriInput = new PasswordField();
        uriInput.setPromptText("mongodb+srv://username:password@cluster-host/");
        MongoConfig.getConfiguredUri().ifPresent(uriInput::setText);

        TextField databaseInput = new TextField(MongoConfig.getDatabaseName());
        Label errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-text-fill: -color-danger;");

        GridPane fields = new GridPane();
        fields.setHgap(12);
        fields.setVgap(10);
        fields.addRow(0, new Label("Connection URI"), uriInput);
        fields.addRow(1, new Label("Database name"), databaseInput);
        GridPane.setHgrow(uriInput, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(databaseInput, javafx.scene.layout.Priority.ALWAYS);

        VBox content = new VBox(14, introduction, fields, errorLabel);
        content.setPadding(new Insets(16, 0, 8, 0));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(520);
        dialog.setResultConverter(button -> null);
        AtomicBoolean connectionSucceeded = new AtomicBoolean();

        Node connectNode = dialog.getDialogPane().lookupButton(connectType);
        Button connectButton = (Button) connectNode;
        connectButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            String uri = uriInput.getText();
            String databaseName = databaseInput.getText();
            if (uri == null || uri.isBlank()) {
                errorLabel.setText("Enter a MongoDB connection URI to continue.");
                uriInput.requestFocus();
                return;
            }

            connectButton.setDisable(true);
            errorLabel.setText("Testing the connection...");
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    MongoConfig.verifyAndSave(uri, databaseName);
                    return null;
                }
            };
            task.setOnSucceeded(success -> {
                connectionSucceeded.set(true);
                dialog.close();
                AppContext.getInstance().connectToDatabase(onConnected, failure ->
                        show(owner, onConnected, onCancelled));
            });
            task.setOnFailed(failure -> {
                Throwable exception = task.getException();
                logFailure(exception);
                String message = exception == null ? "Connection failed."
                        : MongoConfig.sanitize(exception.getMessage());
                errorLabel.setText(message);
                connectButton.setDisable(false);
                AppContext.getInstance().showToast(Toast.error("Database Connection Failed", message, null));
            });
            AppContext.getInstance().getExecutor().execute(task);
        });

        dialog.setOnCloseRequest(event -> {
            if (!connectionSucceeded.get() && onCancelled != null) {
                onCancelled.run();
            }
        });
        dialog.show();
    }

    private static void logFailure(Throwable failure) {
        if (failure == null) {
            return;
        }
        System.err.println("MongoDB connection setup failed: " + MongoConfig.sanitize(failure.getMessage()));
        for (StackTraceElement element : failure.getStackTrace()) {
            System.err.println("\tat " + element);
        }
        if (failure.getCause() != null) {
            logFailure(failure.getCause());
        }
    }
}
