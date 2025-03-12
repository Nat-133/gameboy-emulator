package org.gameboy.display;

import org.gameboy.utils.MultiBitValue.TwoBitValue;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class DisplayWriterTest {
    @Test
    void givenFifo_whenPushPixel_thenCorrectPixelPushed() {
        PixelFifo backgroundFifo = mock(PixelFifo.class);
        when(backgroundFifo.readFromFifo()).thenReturn(TwoBitValue.b01);
        Display display = mock(Display.class);
        var displayWriter = new DisplayWriter(display, backgroundFifo);

        displayWriter.pushPixelToDisplay(5, 12);

        verify(display).setPixel(5, 12, new PixelValue(1));
    }
}