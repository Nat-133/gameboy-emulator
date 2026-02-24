package org.gameboy.io.debug;

import org.gameboy.utils.MultiBitValue.TwoBitValue;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL41.*;

public class TileMapView {
    private static final int MAP_SIZE = 256;

    private static final float[][] PALETTE = {
        {224f / 255f, 248f / 255f, 208f / 255f},
        {136f / 255f, 192f / 255f,  70f / 255f},
        { 52f / 255f, 104f / 255f,  50f / 255f},
        {  8f / 255f,  24f / 255f,  32f / 255f},
    };

    private TwoBitValue[][] tileMapData;
    private final ByteBuffer textureData;
    private int textureId;
    private boolean dirty;

    private int viewportX, viewportY, viewportW, viewportH;
    private boolean hasViewport;

    public TileMapView() {
        tileMapData = new TwoBitValue[MAP_SIZE][MAP_SIZE];
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                tileMapData[y][x] = TwoBitValue.b00;
            }
        }
        textureData = BufferUtils.createByteBuffer(MAP_SIZE * MAP_SIZE * 3);
        dirty = true;
    }

    public void init() {
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, MAP_SIZE, MAP_SIZE, 0,
                GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
    }

    public void updateTileMap(TwoBitValue[][] newData) {
        this.tileMapData = newData;
        this.dirty = true;
    }

    public void setViewport(int x, int y, int width, int height) {
        this.viewportX = x;
        this.viewportY = y;
        this.viewportW = width;
        this.viewportH = height;
        this.hasViewport = true;
        this.dirty = true;
    }

    public void clearViewport() {
        this.hasViewport = false;
        this.dirty = true;
    }

    public void render(int shaderProgram, int vao) {
        if (dirty) {
            rebuildTexture();
            dirty = false;
        }

        glUseProgram(shaderProgram);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform1i(glGetUniformLocation(shaderProgram, "screenTexture"), 0);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glBindVertexArray(0);

        glUseProgram(0);
    }

    public void cleanup() {
        glDeleteTextures(textureId);
    }

    private void rebuildTexture() {
        textureData.clear();
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                float[] color = PALETTE[tileMapData[y][x].value()];
                textureData.put((byte) (color[0] * 255));
                textureData.put((byte) (color[1] * 255));
                textureData.put((byte) (color[2] * 255));
            }
        }

        if (hasViewport) {
            drawViewportOverlay();
        }

        textureData.flip();

        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, MAP_SIZE, MAP_SIZE,
                GL_RGB, GL_UNSIGNED_BYTE, textureData);
    }

    private void drawViewportOverlay() {
        // Draw red rectangle border into the texture data
        byte r = (byte) 255, g = 0, b = 0;

        for (int i = 0; i < viewportW; i++) {
            setPixelInBuffer((viewportX + i) % MAP_SIZE, viewportY, r, g, b);
            setPixelInBuffer((viewportX + i) % MAP_SIZE, (viewportY + viewportH - 1) % MAP_SIZE, r, g, b);
        }
        for (int i = 0; i < viewportH; i++) {
            setPixelInBuffer(viewportX, (viewportY + i) % MAP_SIZE, r, g, b);
            setPixelInBuffer((viewportX + viewportW - 1) % MAP_SIZE, (viewportY + i) % MAP_SIZE, r, g, b);
        }
    }

    private void setPixelInBuffer(int x, int y, byte r, byte g, byte b) {
        int offset = (y * MAP_SIZE + x) * 3;
        textureData.put(offset, r);
        textureData.put(offset + 1, g);
        textureData.put(offset + 2, b);
    }
}
