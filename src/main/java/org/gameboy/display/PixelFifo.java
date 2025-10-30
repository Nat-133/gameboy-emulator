package org.gameboy.display;


import org.gameboy.utils.MultiBitValue.TwoBitValue;

import java.util.*;

public class PixelFifo {
    private final Deque<TwoBitValue> fifo = new ArrayDeque<>(8);
    private final List<FifoReadListener> readListeners = new ArrayList<>();

    public void write(List<TwoBitValue> values) {
        values.stream()
                .skip(fifo.size())
                .forEach(fifo::add);
    }

    public Optional<TwoBitValue> read() {
        Optional<TwoBitValue> value;
        value = Optional.ofNullable(fifo.pollFirst());
        readListeners.forEach(FifoReadListener::onRead);
        return value;
    }

    public boolean isEmpty() {
        return fifo.isEmpty();
    }

    public int size() {
        return fifo.size();
    }

    public void registerReadListener(FifoReadListener listener) {
        readListeners.add(listener);
    }

    public void clear() {
        fifo.clear();
    }
}
