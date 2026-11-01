package com.sisak.fbdstudio.engine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure evaluation logic for every built-in (standard + extended) block type. Each method reads
 * typed inputs from an {@link EvalContext}, mutates the per-instance {@link BlockState} for
 * stateful blocks, and returns a map of output pin name -> value for this scan.
 */
public final class StandardBlockEvaluator {

    private StandardBlockEvaluator() {
    }

    /** @param pinCount resolved number of input pins for variadic blocks (AND/OR/ADD/...). */
    public static Map<String, Object> evaluate(String typeId, EvalContext in, BlockState state,
                                                 long dtMillis, int pinCount) {
        return switch (typeId) {
            case "AND" -> out("Q", in.bools("IN", pinCount).stream().allMatch(Boolean::booleanValue));
            case "OR" -> out("Q", in.bools("IN", pinCount).stream().anyMatch(Boolean::booleanValue));
            case "XOR" -> out("Q", in.bools("IN", pinCount).stream().filter(Boolean::booleanValue).count() % 2 == 1);
            case "NOT" -> out("Q", !in.bool("IN"));

            case "GT" -> out("Q", isMonotonic(in.nums("IN", pinCount), (a, b) -> a > b));
            case "GE" -> out("Q", isMonotonic(in.nums("IN", pinCount), (a, b) -> a >= b));
            case "EQ" -> out("Q", isMonotonic(in.nums("IN", pinCount), (a, b) -> a == b));
            case "NE" -> out("Q", in.num("IN1") != in.num("IN2"));
            case "LE" -> out("Q", isMonotonic(in.nums("IN", pinCount), (a, b) -> a <= b));
            case "LT" -> out("Q", isMonotonic(in.nums("IN", pinCount), (a, b) -> a < b));

            case "ADD" -> out("Q", in.nums("IN", pinCount).stream().mapToDouble(Double::doubleValue).sum());
            case "SUB" -> out("Q", in.num("IN1") - in.num("IN2"));
            case "MUL" -> out("Q", in.nums("IN", pinCount).stream().mapToDouble(d -> d).reduce(1.0, (a, b) -> a * b));
            case "DIV" -> out("Q", in.num("IN2") == 0 ? 0.0 : in.num("IN1") / in.num("IN2"));
            case "MOD" -> out("Q", in.num("IN2") == 0 ? 0.0 : (double) ((long) in.num("IN1") % (long) in.num("IN2")));

            case "MOVE" -> out("Q", in.num("IN"));
            case "SEL" -> out("Q", in.bool("G") ? in.num("IN1") : in.num("IN0"));
            case "MUX" -> evalMux(in, pinCount);
            case "LIMIT" -> out("Q", Math.max(in.num("MN"), Math.min(in.num("MX"), in.num("IN"))));
            case "MAX" -> out("Q", in.nums("IN", pinCount).stream().mapToDouble(d -> d).max().orElse(0));
            case "MIN" -> out("Q", in.nums("IN", pinCount).stream().mapToDouble(d -> d).min().orElse(0));

            case "SR" -> evalSR(in, state);
            case "RS" -> evalRS(in, state);
            case "R_TRIG" -> evalEdge(in, state, true);
            case "F_TRIG" -> evalEdge(in, state, false);

            case "TON" -> evalTon(in, state, dtMillis);
            case "TOF" -> evalTof(in, state, dtMillis);
            case "TP" -> evalTp(in, state, dtMillis);

            case "CTU" -> evalCtu(in, state);
            case "CTD" -> evalCtd(in, state);
            case "CTUD" -> evalCtud(in, state);

            case "PID" -> evalPid(in, state, dtMillis);
            case "SCALE" -> evalScale(in);
            case "HYSTERESIS" -> evalHysteresis(in, state);

            default -> Map.of();
        };
    }

    // ---- helpers -----------------------------------------------------

    private static Map<String, Object> out(String pin, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(pin, value);
        return m;
    }

    private interface Cmp {
        boolean test(double a, double b);
    }

