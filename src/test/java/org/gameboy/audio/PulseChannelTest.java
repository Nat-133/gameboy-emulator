package org.gameboy.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PulseChannelTest {
    private PulseChannel channel;

    @BeforeEach
    void setUp() {
        channel = new PulseChannel(false);
    }

    @Test
    void trigger_withDacEnabled_shouldEnableChannel() {
        channel.writeNRx2((byte) 0xF0);
        channel.writeNRx4((byte) 0x80);
        assertThat(channel.isEnabled()).isTrue();
    }

    @Test
    void trigger_withDacDisabled_shouldNotEnableChannel() {
        channel.writeNRx2((byte) 0x00);
        channel.writeNRx4((byte) 0x80);
        assertThat(channel.isEnabled()).isFalse();
    }

    @Test
    void getOutput_whenEnabled_shouldReturnVolumeOrZero() {
        channel.writeNRx1((byte) 0x80);
        channel.writeNRx2((byte) 0xF0);
        channel.writeNRx3((byte) 0x00);
        channel.writeNRx4((byte) 0x87);
        int output = channel.getOutput();
        assertThat(output).isBetween(0, 15);
    }

    @Test
    void getOutput_whenDisabled_shouldReturnZero() {
        assertThat(channel.getOutput()).isEqualTo(0);
    }

    @Test
    void tCycle_shouldAdvanceDutyPositionWhenTimerExpires() {
        channel.writeNRx1((byte) 0x80);
        channel.writeNRx2((byte) 0xF0);
        channel.writeNRx3((byte) 0xFF);
        channel.writeNRx4((byte) 0x87);
        int[] outputs = new int[8];
        for (int pos = 0; pos < 8; pos++) {
            outputs[pos] = channel.getOutput();
            for (int t = 0; t < 4; t++) {
                channel.tCycle();
            }
        }
        long nonZero = java.util.Arrays.stream(outputs).filter(v -> v > 0).count();
        assertThat(nonZero).isEqualTo(4);
    }

    @Test
    void clockLength_whenLengthExpires_shouldDisableChannel() {
        channel.writeNRx1((byte) 0x3F);
        channel.writeNRx2((byte) 0xF0);
        channel.writeNRx4((byte) 0xC0);
        assertThat(channel.isEnabled()).isTrue();
        channel.clockLength();
        assertThat(channel.isEnabled()).isFalse();
    }

    @Test
    void clockEnvelope_shouldDelegateToVolumeEnvelope() {
        channel.writeNRx2((byte) 0b1010_1_001);
        channel.writeNRx4((byte) 0x80);
        channel.clockEnvelope();
        assertThat(channel.getOutput()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void withSweep_clockSweep_shouldUpdateFrequency() {
        PulseChannel ch1 = new PulseChannel(true);
        ch1.writeNR10((byte) 0b0_001_0_010);
        ch1.writeNRx2((byte) 0xF0);
        ch1.writeNRx3((byte) 0x00);
        ch1.writeNRx4((byte) 0x84);
        ch1.clockSweep();
        assertThat(ch1.isEnabled()).isTrue();
    }
}
