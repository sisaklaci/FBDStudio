package com.sisak.fbdstudio.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A full FBD Studio project: one or more POUs plus project-wide global variables. */
public class Project {

    public static final int FILE_FORMAT_VERSION = 1;

    private int formatVersion = FILE_FORMAT_VERSION;
    private String name = "Untitled";
    private String author = "";
    private String description = "";
    private List<Variable> globalVariables = new ArrayList<>();
    private List<POU> pous = new ArrayList<>();

    public int getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(int formatVersion) {
        this.formatVersion = formatVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Variable> getGlobalVariables() {
        return globalVariables;
    }

    public void setGlobalVariables(List<Variable> globalVariables) {
        this.globalVariables = globalVariables;
    }

    public List<POU> getPous() {
        return pous;
    }

    public void setPous(List<POU> pous) {
        this.pous = pous;
    }

    public Optional<POU> findPou(String name) {
        return pous.stream().filter(p -> p.getName().equals(name)).findFirst();
    }
}
