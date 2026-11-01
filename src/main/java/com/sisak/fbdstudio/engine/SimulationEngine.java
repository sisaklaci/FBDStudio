package com.sisak.fbdstudio.engine;

import com.sisak.fbdstudio.library.BlockLibrary;
import com.sisak.fbdstudio.library.BlockTypeDefinition;
import com.sisak.fbdstudio.model.BlockInstance;
import com.sisak.fbdstudio.model.Connection;
import com.sisak.fbdstudio.model.DataType;
import com.sisak.fbdstudio.model.Network;
import com.sisak.fbdstudio.model.PinDefinition;
import com.sisak.fbdstudio.model.PinRef;
import com.sisak.fbdstudio.model.POU;
import com.sisak.fbdstudio.model.Variable;
import com.sisak.fbdstudio.model.VariableKind;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Scan-cycle interpreter for a single POU's {@link Network}. Each call to {@link #step(long)}
 * performs one PLC scan: read inputs, evaluate blocks in dependency order (feedback edges read the
 * previous scan's value), write outputs. Designed to be driven either by a UI timer (Run mode) or
 * called once at a time (Step mode).
 */
public class SimulationEngine {

    private final POU pou;
    private final BlockLibrary library;
    private ExecutionOrderResolver.Result executionPlan;

    /** Current-scan values, keyed by pin ("blockId.pinName" or "VAR.name"). */
    private final Map<String, Object> currentValues = new LinkedHashMap<>();
    /** Snapshot of currentValues from the end of the previous scan, used to resolve feedback edges. */
    private final Map<String, Object> previousValues = new LinkedHashMap<>();
    /** Per-block persistent state (timers, counters, bistables, edge detectors). */
    private final Map<String, BlockState> blockStates = new HashMap<>();
    /** Forced variable values that override normal evaluation, keyed by variable name. */
    private final Map<String, Object> forcedVariables = new HashMap<>();

    private long scanCount = 0;
    private boolean running = false;

    public SimulationEngine(POU pou) {
        this(pou, BlockLibrary.getInstance());
    }

    public SimulationEngine(POU pou, BlockLibrary library) {
        this.pou = pou;
        this.library = library;
        recompile();
        reset();
    }

    /** Re-resolves the execution order; call after the network's wiring changes. */
    public void recompile() {
        executionPlan = ExecutionOrderResolver.resolve(pou.getNetwork());
    }

    public void reset() {
        currentValues.clear();
        previousValues.clear();
        blockStates.clear();
        scanCount = 0;
        for (Variable v : pou.getVariables()) {
            currentValues.put(key(null, v.getName()), IecLiteral.parse(v.getType(), v.getInitialValue()));
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public long getScanCount() {
        return scanCount;
    }

    public void forceVariable(String name, Object value) {
        forcedVariables.put(name, value);
    }

    public void clearForce(String name) {
        forcedVariables.remove(name);
    }

    public Map<String, Object> getForcedVariables() {
        return forcedVariables;
    }

    /** Performs exactly one scan cycle. {@code dtMillis} is the elapsed (simulated) time since the previous scan. */
    public void step(long dtMillis) {
        previousValues.clear();
        previousValues.putAll(currentValues);

        Network net = pou.getNetwork();
        Set<String> feedbackConnIds = executionPlan.feedbackConnectionIds();

        // VAR_INPUT values are either forced or keep whatever the user/host set on them directly.
        for (Variable v : pou.getVariables()) {
            if (v.getKind() == VariableKind.VAR_INPUT && forcedVariables.containsKey(v.getName())) {
                currentValues.put(key(null, v.getName()), forcedVariables.get(v.getName()));
            }
        }

        for (String blockId : executionPlan.order()) {
            BlockInstance instance = net.findBlock(blockId).orElseThrow();
            BlockTypeDefinition type = library.get(instance.getTypeId());
            if (type == null) {
                continue; // user-defined FB instancing: future extension
            }
            int pinCount = resolvePinCount(type, instance);
            Map<String, Object> inputs = gatherInputs(net, instance, type, pinCount, feedbackConnIds);
            BlockState state = blockStates.computeIfAbsent(blockId, id -> new BlockState());
            Map<String, Object> outputs = StandardBlockEvaluator.evaluate(type.getId(), new EvalContext(inputs), state, dtMillis, pinCount);
            for (Map.Entry<String, Object> e : outputs.entrySet()) {
                currentValues.put(key(blockId, e.getKey()), e.getValue());
            }
        }

        // Propagate block outputs wired directly to POU output variables.
        for (Connection c : net.getConnections()) {
            if (c.getTarget().isVariable()) {
                Object v = resolveSourceValue(net, c.getSource(), feedbackConnIds, c.getId());
                if (v != null) {
                    currentValues.put(key(null, c.getTarget().getPinName()), v);
                }
            }
        }

        for (Map.Entry<String, Object> forced : forcedVariables.entrySet()) {
            if (pou.getVariables().stream().anyMatch(v -> v.getName().equals(forced.getKey())
                    && v.getKind() != VariableKind.VAR_INPUT)) {
                currentValues.put(key(null, forced.getKey()), forced.getValue());
            }
        }

        scanCount++;
    }

    private int resolvePinCount(BlockTypeDefinition type, BlockInstance instance) {
        if (!type.isVariadicInputs()) {
            return type.getInputs().size();
        }
        return instance.getExtraPinCount() > 0 ? instance.getExtraPinCount() : type.getMinInputs();
    }

    private Map<String, Object> gatherInputs(Network net, BlockInstance instance, BlockTypeDefinition type,
                                               int pinCount, Set<String> feedbackConnIds) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        java.util.List<PinDefinition> pins = type.getInputs();
        for (int i = 0; i < pinCount; i++) {
            PinDefinition pinDef = i < pins.size() ? pins.get(i) : variadicPinTemplate(type, i);
            String pinName = pinDef.getName();
            PinRef target = new PinRef(instance.getId(), pinName);
            var incoming = net.connectionsInto(target);
            Object value;
            if (!incoming.isEmpty()) {
                Connection c = incoming.get(0);
                value = resolveSourceValue(net, c.getSource(), feedbackConnIds, c.getId());
            } else {
                String literal = instance.getPinLiterals().getOrDefault(pinName, pinDef.getDefaultLiteral());
                value = IecLiteral.parse(pinDef.getType(), literal);
            }
            if (instance.isNegated(pinName) && value instanceof Boolean b) {
                value = !b;
            }
            inputs.put(pinName, value);
        }
        return inputs;
    }

    private PinDefinition variadicPinTemplate(BlockTypeDefinition type, int index) {
        // For variadic blocks beyond the declared template pins (e.g. IN3, IN4...), reuse the last
        // declared input pin's type/default as the template, renaming it to the positional index.
        PinDefinition last = type.getInputs().get(type.getInputs().size() - 1);
        PinDefinition generated = new PinDefinition("IN" + (index + 1), last.getType(), last.getDefaultLiteral());
        return generated;
    }

    private Object resolveSourceValue(Network net, PinRef source, Set<String> feedbackConnIds, String connectionId) {
        boolean useFeedback = feedbackConnIds.contains(connectionId);
        Map<String, Object> bus = useFeedback ? previousValues : currentValues;
        String k = key(source.getBlockInstanceId(), source.getPinName());
        Object v = bus.get(k);
        if (v == null) {
            v = currentValues.get(k);
        }
        return v;
    }

    private static String key(String blockId, String pinName) {
        return (blockId == null ? "VAR" : blockId) + "." + pinName;
    }

    public Object getValue(String blockId, String pinName) {
        return currentValues.get(key(blockId, pinName));
    }

    public Object getVariableValue(String name) {
        return currentValues.get(key(null, name));
    }

    public void setVariableValue(String name, Object value) {
        currentValues.put(key(null, name), value);
    }

    public DataType variableType(String name) {
        return pou.getVariables().stream().filter(v -> v.getName().equals(name))
                .map(Variable::getType).findFirst().orElse(DataType.ANY);
    }
}
