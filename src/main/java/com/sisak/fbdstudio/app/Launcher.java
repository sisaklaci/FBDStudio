package com.sisak.fbdstudio.app;

/**
 * Plain entry point (does not extend {@link javafx.application.Application}).
 * Keeping the jar's Main-Class free of a direct Application subclass avoids the
 * "JavaFX runtime components are missing" launch restriction when run as a fat jar
 * or from a jpackage native launcher.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
