package org.gameboy.display;

import org.gameboy.utils.MultiBitValue.TwoBitValue;

public class PixelCombinator {
    public PixelValue combinePixels(TwoBitValue backgroundPixel) {
        return PixelValue.of(backgroundPixel);
    }
}
