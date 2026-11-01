package com.sisak.fbdstudio.ui.commands;

import com.sisak.fbdstudio.model.Connection;
import com.sisak.fbdstudio.model.Network;
import com.sisak.fbdstudio.ui.Command;

public class RemoveConnectionCommand implements Command {

    private final Network network;
    private final Connection connection;
    private final Runnable afterApply;

    public RemoveConnectionCommand(Network network, Connection connection, Runnable afterApply) {
        this.network = network;
        this.connection = connection;
        this.afterApply = afterApply;
    }

    @Override
    public void execute() {
        network.getConnections().remove(connection);
        afterApply.run();
    }

    @Override
    public void undo() {
        network.getConnections().add(connection);
        afterApply.run();
    }

    @Override
    public String label() {
        return "Delete wire";
    }
}
