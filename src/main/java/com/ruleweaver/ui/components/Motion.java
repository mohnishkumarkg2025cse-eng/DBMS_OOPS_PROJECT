package com.ruleweaver.ui.components;

import com.ruleweaver.ui.AppContext;
import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.List;

/**
 * Micro-interaction and fluid animation utilities.
 * Automatically respects the global reducedMotion flag.
 */
public class Motion {

    private static boolean isReduced() {
        return AppContext.getInstance().reducedMotionProperty().get();
    }

    /**
     * Page entrance transition: subtle 12px vertical slide + fade in over 220ms.
     */
    public static void pageEnter(Node node) {
        if (node == null) return;
        if (isReduced()) {
            node.setOpacity(1.0);
            node.setTranslateY(0);
            return;
        }

        node.setOpacity(0.0);
        node.setTranslateY(12.0);

        FadeTransition fade = new FadeTransition(Duration.millis(220), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(220), node);
        slide.setFromY(12.0);
        slide.setToY(0.0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, slide).play();
    }

    /**
     * Modal entrance: scale from 0.96 -> 1.0 with fade in over 200ms.
     */
    public static void modalScaleIn(Node modal) {
        if (modal == null) return;
        if (isReduced()) {
            modal.setOpacity(1.0);
            modal.setScaleX(1.0);
            modal.setScaleY(1.0);
            return;
        }

        modal.setOpacity(0.0);
        modal.setScaleX(0.96);
        modal.setScaleY(0.96);

        FadeTransition fade = new FadeTransition(Duration.millis(200), modal);
        fade.setToValue(1.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(200), modal);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, scale).play();
    }

    /**
     * Staggered fade-up for a collection of cards or list items.
     */
    public static void staggeredFadeUp(List<? extends Node> nodes) {
        if (nodes == null || nodes.isEmpty()) return;
        if (isReduced()) {
            for (Node n : nodes) {
                n.setOpacity(1.0);
                n.setTranslateY(0);
            }
            return;
        }

        int delayMs = 0;
        for (Node node : nodes) {
            node.setOpacity(0.0);
            node.setTranslateY(10.0);

            FadeTransition fade = new FadeTransition(Duration.millis(240), node);
            fade.setToValue(1.0);
            fade.setDelay(Duration.millis(delayMs));

            TranslateTransition slide = new TranslateTransition(Duration.millis(240), node);
            slide.setToY(0.0);
            slide.setDelay(Duration.millis(delayMs));

            new ParallelTransition(fade, slide).play();
            delayMs += 45;
        }
    }

    /**
     * Hover lift effect on interactive cards (2px vertical lift over 150ms).
     */
    public static void attachHoverLift(Node node) {
        if (node == null) return;

        node.setOnMouseEntered(e -> {
            if (!isReduced()) {
                TranslateTransition lift = new TranslateTransition(Duration.millis(150), node);
                lift.setToY(-2.0);
                lift.setInterpolator(Interpolator.EASE_OUT);
                lift.play();
            }
        });

        node.setOnMouseExited(e -> {
            if (!isReduced()) {
                TranslateTransition reset = new TranslateTransition(Duration.millis(150), node);
                reset.setToY(0.0);
                reset.setInterpolator(Interpolator.EASE_OUT);
                reset.play();
            }
        });
    }

    /**
     * Shake animation for input validation failure.
     */
    public static void shake(Node node) {
        if (node == null) return;
        if (isReduced()) return;

        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setByX(8.0);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    /**
     * Numeric count-up animation for KPI metrics over 600ms.
     */
    public static void countUp(Label label, int targetValue) {
        if (label == null) return;
        if (isReduced() || targetValue == 0) {
            label.setText(String.valueOf(targetValue));
            return;
        }

        DoubleProperty countProp = new SimpleDoubleProperty(0);
        countProp.addListener((obs, oldVal, newVal) -> {
            label.setText(String.valueOf(newVal.intValue()));
        });

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(countProp, 0)),
                new KeyFrame(Duration.millis(600), new KeyValue(countProp, targetValue, Interpolator.EASE_OUT))
        );
        timeline.play();
    }

    /**
     * Double / percentage count-up animation for KPI metrics.
     */
    public static void countUpPercent(Label label, double targetPercent) {
        if (label == null) return;
        if (isReduced() || targetPercent == 0.0) {
            label.setText(String.format("%.1f%%", targetPercent));
            return;
        }

        DoubleProperty countProp = new SimpleDoubleProperty(0);
        countProp.addListener((obs, oldVal, newVal) -> {
            label.setText(String.format("%.1f%%", newVal.doubleValue()));
        });

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(countProp, 0.0)),
                new KeyFrame(Duration.millis(600), new KeyValue(countProp, targetPercent, Interpolator.EASE_OUT))
        );
        timeline.play();
    }
}
