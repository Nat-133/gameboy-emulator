package org.gameboy.display.debug;

import org.gameboy.common.Memory;
import org.gameboy.utils.BitUtilities;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

public class TileRenderer {
    private static final int TILE_WIDTH = 8;
    private static final int TILE_HEIGHT = 8;
    private static final int BYTES_PER_TILE = 16;
    private static final int TILE_DATA_START = 0x8000;
    private static final int TILE_DATA_SIGNED_BASE = 0x9000;

    private final Memory memory;

    public TileRenderer(Memory memory) {
        this.memory = memory;
    }

    public TwoBitValue[][] renderTile(int tileNumber, boolean useSigned) {
        int tileDataAddress = useSigned
            ? getSignedTileAddress(tileNumber)
            : getUnsignedTileAddress(tileNumber);

        TwoBitValue[][] pixels = new TwoBitValue[TILE_HEIGHT][TILE_WIDTH];

        for (int row = 0; row < TILE_HEIGHT; row++) {
            int rowAddress = tileDataAddress + (row * 2);
            byte lowByte = memory.read((short) rowAddress);
            byte highByte = memory.read((short) (rowAddress + 1));

            for (int col = 0; col < TILE_WIDTH; col++) {
                int bitIndex = 7 - col;
                int pixelValue = (BitUtilities.get_bit(lowByte, bitIndex) ? 1 : 0)
                               + (BitUtilities.get_bit(highByte, bitIndex) ? 2 : 0);
                pixels[row][col] = TwoBitValue.from(pixelValue);
            }
        }

        return pixels;
    }

    public TwoBitValue[][] renderTileMap(int tileMapAddress, boolean useSigned) {
        TwoBitValue[][] fullMap = new TwoBitValue[256][256];

        for (int tileY = 0; tileY < 32; tileY++) {
            for (int tileX = 0; tileX < 32; tileX++) {
                int tileIndex = tileY * 32 + tileX;
                int tileMapAddr = tileMapAddress + tileIndex;
                byte tileNumber = memory.read((short) tileMapAddr);

                TwoBitValue[][] tile = renderTile(Byte.toUnsignedInt(tileNumber), useSigned);

                int basePixelY = tileY * TILE_HEIGHT;
                int basePixelX = tileX * TILE_WIDTH;

                for (int py = 0; py < TILE_HEIGHT; py++) {
                    System.arraycopy(tile[py], 0, fullMap[basePixelY + py], basePixelX + 0, TILE_WIDTH);
                }
            }
        }

        return fullMap;
    }

    private int getUnsignedTileAddress(int tileNumber) {
        return TILE_DATA_START + (tileNumber * BYTES_PER_TILE);
    }

    private int getSignedTileAddress(int tileNumber) {
        return TILE_DATA_SIGNED_BASE + (((byte) tileNumber) * BYTES_PER_TILE);
    }
}
