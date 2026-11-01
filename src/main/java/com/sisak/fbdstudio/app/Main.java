package com.sisak.fbdstudio.app;

import com.sisak.fbdstudio.i18n.Messages;
import com.sisak.fbdstudio.ui.MainWindow;
import com.sisak.fbdstudio.ui.WindowPlacement;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public final class Main extends Application {

    public static final String APP_NAME = "FBD Studio";
    public static final String APP_VERSION = "1.0.0";
    public static final double DEFAULT_WIDTH = 1366;
    public static final double DEFAULT_HEIGHT = 860;

    @Override
    public void start(Stage primaryStage) {
        Messages.loadSavedLanguageOrDetect();
        MainWindow root = new MainWindow(primaryStage);
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        URL css = Main.class.getResource("/css/light.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        primaryStage.setTitle(APP_NAME);
        primaryStage.setScene(scene);
        // Restores the last window position only if its title bar would land on a screen that is
        // actually connected right now; otherwise centers on the primary screen. Prevents the
        // window from ever reopening somewhere unreachable on a changed multi-monitor layout.
        WindowPlacement.restore(primaryStage, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
