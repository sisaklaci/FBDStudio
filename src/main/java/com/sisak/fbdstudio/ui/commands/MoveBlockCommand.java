package com.sisak.fbdstudio.ui.commands;

import com.sisak.fbdstudio.model.BlockInstance;
import com.sisak.fbdstudio.ui.Command;

public class MoveBlockCommand implements Command {

    private final BlockInstance instance;
    private final double oldX, oldY, newX, newY;
    private final Runnable afterApply;

    public MoveBlockCommand(BlockInstance instance, double oldX, double oldY, double newX, double newY, Runnable afterApply) {
        this.instance = instance;
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
        this.afterApply = afterApply;
    }

    @Override
    public void execute() {
        instance.setX(newX);
        instance.setY(newY);
        afterApply.run();
    }

    @Override
    public void undo() {
        instance.setX(oldX);
        instance.setY(oldY);
        afterApply.run();
    }

    @Override
    public String label() {
        return "Move block";
    }
}
