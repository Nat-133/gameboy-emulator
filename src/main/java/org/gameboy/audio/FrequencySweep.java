package org.gameboy.audio;

public class FrequencySweep {
    private int period;
    private boolean negate;
    private int shift;
    private int shadowFrequency;
    private int timer;
    private boolean enabled;
    private boolean overflowed;
    private int newFrequency;

    public void writeNR10(byte value) {
        int v = value & 0xFF;
        period = (v >> 4) & 0x07;
        negate = ((v >> 3) & 1) == 1;
        shift = v & 0x07;
    }

    public void trigger(int frequency) {
        shadowFrequency = frequency;
        timer = (period != 0) ? period : 8;
        enabled = (period != 0) || (shift != 0);
        overflowed = false;
        newFrequency = -1;
        if (shift > 0) {
            int calc = calculateNewFrequency();
            if (calc > 2047) {
                overflowed = true;
            }
        }
    }

    public void clock() {
        newFrequency = -1;
        if (!enabled) return;
        timer--;
        if (timer <= 0) {
            timer = (period != 0) ? period : 8;
            if (period > 0) {
                int calc = calculateNewFrequency();
                if (calc > 2047) {
                    overflowed = true;
                } else if (shift > 0) {
                    shadowFrequency = calc;
                    newFrequency = calc;
                    int secondCalc = calculateNewFrequency();
                    if (secondCalc > 2047) {
                        overflowed = true;
                    }
                }
            }
        }
    }

    private int calculateNewFrequency() {
        int delta = shadowFrequency >> shift;
        if (negate) {
            return shadowFrequency - delta;
        } else {
            return shadowFrequency + delta;
        }
    }

    public boolean hasOverflowed() { return overflowed; }
    public int getNewFrequency() { return newFrequency; }
    public int getShadowFrequency() { return shadowFrequency; }
    public boolean isEnabled() { return enabled; }

    public void reset() {
        period = 0;
        negate = false;
        shift = 0;
        shadowFrequency = 0;
        timer = 0;
        enabled = false;
        overflowed = false;
        newFrequency = -1;
    }
}
