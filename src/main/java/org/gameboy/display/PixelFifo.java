package org.gameboy.display;


import org.gameboy.utils.MultiBitValue.TwoBitValue;

import java.util.*;

public class PixelFifo {
    private final Deque<TwoBitValue> fifo = new ArrayDeque<>(8);
    private final List<FifoReadListener> readListeners = new ArrayList<>();
    // fifo will have 8 pixel queue,
    // + 1 pixel (next pixel value) that will be pulled from the queue.
    // ensures that there will always be a pixel to be pushed to the display

    // still need to find a better way, as this could still cause a 1 cycle delay on the Background fetcher

    // another solution might be to make the queue observable,
    // and add a listener that the background fetcher can register

    // another might be to ensure fifo fill happens last
    // maybe listener on pixel pop? But how to guarantee lister is added before pop
    // maybe listener is long living. Added on fifo creation, and has `pop occurred`
    // we can wait for `pop occurred`
    // javafx.beans ObservableBooleanValue

    public void write(List<TwoBitValue> values) {
        synchronized (fifo) {
            values.stream()
                    .skip(fifo.size())
                    .forEach(fifo::add);
        }
    }

    public Optional<TwoBitValue> read() {
        Optional<TwoBitValue> value;
        synchronized (fifo) {
            try {
                value = Optional.of(fifo.pop());
            } catch (NoSuchElementException ignored) {
                value = Optional.empty();
            }
        }
        readListeners.forEach(FifoReadListener::onRead);
        return value;
    }

    public boolean isEmpty() {
        synchronized (fifo) {
            return fifo.isEmpty();
        }
    }

    public int size() {
        return fifo.size();
    }

    public void registerReadListener(FifoReadListener listener) {
        readListeners.add(listener);
    }

    public void clear() {
        synchronized (fifo) {
            fifo.clear();
        }
    }
}
