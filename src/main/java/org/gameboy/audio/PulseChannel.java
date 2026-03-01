package org.gameboy.audio;

public class PulseChannel {
    private static final boolean[][] DUTY_TABLE = {
        {false, false, false, false, false, false, false, true},
        {false, false, false, false, false, false, true, true},
        {false, false, false, false, true, true, true, true},
        {true, true, true, true, true, true, false, false},
    };

    private final boolean hasSweep;
    private final LengthCounter lengthCounter;
    private final VolumeEnvelope volumeEnvelope;
    private final FrequencySweep frequencySweep;

    private boolean enabled;
    private int duty;
    private int dutyPosition;
    private int frequency;
    private int frequencyTimer;
    private boolean lengthEnabled;

    public PulseChannel(boolean hasSweep) {
        this.hasSweep = hasSweep;
        this.lengthCounter = new LengthCounter(64);
        this.volumeEnvelope = new VolumeEnvelope();
        this.frequencySweep = hasSweep ? new FrequencySweep() : null;
    }

    public void writeNR10(byte value) {
        if (frequencySweep != null) {
            frequencySweep.writeNR10(value);
        }
    }

    public void writeNRx1(byte value) {
        int v = value & 0xFF;
        duty = (v >> 6) & 0x03;
        lengthCounter.load(v & 0x3F);
    }

    public void writeNRx2(byte value) {
        volumeEnvelope.writeNRx2(value);
        if (!volumeEnvelope.isDacEnabled()) {
            enabled = false;
        }
    }

    public void writeNRx3(byte value) {
        frequency = (frequency & 0x700) | (value & 0xFF);
    }

    public void writeNRx4(byte value) {
        int v = value & 0xFF;
        frequency = (frequency & 0xFF) | ((v & 0x07) << 8);
        lengthEnabled = (v & 0x40) != 0;
        lengthCounter.setEnabled(lengthEnabled);
        if ((v & 0x80) != 0) {
            trigger();
        }
    }

    private void trigger() {
        enabled = volumeEnvelope.isDacEnabled();
        frequencyTimer = (2048 - frequency) * 4;
        volumeEnvelope.trigger();
        lengthCounter.trigger();
        dutyPosition = 0;
        if (frequencySweep != null) {
            frequencySweep.trigger(frequency);
            if (frequencySweep.hasOverflowed()) {
                enabled = false;
            }
        }
    }

    public void tCycle() {
        if (frequencyTimer > 0) {
            frequencyTimer--;
        }
        if (frequencyTimer <= 0) {
            frequencyTimer = (2048 - frequency) * 4;
            dutyPosition = (dutyPosition + 1) & 7;
        }
    }

    public void clockLength() {
        if (lengthCounter.clock()) {
            enabled = false;
        }
    }

    public void clockEnvelope() {
        volumeEnvelope.clock();
    }

    public void clockSweep() {
        if (frequencySweep == null) return;
        frequencySweep.clock();
        if (frequencySweep.hasOverflowed()) {
            enabled = false;
        } else {
            int newFreq = frequencySweep.getNewFrequency();
            if (newFreq >= 0) {
                frequency = newFreq;
            }
        }
    }

    public int getOutput() {
        if (!enabled) return 0;
        return DUTY_TABLE[duty][dutyPosition] ? volumeEnvelope.getVolume() : 0;
    }

    public boolean isEnabled() { return enabled; }
    public boolean isDacEnabled() { return volumeEnvelope.isDacEnabled(); }
    public int getFrequency() { return frequency; }

    public void powerOff() {
        enabled = false;
        duty = 0;
        dutyPosition = 0;
        frequency = 0;
        frequencyTimer = 0;
        lengthEnabled = false;
        volumeEnvelope.reset();
        lengthCounter.reset();
        if (frequencySweep != null) frequencySweep.reset();
    }
}