    private static boolean isMonotonic(List<Double> values, Cmp cmp) {
        for (int i = 0; i + 1 < values.size(); i++) {
            if (!cmp.test(values.get(i), values.get(i + 1))) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, Object> evalMux(EvalContext in, int pinCount) {
        int k = (int) in.num("K");
        int n = Math.max(1, pinCount);
        int idx = Math.max(0, Math.min(n - 1, k));
        return out("Q", in.num("IN" + idx));
    }

    private static Map<String, Object> evalSR(EvalContext in, BlockState state) {
        boolean q = state.getBool("Q1", false);
        if (in.bool("S1")) {
            q = true;
        } else if (in.bool("R")) {
            q = false;
        }
        state.put("Q1", q);
        return out("Q1", q);
    }

    private static Map<String, Object> evalRS(EvalContext in, BlockState state) {
        boolean q = state.getBool("Q1", false);
        if (in.bool("R1")) {
            q = false;
        } else if (in.bool("S")) {
            q = true;
        }
        state.put("Q1", q);
        return out("Q1", q);
    }

    private static Map<String, Object> evalEdge(EvalContext in, BlockState state, boolean rising) {
        boolean clk = in.bool("CLK");
        boolean prev = state.getBool("prevClk", false);
        boolean q = rising ? (clk && !prev) : (!clk && prev);
        state.put("prevClk", clk);
        return out("Q", q);
    }

    private static Map<String, Object> evalTon(EvalContext in, BlockState state, long dtMillis) {
        boolean inVal = in.bool("IN");
        long pt = in.time("PT");
        long elapsed = state.getLong("elapsed", 0L);
        if (inVal) {
            elapsed = Math.min(pt, elapsed + dtMillis);
        } else {
            elapsed = 0;
        }
        state.put("elapsed", elapsed);
        boolean q = inVal && elapsed >= pt;
        Map<String, Object> m = out("Q", q);
        m.put("ET", elapsed);
        return m;
    }

    private static Map<String, Object> evalTof(EvalContext in, BlockState state, long dtMillis) {
        boolean inVal = in.bool("IN");
        long pt = in.time("PT");
        boolean prevIn = state.getBool("prevIn", false);
        long elapsed = state.getLong("elapsed", 0L);
        if (inVal) {
            elapsed = 0;
        } else if (prevIn || elapsed > 0) {
            elapsed = Math.min(pt, elapsed + dtMillis);
        }
        state.put("elapsed", elapsed);
        state.put("prevIn", inVal);
        boolean q = inVal || elapsed < pt;
        Map<String, Object> m = out("Q", q);
        m.put("ET", elapsed);
        return m;
    }

    private static Map<String, Object> evalTp(EvalContext in, BlockState state, long dtMillis) {
        boolean inVal = in.bool("IN");
        long pt = in.time("PT");
        boolean prevIn = state.getBool("prevIn", false);
        boolean running = state.getBool("running", false);
        long elapsed = state.getLong("elapsed", 0L);

        if (inVal && !prevIn && !running) {
            running = true;
            elapsed = 0;
        }
        boolean q = false;
        if (running) {
            elapsed = Math.min(pt, elapsed + dtMillis);
            q = true;
            if (elapsed >= pt) {
                running = false;
            }
        }
        state.put("prevIn", inVal);
        state.put("running", running);
        state.put("elapsed", elapsed);
        Map<String, Object> m = out("Q", q);
        m.put("ET", elapsed);
        return m;
    }

    private static Map<String, Object> evalCtu(EvalContext in, BlockState state) {
        long pv = (long) in.num("PV");
        boolean cu = in.bool("CU");
        boolean prevCu = state.getBool("prevCU", false);
        long cv = state.getLong("CV", 0L);
        if (in.bool("R")) {
            cv = 0;
        } else if (cu && !prevCu) {
            cv = Math.min(pv, cv + 1);
        }
        state.put("prevCU", cu);
        state.put("CV", cv);
        Map<String, Object> m = out("Q", cv >= pv);
        m.put("CV", cv);
        return m;
    }

    private static Map<String, Object> evalCtd(EvalContext in, BlockState state) {
        long pv = (long) in.num("PV");
        boolean cd = in.bool("CD");
        boolean prevCd = state.getBool("prevCD", false);
        long cv = state.getLong("CV", pv);
        if (in.bool("LD")) {
            cv = pv;
        } else if (cd && !prevCd) {
            cv = Math.max(0, cv - 1);
        }
        state.put("prevCD", cd);
        state.put("CV", cv);
        Map<String, Object> m = out("Q", cv <= 0);
        m.put("CV", cv);
        return m;
    }

    private static Map<String, Object> evalCtud(EvalContext in, BlockState state) {
        long pv = (long) in.num("PV");
        boolean cu = in.bool("CU");
        boolean cd = in.bool("CD");
        boolean prevCu = state.getBool("prevCU", false);
        boolean prevCd = state.getBool("prevCD", false);
        long cv = state.getLong("CV", 0L);
        if (in.bool("R")) {
            cv = 0;
        } else if (in.bool("LD")) {
            cv = pv;
        } else {
            if (cu && !prevCu) {
                cv = Math.min(pv, cv + 1);
            }
            if (cd && !prevCd) {
                cv = Math.max(0, cv - 1);
            }
        }
        state.put("prevCU", cu);
        state.put("prevCD", cd);
        state.put("CV", cv);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("QU", cv >= pv);
        m.put("QD", cv <= 0);
        m.put("CV", cv);
        return m;
    }

    private static Map<String, Object> evalPid(EvalContext in, BlockState state, long dtMillis) {
        if (!in.bool("EN")) {
            state.put("integral", 0.0);
            state.put("prevErr", 0.0);
            Map<String, Object> m = out("OUT", 0.0);
            m.put("ERR", 0.0);
            return m;
        }
        double dtSec = Math.max(0.001, dtMillis / 1000.0);
        double err = in.num("SP") - in.num("PV");
        double integral = state.getDouble("integral", 0.0) + err * dtSec;
        double prevErr = state.getDouble("prevErr", 0.0);
        double derivative = (err - prevErr) / dtSec;

        double kp = in.num("KP");
        double ki = in.num("KI");
        double kd = in.num("KD");
        double raw = kp * err + ki * integral + kd * derivative;
        double mn = in.num("MN");
        double mx = in.num("MX");
        double clamped = Math.max(mn, Math.min(mx, raw));

        // simple anti-windup: only accumulate the integral while not saturated
        if (raw != clamped) {
            integral = state.getDouble("integral", 0.0);
        }
        state.put("integral", integral);
        state.put("prevErr", err);

        Map<String, Object> m = out("OUT", clamped);
        m.put("ERR", err);
        return m;
    }

    private static Map<String, Object> evalScale(EvalContext in) {
        double inMin = in.num("INMIN");
        double inMax = in.num("INMAX");
        double outMin = in.num("OUTMIN");
        double outMax = in.num("OUTMAX");
        double span = inMax - inMin;
        double ratio = span == 0 ? 0 : (in.num("IN") - inMin) / span;
        return out("OUT", outMin + ratio * (outMax - outMin));
    }

    private static Map<String, Object> evalHysteresis(EvalContext in, BlockState state) {
        boolean q = state.getBool("Q", false);
        double v = in.num("IN");
        if (!q && v >= in.num("HIGH")) {
            q = true;
        } else if (q && v <= in.num("LOW")) {
            q = false;
        }
        state.put("Q", q);
        return out("Q", q);
    }
}
