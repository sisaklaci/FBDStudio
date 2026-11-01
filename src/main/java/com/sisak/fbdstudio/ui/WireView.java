package com.sisak.fbdstudio.ui;

import com.sisak.fbdstudio.model.Connection;
import com.sisak.fbdstudio.model.DataType;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;

/** Orthogonal ("elbow") wire between two pins, rendered as a {@link Path}. */
public class WireView extends Path {

    public interface Listener {
        void onWireContextMenu(WireView wire, javafx.scene.input.ContextMenuEvent event);
        void onWireSelected(WireView wire);
    }

    private final Connection connection;
    private final DataType type;
    private boolean liveTrue;
    private boolean simulationActive;
    private boolean selected;

    public WireView(Connection connection, DataType type, Listener listener) {
        this.connection = connection;
        this.type = type;
        setStrokeWidth(2);
        setFill(null);
        applyColor();
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                listener.onWireSelected(this);
            }
        });
        setOnContextMenuRequested(e -> listener.onWireContextMenu(this, e));
    }

    public void updateRoute(Point2D source, Point2D target) {
        getElements().clear();
        double midX = source.getX() + Math.max(24, (target.getX() - source.getX()) / 2.0);
        getElements().add(new MoveTo(source.getX(), source.getY()));
        if (target.getX() >= source.getX() + 16) {
            getElements().add(new LineTo(midX, source.getY()));
            getElements().add(new LineTo(midX, target.getY()));
            getElements().add(new LineTo(target.getX(), target.getY()));
        } else {
            // target is to the left of (or very close to) the source: route around below.
            double detourY = Math.max(source.getY(), target.getY()) + 30;
            getElements().add(new LineTo(source.getX() + 20, source.getY()));
            getElements().add(new LineTo(source.getX() + 20, detourY));
            getElements().add(new LineTo(target.getX() - 20, detourY));
            getElements().add(new LineTo(target.getX() - 20, target.getY()));
            getElements().add(new LineTo(target.getX(), target.getY()));
        }
    }

    public void setLiveValue(boolean simulationActive, Object value) {
        this.simulationActive = simulationActive;
        this.liveTrue = Boolean.TRUE.equals(value);
        applyColor();
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        applyColor();
    }

    private void applyColor() {
        if (selected) {
            setStroke(Color.web("#2f6fd0"));
            setStrokeWidth(3);
            return;
        }
        setStrokeWidth(2);
        if (simulationActive && type == DataType.BOOL) {
            setStroke(liveTrue ? Color.web("#2e8b57") : Color.web("#c3c9d1"));
        } else {
            setStroke(DataTypeColors.of(type));
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
