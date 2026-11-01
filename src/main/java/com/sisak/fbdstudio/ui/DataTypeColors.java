package com.sisak.fbdstudio.ui;

import com.sisak.fbdstudio.model.DataType;
import javafx.scene.paint.Color;

/** Consistent wire/pin color coding by IEC data type family, used across the canvas and inspector. */
public final class DataTypeColors {

    private DataTypeColors() {
    }

    public static Color of(DataType type) {
        if (type == null) {
            return Color.web("#9aa5b1");
        }
        return switch (type) {
            case BOOL -> Color.web("#2e8b57");
            case TIME -> Color.web("#d2691e");
            case STRING -> Color.web("#8a4fd6");
            case ANY -> Color.web("#9aa5b1");
            default -> type.isNumeric() ? Color.web("#2f6fd0") : Color.web("#9aa5b1");
        };
    }
}
