package com.sisak.fbdstudio.engine;

import com.sisak.fbdstudio.model.BlockInstance;
import com.sisak.fbdstudio.model.Connection;
import com.sisak.fbdstudio.model.Network;
import com.sisak.fbdstudio.model.PinRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes a per-scan-cycle execution order for the blocks in a {@link Network}, mirroring how
 * real PLC runtimes schedule FBD networks: feed-forward dependencies (A's input wired from B's
 * output) force B before A; any dependency that cannot be satisfied without creating a cycle is
 * instead resolved as a "feedback" connection, read from the previous scan's value, exactly like a
 * real PLC does for algebraic loops.
 */
public final class ExecutionOrderResolver {

    public record Result(List<String> order, Set<String> feedbackConnectionIds) {
    }

    private ExecutionOrderResolver() {
    }

    public static Result resolve(Network network) {
        List<BlockInstance> blocks = network.getBlocks();
        Map<String, BlockInstance> byId = blocks.stream()
                .collect(Collectors.toMap(BlockInstance::getId, b -> b, (a, b) -> a, java.util.LinkedHashMap::new));

        // direct block-to-block dependency edges: source block id -> list of connections it feeds
        Map<String, List<Connection>> incoming = new java.util.LinkedHashMap<>();
        for (String id : byId.keySet()) {
            incoming.put(id, new ArrayList<>());
        }
        for (Connection c : network.getConnections()) {
            PinRef src = c.getSource();
            PinRef tgt = c.getTarget();
            if (src != null && tgt != null && !src.isVariable() && !tgt.isVariable()
                    && byId.containsKey(src.getBlockInstanceId()) && byId.containsKey(tgt.getBlockInstanceId())) {
                incoming.get(tgt.getBlockInstanceId()).add(c);
            }
        }

        Set<String> scheduled = new LinkedHashSet<>();
        List<String> order = new ArrayList<>();
        Set<String> feedback = new LinkedHashSet<>();

        List<String> remaining = new ArrayList<>(byId.keySet());
        remaining.sort(Comparator.comparingInt(id -> manualOrderOrMax(byId.get(id))));

        while (!remaining.isEmpty()) {
            String next = null;
            for (String candidate : remaining) {
                boolean ready = incoming.get(candidate).stream()
                        .allMatch(c -> scheduled.contains(c.getSource().getBlockInstanceId()));
                if (ready) {
                    next = candidate;
                    break;
                }
            }
            if (next == null) {
                // Algebraic loop: force-schedule the first remaining block (stable order), and
                // mark any of its still-unmet incoming connections as feedback (previous-scan value).
                next = remaining.get(0);
                for (Connection c : incoming.get(next)) {
                    if (!scheduled.contains(c.getSource().getBlockInstanceId())) {
                        feedback.add(c.getId());
                    }
                }
            }
            order.add(next);
            scheduled.add(next);
            remaining.remove(next);
        }

        return new Result(order, feedback);
    }

    private static int manualOrderOrMax(BlockInstance b) {
        return b.getManualExecutionOrder() >= 0 ? b.getManualExecutionOrder() : Integer.MAX_VALUE;
    }
}
