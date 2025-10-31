package org.gameboy.display;

import org.gameboy.utils.MultiBitValue.TwoBitValue;

public record SpritePixel(
        TwoBitValue colorIndex,
        boolean useOBP1,
        boolean bgPriority
) {

    public boolean isTransparent() {
        return colorIndex == TwoBitValue.b00;
    }
}