package com.ruleweaver.ui.util;

import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Tooltip;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.net.URL;

/** Applies the application stylesheet to controls rendered in their own popup scenes. */
public final class Theme {

    private static final String STYLESHEET_PATH = "/css/style.css";

    private Theme() {
    }

    public static void applyTo(Dialog<?> dialog) {
        if (dialog == null) {
            throw new IllegalArgumentException("Dialog is required.");
        }
        ensureOwner(dialog);
        applyTo(dialog.getDialogPane());
        dialog.setOnShowing(event -> applyTo(dialog.getDialogPane()));
        applyTo(dialog.getDialogPane().getScene());
    }

    public static void applyTo(Tooltip tooltip) {
        applyTo((PopupWindow) tooltip);
    }

    public static void applyTo(ContextMenu contextMenu) {
        applyTo((PopupWindow) contextMenu);
    }

    private static void applyTo(PopupWindow popup) {
        if (popup == null) {
            throw new IllegalArgumentException("Popup is required.");
        }
        popup.addEventHandler(WindowEvent.WINDOW_SHOWING, event -> applyTo(popup.getScene()));
        applyTo(popup.getScene());
    }

    private static void applyTo(javafx.scene.control.DialogPane dialogPane) {
        String stylesheet = stylesheetUrl();
        if (!dialogPane.getStylesheets().contains(stylesheet)) {
            dialogPane.getStylesheets().add(stylesheet);
        }
        applyTo(dialogPane.getScene());
    }

    public static void applyTo(Scene scene) {
        if (scene == null) {
            return;
        }
        String stylesheet = stylesheetUrl();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }

    private static String stylesheetUrl() {
        URL stylesheet = Theme.class.getResource(STYLESHEET_PATH);
        if (stylesheet == null) {
            String message = "CSS Resource Error: required stylesheet " + STYLESHEET_PATH
                    + " was not found on the application classpath.";
            System.err.println(message);
            throw new IllegalStateException(message);
        }
        return stylesheet.toExternalForm();
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
