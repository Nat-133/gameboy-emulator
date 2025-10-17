package org.gameboy.display;

public interface Display {
    int DISPLAY_WIDTH = 160;
    int DISPLAY_HEIGHT = 144;

    void setPixel(int x, int y, PixelValue value);

    default void onVBlank() {
        // Optional hook for displays to refresh on VBlank
    }
}
