package com.ruleweaver.ui.shell;

import com.ruleweaver.ui.components.Motion;
import com.ruleweaver.ui.views.BaseView;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;

/**
 * Animated View Router that transitions between center scene nodes.
 */
public class ViewRouter {

    private final StackPane container;
    private final Map<Sidebar.Destination, BaseView> views = new HashMap<>();
    private BaseView currentView;

    public ViewRouter(StackPane container) {
        this.container = container;
    }

    public void registerView(Sidebar.Destination destination, BaseView view) {
        views.put(destination, view);
    }

    public void navigateTo(Sidebar.Destination destination) {
        BaseView view = views.get(destination);
        if (view == null || view == currentView) {
            return;
        }

        if (currentView != null) {
            currentView.onHide();
        }
        currentView = view;
        Node root = view.getRoot();

        container.getChildren().setAll(root);
        Motion.pageEnter(root);
        view.onShow();
    }

    public BaseView getCurrentView() {
        return currentView;
    }
}
