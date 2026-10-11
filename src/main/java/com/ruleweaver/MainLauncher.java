package com.ruleweaver;

import com.ruleweaver.ui.MainApp;

/**
 * Stable, non-Application entry point for the shaded executable JAR.
 */
public final class MainLauncher {

    private MainLauncher() {
    }

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
