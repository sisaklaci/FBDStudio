package com.sisak.fbdstudio.ui;

import com.sisak.fbdstudio.engine.SimulationEngine;
import com.sisak.fbdstudio.library.BlockLibrary;
import com.sisak.fbdstudio.library.BlockTypeDefinition;
import com.sisak.fbdstudio.model.BlockInstance;
import com.sisak.fbdstudio.model.Connection;
import com.sisak.fbdstudio.model.DataType;
import com.sisak.fbdstudio.model.Network;
import com.sisak.fbdstudio.model.PinRef;
import com.sisak.fbdstudio.model.POU;
import com.sisak.fbdstudio.model.Variable;
import com.sisak.fbdstudio.model.VariableKind;
import com.sisak.fbdstudio.ui.commands.AddBlockCommand;
import com.sisak.fbdstudio.ui.commands.AddConnectionCommand;
import com.sisak.fbdstudio.ui.commands.MoveBlockCommand;
import com.sisak.fbdstudio.ui.commands.RemoveBlockCommand;
import com.sisak.fbdstudio.ui.commands.RemoveConnectionCommand;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.transform.Scale;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** Editable + simulatable FBD canvas for one POU's network. */
public class DiagramView extends BorderPane
        implements BlockNodeView.Listener, VariablePinView.Listener, WireView.Listener {

    public static final double CONTENT_WIDTH = 4000;
    public static final double CONTENT_HEIGHT = 2600;
    private static final double GRID_SIZE = 20;
    private static final double LEFT_VAR_X = 30;
    private static final double DEFAULT_OUTPUT_COLUMN_X = 420;
    private static final double OUTPUT_COLUMN_MARGIN = 160;
    private static final double APPROX_BLOCK_WIDTH = 140;

    private final POU pou;
    private final UndoManager undo;
    private final BlockLibrary library = BlockLibrary.getInstance();

    private final Pane content = new Pane();
    private final Group zoomGroup;
    private final Group panGroup;
    private final Pane viewport = new Pane();
    private final Scale scaleTransform = new Scale(1, 1, 0, 0);
    private double panX = 0, panY = 0, zoom = 1.0;
    private double panDragStartX, panDragStartY, panStartTX, panStartTY;
    private boolean panning;

    private final Map<String, BlockNodeView> blockViews = new LinkedHashMap<>();
    private final Map<String, VariablePinView> inputVarViews = new LinkedHashMap<>();
    private final Map<String, VariablePinView> outputVarViews = new LinkedHashMap<>();
    private final Map<String, WireView> wireViews = new LinkedHashMap<>();

    private String selectedBlockId;
    private WireView selectedWire;
    private double dragOriginX, dragOriginY;

    private PinHandle pendingSource;
    private Path previewWire;

    private SimulationEngine simulationEngine;
    private Consumer<BlockInstance> onBlockSelected = b -> {
    };
    private Runnable onModelChanged = () -> {
    };

    public DiagramView(POU pou, UndoManager undo) {
        this.pou = pou;
        this.undo = undo;

        content.setPrefSize(CONTENT_WIDTH, CONTENT_HEIGHT);
        content.setMinSize(CONTENT_WIDTH, CONTENT_HEIGHT);
        content.getChildren().add(buildGrid());

        zoomGroup = new Group(content);
        zoomGroup.getTransforms().add(scaleTransform);
        panGroup = new Group(zoomGroup);

        viewport.getChildren().add(panGroup);
        viewport.setStyle("-fx-background-color: #eef1f6;");
        setCenter(viewport);
        viewport.setClip(new javafx.scene.shape.Rectangle(0, 0, 4000, 4000) {{
            widthProperty().bind(viewport.widthProperty());
            heightProperty().bind(viewport.heightProperty());
        }});

        installViewportGestures();
        installDragAndDrop();
        installKeyHandlers();

        renderAll();
        com.sisak.fbdstudio.i18n.Messages.addListener(this::renderAll);
    }

    private Canvas buildGrid() {
        Canvas canvas = new Canvas(CONTENT_WIDTH, CONTENT_HEIGHT);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, CONTENT_WIDTH, CONTENT_HEIGHT);
        g.setStroke(Color.web("#dfe4ec"));
        g.setLineWidth(1);
        for (double x = 0; x <= CONTENT_WIDTH; x += GRID_SIZE) {
            g.strokeLine(x, 0, x, CONTENT_HEIGHT);
        }
        for (double y = 0; y <= CONTENT_HEIGHT; y += GRID_SIZE) {
            g.strokeLine(0, y, CONTENT_WIDTH, y);
        }
        return canvas;
    }

    // ---- zoom / pan ----------------------------------------------------

    private void installViewportGestures() {
        viewport.setOnScroll(e -> {
            double factor = e.getDeltaY() > 0 ? 1.1 : 1 / 1.1;
            double newZoom = Math.max(0.25, Math.min(3.0, zoom * factor));
            double mx = e.getX(), my = e.getY();
            double contentX = (mx - panX) / zoom;
            double contentY = (my - panY) / zoom;
            zoom = newZoom;
            panX = mx - contentX * zoom;
            panY = my - contentY * zoom;
            applyTransform();
            e.consume();
        });
        viewport.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getTarget() == viewport) {
                panning = true;
                panDragStartX = e.getSceneX();
                panDragStartY = e.getSceneY();
                panStartTX = panX;
                panStartTY = panY;
                clearSelection();
            }
        });
        viewport.setOnMouseDragged(e -> {
            if (panning) {
                panX = panStartTX + (e.getSceneX() - panDragStartX);
                panY = panStartTY + (e.getSceneY() - panDragStartY);
                applyTransform();
            }
        });
        viewport.setOnMouseReleased(e -> panning = false);
    }

    private void applyTransform() {
        scaleTransform.setX(zoom);
        scaleTransform.setY(zoom);
        panGroup.setTranslateX(panX);
        panGroup.setTranslateY(panY);
    }

    /** Scales and pans so every block and I/O pin on the canvas is visible at once. */
    public void zoomToFit() {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean any = false;
        for (Node n : content.getChildren()) {
            if (n instanceof BlockNodeView || n instanceof VariablePinView) {
                Bounds b = n.getBoundsInParent();
                minX = Math.min(minX, b.getMinX());
                minY = Math.min(minY, b.getMinY());
                maxX = Math.max(maxX, b.getMaxX());
                maxY = Math.max(maxY, b.getMaxY());
                any = true;
            }
        }
        if (!any) {
            zoom = 1.0;
            panX = 20;
            panY = 20;
            applyTransform();
            return;
        }
        double margin = 40;
        minX -= margin;
        minY -= margin;
        maxX += margin;
        maxY += margin;
        double contentW = Math.max(50, maxX - minX);
        double contentH = Math.max(50, maxY - minY);
        double viewW = viewport.getWidth() > 0 ? viewport.getWidth() : 900;
        double viewH = viewport.getHeight() > 0 ? viewport.getHeight() : 600;
        zoom = Math.max(0.25, Math.min(1.5, Math.min(viewW / contentW, viewH / contentH)));
        panX = -minX * zoom + Math.max(0, (viewW - contentW * zoom) / 2);
        panY = -minY * zoom + Math.max(0, (viewH - contentH * zoom) / 2);
        applyTransform();
    }

    // ---- drag and drop from palette ------------------------------------

    private void installDragAndDrop() {
        content.setOnDragOver(e -> {
            if (e.getDragboard().hasString() && library.contains(e.getDragboard().getString())) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        content.setOnDragDropped(e -> {
            String typeId = e.getDragboard().getString();
            BlockTypeDefinition type = library.get(typeId);
            if (type != null) {
                double x = snap(e.getX() - 60);
                double y = snap(e.getY() - 30);
                BlockInstance instance = new BlockInstance(typeId, x, y);
                if (type.isStateful()) {
                    instance.setInstanceName(typeId.toLowerCase() + "_" + (pou.getNetwork().getBlocks().size() + 1));
                }
                undo.perform(new AddBlockCommand(pou.getNetwork(), instance, this::renderAll));
                onModelChanged.run();
            }
            e.setDropCompleted(true);
            e.consume();
        });
    }

    private double snap(double v) {
        return Math.round(v / GRID_SIZE) * GRID_SIZE;
    }

    private void installKeyHandlers() {
        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                deleteSelection();
            }
        });
    }

    private void deleteSelection() {
        if (selectedBlockId != null) {
            BlockInstance inst = pou.getNetwork().findBlock(selectedBlockId).orElse(null);
            if (inst != null) {
                undo.perform(new RemoveBlockCommand(pou.getNetwork(), inst, this::renderAll));
                onModelChanged.run();
            }
            selectedBlockId = null;
        } else if (selectedWire != null) {
            undo.perform(new RemoveConnectionCommand(pou.getNetwork(), selectedWire.getConnection(), this::renderAll));
            onModelChanged.run();
            selectedWire = null;
        }
    }

    // ---- rendering -------------------------------------------------------

    public void renderAll() {
        content.getChildren().removeIf(n -> n instanceof BlockNodeView || n instanceof VariablePinView || n instanceof WireView);
        blockViews.clear();
        inputVarViews.clear();
        outputVarViews.clear();
        wireViews.clear();

        double outputColumnX = computeOutputColumnX();
        int inRow = 0, outRow = 0;
        for (Variable v : pou.getVariables()) {
            if (v.getKind() == VariableKind.VAR_INPUT) {
                VariablePinView pv = new VariablePinView(v, true, this);
                pv.setLayoutX(LEFT_VAR_X);
                pv.setLayoutY(40 + inRow++ * 44);
                inputVarViews.put(v.getName(), pv);
                content.getChildren().add(pv);
            } else if (v.getKind() == VariableKind.VAR_OUTPUT) {
                VariablePinView pv = new VariablePinView(v, false, this);
                pv.setLayoutX(outputColumnX);
                pv.setLayoutY(40 + outRow++ * 44);
                outputVarViews.put(v.getName(), pv);
                content.getChildren().add(pv);
            }
        }

        for (BlockInstance instance : pou.getNetwork().getBlocks()) {
            BlockTypeDefinition type = library.get(instance.getTypeId());
            if (type == null) continue;
            int pinCount = type.isVariadicInputs()
                    ? (instance.getExtraPinCount() > 0 ? instance.getExtraPinCount() : type.getMinInputs())
                    : type.getInputs().size();
            BlockNodeView view = new BlockNodeView(instance, type, pinCount, this);
            view.setLayoutX(instance.getX());
            view.setLayoutY(instance.getY());
            view.setSelected(instance.getId().equals(selectedBlockId));
            blockViews.put(instance.getId(), view);
            content.getChildren().add(view);
        }

        for (Connection c : pou.getNetwork().getConnections()) {
            DataType type = resolveConnectionType(c);
            WireView wire = new WireView(c, type, this);
            wireViews.put(c.getId(), wire);
            content.getChildren().add(wire);
        }

        // Newly added blocks/pins have not been through a layout pass yet, so their pin circles'
        // localToScene() would still report stale/zero positions (everything collapsed near each
        // node's top-left corner) if we read them right now — forcing layout here is what makes
        // wires render in the right place immediately instead of only after the user drags
        // something (which incidentally triggers a layout pass beforehand).
        content.applyCss();
        content.layout();
        refreshAllWireRoutes();
    }

    /**
     * Places the VAR_OUTPUT pin column just past the rightmost block, instead of at a fixed huge
     * X near the edge of the whole pannable canvas — otherwise wires to small, left-clustered
     * diagrams would stretch most of the way across thousands of empty pixels, looking like they
     * run off into nowhere on the right.
     */
    private double computeOutputColumnX() {
        double maxRight = DEFAULT_OUTPUT_COLUMN_X;
        for (BlockInstance b : pou.getNetwork().getBlocks()) {
            maxRight = Math.max(maxRight, b.getX() + APPROX_BLOCK_WIDTH);
        }
        return Math.min(CONTENT_WIDTH - 220, maxRight + OUTPUT_COLUMN_MARGIN);
    }

    private DataType resolveConnectionType(Connection c) {
        PinRef src = c.getSource();
        if (!src.isVariable()) {
            BlockInstance inst = pou.getNetwork().findBlock(src.getBlockInstanceId()).orElse(null);
            BlockTypeDefinition type = inst == null ? null : library.get(inst.getTypeId());
            if (type != null) {
                var pin = type.findOutput(src.getPinName());
                if (pin != null) return pin.getType();
            }
        } else {
            return pou.getVariables().stream().filter(v -> v.getName().equals(src.getPinName()))
                    .map(Variable::getType).findFirst().orElse(DataType.ANY);
        }
        return DataType.ANY;
    }

    public void refreshAllWireRoutes() {
        for (WireView wire : wireViews.values()) {
            Point2D s = pinPosition(wire.getConnection().getSource(), true);
            Point2D t = pinPosition(wire.getConnection().getTarget(), false);
            wire.updateRoute(s, t);
        }
        if (previewWire != null) {
            // handled live during drag
        }
    }

    private Point2D pinPosition(PinRef ref, boolean output) {
        if (ref.isVariable()) {
            VariablePinView pv = output ? inputVarViews.get(ref.getPinName()) : outputVarViews.get(ref.getPinName());
            if (pv != null) return pv.pinPositionInContent(content);
            return new Point2D(0, 0);
        }
        BlockNodeView view = blockViews.get(ref.getBlockInstanceId());
        if (view == null) return new Point2D(0, 0);
        return view.pinPositionInContent(content, ref.getPinName(), output);
    }

    // ---- BlockNodeView.Listener -----------------------------------------

    @Override
    public void onMovePreview(BlockNodeView view, double dxScene, double dyScene) {
        if (selectedBlockId == null || !selectedBlockId.equals(view.getInstance().getId())) {
            selectBlock(view);
        }
        double dx = dxScene / zoom;
        double dy = dyScene / zoom;
        double nx = snap(dragOriginX + dx);
        double ny = snap(dragOriginY + dy);
        view.getInstance().setX(nx);
        view.getInstance().setY(ny);
        view.setLayoutX(nx);
        view.setLayoutY(ny);
        refreshAllWireRoutes();
    }

    @Override
    public void onMoveCommitted(BlockNodeView view, double oldX, double oldY) {
        BlockInstance inst = view.getInstance();
        if (oldX != inst.getX() || oldY != inst.getY()) {
            undo.perform(new MoveBlockCommand(inst, oldX, oldY, inst.getX(), inst.getY(), () -> {
                view.setLayoutX(inst.getX());
                view.setLayoutY(inst.getY());
                refreshAllWireRoutes();
            }));
            onModelChanged.run();
        }
    }

    @Override
    public void onSelected(BlockNodeView view, boolean toggleAdd) {
        selectBlock(view);
    }

    private void selectBlock(BlockNodeView view) {
        clearSelection();
        selectedBlockId = view.getInstance().getId();
        view.setSelected(true);
        dragOriginX = view.getInstance().getX();
        dragOriginY = view.getInstance().getY();
        onBlockSelected.accept(view.getInstance());
        requestFocus();
    }

    private void clearSelection() {
        if (selectedBlockId != null) {
            BlockNodeView v = blockViews.get(selectedBlockId);
            if (v != null) v.setSelected(false);
            selectedBlockId = null;
        }
        if (selectedWire != null) {
            selectedWire.setSelected(false);
            selectedWire = null;
        }
        onBlockSelected.accept(null);
    }

    @Override
    public void onPinPressed(PinHandle pin, javafx.scene.input.MouseEvent event) {
        pendingSource = pin;
        previewWire = new Path();
        previewWire.setStroke(DataTypeColors.of(pin.type()));
        previewWire.getStrokeDashArray().addAll(6.0, 4.0);
        previewWire.setStrokeWidth(2);
        content.getChildren().add(previewWire);
        updatePreview(event);
    }

    @Override
    public void onPinDragged(javafx.scene.input.MouseEvent event) {
        updatePreview(event);
    }

    private void updatePreview(javafx.scene.input.MouseEvent event) {
        if (previewWire == null || pendingSource == null) return;
        Point2D start = pinPosition(pendingSource.ref(), pendingSource.output());
        Point2D now = content.sceneToLocal(event.getSceneX(), event.getSceneY());
        previewWire.getElements().setAll(new MoveTo(start.getX(), start.getY()), new LineTo(now.getX(), now.getY()));
    }

    @Override
    public void onPinReleased(javafx.scene.input.MouseEvent event) {
        if (previewWire != null) {
            content.getChildren().remove(previewWire);
            previewWire = null;
        }
        if (pendingSource == null) return;
        Node picked = event.getPickResult().getIntersectedNode();
        PinHandle target = findPinHandle(picked);
        if (target != null && target.output() != pendingSource.output()) {
            PinHandle outPin = pendingSource.output() ? pendingSource : target;
            PinHandle inPin = pendingSource.output() ? target : pendingSource;
            if (inPin.type().isCompatibleWith(outPin.type()) || outPin.type().isCompatibleWith(inPin.type())) {
                Connection c = new Connection(outPin.ref(), inPin.ref());
                undo.perform(new AddConnectionCommand(pou.getNetwork(), c, this::renderAll));
                onModelChanged.run();
            }
        }
        pendingSource = null;
    }

    private PinHandle findPinHandle(Node node) {
        Node n = node;
        while (n != null) {
            Object h = n.getProperties().get("pinHandle");
            if (h instanceof PinHandle ph) return ph;
            n = n.getParent();
        }
        return null;
    }

    @Override
    public void onContextMenu(BlockNodeView view, javafx.scene.input.ContextMenuEvent event) {
        selectBlock(view);
        ContextMenu menu = new ContextMenu();
        MenuItem delete = new MenuItem("Delete block");
        delete.setOnAction(e -> deleteSelection());
        menu.getItems().add(delete);
        menu.show(view, event.getScreenX(), event.getScreenY());
    }

    // ---- VariablePinView.Listener (shares pin-drag logic) ---------------

    @Override
    public void onWireContextMenu(WireView wire, javafx.scene.input.ContextMenuEvent event) {
        selectedWire = wire;
        wire.setSelected(true);
        ContextMenu menu = new ContextMenu();
        MenuItem delete = new MenuItem("Delete wire");
        delete.setOnAction(e -> deleteSelection());
        menu.getItems().add(delete);
        menu.show(wire, event.getScreenX(), event.getScreenY());
    }

    @Override
    public void onWireSelected(WireView wire) {
        clearSelection();
        selectedWire = wire;
        wire.setSelected(true);
        requestFocus();
    }

    // ---- simulation overlay ----------------------------------------------

    public void setSimulationEngine(SimulationEngine engine) {
        this.simulationEngine = engine;
    }

    public void refreshLiveOverlay() {
        if (simulationEngine == null) return;
        for (WireView wire : wireViews.values()) {
            PinRef src = wire.getConnection().getSource();
            Object value = src.isVariable()
                    ? simulationEngine.getVariableValue(src.getPinName())
                    : simulationEngine.getValue(src.getBlockInstanceId(), src.getPinName());
            wire.setLiveValue(true, value);
        }
        for (VariablePinView pv : inputVarViews.values()) {
            pv.setLiveValue(true, simulationEngine.getVariableValue(pv.getVariableName()));
        }
        for (VariablePinView pv : outputVarViews.values()) {
            pv.setLiveValue(true, simulationEngine.getVariableValue(pv.getVariableName()));
        }
    }

    public void clearLiveOverlay() {
        for (WireView wire : wireViews.values()) {
            wire.setLiveValue(false, null);
        }
        for (VariablePinView pv : inputVarViews.values()) {
            pv.setLiveValue(false, null);
        }
        for (VariablePinView pv : outputVarViews.values()) {
            pv.setLiveValue(false, null);
        }
    }

    /**
     * Lets the user force/toggle a VAR_INPUT (or local VAR) value while a simulation is attached —
     * the only way to drive a BOOL input from FALSE to TRUE and watch the logic react live.
     */
    @Override
    public void onVariableValueClicked(VariablePinView pin) {
        if (simulationEngine == null) {
            return;
        }
        String name = pin.getVariableName();
        if (pin.getType() == DataType.BOOL) {
            boolean current = Boolean.TRUE.equals(simulationEngine.getVariableValue(name));
            simulationEngine.setVariableValue(name, !current);
        } else {
            Object currentValue = simulationEngine.getVariableValue(name);
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(
                    currentValue == null ? "" : String.valueOf(currentValue));
            dialog.setTitle(name);
            dialog.setHeaderText(name + " (" + pin.getType().iecName() + ")");
            dialog.showAndWait().ifPresent(text ->
                    simulationEngine.setVariableValue(name, com.sisak.fbdstudio.engine.IecLiteral.parse(pin.getType(), text)));
        }
        refreshLiveOverlay();
    }

    public void setOnBlockSelected(Consumer<BlockInstance> consumer) {
        this.onBlockSelected = consumer;
    }

    public void setOnModelChanged(Runnable r) {
        this.onModelChanged = r;
    }

    public POU getPou() {
        return pou;
    }

    public Optional<BlockInstance> getSelectedBlock() {
        return selectedBlockId == null ? Optional.empty() : pou.getNetwork().findBlock(selectedBlockId);
    }
}
