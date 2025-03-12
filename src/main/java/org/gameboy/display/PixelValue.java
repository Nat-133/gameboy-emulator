package org.gameboy.display;

import org.gameboy.utils.MultiBitValue;

public record PixelValue(int value) {
    public static PixelValue of(MultiBitValue.TwoBitValue value) {
        return new PixelValue(value.value());
    }
}
