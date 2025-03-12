package org.gameboy.display;

import org.gameboy.utils.MultiBitValue.TwoBitValue;

public class DisplayWriter {
    private final Display display;
    private final PixelFifo backgroundFifo;

    public DisplayWriter(Display display, PixelFifo backgroundFifo) {
        this.display = display;
        this.backgroundFifo = backgroundFifo;
    }

    public void pushPixelToDisplay(int x, int y) {
        TwoBitValue value = backgroundFifo.readFromFifo();
        PixelValue pixel = PixelValue.of(value);

        display.setPixel(x, y, pixel);
    }
}
