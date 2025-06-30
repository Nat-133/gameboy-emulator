package org.gameboy.display;

import static org.gameboy.common.MemoryMapConstants.TILE_MAP_A_ADDRESS;
import static org.gameboy.common.MemoryMapConstants.TILE_MAP_B_ADDRESS;
import static org.gameboy.utils.BitUtilities.get_bit;

public class LcdcParser {
    static boolean lcdEnabled(byte lcdc) {
        return get_bit(lcdc, 7);
    }

    static int windowTileMap(byte lcdc) {
        return get_bit(lcdc, 6) ? TILE_MAP_B_ADDRESS : TILE_MAP_A_ADDRESS;
    }

    static boolean windowDisplayEnabled(byte lcdc) {
        return get_bit(lcdc, 5);
    }

    static boolean useUnsignedTileDataSelect(byte lcdc) {
        // if true, uses 8000 method, else uses 8800 method
        return get_bit(lcdc, 4);
    }

    static int backgroundTileMap(byte lcdc) {
        return get_bit(lcdc, 3) ? TILE_MAP_B_ADDRESS : TILE_MAP_A_ADDRESS;
    }

    static int spriteSize(byte lcdc) {
        return get_bit(lcdc, 2) ? 16 : 8;
    }

    static boolean backgroundAndWindowEnable(byte lcdc) {
        return get_bit(lcdc, 0);
    }
}
