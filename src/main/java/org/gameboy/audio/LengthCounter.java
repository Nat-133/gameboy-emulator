package org.gameboy.audio;

public class LengthCounter {
    private final int maxLength;
    private int counter;
    private boolean enabled;

    public LengthCounter(int maxLength) {
        this.maxLength = maxLength;
        this.counter = 0;
        this.enabled = false;
    }

    public void load(int lengthData) {
        counter = maxLength - lengthData;
    }

    public boolean clock() {
        if (enabled && counter > 0) {
            counter--;
            return counter == 0;
        }
        return false;
    }

    public void trigger() {
        if (counter == 0) {
            counter = maxLength;
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getCounter() {
        return counter;
    }

    public void reset() {
        counter = 0;
        enabled = false;
    }
}
