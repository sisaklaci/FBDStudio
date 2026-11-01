package com.sisak.fbdstudio.ui.commands;

import com.sisak.fbdstudio.model.BlockInstance;
import com.sisak.fbdstudio.model.Connection;
import com.sisak.fbdstudio.model.Network;
import com.sisak.fbdstudio.ui.Command;

import java.util.ArrayList;
import java.util.List;

/** Removes a block and remembers the connections attached to it so undo can restore both. */
public class RemoveBlockCommand implements Command {

    private final Network network;
    private final BlockInstance instance;
    private final List<Connection> removedConnections = new ArrayList<>();
    private final Runnable afterApply;

    public RemoveBlockCommand(Network network, BlockInstance instance, Runnable afterApply) {
        this.network = network;
        this.instance = instance;
        this.afterApply = afterApply;
    }

    @Override
    public void execute() {
        removedConnections.clear();
        for (Connection c : network.getConnections()) {
            if (instance.getId().equals(c.getSource().getBlockInstanceId())
                    || instance.getId().equals(c.getTarget().getBlockInstanceId())) {
                removedConnections.add(c);
            }
        }
        network.removeBlock(instance.getId());
        afterApply.run();
    }

    @Override
    public void undo() {
        network.getBlocks().add(instance);
        network.getConnections().addAll(removedConnections);
        afterApply.run();
    }

    @Override
    public String label() {
        return "Delete " + instance.getTypeId();
    }
}
