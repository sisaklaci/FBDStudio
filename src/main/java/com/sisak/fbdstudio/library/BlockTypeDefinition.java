package com.sisak.fbdstudio.library;

import com.sisak.fbdstudio.i18n.Messages;
import com.sisak.fbdstudio.model.PinDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Declarative description of a block type that can be dragged onto the canvas: a standard IEC
 * 61131-3 block, an extended vendor-common block, or (at runtime) a user-defined function block
 * derived from a {@link com.sisak.fbdstudio.model.POU} of kind {@code FUNCTION_BLOCK}.
 */
public class BlockTypeDefinition {

    private String id;
    private String displayName;
    private BlockCategory category = BlockCategory.EXTENDED;
    private boolean standard;
    private String description = "";
    private List<PinDefinition> inputs = new ArrayList<>();
    private List<PinDefinition> outputs = new ArrayList<>();

    /** True for AND/OR/XOR/ADD/MUL/MAX/MIN-style blocks whose input pin count can be extended. */
    private boolean variadicInputs;
    private int minInputs = 2;
    private int maxInputs = 16;

    /** True for blocks with scan-to-scan internal state (timers, counters, bistables, edge detectors). */
    private boolean stateful;

    public BlockTypeDefinition() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public BlockCategory getCategory() {
        return category;
    }

    public void setCategory(BlockCategory category) {
        this.category = category;
    }

    public boolean isStandard() {
        return standard;
    }

    public void setStandard(boolean standard) {
        this.standard = standard;
    }

    /**
     * Looked up live from the current language ({@code block.desc.<ID>}); falls back to the
     * English text from the block's JSON definition if that key is ever missing from a locale.
     */
    public String getDescription() {
        String key = "block.desc." + id;
        String translated = Messages.get(key);
        return translated.equals(key) ? description : translated;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PinDefinition> getInputs() {
        return inputs;
    }

    public void setInputs(List<PinDefinition> inputs) {
        this.inputs = inputs;
    }

    public List<PinDefinition> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<PinDefinition> outputs) {
        this.outputs = outputs;
    }

    public boolean isVariadicInputs() {
        return variadicInputs;
    }

    public void setVariadicInputs(boolean variadicInputs) {
        this.variadicInputs = variadicInputs;
    }

    public int getMinInputs() {
        return minInputs;
    }

    public void setMinInputs(int minInputs) {
        this.minInputs = minInputs;
    }

    public int getMaxInputs() {
        return maxInputs;
    }

    public void setMaxInputs(int maxInputs) {
        this.maxInputs = maxInputs;
    }

    public boolean isStateful() {
        return stateful;
    }

    public void setStateful(boolean stateful) {
        this.stateful = stateful;
    }

    public PinDefinition findInput(String name) {
        return inputs.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

    public PinDefinition findOutput(String name) {
        return outputs.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }
}
