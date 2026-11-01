package com.sisak.fbdstudio.library;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads the built-in standard + extended block definitions from classpath JSON resources. */
public final class BlockLibrary {

    private static final List<String> RESOURCE_FILES = List.of(
            "/blocks/logic.json",
            "/blocks/comparison.json",
            "/blocks/arithmetic.json",
            "/blocks/selection.json",
            "/blocks/bistable.json",
            "/blocks/edge.json",
            "/blocks/timer.json",
            "/blocks/counter.json",
            "/blocks/extended.json"
    );

    private static BlockLibrary instance;

    private final Map<String, BlockTypeDefinition> byId = new LinkedHashMap<>();

    private BlockLibrary() {
        ObjectMapper mapper = new ObjectMapper();
        for (String resource : RESOURCE_FILES) {
            try (InputStream in = BlockLibrary.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IllegalStateException("Missing block library resource: " + resource);
                }
                List<BlockTypeDefinition> defs = mapper.readValue(in, mapper.getTypeFactory()
                        .constructCollectionType(List.class, BlockTypeDefinition.class));
                for (BlockTypeDefinition def : defs) {
                    byId.put(def.getId(), def);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load block library resource: " + resource, e);
            }
        }
    }

    public static synchronized BlockLibrary getInstance() {
        if (instance == null) {
            instance = new BlockLibrary();
        }
        return instance;
    }

    public BlockTypeDefinition get(String id) {
        return byId.get(id);
    }

    public boolean contains(String id) {
        return byId.containsKey(id);
    }

    public List<BlockTypeDefinition> all() {
        return Collections.unmodifiableList(new ArrayList<>(byId.values()));
    }

    public List<BlockTypeDefinition> byCategory(BlockCategory category) {
        return all().stream().filter(b -> b.getCategory() == category).toList();
    }
}
