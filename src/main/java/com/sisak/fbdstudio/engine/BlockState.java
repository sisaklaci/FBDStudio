package com.sisak.fbdstudio.engine;

import java.util.HashMap;
import java.util.Map;

/** Scratch storage for one block instance's scan-to-scan internal state (timers, counters, edges, bistables). */
public class BlockState {
    private final Map<String, Object> fields = new HashMap<>();

    public Object get(String key) {
        return fields.get(key);
    }

    public double getDouble(String key, double def) {
        Object v = fields.get(key);
        return v instanceof Number n ? n.doubleValue() : def;
    }

    public boolean getBool(String key, boolean def) {
        Object v = fields.get(key);
        return v instanceof Boolean b ? b : def;
    }

    public long getLong(String key, long def) {
        Object v = fields.get(key);
        return v instanceof Number n ? n.longValue() : def;
    }

    public void put(String key, Object value) {
        fields.put(key, value);
    }

    public void reset() {
        fields.clear();
    }
}
