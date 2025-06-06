package org.gameboy.display;

import static org.gameboy.utils.BitUtilities.get_bit;

public record SpriteData(byte y, byte x, byte tileNumber, byte flags) {
    public boolean drawSpriteOverBackgroundFlag() {
        return get_bit(flags, 7);
    }

    public boolean yFlipFlag() {
        return get_bit(flags, 6);
    }

    public boolean xFlipFlag() {
        return get_bit(flags, 5);
    }

    public boolean paletteFlag() {
        return get_bit(flags, 4);
    }
}
