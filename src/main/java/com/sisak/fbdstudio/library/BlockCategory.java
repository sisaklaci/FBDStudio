package com.sisak.fbdstudio.library;

import com.sisak.fbdstudio.i18n.Messages;

/** Grouping used by the block palette tree. */
public enum BlockCategory {
    LOGIC,
    COMPARISON,
    ARITHMETIC,
    SELECTION,
    BISTABLE,
    EDGE,
    TIMER,
    COUNTER,
    EXTENDED,
    USER;

    /** Looked up live from the current language so the palette relabels on language switch. */
    public String displayName() {
        return Messages.get("category." + name());
    }
}
