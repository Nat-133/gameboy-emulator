package org.gameboy.audio;

public class VolumeEnvelope {
    private int initialVolume;
    private boolean addMode;
    private int period;
    private int timer;
    private int volume;

    public void writeNRx2(byte value) {
        int v = value & 0xFF;
        initialVolume = (v >> 4) & 0x0F;
        addMode = ((v >> 3) & 1) == 1;
        period = v & 0x07;
    }

    public boolean isDacEnabled() {
        return initialVolume > 0 || addMode;
    }

    public void trigger() {
        timer = period;
        volume = initialVolume;
    }

    public void clock() {
        if (period == 0) return;
        timer--;
        if (timer <= 0) {
            timer = period;
            int newVolume = volume + (addMode ? 1 : -1);
            if (newVolume >= 0 && newVolume <= 15) {
                volume = newVolume;
            }
        }
    }

    public int getVolume() {
        return volume;
    }

    public int getInitialVolume() {
        return initialVolume;
    }

    public boolean isAddMode() {
        return addMode;
    }

    public int getPeriod() {
        return period;
    }

    public void reset() {
        initialVolume = 0;
        addMode = false;
        period = 0;
        timer = 0;
        volume = 0;
    }
}
