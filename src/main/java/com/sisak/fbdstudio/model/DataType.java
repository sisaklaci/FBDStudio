package com.sisak.fbdstudio.model;

/**
 * IEC 61131-3 elementary data types supported by the editor and simulator.
 */
public enum DataType {
    BOOL("BOOL", false),
    BYTE("BYTE", (short) 0),
    WORD("WORD", 0),
    DWORD("DWORD", 0L),
    SINT("SINT", (byte) 0),
    INT("INT", (short) 0),
    DINT("DINT", 0),
    LINT("LINT", 0L),
    USINT("USINT", (short) 0),
    UINT("UINT", 0),
    UDINT("UDINT", 0L),
    ULINT("ULINT", 0L),
    REAL("REAL", 0.0f),
    LREAL("LREAL", 0.0d),
    TIME("TIME", 0L),
    STRING("STRING", ""),
    ANY("ANY", null);

    private final String iecName;
    private final Object defaultValue;

    DataType(String iecName, Object defaultValue) {
        this.iecName = iecName;
        this.defaultValue = defaultValue;
    }

    public String iecName() {
        return iecName;
    }

    public Object defaultValue() {
        return defaultValue;
    }

    public boolean isNumeric() {
        return switch (this) {
            case BYTE, WORD, DWORD, SINT, INT, DINT, LINT, USINT, UINT, UDINT, ULINT, REAL, LREAL, TIME -> true;
            default -> false;
        };
    }

    public boolean isBoolean() {
        return this == BOOL;
    }

    /**
     * Whether a value of type {@code source} may be connected into a pin declared as this type.
     * {@code ANY} accepts/produces anything (used by generic blocks such as MOVE, SEL, MUX).
     */
    public boolean isCompatibleWith(DataType source) {
        if (this == ANY || source == ANY) {
            return true;
        }
        if (this == source) {
            return true;
        }
        return this.isNumeric() && source.isNumeric();
    }

    public static DataType fromIecName(String name) {
        for (DataType t : values()) {
            if (t.iecName.equalsIgnoreCase(name)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown IEC data type: " + name);
    }
}
