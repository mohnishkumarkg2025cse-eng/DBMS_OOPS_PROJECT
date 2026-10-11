package com.ruleweaver;

import com.ruleweaver.ui.MainApp;
import javafx.application.Application;

/**
 * Bootstrap entry point for RuleWeaver X application.
 * Required to launch JavaFX without module-path limitations.
 */
public class Main {

    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}
