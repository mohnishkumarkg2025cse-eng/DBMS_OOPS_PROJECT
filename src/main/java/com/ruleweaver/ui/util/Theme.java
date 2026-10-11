package com.ruleweaver.ui.util;

import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.Tooltip;

import java.net.URL;

/** Applies the application stylesheet to controls rendered in their own popup scenes. */
public final class Theme {

    private Theme() {
    }

    public static void applyTo(Dialog<?> dialog) {
        dialog.setOnShowing(event -> applyTo(dialog.getDialogPane().getScene()));
        applyTo(dialog.getDialogPane().getScene());
    }

    public static void applyTo(Tooltip tooltip) {
        tooltip.setOnShowing(event -> applyTo(tooltip.getScene()));
        applyTo(tooltip.getScene());
    }

    public static void applyTo(Scene scene) {
        if (scene == null) {
            return;
        }
        URL stylesheet = Theme.class.getResource("/css/style.css");
        if (stylesheet != null) {
            String url = stylesheet.toExternalForm();
            if (!scene.getStylesheets().contains(url)) {
                scene.getStylesheets().add(url);
            }
        }
    }
}
