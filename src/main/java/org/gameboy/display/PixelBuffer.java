package org.gameboy.display;

public class PixelBuffer {
    private final PixelValue[][] activePixels = new PixelValue[Display.DISPLAY_WIDTH][Display.DISPLAY_HEIGHT];
    private final PixelValue[][] buffer = new PixelValue[Display.DISPLAY_WIDTH][Display.DISPLAY_HEIGHT];

    public PixelBuffer() {
        for (int x = 0; x < Display.DISPLAY_WIDTH; x++) {
            for (int y = 0; y < Display.DISPLAY_HEIGHT; y++) {
                activePixels[x][y] = new PixelValue((x + y) % 4);
                buffer[x][y] = new PixelValue((x + y) % 4);
            }
        }
    }

    public void setPixel(int x, int y, PixelValue value) {
        if (x >= 0 && x < Display.DISPLAY_WIDTH && y >= 0 && y < Display.DISPLAY_HEIGHT) {
            this.activePixels[x][y] = value;
        }
    }

    public void swapBuffers() {
        for (int x = 0; x < Display.DISPLAY_WIDTH; x++) {
            System.arraycopy(activePixels[x], 0, buffer[x], 0, Display.DISPLAY_HEIGHT);
        }
    }

    public PixelValue getDisplayPixel(int x, int y) {
        if (x >= 0 && x < Display.DISPLAY_WIDTH && y >= 0 && y < Display.DISPLAY_HEIGHT) {
            return buffer[x][y];
        }
        return new PixelValue(0);
    }
}
