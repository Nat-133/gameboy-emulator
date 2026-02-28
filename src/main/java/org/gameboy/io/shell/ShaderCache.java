package org.gameboy.io.shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShaderCache {
    private final Map<String, ShaderProgram> cache = new HashMap<>();

    public ShaderProgram get(String vertexPath, String fragmentPath) {
        String key = vertexPath + "|" + fragmentPath;
        return cache.computeIfAbsent(key, k -> new ShaderProgram(vertexPath, fragmentPath));
    }

    public void cleanup() {
        List<ShaderProgram> programs = new ArrayList<>(cache.values());
        cache.clear();
        for (ShaderProgram program : programs) {
            program.cleanup();
        }
    }
}
