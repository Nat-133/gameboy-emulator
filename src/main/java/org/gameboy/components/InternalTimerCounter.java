package org.gameboy.components;

public class InternalTimerCounter {
    private int counter = 0;
    private ResetListener resetListener = () -> {};

    public void tCycle() {
        counter = (counter + 1) & 0xFFFF;
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
