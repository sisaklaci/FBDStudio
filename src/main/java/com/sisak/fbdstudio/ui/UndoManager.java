package com.sisak.fbdstudio.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.ArrayDeque;
import java.util.Deque;

/** Simple linear undo/redo command stack shared by every editing action in the app. */
public class UndoManager {

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private final BooleanProperty canUndo = new SimpleBooleanProperty(false);
    private final BooleanProperty canRedo = new SimpleBooleanProperty(false);
    private Runnable onChange = () -> {
    };

    public void perform(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
        updateProps();
        onChange.run();
    }

    public void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        Command c = undoStack.pop();
        c.undo();
        redoStack.push(c);
        updateProps();
        onChange.run();
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        Command c = redoStack.pop();
        c.execute();
        undoStack.push(c);
        updateProps();
        onChange.run();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        updateProps();
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    private void updateProps() {
        canUndo.set(!undoStack.isEmpty());
        canRedo.set(!redoStack.isEmpty());
    }

    public BooleanProperty canUndoProperty() {
        return canUndo;
    }

    public BooleanProperty canRedoProperty() {
        return canRedo;
    }
}
