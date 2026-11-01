package com.sisak.fbdstudio.engine;

import com.sisak.fbdstudio.model.BlockInstance;
import com.sisak.fbdstudio.model.Connection;
import com.sisak.fbdstudio.model.DataType;
import com.sisak.fbdstudio.model.POU;
import com.sisak.fbdstudio.model.POUKind;
import com.sisak.fbdstudio.model.PinRef;
import com.sisak.fbdstudio.model.Variable;
import com.sisak.fbdstudio.model.VariableKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationEngineTest {

    @Test
    void andGateDrivenByTwoInputVariables() {
        POU pou = new POU("MAIN", POUKind.PROGRAM);
        pou.getVariables().add(new Variable("A", DataType.BOOL, VariableKind.VAR_INPUT));
        pou.getVariables().add(new Variable("B", DataType.BOOL, VariableKind.VAR_INPUT));
        pou.getVariables().add(new Variable("OUT", DataType.BOOL, VariableKind.VAR_OUTPUT));

        BlockInstance and = new BlockInstance("AND", 0, 0);
        pou.getNetwork().getBlocks().add(and);
        pou.getNetwork().getConnections().add(new Connection(PinRef.ofVariable("A"), new PinRef(and.getId(), "IN1")));
        pou.getNetwork().getConnections().add(new Connection(PinRef.ofVariable("B"), new PinRef(and.getId(), "IN2")));
        pou.getNetwork().getConnections().add(new Connection(new PinRef(and.getId(), "Q"), PinRef.ofVariable("OUT")));

        SimulationEngine engine = new SimulationEngine(pou);
        engine.setVariableValue("A", true);
        engine.setVariableValue("B", false);
        engine.step(100);
        assertEquals(Boolean.FALSE, engine.getVariableValue("OUT"));

        engine.setVariableValue("B", true);
        engine.step(100);
        assertEquals(Boolean.TRUE, engine.getVariableValue("OUT"));
    }

    @Test
    void tonTimerReachesSetpointAfterPresetTime() {
        POU pou = new POU("MAIN", POUKind.PROGRAM);
        pou.getVariables().add(new Variable("START", DataType.BOOL, VariableKind.VAR_INPUT));
        pou.getVariables().add(new Variable("DONE", DataType.BOOL, VariableKind.VAR_OUTPUT));

        BlockInstance ton = new BlockInstance("TON", 0, 0);
        ton.getPinLiterals().put("PT", "T#1s");
        pou.getNetwork().getBlocks().add(ton);
        pou.getNetwork().getConnections().add(new Connection(PinRef.ofVariable("START"), new PinRef(ton.getId(), "IN")));
        pou.getNetwork().getConnections().add(new Connection(new PinRef(ton.getId(), "Q"), PinRef.ofVariable("DONE")));

        SimulationEngine engine = new SimulationEngine(pou);
        engine.setVariableValue("START", true);

        for (int i = 0; i < 9; i++) {
            engine.step(100);
        }
        assertFalse((Boolean) engine.getVariableValue("DONE"), "should not be done before 1s elapsed");

        engine.step(100); // total 1000ms
        assertTrue((Boolean) engine.getVariableValue("DONE"), "should be done at exactly 1s");
    }

    @Test
    void srLatchIsSetDominant() {
        POU pou = new POU("MAIN", POUKind.PROGRAM);
        pou.getVariables().add(new Variable("SET", DataType.BOOL, VariableKind.VAR_INPUT));
        pou.getVariables().add(new Variable("RESET", DataType.BOOL, VariableKind.VAR_INPUT));
        pou.getVariables().add(new Variable("Q", DataType.BOOL, VariableKind.VAR_OUTPUT));

        BlockInstance sr = new BlockInstance("SR", 0, 0);
        pou.getNetwork().getBlocks().add(sr);
        pou.getNetwork().getConnections().add(new Connection(PinRef.ofVariable("SET"), new PinRef(sr.getId(), "S1")));
        pou.getNetwork().getConnections().add(new Connection(PinRef.ofVariable("RESET"), new PinRef(sr.getId(), "R")));
        pou.getNetwork().getConnections().add(new Connection(new PinRef(sr.getId(), "Q1"), PinRef.ofVariable("Q")));

        SimulationEngine engine = new SimulationEngine(pou);
        engine.setVariableValue("SET", true);
        engine.setVariableValue("RESET", false);
        engine.step(10);
        assertTrue((Boolean) engine.getVariableValue("Q"));

        engine.setVariableValue("SET", false);
        engine.step(10);
        assertTrue((Boolean) engine.getVariableValue("Q"), "latch should hold after SET goes false");

        engine.setVariableValue("RESET", true);
        engine.step(10);
        assertFalse((Boolean) engine.getVariableValue("Q"));

        // both true -> set dominant
        engine.setVariableValue("SET", true);
        engine.step(10);
        assertTrue((Boolean) engine.getVariableValue("Q"));
    }

    @Test
    void feedbackLoopIsResolvedUsingPreviousScanValue() {
        // NOT block fed by its own output (classic oscillator / algebraic loop test): should not
        // deadlock, and the resolver should mark the loop-closing connection as feedback.
        POU pou = new POU("MAIN", POUKind.PROGRAM);
        BlockInstance not1 = new BlockInstance("NOT", 0, 0);
        pou.getNetwork().getBlocks().add(not1);
        Connection loop = new Connection(new PinRef(not1.getId(), "Q"), new PinRef(not1.getId(), "IN"));
        pou.getNetwork().getConnections().add(loop);

        ExecutionOrderResolver.Result result = ExecutionOrderResolver.resolve(pou.getNetwork());
        assertEquals(1, result.order().size());
        assertTrue(result.feedbackConnectionIds().contains(loop.getId()));

        SimulationEngine engine = new SimulationEngine(pou);
        engine.step(10);
        engine.step(10);
        engine.step(10); // must terminate without exceptions/infinite loop
    }
}
