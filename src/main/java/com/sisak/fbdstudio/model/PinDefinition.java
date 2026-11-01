package com.sisak.fbdstudio.model;

/** Declarative description of one input or output pin on a {@link com.sisak.fbdstudio.library.BlockTypeDefinition}. */
public class PinDefinition {

    private String name;
    private DataType type = DataType.BOOL;
    private String defaultLiteral = "";
    private String description = "";

    public PinDefinition() {
    }

    public PinDefinition(String name, DataType type) {
        this.name = name;
        this.type = type;
    }

    public PinDefinition(String name, DataType type, String defaultLiteral) {
        this.name = name;
        this.type = type;
        this.defaultLiteral = defaultLiteral;
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

    public String getDefaultLiteral() {
        return defaultLiteral;
    }

    public void setDefaultLiteral(String defaultLiteral) {
        this.defaultLiteral = defaultLiteral;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
