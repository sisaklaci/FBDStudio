package com.sisak.fbdstudio.ui;

import com.sisak.fbdstudio.i18n.Messages;
import com.sisak.fbdstudio.model.DataType;
import com.sisak.fbdstudio.model.PinRef;
import com.sisak.fbdstudio.model.Variable;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;

/**
 * Canvas-edge representation of one POU interface variable (VAR_INPUT/VAR act as an output pin
 * feeding the network; VAR_OUTPUT/VAR act as an input pin fed by the network), matching how FBD
 * editors show interface variables as border pins on the diagram. While a simulation is attached,
 * an input-side BOOL pin can be clicked to force/toggle its live value.
 */
public class VariablePinView extends HBox {

    public interface Listener {
        void onPinPressed(PinHandle pin, javafx.scene.input.MouseEvent event);
        void onPinDragged(javafx.scene.input.MouseEvent event);
        void onPinReleased(javafx.scene.input.MouseEvent event);
        /** Fired when the user clicks the label of an input-side (output=true) variable pin. */
        default void onVariableValueClicked(VariablePinView pin) {
        }
    }

    private final Circle circle;
    private final Label label;
    private final boolean output;
    private final String variableName;
    private final DataType type;

    public VariablePinView(Variable variable, boolean output, Listener listener) {
        this.output = output;
        this.variableName = variable.getName();
        this.type = variable.getType();

        circle = new Circle(6);
        circle.setStroke(DataTypeColors.of(variable.getType()));
        circle.setStrokeWidth(2);
        circle.setStrokeType(StrokeType.OUTSIDE);
        circle.setFill(Color.WHITE);

        PinHandle handle = new PinHandle(PinRef.ofVariable(variable.getName()), output, variable.getType());
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

        label = new Label(variable.getName());
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        if (output) {
            label.setCursor(javafx.scene.Cursor.HAND);
            label.setOnMouseClicked(e -> listener.onVariableValueClicked(this));
            String baseTip = variable.getKind() + " " + variable.getName() + " : " + variable.getType().iecName();
            Tooltip.install(label, new Tooltip(baseTip + "\n" + Messages.get("pin.forceHint")));
            Tooltip.install(circle, new Tooltip(baseTip));
        } else {
            Tooltip.install(circle, new Tooltip(variable.getKind() + " " + variable.getName() + " : " + variable.getType().iecName()));
        }

        getChildren().addAll(output ? new javafx.scene.Node[]{circle, label} : new javafx.scene.Node[]{label, circle});
        setAlignment(output ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        setSpacing(5);
        setPadding(new Insets(2));
    }

    public Point2D pinPositionInContent(javafx.scene.Node content) {
        Point2D scenePoint = circle.localToScene(circle.getCenterX(), circle.getCenterY());
        return content.sceneToLocal(scenePoint);
    }

    /** Colors the pin to reflect its current live value while a simulation is attached. */
    public void setLiveValue(boolean simulationActive, Object value) {
        if (!simulationActive) {
            circle.setFill(Color.WHITE);
            return;
        }
        if (type == DataType.BOOL) {
            circle.setFill(Boolean.TRUE.equals(value) ? Color.web("#2e8b57") : Color.web("#e3e6ec"));
        } else {
            circle.setFill(Color.web("#2f6fd0", 0.25));
        }
    }

    public String getVariableName() {
        return variableName;
    }

    public DataType getType() {
        return type;
    }

    public boolean isOutput() {
        return output;
    }
}
