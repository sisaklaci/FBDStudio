package com.sisak.fbdstudio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

/**
 * Identifies one pin in a network: either a pin on a block instance
 * ({@code blockInstanceId} set), or a POU interface variable used directly as a
 * source/sink in the diagram ({@code blockInstanceId} null, {@code pinName} = variable name).
 */
public class PinRef {

    private String blockInstanceId;
    private String pinName;

    public PinRef() {
    }

    public PinRef(String blockInstanceId, String pinName) {
        this.blockInstanceId = blockInstanceId;
        this.pinName = pinName;
    }

    public static PinRef ofVariable(String variableName) {
        return new PinRef(null, variableName);
    }

    @JsonIgnore
    public boolean isVariable() {
        return blockInstanceId == null;
    }

    public String getBlockInstanceId() {
        return blockInstanceId;
    }

    public void setBlockInstanceId(String blockInstanceId) {
        this.blockInstanceId = blockInstanceId;
    }

    public String getPinName() {
        return pinName;
    }

    public void setPinName(String pinName) {
        this.pinName = pinName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PinRef other)) return false;
        return Objects.equals(blockInstanceId, other.blockInstanceId) && Objects.equals(pinName, other.pinName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockInstanceId, pinName);
    }

    @Override
    public String toString() {
        return (blockInstanceId == null ? "VAR" : blockInstanceId) + "." + pinName;
    }
}
