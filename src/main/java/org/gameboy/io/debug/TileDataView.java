package org.gameboy.io.debug;

import org.gameboy.utils.MultiBitValue.TwoBitValue;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL41.*;

public class TileDataView {
    private static final int TILE_SIZE = 8;
    private static final int TILES_PER_ROW = 32;
    private static final int TILE_ROWS = 12;
    static final int TEX_WIDTH = TILES_PER_ROW * TILE_SIZE;   // 256
    static final int TEX_HEIGHT = TILE_ROWS * TILE_SIZE;       // 96

    private static final float[][] PALETTE = {
        {224f / 255f, 248f / 255f, 208f / 255f},
        {136f / 255f, 192f / 255f,  70f / 255f},
        { 52f / 255f, 104f / 255f,  50f / 255f},
        {  8f / 255f,  24f / 255f,  32f / 255f},
    };

    private TwoBitValue[][][] tileData;
    private final ByteBuffer textureData;
    private int textureId;
    private boolean dirty;

    public TileDataView() {
        tileData = new TwoBitValue[TILES_PER_ROW * TILE_ROWS][TILE_SIZE][TILE_SIZE];
        for (int tile = 0; tile < TILES_PER_ROW * TILE_ROWS; tile++) {
            for (int row = 0; row < TILE_SIZE; row++) {
                for (int col = 0; col < TILE_SIZE; col++) {
                    tileData[tile][row][col] = TwoBitValue.b00;
                }
            }
        }
        textureData = BufferUtils.createByteBuffer(TEX_WIDTH * TEX_HEIGHT * 3);
        dirty = true;
    }

    public void init() {
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, TEX_WIDTH, TEX_HEIGHT, 0,
                GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
    }

    public void updateTileData(TwoBitValue[][][] newTileData) {
        this.tileData = newTileData;
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
        for (int pixelY = 0; pixelY < TEX_HEIGHT; pixelY++) {
            for (int pixelX = 0; pixelX < TEX_WIDTH; pixelX++) {
                int tileX = pixelX / TILE_SIZE;
                int tileY = pixelY / TILE_SIZE;
                int tileIdx = tileY * TILES_PER_ROW + tileX;
                int row = pixelY % TILE_SIZE;
                int col = pixelX % TILE_SIZE;

                float[] color;
                if (tileIdx < tileData.length) {
                    color = PALETTE[tileData[tileIdx][row][col].value()];
                } else {
                    color = PALETTE[0];
                }
                textureData.put((byte) (color[0] * 255));
                textureData.put((byte) (color[1] * 255));
                textureData.put((byte) (color[2] * 255));
            }
        }
        textureData.flip();

        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, TEX_WIDTH, TEX_HEIGHT,
                GL_RGB, GL_UNSIGNED_BYTE, textureData);
    }
}
