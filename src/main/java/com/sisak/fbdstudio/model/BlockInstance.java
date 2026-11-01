package com.sisak.fbdstudio.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** One placed instance of a block type (standard, extended, or user-defined function block) in a {@link Network}. */
public class BlockInstance {

    private String id = UUID.randomUUID().toString();
    private String typeId;
    private String instanceName = "";
    private double x;
    private double y;
    private int manualExecutionOrder = -1;
    private int extraPinCount = -1;

    /** Literal values typed directly onto unconnected input pins, keyed by pin name. */
    private Map<String, String> pinLiterals = new LinkedHashMap<>();

    /** Negated input pins (small circle on the wire per IEC 61131-3 FBD notation), keyed by pin name. */
    private Map<String, Boolean> negatedPins = new LinkedHashMap<>();

    public BlockInstance() {
    }

    public BlockInstance(String typeId, double x, double y) {
        this.typeId = typeId;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getManualExecutionOrder() {
        return manualExecutionOrder;
    }

    public void setManualExecutionOrder(int manualExecutionOrder) {
        this.manualExecutionOrder = manualExecutionOrder;
    }

    /** -1 means "use the block type's default pin count" (for variadic blocks like AND/OR/ADD). */
    public int getExtraPinCount() {
        return extraPinCount;
    }

    public void setExtraPinCount(int extraPinCount) {
        this.extraPinCount = extraPinCount;
    }

    public Map<String, String> getPinLiterals() {
        return pinLiterals;
    }

    public void setPinLiterals(Map<String, String> pinLiterals) {
        this.pinLiterals = pinLiterals;
    }

    public Map<String, Boolean> getNegatedPins() {
        return negatedPins;
    }

    public void setNegatedPins(Map<String, Boolean> negatedPins) {
        this.negatedPins = negatedPins;
    }

    public boolean isNegated(String pinName) {
        return negatedPins.getOrDefault(pinName, false);
    }
}
