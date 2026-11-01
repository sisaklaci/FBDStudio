package com.sisak.fbdstudio.ui;

import com.sisak.fbdstudio.app.Main;
import com.sisak.fbdstudio.engine.SimulationEngine;
import com.sisak.fbdstudio.i18n.Language;
import com.sisak.fbdstudio.i18n.Messages;
import com.sisak.fbdstudio.model.POU;
import com.sisak.fbdstudio.model.POUKind;
import com.sisak.fbdstudio.model.Project;
import com.sisak.fbdstudio.persistence.ProjectIO;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Application shell: menu/toolbar, palette, per-POU diagram tabs, inspector, variables, status bar. */
public class MainWindow extends BorderPane {

    private final Stage stage;
    private final UndoManager undo = new UndoManager();
    private final BlockPaletteView palette = new BlockPaletteView();
    private final InspectorPanel inspector = new InspectorPanel(undo, this::onModelChanged);
    private final VariablesTableView variablesTable = new VariablesTableView(this::onModelChanged);
    private final TabPane tabPane = new TabPane();
    private final Label statusLabel = new Label();
    private final Label zoomLabel = new Label("100%");
    private final Label scanLabel = new Label("");

    /** Re-applies translated text to already-built controls; fired on every language change. */
    private final List<Runnable> i18n = new ArrayList<>();

    private final Map<POU, DiagramView> diagramViews = new HashMap<>();
    private final Map<POU, SimulationEngine> engines = new HashMap<>();

    private Project project;
    private File currentFile;
    private AnimationTimer simTimer;
    private long lastTickNanos;

    public MainWindow(Stage stage) {
        this.stage = stage;
        setTop(buildTopArea());
        setLeft(palette);
        setRight(buildRightArea());
        setBottom(buildStatusBar());
        loadProject(ProjectIO.newBlankProject("Untitled"));
        retranslate();
        Messages.addListener(this::retranslate);
    }

    private void retranslate() {
        for (Runnable r : i18n) {
            r.run();
        }
        statusLabel.setText(Messages.get("status.ready"));
    }

    private VBox buildTopArea() {
        return new VBox(buildMenuBar(), buildToolBar());
    }

    private MenuBar buildMenuBar() {
        Menu file = new Menu();
        MenuItem newProj = item(this::actionNew);
        MenuItem open = item(this::actionOpen);
        MenuItem save = item(this::actionSave);
        MenuItem saveAs = item(this::actionSaveAs);
        MenuItem examples = item(this::actionOpenExample);
        MenuItem exit = item(() -> stage.close());
        file.getItems().addAll(newProj, open, save, saveAs, new SeparatorMenuItem(), examples, new SeparatorMenuItem(), exit);
        i18n.add(() -> {
            file.setText(Messages.get("menu.file"));
            newProj.setText(Messages.get("menu.file.new"));
            open.setText(Messages.get("menu.file.open"));
            save.setText(Messages.get("menu.file.save"));
            saveAs.setText(Messages.get("menu.file.saveAs"));
            examples.setText(Messages.get("menu.file.openExample"));
            exit.setText(Messages.get("menu.file.exit"));
        });

        Menu edit = new Menu();
        MenuItem undoItem = item(undo::undo);
        undoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        undoItem.disableProperty().bind(undo.canUndoProperty().not());
        MenuItem redoItem = item(undo::redo);
        redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        redoItem.disableProperty().bind(undo.canRedoProperty().not());
        edit.getItems().addAll(undoItem, redoItem);
        i18n.add(() -> {
            edit.setText(Messages.get("menu.edit"));
            undoItem.setText(Messages.get("menu.edit.undo"));
            redoItem.setText(Messages.get("menu.edit.redo"));
        });

        Menu pouMenu = new Menu();
        MenuItem newProgram = item(() -> actionNewPou(POUKind.PROGRAM));
        MenuItem newFb = item(() -> actionNewPou(POUKind.FUNCTION_BLOCK));
        MenuItem newFunction = item(() -> actionNewPou(POUKind.FUNCTION));
        pouMenu.getItems().addAll(newProgram, newFb, newFunction);
        i18n.add(() -> {
            pouMenu.setText(Messages.get("menu.pou"));
            newProgram.setText(Messages.get("menu.pou.newProgram"));
            newFb.setText(Messages.get("menu.pou.newFunctionBlock"));
            newFunction.setText(Messages.get("menu.pou.newFunction"));
        });

        Menu sim = new Menu();
        MenuItem run = item(this::actionRun);
        MenuItem pause = item(this::actionPause);
        MenuItem step = item(this::actionStep);
        MenuItem reset = item(this::actionReset);
        sim.getItems().addAll(run, pause, step, reset);
        i18n.add(() -> {
            sim.setText(Messages.get("menu.simulation"));
            run.setText(Messages.get("menu.simulation.run"));
            pause.setText(Messages.get("menu.simulation.pause"));
            step.setText(Messages.get("menu.simulation.step"));
            reset.setText(Messages.get("menu.simulation.reset"));
        });

        Menu window = new Menu();
        MenuItem center = item(this::actionCenterWindow);
        center.setAccelerator(new KeyCodeCombination(KeyCode.F9));
        window.getItems().add(center);
        i18n.add(() -> {
            window.setText(Messages.get("menu.window"));
            center.setText(Messages.get("menu.window.center"));
        });

        Menu help = new Menu();
        MenuItem about = item(this::actionAbout);
        help.getItems().add(about);
        i18n.add(() -> {
            help.setText(Messages.get("menu.help"));
            about.setText(Messages.get("menu.help.about"));
        });

        Menu language = new Menu();
        for (Language lang : Language.values()) {
            MenuItem langItem = new MenuItem(lang.nativeName());
            langItem.setGraphic(FlagIcon.create(lang));
            langItem.setOnAction(e -> switchLanguage(lang));
            language.getItems().add(langItem);
        }
        i18n.add(() -> language.setText(Messages.get("menu.language")));

        return new MenuBar(file, edit, pouMenu, sim, window, help, language);
    }

