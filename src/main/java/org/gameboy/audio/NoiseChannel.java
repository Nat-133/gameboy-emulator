package org.gameboy.audio;

public class NoiseChannel {
    private static final int[] DIVISOR_TABLE = {8, 16, 32, 48, 64, 80, 96, 112};

    private final LengthCounter lengthCounter;
    private final VolumeEnvelope volumeEnvelope;

    private boolean enabled;
    private int lfsr;
    private int clockShift;
    private boolean narrowMode;
    private int divisorCode;
    private int frequencyTimer;
    private boolean lengthEnabled;

    public NoiseChannel() {
        this.lengthCounter = new LengthCounter(64);
        this.volumeEnvelope = new VolumeEnvelope();
        this.lfsr = 0x7FFF;
    }

    public void writeNR41(byte value) { lengthCounter.load(value & 0x3F); }

    public void writeNR42(byte value) {
        volumeEnvelope.writeNRx2(value);
        if (!volumeEnvelope.isDacEnabled()) enabled = false;
    }

    public void writeNR43(byte value) {
        int v = value & 0xFF;
        clockShift = (v >> 4) & 0x0F;
        narrowMode = (v & 0x08) != 0;
        divisorCode = v & 0x07;
    }

    public void writeNR44(byte value) {
        int v = value & 0xFF;
        lengthEnabled = (v & 0x40) != 0;
        lengthCounter.setEnabled(lengthEnabled);
        if ((v & 0x80) != 0) trigger();
    }

    private void trigger() {
        enabled = volumeEnvelope.isDacEnabled();
        lfsr = 0x7FFF;
        frequencyTimer = DIVISOR_TABLE[divisorCode] << clockShift;
        volumeEnvelope.trigger();
        lengthCounter.trigger();
    }

    public void tCycle() {
        if (frequencyTimer > 0) frequencyTimer--;
        if (frequencyTimer <= 0) {
            frequencyTimer = DIVISOR_TABLE[divisorCode] << clockShift;
            clockLfsr();
        }
    }

    private void clockLfsr() {
        int xorBit = (lfsr & 1) ^ ((lfsr >> 1) & 1);
        lfsr >>= 1;
        lfsr |= (xorBit << 14);
        if (narrowMode) {
            lfsr = (lfsr & ~(1 << 6)) | (xorBit << 6);
        }
    }

    public void clockLength() { if (lengthCounter.clock()) enabled = false; }
    public void clockEnvelope() { volumeEnvelope.clock(); }

    public int getOutput() {
        if (!enabled) return 0;
        return ((~lfsr) & 1) * volumeEnvelope.getVolume();
    }

    public boolean isEnabled() { return enabled; }
    public boolean isDacEnabled() { return volumeEnvelope.isDacEnabled(); }

    public void powerOff() {
        enabled = false;
        lfsr = 0x7FFF;
        clockShift = 0;
        narrowMode = false;
        divisorCode = 0;
        frequencyTimer = 0;
        lengthEnabled = false;
        volumeEnvelope.reset();
        lengthCounter.reset();
    }
}
