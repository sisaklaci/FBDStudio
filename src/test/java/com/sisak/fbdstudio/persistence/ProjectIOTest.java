package com.sisak.fbdstudio.persistence;

import com.sisak.fbdstudio.engine.SimulationEngine;
import com.sisak.fbdstudio.model.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProjectIOTest {

    @Test
    void saveThenLoadRoundTripsEquivalentProject(@org.junit.jupiter.api.io.TempDir File tempDir) throws IOException {
        Project original = ProjectIO.newBlankProject("Round Trip Test");
        File file = new File(tempDir, "round-trip.fbdproj");
        ProjectIO.save(original, file);

        Project loaded = ProjectIO.load(file);
        assertEquals(original.getName(), loaded.getName());
        assertEquals(original.getPous().size(), loaded.getPous().size());
        assertEquals(original.getFormatVersion(), loaded.getFormatVersion());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "logic_basics.fbdproj", "blinking_alarm.fbdproj", "conveyor_interlock.fbdproj",
            "batch_counter.fbdproj", "tank_hysteresis.fbdproj", "pid_temperature.fbdproj",
            "seatbelt_warning.fbdproj", "two_pump_alternation.fbdproj", "debounce_circuit.fbdproj",
            "parking_garage_counter.fbdproj", "emergency_stop_chain.fbdproj", "star_delta_start.fbdproj"
    })
    void bundledExampleLoadsAndRunsTenScansWithoutError(String resourceName) throws IOException {
        Project project = ProjectIO.load(getClass().getResourceAsStream("/examples/" + resourceName));
        assertNotNull(project);
        assertFalse(project.getPous().isEmpty(), resourceName + " should declare at least one POU");

        SimulationEngine engine = new SimulationEngine(project.getPous().get(0));
        for (int i = 0; i < 10; i++) {
            engine.step(100);
        }
        assertEquals(10, engine.getScanCount());
    }
}
