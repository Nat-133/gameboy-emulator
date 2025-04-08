package org.gameboy.display;

import org.gameboy.cpu.components.Clock;
import org.gameboy.cpu.components.CpuClock;
import org.gameboy.utils.MultiBitValue.TwoBitValue;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ScanlineControllerTest {
    @Test
    void givenBackgroundFifoContinuouslyFilled_whenRenderScanline_thenCorrectDataRendered() {
        PixelFifo backgroundFifo = mock(PixelFifo.class);
        AtomicInteger i = new AtomicInteger(0);
        when(backgroundFifo.read()).thenAnswer(invocation -> TwoBitValue.from(i.getAndIncrement()));
        Display display = mock(Display.class);

        ScanlineController controller = new ScanlineController(mock(Clock.class), display, backgroundFifo, new PixelCombinator());

        controller.renderScanline(4);

        ArgumentCaptor<Integer> xCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> yCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<PixelValue> valueCaptor = ArgumentCaptor.forClass(PixelValue.class);

        verify(display, times(160))
                .setPixel(xCaptor.capture(), yCaptor.capture(), valueCaptor.capture());

        List<Integer> xValues = xCaptor.getAllValues();
        List<Integer> yValues = yCaptor.getAllValues();
        List<PixelValue> pixelValues = valueCaptor.getAllValues();

        for (int x = 0; x < 160; x++) {
            assertThat(x).isEqualTo(xValues.get(x));
            assertThat(4).isEqualTo(yValues.get(x));
            assertThat(new PixelValue(x%4))
                    .withFailMessage("pixel (%d, %d) expected to be value %d but was %d".formatted(x, 4, x%4, pixelValues.get(x).value()))
                    .isEqualTo(pixelValues.get(x));
        }
    }

    @Test
    void givenBackgroundFifoContinuouslyFilled_whenRenderScanline_thenClockRegistersCorrectTime() {
        Display display = mock(Display.class);
        PixelFifo backgroundFifo = mock(PixelFifo.class);
        when(backgroundFifo.read()).thenReturn(TwoBitValue.b01);

        Clock clock = new CpuClock();
        ScanlineController controller = new ScanlineController(clock, display, backgroundFifo, new PixelCombinator());

        controller.renderScanline(4);

        assertThat(clock.getTime()).isEqualTo(160);
    }
}