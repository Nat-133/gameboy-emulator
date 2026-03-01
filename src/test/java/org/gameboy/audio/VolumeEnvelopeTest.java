package org.gameboy.audio;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class VolumeEnvelopeTest {
    @Test
    void clock_addMode_shouldIncreaseVolume() {
        VolumeEnvelope env = new VolumeEnvelope();
        env.writeNRx2((byte) 0b0101_1_010);
        env.trigger();
        env.clock();
        assertThat(env.getVolume()).isEqualTo(5);
        env.clock();
        assertThat(env.getVolume()).isEqualTo(6);
    }

    @Test
    void clock_subtractMode_shouldDecreaseVolume() {
        VolumeEnvelope env = new VolumeEnvelope();
        env.writeNRx2((byte) 0b1010_0_001);
        env.trigger();
        env.clock();
        assertThat(env.getVolume()).isEqualTo(9);
    }

    @Test
    void clock_shouldNotExceed15() {
        VolumeEnvelope env = new VolumeEnvelope();
        env.writeNRx2((byte) 0b1111_1_001);
        env.trigger();
        env.clock();
        assertThat(env.getVolume()).isEqualTo(15);
    }

    @Test
    void clock_shouldNotGoBelowZero() {
        VolumeEnvelope env = new VolumeEnvelope();
        env.writeNRx2((byte) 0b0000_0_001);
        env.trigger();
        env.clock();
        assertThat(env.getVolume()).isEqualTo(0);
    }

    @Test
    void clock_periodZero_shouldNotChangeVolume() {
        VolumeEnvelope env = new VolumeEnvelope();
        env.writeNRx2((byte) 0b1000_1_000);
        env.trigger();
        env.clock();
        env.clock();
        assertThat(env.getVolume()).isEqualTo(8);
    }

    @Test
    void trigger_shouldResetVolumeAndTimer() {
        VolumeEnvelope env = new VolumeEnvelope();
        env.writeNRx2((byte) 0b1100_0_011);
        env.trigger();
        assertThat(env.getVolume()).isEqualTo(12);
    }

    @Test
    void isDacEnabled_upperFiveBitsNonZero_shouldReturnTrue() {
        VolumeEnvelope env = new VolumeEnvelope();
        env.writeNRx2((byte) 0b0001_0_000);
        assertThat(env.isDacEnabled()).isTrue();
    }

    @Test
    void isDacEnabled_upperFiveBitsZero_shouldReturnFalse() {
        VolumeEnvelope env = new VolumeEnvelope();
        env.writeNRx2((byte) 0b0000_0_111);
        assertThat(env.isDacEnabled()).isFalse();
    }

    @Test
    void isDacEnabled_addModeOnly_shouldReturnTrue() {
        VolumeEnvelope env = new VolumeEnvelope();
        env.writeNRx2((byte) 0b0000_1_000);
        assertThat(env.isDacEnabled()).isTrue();
    }
}
