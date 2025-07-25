package org.gameboy.display;

import org.gameboy.utils.MultiBitValue.TwoBitValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PixelFifoTest {
    @Test
    void givenEmptyFifo_whenAddEightElements_thenElementsCanBePoppedInOrder() {
        PixelFifo fifo = new PixelFifo();

        List<TwoBitValue> expectedElements = IntStream.range(0, 8).mapToObj(TwoBitValue::from).toList();
        fifo.write(expectedElements);

        for (int i = 0; i < expectedElements.size(); i++) {
            TwoBitValue actual = fifo.read();
            assertThat(actual)
                    .withFailMessage("expected fifo element %d to be %s, but was %s".formatted(i, expectedElements.get(i), actual))
                    .isEqualTo(expectedElements.get(i));
        }
    }

    @Test
    void givenFifoWithTwoElements_whenAddEightElements_thenFirstTwoAddedElementsDiscarded() {
        PixelFifo fifo = new PixelFifo();
        fifo.write(List.of(TwoBitValue.b11, TwoBitValue.b10));

        List<TwoBitValue> expectedElements = IntStream.range(0, 8).mapToObj(TwoBitValue::from).toList();
        fifo.write(expectedElements);

        assertThat(fifo.read()).withFailMessage("fifo element 0 unexpectedly overwritten").isEqualTo(TwoBitValue.b11);
        assertThat(fifo.read()).withFailMessage("fifo element 1 unexpectedly overwritten").isEqualTo(TwoBitValue.b10);
        for (int i = 2; i < expectedElements.size(); i++) {
            TwoBitValue actual = fifo.read();
            assertThat(actual)
                    .withFailMessage("expected fifo element %d to be %s, but was %s".formatted(i, expectedElements.get(i), actual))
                    .isEqualTo(expectedElements.get(i));
        }
    }

    @Test
    void givenFifoWithElements_whenRead_thenListenerNotified() {
        PixelFifo fifo = new PixelFifo();
        fifo.write(List.of(TwoBitValue.b11, TwoBitValue.b10));
        AtomicInteger readCount = new AtomicInteger(0);
        fifo.registerReadListener(readCount::incrementAndGet);

        fifo.read();

        assertThat(readCount.get()).isEqualTo(1);
    }
}