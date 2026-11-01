package com.sisak.fbdstudio.ui;

import com.sisak.fbdstudio.model.DataType;
import com.sisak.fbdstudio.model.PinRef;

/** Identifies a pin drawn on the canvas (either a block pin or a POU interface-variable pin). */
public record PinHandle(PinRef ref, boolean output, DataType type) {
}
