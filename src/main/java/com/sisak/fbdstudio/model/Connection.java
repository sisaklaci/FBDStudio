package com.sisak.fbdstudio.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A wire from one output pin to one input pin within a {@link Network}. */
public class Connection {

    private String id = UUID.randomUUID().toString();
    private PinRef source;
    private PinRef target;

    /** Optional routing waypoints (diagram coordinates) for orthogonal wire rendering. */
    private List<double[]> waypoints = new ArrayList<>();

    public Connection() {
    }

    public Connection(PinRef source, PinRef target) {
        this.source = source;
        this.target = target;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PinRef getSource() {
        return source;
    }

    public void setSource(PinRef source) {
        this.source = source;
    }

    public PinRef getTarget() {
        return target;
    }

    public void setTarget(PinRef target) {
        this.target = target;
    }

    public List<double[]> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<double[]> waypoints) {
        this.waypoints = waypoints;
    }
}
