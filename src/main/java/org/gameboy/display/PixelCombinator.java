package org.gameboy.display;

import org.gameboy.utils.MultiBitValue;

public class PixelCombinator {
    public PixelValue combinePixels(MultiBitValue.TwoBitValue read) {
        return PixelValue.of(read);
    }
}
