package com.sisak.fbdstudio.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Convenience wrapper around a block's resolved input-pin values for one scan. */
public final class EvalContext {

    private final Map<String, Object> inputs;

    public EvalContext(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public boolean bool(String pin) {
        Object v = inputs.get(pin);
        return v instanceof Boolean b && b;
    }

    public double num(String pin) {
        Object v = inputs.get(pin);
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }

    public long time(String pin) {
        Object v = inputs.get(pin);
        return v instanceof Number n ? n.longValue() : 0L;
    }

    public String str(String pin) {
        Object v = inputs.get(pin);
        return v == null ? "" : String.valueOf(v);
    }

    public List<Double> nums(String prefix, int count) {
        List<Double> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(num(prefix + i));
        }
        return list;
    }

    public List<Boolean> bools(String prefix, int count) {
        List<Boolean> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(bool(prefix + i));
        }
        return list;
    }
}
