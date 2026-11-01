package com.sisak.fbdstudio.model;

/** A single variable declaration inside a POU's interface or local body. */
public class Variable {

    private String name;
    private DataType type = DataType.BOOL;
    private VariableKind kind = VariableKind.VAR;
    private String initialValue = "";
    private String comment = "";

    public Variable() {
    }

    public Variable(String name, DataType type, VariableKind kind) {
        this.name = name;
        this.type = type;
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    public VariableKind getKind() {
        return kind;
    }

    public void setKind(VariableKind kind) {
        this.kind = kind;
    }

    public String getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(String initialValue) {
        this.initialValue = initialValue;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
