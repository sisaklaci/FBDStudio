package com.sisak.fbdstudio.ui;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

/**
 * Remembers the main window's position/size across runs, but only trusts a saved position if its
 * title-bar strip would actually land on a currently-connected screen. Multi-monitor setups change
 * (a monitor gets unplugged, repositioned, or a different one becomes primary) and a naive restore
 * can otherwise place the window where no title bar is reachable to drag it back.
 */
public final class WindowPlacement {

    private static final Preferences PREFS = Preferences.userNodeForPackage(WindowPlacement.class);
    private static final String KEY_X = "window.x";
    private static final String KEY_Y = "window.y";
    private static final String KEY_W = "window.w";
    private static final String KEY_H = "window.h";
    private static final String KEY_MAX = "window.maximized";
    private static final double TITLE_BAR_PROBE_HEIGHT = 32;

    private WindowPlacement() {
    }

    public static void restore(Stage stage, double defaultWidth, double defaultHeight) {
        double x = PREFS.getDouble(KEY_X, Double.NaN);
        double y = PREFS.getDouble(KEY_Y, Double.NaN);
        double w = PREFS.getDouble(KEY_W, defaultWidth);
        double h = PREFS.getDouble(KEY_H, defaultHeight);
        boolean maximized = PREFS.getBoolean(KEY_MAX, false);

        stage.setWidth(w);
        stage.setHeight(h);

        boolean hasSaved = !Double.isNaN(x) && !Double.isNaN(y);
        if (hasSaved && isTitleBarReachable(x, y, w)) {
            stage.setX(x);
            stage.setY(y);
            stage.setMaximized(maximized);
        } else {
            // No usable saved position (first run, or the monitor layout changed since last run):
            // always fall back to a safely centered window on the primary screen.
            stage.setMaximized(false);
            stage.centerOnScreen();
        }

        stage.xProperty().addListener((o, a, b) -> persist(stage));
        stage.yProperty().addListener((o, a, b) -> persist(stage));
        stage.widthProperty().addListener((o, a, b) -> persist(stage));
        stage.heightProperty().addListener((o, a, b) -> persist(stage));
        stage.maximizedProperty().addListener((o, a, b) -> persist(stage));
    }

    /** Resets to a centered, default-sized window on the primary screen (manual recovery action). */
    public static void center(Stage stage, double defaultWidth, double defaultHeight) {
        stage.setMaximized(false);
        stage.setWidth(defaultWidth);
        stage.setHeight(defaultHeight);
        stage.centerOnScreen();
    }

    private static boolean isTitleBarReachable(double x, double y, double w) {
        Rectangle2D probe = new Rectangle2D(x, y, Math.max(50, w), TITLE_BAR_PROBE_HEIGHT);
        return !Screen.getScreensForRectangle(probe).isEmpty();
    }

    private static void persist(Stage stage) {
        PREFS.putBoolean(KEY_MAX, stage.isMaximized());
        if (!stage.isMaximized()) {
            PREFS.putDouble(KEY_X, stage.getX());
            PREFS.putDouble(KEY_Y, stage.getY());
            PREFS.putDouble(KEY_W, stage.getWidth());
            PREFS.putDouble(KEY_H, stage.getHeight());
        }
    }
}
