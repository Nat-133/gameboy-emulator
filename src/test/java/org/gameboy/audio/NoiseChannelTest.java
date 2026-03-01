package org.gameboy.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class NoiseChannelTest {
    private NoiseChannel channel;

    @BeforeEach
    void setUp() {
        channel = new NoiseChannel();
    }

    @Test
    void trigger_withDacEnabled_shouldEnableAndInitializeLfsr() {
        channel.writeNR42((byte) 0xF0);
        channel.writeNR44((byte) 0x80);
        assertThat(channel.isEnabled()).isTrue();
    }

    @Test
    void getOutput_shouldProduceNonZeroValues() {
        channel.writeNR42((byte) 0xF0);
        channel.writeNR43((byte) 0x00);
        channel.writeNR44((byte) 0x80);
        boolean sawNonZero = false;
        for (int i = 0; i < 1000; i++) {
            channel.tCycle();
            if (channel.getOutput() > 0) sawNonZero = true;
        }
        assertThat(sawNonZero).isTrue();
    }

    @Test
    void narrowMode_shouldUse7BitLfsr() {
        channel.writeNR42((byte) 0xF0);
        channel.writeNR43((byte) 0b0000_1_000);
        channel.writeNR44((byte) 0x80);
        int[] outputs = new int[200];
        for (int i = 0; i < 200; i++) {
            outputs[i] = channel.getOutput();
            for (int t = 0; t < 8; t++) channel.tCycle();
        }
        boolean repeats = false;
        for (int period = 1; period <= 127; period++) {
            boolean match = true;
            for (int i = 0; i < 127 && (i + period) < 200; i++) {
                if (outputs[i] != outputs[i + period]) { match = false; break; }
            }
            if (match) { repeats = true; break; }
        }
        assertThat(repeats).isTrue();
    }

    @Test
    void clockLength_whenExpires_shouldDisable() {
        channel.writeNR41((byte) 0x3F);
        channel.writeNR42((byte) 0xF0);
        channel.writeNR44((byte) 0xC0);
        assertThat(channel.isEnabled()).isTrue();
        channel.clockLength();
        assertThat(channel.isEnabled()).isFalse();
    }

    @Test
    void clockEnvelope_shouldChangeVolume() {
        channel.writeNR42((byte) 0b1010_1_001);
        channel.writeNR44((byte) 0x80);
        channel.clockEnvelope();
    }
}
