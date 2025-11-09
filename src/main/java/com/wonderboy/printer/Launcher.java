package com.wonderboy.printer;

import com.wonderboy.printer.ui.PrinterUI;

/**
 * A launcher class to work around classpath issues with JavaFX Maven plugins.
 * This class does not extend Application and is used as the main entry point
 * in the JAR manifest.
 */
public class Launcher {
    public static void main(String[] args) {
        PrinterUI.main(args);
    }
}