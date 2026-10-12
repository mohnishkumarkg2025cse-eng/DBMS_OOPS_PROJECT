package com.ruleweaver.ui.components;

import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Centralized Material Design 2 icons rendered with Ikonli.
 */
public final class Icons {

    public static final String DASHBOARD = "dashboard";
    public static final String APPLICANTS = "applicants";
    public static final String POLICIES = "policies";
    public static final String EVALUATION = "evaluation";
    public static final String COMPARISON = "comparison";
    public static final String PLUS = "plus";
    public static final String TRASH = "trash";
    public static final String EDIT = "edit";
    public static final String SEARCH = "search";
    public static final String CLOSE = "close";
    public static final String REFRESH = "refresh";
    public static final String SWAP = "swap";
    public static final String EYE = "eye";
    public static final String PLAY = "play";
    public static final String CHECK = "check";
    public static final String SPARKLES = "sparkles";
    public static final String ARROW_UP = "arrow-up";
    public static final String ARROW_DOWN = "arrow-down";
    public static final String ARROW_RIGHT = "arrow-right";
    public static final String INFO = "info";

    private Icons() {
    }

    public static FontIcon create(String name, String styleClass) {
        FontIcon icon = create(name, 1, null);
        if (styleClass != null && !styleClass.isBlank()) {
            icon.getStyleClass().add(styleClass);
        }
        return icon;
    }

    public static FontIcon create(String name, double scale, String colorClass) {
        FontIcon icon = new FontIcon(iconLiteral(name));
        icon.setIconSize((int) Math.round(24 * scale));
        String styleClass = colorStyleClass(colorClass);
        if (styleClass != null) {
            icon.getStyleClass().add(styleClass);
        }
        return icon;
    }

    private static String iconLiteral(String name) {
        return switch (name) {
            case DASHBOARD -> "mdi2v-view-dashboard";
            case APPLICANTS -> "mdi2a-account-multiple";
            case POLICIES -> "mdi2f-file-document-multiple";
            case EVALUATION -> "mdi2c-check";
            case COMPARISON -> "mdi2c-compare";
            case PLUS -> "mdi2p-plus";
            case TRASH -> "mdi2d-delete";
            case EDIT -> "mdi2p-pencil";
            case SEARCH -> "mdi2m-magnify";
            case CLOSE -> "mdi2c-close";
            case REFRESH -> "mdi2r-refresh";
            case SWAP -> "mdi2s-swap-horizontal";
            case EYE -> "mdi2e-eye";
            case PLAY -> "mdi2p-play";
            case CHECK -> "mdi2c-check";
            case SPARKLES -> "mdi2c-creation";
            case ARROW_UP -> "mdi2a-arrow-up";
            case ARROW_DOWN -> "mdi2a-arrow-down";
            case ARROW_RIGHT -> "mdi2a-arrow-right";
            case INFO -> "mdi2i-information-outline";
            default -> throw new IllegalArgumentException("Unknown icon: " + name);
        };
    }

    private static String colorStyleClass(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        return switch (color.toLowerCase()) {
            case "#11111b" -> "icon-fill-background";
            case "#9aa0c3" -> "icon-fill-secondary";
            case "#5c607f" -> "icon-fill-disabled";
            case "#a6e3a1" -> "icon-fill-success";
            case "#f38ba8" -> "icon-fill-danger";
            case "#f9e2af" -> "icon-fill-warning";
            case "#74c7ec" -> "icon-fill-info";
            case "#cdd6f4" -> "icon-fill-primary";
            case "#cba6f7" -> "icon-fill-accent-end";
            default -> "icon-fill-accent";
        };
    }
}
