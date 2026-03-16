package org.gameboy.audio;

import com.google.inject.Inject;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Apu {
    private static final int CPU_CLOCK_RATE = 4_194_304;
    private static final int SAMPLE_RATE = 44_100;
    private static final int BUFFER_SIZE = 1024;

    private final ApuRegisters registers;
    private final ConcurrentLinkedQueue<short[]> sampleQueue;

    private final PulseChannel channel1;
    private final PulseChannel channel2;
    private final WaveChannel channel3;
    private final NoiseChannel channel4;
    private final FrameSequencer frameSequencer;

    private boolean poweredOn;
    private int sampleCounter;
    private float leftAccumulator;
    private float rightAccumulator;
    private int accumulatorCount;
    private short[] sampleBuffer;
    private int bufferPosition;

    @Inject
    public Apu(ApuRegisters registers, ConcurrentLinkedQueue<short[]> sampleQueue) {
        this.registers = registers;
        this.sampleQueue = sampleQueue;
        this.channel1 = new PulseChannel(true);
        this.channel2 = new PulseChannel(false);
        this.channel3 = new WaveChannel();
        this.channel4 = new NoiseChannel();
        this.frameSequencer = new FrameSequencer(this::clockLength, this::clockSweep, this::clockEnvelope);
        this.sampleBuffer = new short[BUFFER_SIZE * 2];
        this.bufferPosition = 0;
        wireRegisters();
    }

    private void wireRegisters() {
        registers.nr52.setWriteCallback(v -> {
            boolean newPower = (v & 0x80) != 0;
            if (!newPower && poweredOn) powerOff();
            poweredOn = newPower;
        });
        registers.nr10.setWriteCallback(v -> { if (poweredOn) channel1.writeNR10(v); });
        registers.nr11.setWriteCallback(v -> { if (poweredOn) channel1.writeNRx1(v); });
        registers.nr12.setWriteCallback(v -> { if (poweredOn) channel1.writeNRx2(v); });
        registers.nr13.setWriteCallback(v -> { if (poweredOn) channel1.writeNRx3(v); });
        registers.nr14.setWriteCallback(v -> { if (poweredOn) channel1.writeNRx4(v); });
        registers.nr21.setWriteCallback(v -> { if (poweredOn) channel2.writeNRx1(v); });
        registers.nr22.setWriteCallback(v -> { if (poweredOn) channel2.writeNRx2(v); });
        registers.nr23.setWriteCallback(v -> { if (poweredOn) channel2.writeNRx3(v); });
        registers.nr24.setWriteCallback(v -> { if (poweredOn) channel2.writeNRx4(v); });
        registers.nr30.setWriteCallback(v -> { if (poweredOn) channel3.writeNR30(v); });
        registers.nr31.setWriteCallback(v -> { if (poweredOn) channel3.writeNR31(v); });
        registers.nr32.setWriteCallback(v -> { if (poweredOn) channel3.writeNR32(v); });
        registers.nr33.setWriteCallback(v -> { if (poweredOn) channel3.writeNR33(v); });
        registers.nr34.setWriteCallback(v -> { if (poweredOn) channel3.writeNR34(v); });
        registers.nr41.setWriteCallback(v -> { if (poweredOn) channel4.writeNR41(v); });
        registers.nr42.setWriteCallback(v -> { if (poweredOn) channel4.writeNR42(v); });
        registers.nr43.setWriteCallback(v -> { if (poweredOn) channel4.writeNR43(v); });
        registers.nr44.setWriteCallback(v -> { if (poweredOn) channel4.writeNR44(v); });
    }

    public void tCycle() {
        if (!poweredOn) return;
        frameSequencer.tCycle();
        channel1.tCycle();
        channel2.tCycle();
        channel3.tCycle();
        channel4.tCycle();
        syncWaveRam();
        updateNr52Status();
        mix();
    }

    private void syncWaveRam() {
        byte[] waveRam = registers.getWaveRam();
        for (int i = 0; i < 16; i++) channel3.writeWaveRam(i, waveRam[i]);
    }

    private void updateNr52Status() {
        int status = (poweredOn ? 0x80 : 0x00)
            | (channel1.isEnabled() ? 0x01 : 0)
            | (channel2.isEnabled() ? 0x02 : 0)
            | (channel3.isEnabled() ? 0x04 : 0)
            | (channel4.isEnabled() ? 0x08 : 0);
        registers.nr52.setRawValue(status);
    }

    private void mix() {
        int nr51 = registers.nr51.getRawValue();
        int nr50 = registers.nr50.getRawValue();
        float ch1Dac = dacConvert(channel1.getOutput(), channel1.isDacEnabled());
        float ch2Dac = dacConvert(channel2.getOutput(), channel2.isDacEnabled());
        float ch3Dac = dacConvert(channel3.getOutput(), channel3.isDacEnabled());
        float ch4Dac = dacConvert(channel4.getOutput(), channel4.isDacEnabled());
        float left = 0;
        if ((nr51 & 0x10) != 0) left += ch1Dac;
        if ((nr51 & 0x20) != 0) left += ch2Dac;
        if ((nr51 & 0x40) != 0) left += ch3Dac;
        if ((nr51 & 0x80) != 0) left += ch4Dac;
        float right = 0;
        if ((nr51 & 0x01) != 0) right += ch1Dac;
        if ((nr51 & 0x02) != 0) right += ch2Dac;
        if ((nr51 & 0x04) != 0) right += ch3Dac;
        if ((nr51 & 0x08) != 0) right += ch4Dac;
        int leftVol = ((nr50 >> 4) & 0x07) + 1;
        int rightVol = (nr50 & 0x07) + 1;
        left *= leftVol / 8.0f;
        right *= rightVol / 8.0f;
        leftAccumulator += left;
        rightAccumulator += right;
        accumulatorCount++;
        sampleCounter += SAMPLE_RATE;
        if (sampleCounter >= CPU_CLOCK_RATE) {
            sampleCounter -= CPU_CLOCK_RATE;
            emitSample();
        }
    }

    private float dacConvert(int digitalOutput, boolean dacEnabled) {
        if (!dacEnabled) return 0.0f;
        return (digitalOutput / 7.5f) - 1.0f;
    }

    private void emitSample() {
        float left = leftAccumulator / accumulatorCount;
        float right = rightAccumulator / accumulatorCount;
        leftAccumulator = 0;
        rightAccumulator = 0;
        accumulatorCount = 0;
        sampleBuffer[bufferPosition++] = (short) (left * Short.MAX_VALUE * 0.25f);
        sampleBuffer[bufferPosition++] = (short) (right * Short.MAX_VALUE * 0.25f);
        if (bufferPosition >= sampleBuffer.length) {
            sampleQueue.offer(sampleBuffer);
            sampleBuffer = new short[BUFFER_SIZE * 2];
            bufferPosition = 0;
        }
    }

    private void clockLength() {
        channel1.clockLength();
        channel2.clockLength();
        channel3.clockLength();
        channel4.clockLength();
    }

    private void clockSweep() { channel1.clockSweep(); }

    private void clockEnvelope() {
        channel1.clockEnvelope();
        channel2.clockEnvelope();
        channel4.clockEnvelope();
    }

    private void powerOff() {
        channel1.powerOff();
        channel2.powerOff();
        channel3.powerOff();
        channel4.powerOff();
        frameSequencer.reset();
        registers.nr10.setRawValue(0);
        registers.nr11.setRawValue(0);
        registers.nr12.setRawValue(0);
        registers.nr13.setRawValue(0);
        registers.nr14.setRawValue(0);
        registers.nr21.setRawValue(0);
        registers.nr22.setRawValue(0);
        registers.nr23.setRawValue(0);
        registers.nr24.setRawValue(0);
        registers.nr30.setRawValue(0);
        registers.nr31.setRawValue(0);
        registers.nr32.setRawValue(0);
        registers.nr33.setRawValue(0);
        registers.nr34.setRawValue(0);
        registers.nr41.setRawValue(0);
        registers.nr42.setRawValue(0);
        registers.nr43.setRawValue(0);
        registers.nr44.setRawValue(0);
        registers.nr50.setRawValue(0);
        registers.nr51.setRawValue(0);
    }

    public boolean isPoweredOn() { return poweredOn; }
}
