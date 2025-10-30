package org.gameboy.display;

import java.util.*;

public class Fifo<T> {
    private final Deque<T> fifo = new ArrayDeque<>(8);
    private final List<FifoReadListener> readListeners = new ArrayList<>();

    public void write(List<T> values) {
        values.stream()
                .skip(fifo.size())
                .forEach(fifo::add);
    }

    public Optional<T> read() {
        Optional<T> value;
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
