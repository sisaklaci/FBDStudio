package com.sisak.fbdstudio.ui.commands;

import com.sisak.fbdstudio.ui.Command;

import java.util.function.Consumer;

/** Generic undoable edit of a single mutable field, given its old/new value and a setter. */
public class PropertyCommand<T> implements Command {

    private final String label;
    private final T oldValue;
    private final T newValue;
    private final Consumer<T> setter;
    private final Runnable afterApply;

    public PropertyCommand(String label, T oldValue, T newValue, Consumer<T> setter) {
        this(label, oldValue, newValue, setter, () -> {
        });
    }

    public PropertyCommand(String label, T oldValue, T newValue, Consumer<T> setter, Runnable afterApply) {
        this.label = label;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.setter = setter;
        this.afterApply = afterApply;
    }

    @Override
    public void execute() {
        setter.accept(newValue);
        afterApply.run();
    }

    @Override
    public void undo() {
        setter.accept(oldValue);
        afterApply.run();
    }

    @Override
    public String label() {
        return label;
    }
}
