package org.gameboy.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class WaveChannelTest {
    private WaveChannel channel;

    @BeforeEach
    void setUp() {
        channel = new WaveChannel();
    }

    @Test
    void trigger_withDacEnabled_shouldEnableChannel() {
        channel.writeNR30((byte) 0x80);
        channel.writeNR32((byte) 0x20);
        channel.writeNR34((byte) 0x80);
        assertThat(channel.isEnabled()).isTrue();
    }

    @Test
    void trigger_withDacDisabled_shouldNotEnableChannel() {
        channel.writeNR30((byte) 0x00);
        channel.writeNR34((byte) 0x80);
        assertThat(channel.isEnabled()).isFalse();
    }

    @Test
    void getOutput_shouldReadFromWaveRam() {
        channel.writeNR30((byte) 0x80);
        channel.writeNR32((byte) 0x20);
        channel.writeWaveRam(0, (byte) 0xAB);
        channel.writeNR34((byte) 0x80);
        int output = channel.getOutput();
        assertThat(output).isEqualTo(10);
    }

    @Test
    void getOutput_volumeShift_shouldShiftRight() {
        channel.writeNR30((byte) 0x80);
        channel.writeNR32((byte) 0x40);
        channel.writeWaveRam(0, (byte) 0xF0);
        channel.writeNR34((byte) 0x80);
        assertThat(channel.getOutput()).isEqualTo(7);
    }

    @Test
    void getOutput_volumeMute_shouldReturnZero() {
        channel.writeNR30((byte) 0x80);
        channel.writeNR32((byte) 0x00);
        channel.writeWaveRam(0, (byte) 0xF0);
        channel.writeNR34((byte) 0x80);
        assertThat(channel.getOutput()).isEqualTo(0);
    }

    @Test
    void tCycle_shouldAdvancePositionWhenTimerExpires() {
        channel.writeNR30((byte) 0x80);
        channel.writeNR32((byte) 0x20);
        channel.writeWaveRam(0, (byte) 0x12);
        channel.writeNR33((byte) 0xFF);
        channel.writeNR34((byte) 0x87);
        int first = channel.getOutput();
        channel.tCycle();
        channel.tCycle();
        int second = channel.getOutput();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void clockLength_whenExpires_shouldDisable() {
        channel.writeNR30((byte) 0x80);
        channel.writeNR31((byte) 0xFF);
        channel.writeNR34((byte) 0xC0);
        assertThat(channel.isEnabled()).isTrue();
        channel.clockLength();
        assertThat(channel.isEnabled()).isFalse();
    }
}
