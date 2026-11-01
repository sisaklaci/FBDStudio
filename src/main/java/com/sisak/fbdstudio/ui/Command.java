package com.sisak.fbdstudio.ui;

/** One undoable/redoable edit to the project. */
public interface Command {
    void execute();
    void undo();

    default String label() {
        return getClass().getSimpleName();
    }
}
