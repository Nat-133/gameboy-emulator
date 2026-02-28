package org.gameboy.io;

import org.gameboy.display.Display;
import org.gameboy.display.PixelBuffer;
import org.gameboy.display.PixelValue;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL41.*;

public class WindowDisplay implements Display {
    private final PixelBuffer pixelBuffer;
    private final float[][] palette;
    private final ByteBuffer textureData;

    private int textureId;
    private int shaderProgram;
    private int vao;
    private int vbo;
    private boolean dirty;

    public WindowDisplay(float[][] palette) {
        this.pixelBuffer = new PixelBuffer();
        this.palette = palette;
        this.textureData = BufferUtils.createByteBuffer(DISPLAY_WIDTH * DISPLAY_HEIGHT * 3);
        this.dirty = true;
    }

    @Override
    public void setPixel(int x, int y, PixelValue value) {
        pixelBuffer.setPixel(x, y, value);
    }

    @Override
    public void onVBlank() {
        pixelBuffer.swapBuffers();
        dirty = true;
    }

    public void init() {
        shaderProgram = createShaderProgram();
        textureId = createTexture();
        createQuad();
    }

    public void updateTexture() {
        if (dirty) {
            updateTextureData();
            uploadTexture();
            dirty = false;
        }
    }

    public int getTextureId() {
        return textureId;
    }

    public void render() {
        updateTexture();

        glUseProgram(shaderProgram);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform1i(glGetUniformLocation(shaderProgram, "screenTexture"), 0);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);

        glUseProgram(0);
    }

    public boolean needsRender() {
        return dirty;
    }

    public void cleanup() {
        glDeleteProgram(shaderProgram);
        glDeleteTextures(textureId);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }

    private void updateTextureData() {
        textureData.clear();
        for (int y = 0; y < DISPLAY_HEIGHT; y++) {
            for (int x = 0; x < DISPLAY_WIDTH; x++) {
                PixelValue pixel = pixelBuffer.getDisplayPixel(x, y);
                int colorIndex = pixel.value() & 0x3;
                float[] color = palette[colorIndex];
                textureData.put((byte) (color[0] * 255));
                textureData.put((byte) (color[1] * 255));
                textureData.put((byte) (color[2] * 255));
            }
        }
        textureData.flip();
    }

    private void uploadTexture() {
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0,
                DISPLAY_WIDTH, DISPLAY_HEIGHT,
                GL_RGB, GL_UNSIGNED_BYTE, textureData);
    }

    private int createTexture() {
        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB,
                DISPLAY_WIDTH, DISPLAY_HEIGHT, 0,
                GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        return tex;
    }

    private int createShaderProgram() {
        String vertexSource = loadShaderResource("shaders/vertex.glsl");
        String fragmentSource = loadShaderResource("shaders/fragment.glsl");

        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new RuntimeException("Shader program linking failed: " + log);
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    private int compileShader(int type, String source) {
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

    private String loadShaderResource(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Shader resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader: " + path, e);
        }
    }

    private void createQuad() {
        // Fullscreen quad: position (x,y) + texcoord (u,v)
        // Texture v is flipped (top-left origin for Game Boy, bottom-left for OpenGL)
        float[] vertices = {
            -1.0f,  1.0f,  0.0f, 0.0f,  // top-left
            -1.0f, -1.0f,  0.0f, 1.0f,  // bottom-left
             1.0f,  1.0f,  1.0f, 0.0f,  // top-right
             1.0f, -1.0f,  1.0f, 1.0f,  // bottom-right
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        ByteBuffer vertexBuffer = BufferUtils.createByteBuffer(vertices.length * Float.BYTES);
        for (float v : vertices) {
            vertexBuffer.putFloat(v);
        }
        vertexBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        // Position attribute (location = 0)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Texture coordinate attribute (location = 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }
}
