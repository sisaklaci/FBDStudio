package com.sisak.fbdstudio.ui.commands;

import com.sisak.fbdstudio.model.Connection;
import com.sisak.fbdstudio.model.Network;
import com.sisak.fbdstudio.ui.Command;

public class AddConnectionCommand implements Command {

    private final Network network;
    private final Connection connection;
    private final Runnable afterApply;
    private Connection replaced;

    public AddConnectionCommand(Network network, Connection connection, Runnable afterApply) {
        this.network = network;
        this.connection = connection;
        this.afterApply = afterApply;
    }

    @Override
    public void execute() {
        replaced = network.getConnections().stream()
                .filter(c -> c.getTarget().equals(connection.getTarget()))
                .findFirst().orElse(null);
        if (replaced != null) {
            network.getConnections().remove(replaced);
        }
        network.getConnections().add(connection);
        afterApply.run();
    }

    @Override
    public void undo() {
        network.getConnections().remove(connection);
        if (replaced != null) {
            network.getConnections().add(replaced);
        }
        afterApply.run();
    }

    @Override
    public String label() {
        return "Connect";
    }
}
