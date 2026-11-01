package com.sisak.fbdstudio.ui.commands;

import com.sisak.fbdstudio.model.BlockInstance;
import com.sisak.fbdstudio.model.Network;
import com.sisak.fbdstudio.ui.Command;

public class AddBlockCommand implements Command {

    private final Network network;
    private final BlockInstance instance;
    private final Runnable afterApply;

    public AddBlockCommand(Network network, BlockInstance instance, Runnable afterApply) {
        this.network = network;
        this.instance = instance;
        this.afterApply = afterApply;
    }

    @Override
    public void execute() {
        network.getBlocks().add(instance);
        afterApply.run();
    }

    @Override
    public void undo() {
        network.removeBlock(instance.getId());
        afterApply.run();
    }

    @Override
    public String label() {
        return "Add " + instance.getTypeId();
    }
}