    private ToolBar buildToolBar() {
        Button run = new Button();
        run.setOnAction(e -> actionRun());
        Button pause = new Button();
        pause.setOnAction(e -> actionPause());
        Button step = new Button();
        step.setOnAction(e -> actionStep());
        Button reset = new Button();
        reset.setOnAction(e -> actionReset());
        Button zoomFit = new Button();
        zoomFit.setOnAction(e -> currentDiagram().ifPresent(DiagramView::zoomToFit));
        i18n.add(() -> {
            run.setText(Messages.get("toolbar.run"));
            pause.setText(Messages.get("toolbar.pause"));
            step.setText(Messages.get("toolbar.step"));
            reset.setText(Messages.get("toolbar.reset"));
            zoomFit.setText(Messages.get("toolbar.fit"));
        });

        ComboBox<Language> languageBox = buildLanguagePicker();

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToolBar bar = new ToolBar(run, pause, step, reset, new Separator(), zoomFit);
        bar.getItems().addAll(spacer, languageBox);
        return bar;
    }

    private ComboBox<Language> buildLanguagePicker() {
        ComboBox<Language> box = new ComboBox<>(FXCollections.observableArrayList(Language.values()));
        box.setCellFactory(lv -> languageCell());
        box.setButtonCell(languageCell());
        Language current = Language.fromCode(Messages.getLocale().getLanguage());
        box.getSelectionModel().select(current);
        box.valueProperty().addListener((obs, old, val) -> {
            if (val != null && val != old) {
                switchLanguage(val);
            }
        });
        return box;
    }

