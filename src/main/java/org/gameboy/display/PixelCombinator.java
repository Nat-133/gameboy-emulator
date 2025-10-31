package org.gameboy.display;

import org.gameboy.utils.MultiBitValue.TwoBitValue;

import static org.gameboy.display.PpuRegisters.PpuRegister.*;

public class PixelCombinator {
    private final PpuRegisters registers;

    public PixelCombinator(PpuRegisters registers) {
        this.registers = registers;
    }

    public PixelValue combinePixels(TwoBitValue backgroundPixel, SpritePixel spritePixel) {
        if (spritePixel == null || spritePixel.isTransparent()) {
            TwoBitValue finalColor = applyBackgroundPalette(backgroundPixel);
            return PixelValue.of(finalColor);
        }

        if (spritePixel.bgPriority() && backgroundPixel != TwoBitValue.b00) {
            TwoBitValue finalColor = applyBackgroundPalette(backgroundPixel);
            return PixelValue.of(finalColor);
        }

        TwoBitValue finalColor = applySpritePalette(spritePixel);
        return PixelValue.of(finalColor);
    }

    public PixelValue combinePixels(TwoBitValue backgroundPixel) {
        TwoBitValue finalColor = applyBackgroundPalette(backgroundPixel);
        return PixelValue.of(finalColor);
    }

    private TwoBitValue applyBackgroundPalette(TwoBitValue colorIndex) {
        byte bgp = registers.read(BGP);
        return applyPalette(colorIndex, bgp);
    }

    private TwoBitValue applySpritePalette(SpritePixel spritePixel) {
        byte palette = spritePixel.useOBP1()
            ? registers.read(OBP1)
            : registers.read(OBP0);
        return applyPalette(spritePixel.colorIndex(), palette);
    }

    private TwoBitValue applyPalette(TwoBitValue colorIndex, byte palette) {
        int index = colorIndex.value();
        int shift = index * 2;
        int color = (palette >> shift) & 0x03;
        return TwoBitValue.from(color);
    }
}
