package org.gameboy.io.shell;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL41.*;

public class ShaderProgram {
    private final int programId;

    public ShaderProgram(String vertexPath, String fragmentPath) {
        String vertexSource = loadShaderResource(vertexPath);
        String fragmentSource = loadShaderResource(fragmentPath);

        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        programId = glCreateProgram();
        glAttachShader(programId, vertexShader);
        glAttachShader(programId, fragmentShader);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programId);
            glDeleteProgram(programId);
            throw new RuntimeException("Shader program linking failed: " + log);
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    public void use() {
        glUseProgram(programId);
    }

    public int getUniformLocation(String name) {
        return glGetUniformLocation(programId, name);
    }

    public void setUniform1f(int location, float value) {
        glUniform1f(location, value);
    }

    public void setUniform2f(int location, float x, float y) {
        glUniform2f(location, x, y);
    }

    public void setUniform3f(int location, float x, float y, float z) {
        glUniform3f(location, x, y, z);
    }

    public void setUniform4f(int location, float x, float y, float z, float w) {
        glUniform4f(location, x, y, z, w);
    }

    public void setUniform1i(int location, int value) {
        glUniform1i(location, value);
    }

    public void setUniform1fv(int location, float[] values) {
        glUniform1fv(location, values);
    }

    public void setUniform1iv(int location, int[] values) {
        glUniform1iv(location, values);
    }

    public void cleanup() {
        glDeleteProgram(programId);
    }

    private static int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new RuntimeException("Shader compilation failed: " + log);
        }

        return shader;
    }

    private static String loadShaderResource(String path) {
        try (InputStream is = ShaderProgram.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Shader resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader: " + path, e);
        }
    }
}
