package org.gameboy.audio;

public class WaveChannel {
    private static final int[] VOLUME_SHIFT = {4, 0, 1, 2};

    private final byte[] waveRam = new byte[16];
    private final LengthCounter lengthCounter;

    private boolean dacEnabled;
    private boolean enabled;
    private int volumeCode;
    private int frequency;
    private int frequencyTimer;
    private int wavePosition;
    private boolean lengthEnabled;

    public WaveChannel() {
        this.lengthCounter = new LengthCounter(256);
    }

    public void writeNR30(byte value) {
        dacEnabled = (value & 0x80) != 0;
        if (!dacEnabled) enabled = false;
    }

    public void writeNR31(byte value) {
        lengthCounter.load(value & 0xFF);
    }

    public void writeNR32(byte value) {
        volumeCode = (value >> 5) & 0x03;
    }

    public void writeNR33(byte value) {
        frequency = (frequency & 0x700) | (value & 0xFF);
    }

    public void writeNR34(byte value) {
        int v = value & 0xFF;
        frequency = (frequency & 0xFF) | ((v & 0x07) << 8);
        lengthEnabled = (v & 0x40) != 0;
        lengthCounter.setEnabled(lengthEnabled);
        if ((v & 0x80) != 0) trigger();
    }

    private void trigger() {
        enabled = dacEnabled;
        frequencyTimer = (2048 - frequency) * 2;
        wavePosition = 0;
        lengthCounter.trigger();
    }

    public void writeWaveRam(int offset, byte value) {
        if (offset >= 0 && offset < 16) waveRam[offset] = value;
    }

    public byte readWaveRam(int offset) {
        if (offset >= 0 && offset < 16) return waveRam[offset];
        return (byte) 0xFF;
    }

    public void tCycle() {
        if (frequencyTimer > 0) frequencyTimer--;
        if (frequencyTimer <= 0) {
            frequencyTimer = (2048 - frequency) * 2;
            wavePosition = (wavePosition + 1) & 31;
        }
    }

    public void clockLength() {
        if (lengthCounter.clock()) enabled = false;
    }

    public int getOutput() {
        if (!enabled) return 0;
        int byteIndex = wavePosition / 2;
        int sample;
        if ((wavePosition & 1) == 0) {
            sample = (waveRam[byteIndex] >> 4) & 0x0F;
        } else {
            sample = waveRam[byteIndex] & 0x0F;
        }
        return sample >> VOLUME_SHIFT[volumeCode];
    }

    public boolean isEnabled() { return enabled; }
    public boolean isDacEnabled() { return dacEnabled; }

    public void powerOff() {
        dacEnabled = false;
        enabled = false;
        volumeCode = 0;
        frequency = 0;
        frequencyTimer = 0;
        wavePosition = 0;
        lengthEnabled = false;
        lengthCounter.reset();
    }
}
