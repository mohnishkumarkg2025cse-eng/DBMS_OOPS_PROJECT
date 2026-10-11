package com.ruleweaver.ui;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.application.Platform;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.collections.ListChangeListener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.Set;

/** Opt-in diagnostics for stylesheet provenance and live JavaFX node styling. */
public final class StyleDebug {

    private static final Path SOURCE_STYLESHEET =
            Path.of("src", "main", "resources", "css", "style.css").toAbsolutePath().normalize();
    private static final Set<Window> OBSERVED_WINDOWS =
            java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    private StyleDebug() {
    }

    public static void install() {
        OBSERVED_WINDOWS.addAll(Window.getWindows());
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                for (Window window : change.getAddedSubList()) {
                    if (OBSERVED_WINDOWS.add(window)) {
                        Platform.runLater(() -> dumpWindow("created", window));
                    }
                }
            }
        });
    }

    public static void dump(String view, Stage stage) {
        System.out.println("STYLEDEBUG view=" + view);
        Set<Scene> scenes = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (Window window : Window.getWindows()) {
            Scene scene = window.getScene();
            if (scene == null || !scenes.add(scene)) {
                continue;
            }
            dumpScene(windowKind(window), scene);
        }
        if (stage.getScene() != null && scenes.add(stage.getScene())) {
            dumpScene("Stage", stage.getScene());
        }
    }

    private static void dumpWindow(String event, Window window) {
        Scene scene = window.getScene();
        if (scene != null) {
            dumpScene(event + " " + windowKind(window), scene);
        }
    }

    private static String windowKind(Window window) {
        return window instanceof PopupWindow ? "PopupWindow"
                : window instanceof Stage ? "Stage" : window.getClass().getSimpleName();
    }

    private static void dumpScene(String owner, Scene scene) {
        System.out.println("STYLEDEBUG scene owner=" + owner + " stylesheets=" + scene.getStylesheets().size());
        for (String stylesheet : scene.getStylesheets()) {
            describeStylesheet(stylesheet);
        }
        Parent root = scene.getRoot();
        root.applyCss();
        dumpSubScenes(root);
        dumpTargets(root);
    }

    private static void describeStylesheet(String stylesheet) {
        try {
            Path path = Path.of(java.net.URI.create(stylesheet)).toAbsolutePath().normalize();
            byte[] bytes = Files.readAllBytes(path);
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            String sourceHash = Files.exists(SOURCE_STYLESHEET)
                    ? HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(Files.readAllBytes(SOURCE_STYLESHEET)))
                    : "missing";
            System.out.println("STYLEDEBUG stylesheetPath=" + path + " lines=" + Files.readAllLines(path).size()
                    + " sha256=" + hash + " equalsSourceFile=" + path.equals(SOURCE_STYLESHEET)
                    + " sourceContentMatches=" + hash.equals(sourceHash));
        } catch (Exception exception) {
            String name = stylesheet.substring(stylesheet.lastIndexOf('/') + 1);
            System.out.println("STYLEDEBUG stylesheet=" + name + " resolved=false type="
                    + exception.getClass().getSimpleName());
        }
    }

    private static void dumpSubScenes(Node root) {
        ArrayDeque<Node> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            Node node = pending.pop();
            if (node instanceof SubScene subScene) {
                System.out.println("STYLEDEBUG SubScene rootStylesheets="
                        + subScene.getRoot().getStylesheets().size()
                        + " root=" + describe(subScene.getRoot()));
                subScene.getRoot().getStylesheets().forEach(StyleDebug::describeStylesheet);
                dumpTargets(subScene.getRoot());
            }
            if (node instanceof Parent parent) {
                parent.getChildrenUnmodifiable().forEach(pending::push);
            }
        }
    }

    private static void dumpTargets(Node root) {
        ArrayDeque<Node> pending = new ArrayDeque<>();
        Set<Node> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        pending.push(root);
        while (!pending.isEmpty()) {
            Node node = pending.pop();
            if (visited.add(node) && isTarget(node)) {
                System.out.println("STYLEDEBUG target");
                for (Node current = node; current != null; current = current.getParent()) {
                    System.out.println("STYLEDEBUG   " + describe(current));
                    if (current == root) {
                        break;
                    }
                }
            }
            if (node instanceof Parent parent) {
                parent.getChildrenUnmodifiable().forEach(pending::push);
            }
        }
    }

    private static boolean isTarget(Node node) {
        if (node instanceof ScrollPane || node instanceof ListView
                || node.getClass().getSimpleName().equals("KpiCard")
                || node.getStyleClass().contains("scroll-bar")
                || isScrollbarPart(node)
                || node.getStyleClass().contains("kpi-icon-box")
                || node.getStyleClass().stream().anyMatch(styleClass -> styleClass.startsWith("kpi-glyph-"))
                || node.getStyleClass().contains("policy-condition-row")
                || node.getStyleClass().contains("dash-page")) {
            return true;
        }
        return node instanceof Label label && Set.of("DEFINED POLICIES", "VERSION HISTORY",
                "ACTIVE EVALUATION RULES").contains(label.getText());
    }

    private static String describe(Node node) {
        String background = node instanceof Region region ? describeBackground(region.getBackground()) : "n/a";
        String text = node instanceof Label label ? " text=" + quote(label.getText()) : "";
        String style = node.getStyle().isBlank() ? "<none>" : quote(node.getStyle());
        String size = node instanceof Region region
                ? " prefSize=" + region.prefWidth(-1) + "x" + region.prefHeight(-1) : "";
        return node.getClass().getSimpleName() + " classes=" + node.getStyleClass()
                + " inlineStyle=" + style + " background=" + background + size + text;
    }

    private static boolean isScrollbarPart(Node node) {
        if (!node.getStyleClass().contains("thumb")
                && !node.getStyleClass().contains("track")
                && !node.getStyleClass().contains("increment-button")
                && !node.getStyleClass().contains("decrement-button")) {
            return false;
        }
        for (Node parent = node.getParent(); parent != null; parent = parent.getParent()) {
            if (parent.getStyleClass().contains("scroll-bar")) {
                return true;
            }
        }
        return false;
    }

    private static String describeBackground(Background background) {
        if (background == null || background.getFills().isEmpty()) {
            return "<none>";
        }
        return background.getFills().stream().map(BackgroundFill::getFill)
                .map(Object::toString).toList().toString();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }
}
