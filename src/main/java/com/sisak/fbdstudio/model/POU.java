package com.sisak.fbdstudio.model;

import java.util.ArrayList;
import java.util.List;

/** Program organization unit: a Program, Function Block, or Function with its own variables and FBD body. */
public class POU {

    private String name;
    private POUKind kind = POUKind.PROGRAM;
    private List<Variable> variables = new ArrayList<>();
    private Network network = new Network();
    private String comment = "";

    public POU() {
    }

    public POU(String name, POUKind kind) {
        this.name = name;
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public POUKind getKind() {
        return kind;
    }

    public void setKind(POUKind kind) {
        this.kind = kind;
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Variable> variablesOfKind(VariableKind kind) {
        return variables.stream().filter(v -> v.getKind() == kind).toList();
    }
}