    private ListCell<Language> languageCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Language item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(FlagIcon.create(item));
                    setText(item.nativeName());
                }
            }
        };
    }

    private void switchLanguage(Language lang) {
        Messages.setLocale(lang.locale());
        Messages.saveLanguage(lang);
    }

    private MenuItem item(Runnable action) {
        MenuItem item = new MenuItem();
        item.setOnAction(e -> action.run());
        return item;
    }

    private SplitPane buildRightArea() {
        VBox box = new VBox(inspector, variablesTable);
        box.setPrefWidth(340);
        return new SplitPane(box);
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(16, statusLabel, zoomLabel, scanLabel);
        bar.setStyle("-fx-padding: 4 10 4 10; -fx-background-color: #e7eaf0;");
        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        return bar;
    }

    // ---- project / tab management ---------------------------------------

    private void loadProject(Project project) {
        stopSimulation();
        this.project = project;
        tabPane.getTabs().clear();
        diagramViews.clear();
        engines.clear();
        undo.clear();
        for (POU pou : project.getPous()) {
            addTabFor(pou);
        }
        setCenter(tabPane);
        stage.setTitle(Main.APP_NAME + " - " + project.getName());
        statusLabel.setText(Messages.get("status.loaded", project.getName()));
    }

    private void addTabFor(POU pou) {
        DiagramView diagram = new DiagramView(pou, undo);
        diagram.setOnBlockSelected(inspector::setSelection);
        diagram.setOnModelChanged(this::onModelChanged);
        diagramViews.put(pou, diagram);

        Tab tab = new Tab(pou.getName(), diagram);
        tab.setUserData(pou);
        tab.setClosable(false);
        tabPane.getTabs().add(tab);
        // Defer to runLater: the viewport has no real width/height until after this layout pass,
        // which zoomToFit needs to compute a sensible initial zoom/pan.
        javafx.application.Platform.runLater(diagram::zoomToFit);
    }

    private Optional<POU> currentPou() {
        Tab tab = tabPane.getSelectionModel().getSelectedItem();
        return tab == null ? Optional.empty() : Optional.of((POU) tab.getUserData());
    }

    private Optional<DiagramView> currentDiagram() {
        return currentPou().map(diagramViews::get);
    }

    {
        // Keep the inspector/variables panel in sync with the active tab.
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            currentPou().ifPresent(pou -> {
                inspector.setPou(pou);
                inspector.setSelection(null);
                variablesTable.setPou(pou);
            });
        });
    }

    private void onModelChanged() {
        currentPou().ifPresent(pou -> {
            SimulationEngine engine = engines.get(pou);
            if (engine != null) {
                engine.recompile();
            }
        });
        variablesTable.refresh();
    }

    // ---- actions ----------------------------------------------------------

    private void actionNew() {
        loadProject(ProjectIO.newBlankProject("Untitled"));
        currentFile = null;
    }

    private void actionOpen() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("FBD Studio project", "*.fbdproj"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            try {
                loadProject(ProjectIO.load(file));
                currentFile = file;
            } catch (IOException e) {
                showError(Messages.get("dialog.error.open.title"), e.getMessage());
            }
        }
    }

    private void actionSave() {
        if (currentFile == null) {
            actionSaveAs();
            return;
        }
        try {
            ProjectIO.save(project, currentFile);
            statusLabel.setText(Messages.get("status.saved", currentFile.getName()));
        } catch (IOException e) {
            showError(Messages.get("dialog.error.save.title"), e.getMessage());
        }
    }

    private void actionSaveAs() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("FBD Studio project", "*.fbdproj"));
        chooser.setInitialFileName(project.getName() + ".fbdproj");
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            currentFile = file;
            actionSave();
        }
    }

    private void actionOpenExample() {
        ExampleGalleryDialog.chooseExample(stage).ifPresent(url -> {
            try (var in = url.openStream()) {
                Project loaded = ProjectIO.load(in);
                loadProject(loaded);
                currentFile = null;
            } catch (IOException e) {
                showError(Messages.get("dialog.error.openExample.title"), e.getMessage());
            }
        });
    }

    private void actionNewPou(POUKind kind) {
        String kindName = Messages.get("pouKind." + kind.name());
        TextInputDialog dialog = new TextInputDialog(kind.name().toLowerCase());
        dialog.setHeaderText(Messages.get("dialog.newPou.header", kindName));
        Optional<String> name = dialog.showAndWait();
        name.ifPresent(n -> {
            if (n.isBlank()) return;
            POU pou = new POU(n.trim(), kind);
            project.getPous().add(pou);
            addTabFor(pou);
            tabPane.getSelectionModel().select(tabPane.getTabs().size() - 1);
        });
    }

    private void actionRun() {
        currentPou().ifPresent(pou -> {
            SimulationEngine engine = engines.computeIfAbsent(pou, SimulationEngine::new);
            engine.setRunning(true);
            diagramViews.get(pou).setSimulationEngine(engine);
            startSimulationLoopIfNeeded();
            statusLabel.setText(Messages.get("status.simRunning", pou.getName()));
        });
    }

    private void actionPause() {
        currentPou().flatMap(p -> Optional.ofNullable(engines.get(p))).ifPresent(e -> e.setRunning(false));
        statusLabel.setText(Messages.get("status.simPaused"));
    }

    private void actionStep() {
        currentPou().ifPresent(pou -> {
            SimulationEngine engine = engines.computeIfAbsent(pou, SimulationEngine::new);
            diagramViews.get(pou).setSimulationEngine(engine);
            engine.step(100);
            diagramViews.get(pou).refreshLiveOverlay();
            scanLabel.setText(Messages.get("status.scan", engine.getScanCount()));
        });
    }

    private void actionReset() {
        currentPou().ifPresent(pou -> {
            SimulationEngine engine = engines.get(pou);
            if (engine != null) {
                engine.setRunning(false);
                engine.reset();
                diagramViews.get(pou).clearLiveOverlay();
                scanLabel.setText(Messages.get("status.scan", 0));
            }
        });
    }

    private void startSimulationLoopIfNeeded() {
        if (simTimer != null) return;
        lastTickNanos = 0;
        simTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTickNanos == 0) {
                    lastTickNanos = now;
                    return;
                }
                long elapsedMs = (now - lastTickNanos) / 1_000_000;
                if (elapsedMs < 100) return;
                lastTickNanos = now;
                currentPou().ifPresent(pou -> {
                    SimulationEngine engine = engines.get(pou);
                    DiagramView view = diagramViews.get(pou);
                    if (engine != null && engine.isRunning() && view != null) {
                        engine.step(elapsedMs);
                        view.refreshLiveOverlay();
                        scanLabel.setText(Messages.get("status.scan", engine.getScanCount()));
                    }
                });
            }
        };
        simTimer.start();
    }

    private void stopSimulation() {
        if (simTimer != null) {
            simTimer.stop();
            simTimer = null;
        }
    }

    private void actionCenterWindow() {
        WindowPlacement.center(stage, Main.DEFAULT_WIDTH, Main.DEFAULT_HEIGHT);
    }

    private void actionAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Messages.get("about.title"));
        alert.setHeaderText(Main.APP_NAME + " " + Main.APP_VERSION);
        alert.setContentText(Messages.get("about.description")
                + "\n\n" + Messages.get("about.createdBy") + " Sisak László · helmetsoft.com");
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "" : message, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }
}
