package org.gameboy.display;

import org.gameboy.cpu.components.Clock;

public class ScanlineController {
    private final Clock ppuClock;
    private final Display display;
    private final PixelFifo backgroundFifo;
    private final PixelCombinator pixelCombinator;

    public ScanlineController(Clock ppuClock, Display display, PixelFifo backgroundFifo, PixelCombinator pixelCombinator) {
        this.ppuClock = ppuClock;
        this.display = display;
        this.backgroundFifo = backgroundFifo;
        this.pixelCombinator = pixelCombinator;
    }

    public void renderScanline(int y) {
        for (int x=0; x<160; x++) {
            PixelValue pixel = pixelCombinator.combinePixels(backgroundFifo.read());
            display.setPixel(x, y, pixel);

            ppuClock.tick();
        }
    }
}
