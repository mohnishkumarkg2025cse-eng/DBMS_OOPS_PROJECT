package com.ruleweaver.ui.views;

import javafx.scene.Node;

/**
 * Common contract for top-level navigation views.
 */
public interface BaseView {

    /**
     * Returns root scene graph node of the view.
     */
    Node getRoot();

    /**
     * Called when the view is removed from the active route.
     */
    default void onHide() {
    }

    /**
     * Lifecycle callback invoked when the view is transitioned into view.
     */
    void onShow();
}
