package org.gameboy.display;

import org.gameboy.utils.MultiBitValue.TwoBitValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.display.PpuRegisters.PpuRegister.*;
import static org.mockito.Mockito.when;

class PixelCombinatorTest {
    private PixelCombinator pixelCombinator;

    @BeforeEach
    void setUp() {
        PpuRegisters registers = Mockito.mock(PpuRegisters.class);
        // Mock palette registers to return identity mapping (e.g., 11100100 binary = 0xE4)
        // This maps: 00->00, 01->01, 10->10, 11->11 (identity)
        when(registers.read(BGP)).thenReturn((byte) 0xE4);
        when(registers.read(OBP0)).thenReturn((byte) 0xE4);
        when(registers.read(OBP1)).thenReturn((byte) 0xE4);
        pixelCombinator = new PixelCombinator(registers);
    }

    @Test
    void givenFifo_whenPushPixel_thenCorrectPixelPushed() {
        PixelValue pixel = pixelCombinator.combinePixels(TwoBitValue.b01);

        assertThat(pixel).isEqualTo(new PixelValue(1));
    }
}