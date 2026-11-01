package com.sisak.fbdstudio.ui;

import com.sisak.fbdstudio.library.BlockTypeDefinition;
import com.sisak.fbdstudio.model.BlockInstance;
import com.sisak.fbdstudio.model.PinDefinition;
import com.sisak.fbdstudio.model.PinRef;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;

import java.util.LinkedHashMap;
import java.util.Map;

/** Visual node on the canvas for one {@link BlockInstance}. */
public class BlockNodeView extends BorderPane {

    public interface Listener {
        void onMovePreview(BlockNodeView view, double dxLocal, double dyLocal);
        void onMoveCommitted(BlockNodeView view, double oldX, double oldY);
        void onSelected(BlockNodeView view, boolean toggleAdd);
        void onPinPressed(PinHandle pin, javafx.scene.input.MouseEvent event);
        void onPinDragged(javafx.scene.input.MouseEvent event);
        void onPinReleased(javafx.scene.input.MouseEvent event);
        void onContextMenu(BlockNodeView view, javafx.scene.input.ContextMenuEvent event);
    }

    private final BlockInstance instance;
    private final BlockTypeDefinition type;
    private final Map<String, Circle> inputCircles = new LinkedHashMap<>();
    private final Map<String, Circle> outputCircles = new LinkedHashMap<>();

    private double pressSceneX, pressSceneY;
    private double startX, startY;
    private boolean selected;

    public BlockNodeView(BlockInstance instance, BlockTypeDefinition type, int resolvedPinCount, Listener listener) {
        this.instance = instance;
        this.type = type;
        setPickOnBounds(false);

        VBox inputs = new VBox(6);
        inputs.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < resolvedPinCount; i++) {
            PinDefinition pin = i < type.getInputs().size() ? type.getInputs().get(i)
                    : derivedVariadicPin(type, i);
            inputs.getChildren().add(pinRow(pin, true, listener));
        }

        VBox outputs = new VBox(6);
        outputs.setAlignment(Pos.CENTER_RIGHT);
        for (PinDefinition pin : type.getOutputs()) {
            outputs.getChildren().add(pinRow(pin, false, listener));
        }

        String title = type.getDisplayName() + (instance.getInstanceName().isBlank() ? "" : "\n" + instance.getInstanceName());
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(110);
        Tooltip.install(titleLabel, new Tooltip(type.getDescription()));

        setLeft(inputs);
        setRight(outputs);
        setCenter(titleLabel);
        setPadding(new Insets(8, 10, 8, 10));
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        BorderPane.setMargin(inputs, new Insets(0, 8, 0, 0));
        BorderPane.setMargin(outputs, new Insets(0, 0, 0, 8));

        applyStyle();

        setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            pressSceneX = e.getSceneX();
            pressSceneY = e.getSceneY();
            startX = instance.getX();
            startY = instance.getY();
            listener.onSelected(this, e.isShiftDown());
            e.consume();
        });
        setOnMouseDragged(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            listener.onMovePreview(this, e.getSceneX() - pressSceneX, e.getSceneY() - pressSceneY);
            e.consume();
        });
        setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            listener.onMoveCommitted(this, startX, startY);
            e.consume();
        });
        setOnContextMenuRequested(e -> listener.onContextMenu(this, e));
    }

    private PinDefinition derivedVariadicPin(BlockTypeDefinition type, int index) {
        PinDefinition last = type.getInputs().get(type.getInputs().size() - 1);
        return new PinDefinition("IN" + (index + 1), last.getType(), last.getDefaultLiteral());
    }

    private HBox pinRow(PinDefinition pin, boolean isInput, Listener listener) {
        Circle circle = new Circle(5);
        circle.setStroke(DataTypeColors.of(pin.getType()));
        circle.setStrokeWidth(2);
        circle.setStrokeType(StrokeType.OUTSIDE);
        circle.setFill(Color.WHITE);

        boolean negated = !isInput ? false : instance.isNegated(pin.getName());
        if (negated) {
            circle.setFill(Color.BLACK);
        }

        PinHandle handle = new PinHandle(new PinRef(instance.getId(), pin.getName()), !isInput, pin.getType());
        circle.getProperties().put("pinHandle", handle);
        circle.setOnMousePressed(e -> {
            listener.onPinPressed(handle, e);
            e.consume();
        });
        circle.setOnMouseDragged(e -> {
            listener.onPinDragged(e);
            e.consume();
        });
        circle.setOnMouseReleased(e -> {
            listener.onPinReleased(e);
            e.consume();
        });
        Tooltip.install(circle, new Tooltip(pin.getName() + " : " + pin.getType().iecName()));

        Label label = new Label(pin.getName());
        label.setStyle("-fx-font-size: 10px; -fx-text-fill: -fx-text-base-color;");

        HBox row = isInput ? new HBox(4, circle, label) : new HBox(4, label, circle);
        row.setAlignment(isInput ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        (isInput ? inputCircles : outputCircles).put(pin.getName(), circle);
        return row;
    }

    private void applyStyle() {
        Color bg = selected ? Color.web("#dceeff") : Color.web("#f7f9fc");
        Color border = selected ? Color.web("#2f6fd0") : Color.web("#7a8699");
        setBackground(new Background(new BackgroundFill(bg, new CornerRadii(8), Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(border, BorderStrokeStyle.SOLID, new CornerRadii(8),
                new BorderWidths(selected ? 2 : 1))));
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        applyStyle();
    }

    public boolean isSelected() {
        return selected;
    }

    public BlockInstance getInstance() {
        return instance;
    }

    public Point2D pinPositionInContent(javafx.scene.Node content, String pinName, boolean output) {
        Circle c = (output ? outputCircles : inputCircles).get(pinName);
        if (c == null) {
            return new Point2D(getLayoutX(), getLayoutY());
        }
        Point2D scenePoint = c.localToScene(c.getCenterX(), c.getCenterY());
        return content.sceneToLocal(scenePoint);
    }
}
