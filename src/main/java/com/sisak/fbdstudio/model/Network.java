package com.sisak.fbdstudio.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The FBD diagram body of a POU: the blocks placed on the canvas and the wires between them. */
public class Network {

    private List<BlockInstance> blocks = new ArrayList<>();
    private List<Connection> connections = new ArrayList<>();

    public List<BlockInstance> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<BlockInstance> blocks) {
        this.blocks = blocks;
    }

    public List<Connection> getConnections() {
        return connections;
    }

    public void setConnections(List<Connection> connections) {
        this.connections = connections;
    }

    public Optional<BlockInstance> findBlock(String id) {
        return blocks.stream().filter(b -> b.getId().equals(id)).findFirst();
    }

    public List<Connection> connectionsInto(PinRef target) {
        return connections.stream().filter(c -> c.getTarget().equals(target)).toList();
    }

    public List<Connection> connectionsFrom(PinRef source) {
        return connections.stream().filter(c -> c.getSource().equals(source)).toList();
    }

    public void removeBlock(String blockId) {
        blocks.removeIf(b -> b.getId().equals(blockId));
        connections.removeIf(c -> blockId.equals(c.getSource().getBlockInstanceId())
                || blockId.equals(c.getTarget().getBlockInstanceId()));
    }
}
