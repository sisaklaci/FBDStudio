package com.sisak.fbdstudio.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sisak.fbdstudio.model.Project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/** Reads/writes the native {@code .fbdproj} JSON project format. */
public final class ProjectIO {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private ProjectIO() {
    }

    public static void save(Project project, File file) throws IOException {
        MAPPER.writeValue(file, project);
    }

    public static Project load(File file) throws IOException {
        return MAPPER.readValue(file, Project.class);
    }

    public static Project load(InputStream in) throws IOException {
        return MAPPER.readValue(in, Project.class);
    }

    public static Project newBlankProject(String name) {
        Project p = new Project();
        p.setName(name);
        com.sisak.fbdstudio.model.POU main = new com.sisak.fbdstudio.model.POU("MAIN", com.sisak.fbdstudio.model.POUKind.PROGRAM);
        p.getPous().add(main);
        return p;
    }
}
