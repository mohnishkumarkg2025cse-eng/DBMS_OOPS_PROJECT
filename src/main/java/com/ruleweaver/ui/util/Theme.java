package com.ruleweaver.ui.util;

import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.components.Toast;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Tooltip;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.net.URL;

/** Applies the application stylesheet to every JavaFX scene and popup surface. */
public final class Theme {

    private static final String STYLESHEET_PATH = "/css/style.css";
    private static final String WINDOW_HANDLER_KEY = Theme.class.getName() + ".windowHandler";
    private static String stylesheetUrl;
    private static boolean missingLogged;
    private static boolean missingToastScheduled;
    private static boolean windowListenerInstalled;

    private Theme() {
    }

    public static void apply(Dialog<?> dialog) {
        if (dialog == null) {
            throw new IllegalArgumentException("Dialog is required.");
        }
        ensureOwner(dialog);
        DialogPane pane = dialog.getDialogPane();
        apply(pane);
        dialog.setOnShowing(event -> apply(pane));
    }

    public static void apply(DialogPane dialogPane) {
        if (dialogPane == null) {
            throw new IllegalArgumentException("Dialog pane is required.");
        }
        addStylesheet(dialogPane.getStylesheets());
        apply(dialogPane.getScene());
    }

    public static void apply(Scene scene) {
        if (scene == null) {
            return;
        }
        installWindowListener();
        String stylesheet = stylesheetUrl();
        if (stylesheet == null) {
            notifyMissingStylesheet();
            return;
        }
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
        applySubScenes(scene.getRoot(), stylesheet);
    }

    public static void apply(SubScene subScene) {
        if (subScene == null) {
            return;
        }
        String stylesheet = stylesheetUrl();
        if (stylesheet == null) {
            notifyMissingStylesheet();
            return;
        }
        Scene scene = subScene.getScene();
        if (scene != null) {
            apply(scene);
        } else {
            applySubScenes(subScene.getRoot(), stylesheet);
        }
    }

    public static void apply(Window window) {
        if (window == null) {
            return;
        }
        String stylesheet = stylesheetUrl();
        if (stylesheet == null) {
            notifyMissingStylesheet();
            return;
        }
        if (window instanceof Stage stage) {
            apply(stage.getScene());
            return;
        }
        if (window instanceof PopupWindow popup) {
            if (window.getProperties().putIfAbsent(WINDOW_HANDLER_KEY, Boolean.TRUE) == null) {
                popup.addEventHandler(WindowEvent.WINDOW_SHOWING,
                        event -> apply(popup.getScene()));
            }
            apply(popup.getScene());
        }
    }

    public static void apply(Tooltip tooltip) {
        apply((Window) tooltip);
    }

    public static void apply(ContextMenu contextMenu) {
        apply((Window) contextMenu);
    }

    private static void addStylesheet(java.util.List<String> stylesheets) {
        String stylesheet = stylesheetUrl();
        if (stylesheet == null) {
            notifyMissingStylesheet();
        } else if (!stylesheets.contains(stylesheet)) {
            stylesheets.add(stylesheet);
        }
    }

    private static void applySubScenes(Node node, String stylesheet) {
        if (node instanceof SubScene subScene) {
            applySubScenes(subScene.getRoot(), stylesheet);
        } else if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applySubScenes(child, stylesheet);
            }
        }
    }

    private static void installWindowListener() {
        if (windowListenerInstalled) {
            return;
        }
        windowListenerInstalled = true;
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                for (Window window : change.getAddedSubList()) {
                    apply(window);
                }
            }
        });
        for (Window window : Window.getWindows()) {
            apply(window);
        }
    }

    private static String stylesheetUrl() {
        if (stylesheetUrl != null) {
            return stylesheetUrl;
        }
        URL stylesheet = Theme.class.getResource(STYLESHEET_PATH);
        if (stylesheet == null) {
            if (!missingLogged) {
                System.err.println("CSS Resource Error: required stylesheet " + STYLESHEET_PATH
                        + " was not found on the application classpath.");
                missingLogged = true;
            }
            return null;
        }
        stylesheetUrl = stylesheet.toExternalForm();
        return stylesheetUrl;
    }

    private static void notifyMissingStylesheet() {
        if (missingToastScheduled || !Platform.isFxApplicationThread()) {
            return;
        }
        missingToastScheduled = true;
        Platform.runLater(() -> AppContext.getInstance().showToast(
                Toast.error("Stylesheet Missing",
                        "The application stylesheet " + STYLESHEET_PATH + " could not be loaded.", null)));
    }

    private static void ensureOwner(Dialog<?> dialog) {
        if (dialog.getOwner() != null) {
            return;
        }
        Window.getWindows().stream()
                .filter(Window::isFocused)
                .findFirst()
                .ifPresent(dialog::initOwner);
        if (dialog.getOwner() == null) {
            String message = "Dialog Owner Warning: no owner window is available for "
                    + dialog.getTitle() + ".";
            System.err.println(message);
            throw new IllegalStateException(message);
        }
    }
}
