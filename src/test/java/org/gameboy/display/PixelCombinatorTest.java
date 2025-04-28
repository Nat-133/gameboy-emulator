package org.gameboy.display;

import org.gameboy.utils.MultiBitValue.TwoBitValue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PixelCombinatorTest {
    @Test
    void givenFifo_whenPushPixel_thenCorrectPixelPushed() {
        PixelValue pixel = new PixelCombinator().combinePixels(TwoBitValue.b01);

        assertThat(pixel).isEqualTo(new PixelValue(1));
    }
}