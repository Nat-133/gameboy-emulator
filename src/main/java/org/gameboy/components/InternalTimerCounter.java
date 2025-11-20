package org.gameboy.components;

import java.util.HashMap;
import java.util.Map;

public class InternalTimerCounter {
    private int counter;
    private ResetListener resetListener = () -> {};
    private final Map<Integer, Runnable> fallingEdgeListeners = new HashMap<>();

    public InternalTimerCounter(int initialValue) {
        this.counter = initialValue & 0xFFFF;
    }

    public void tCycle() {
        int oldValue = counter;
        counter = (counter + 1) & 0xFFFF;

        // Detect falling edges: bits that were 1 and are now 0
        int fallingEdges = oldValue & ~counter;

        for (var entry : fallingEdgeListeners.entrySet()) {
            int bit = entry.getKey();
            if ((fallingEdges & (1 << bit)) != 0) {
                entry.getValue().run();
            }
        }
    }

    public void onFallingEdge(int bit, Runnable callback) {
        if (bit < 0 || bit > 15) {
            throw new IllegalArgumentException("Bit must be 0-15, got: " + bit);
        }
        fallingEdgeListeners.put(bit, callback);
    }

    public void reset() {
        counter = 0;
        resetListener.onReset();
    }

    public void setResetListener(ResetListener listener) {
        this.resetListener = listener;
    }

    public int getValue() {
        return counter;
    }

    @FunctionalInterface
    public interface ResetListener {
        void onReset();
    }
}
