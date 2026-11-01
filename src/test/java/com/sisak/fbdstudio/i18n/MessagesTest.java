package com.sisak.fbdstudio.i18n;

import com.sisak.fbdstudio.library.BlockLibrary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MessagesTest {

    private static final List<String> EXAMPLE_IDS = List.of(
            "logic_basics", "blinking_alarm", "conveyor_interlock", "batch_counter",
            "tank_hysteresis", "pid_temperature", "seatbelt_warning", "two_pump_alternation",
            "debounce_circuit", "parking_garage_counter", "emergency_stop_chain", "star_delta_start"
    );

    // Every key the UI looks up; keeps all 9 properties files honest as new strings are added.
    // Block descriptions and example titles/descriptions are appended dynamically below, from the
    // actual block library and example list, instead of being duplicated here by hand.
    private static final List<String> REQUIRED_KEYS = buildRequiredKeys();

    private static List<String> buildRequiredKeys() {
        List<String> keys = new ArrayList<>(List.of(
            "app.title",
            "menu.file", "menu.file.new", "menu.file.open", "menu.file.save", "menu.file.saveAs",
            "menu.file.openExample", "menu.file.exit",
            "menu.edit", "menu.edit.undo", "menu.edit.redo",
            "menu.pou", "menu.pou.newProgram", "menu.pou.newFunctionBlock", "menu.pou.newFunction",
            "menu.simulation", "menu.simulation.run", "menu.simulation.pause", "menu.simulation.step",
            "menu.simulation.reset",
            "menu.window", "menu.window.center",
            "menu.help", "menu.help.about",
            "menu.language",
            "toolbar.run", "toolbar.pause", "toolbar.step", "toolbar.reset", "toolbar.fit",
            "palette.title", "palette.search",
            "inspector.title", "inspector.noSelection", "inspector.instanceName", "inspector.pinCount",
            "inspector.unknownType",
            "variables.title", "variables.col.name", "variables.col.kind", "variables.col.type",
            "variables.col.initial", "variables.col.comment", "variables.add", "variables.remove",
            "status.ready", "status.loaded", "status.saved", "status.simRunning", "status.simPaused",
            "status.scan",
            "dialog.newPou.header", "dialog.error.open.title", "dialog.error.save.title",
            "dialog.error.openExample.title",
            "about.title", "about.createdBy", "about.description",
            "examples.dialog.title",
            "category.LOGIC", "category.COMPARISON", "category.ARITHMETIC", "category.SELECTION",
            "category.BISTABLE", "category.EDGE", "category.TIMER", "category.COUNTER",
            "category.EXTENDED", "category.USER",
            "pouKind.PROGRAM", "pouKind.FUNCTION_BLOCK", "pouKind.FUNCTION",
            "pin.forceHint"
        ));
        for (var def : BlockLibrary.getInstance().all()) {
            keys.add("block.desc." + def.getId());
        }
        for (String exampleId : EXAMPLE_IDS) {
            keys.add("example." + exampleId + ".title");
            keys.add("example." + exampleId + ".description");
        }
        return keys;
    }

    @AfterEach
    void resetToEnglish() {
        Messages.setLocale(Language.EN.locale());
    }

    @ParameterizedTest
    @EnumSource(Language.class)
    void everyRequiredKeyResolvesInEveryLanguage(Language lang) {
        Messages.setLocale(lang.locale());
        for (String key : REQUIRED_KEYS) {
            String value = Messages.get(key);
            assertNotEquals(key, value, "Missing translation for '" + key + "' in " + lang);
        }
    }
}
