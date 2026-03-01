package org.gameboy.audio;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FrequencySweepTest {
    @Test
    void clock_shouldCalculateNewFrequencyWithAddition() {
        FrequencySweep sweep = new FrequencySweep();
        sweep.writeNR10((byte) 0b0_010_0_010);
        sweep.trigger(1024);
        sweep.clock();
        assertThat(sweep.getNewFrequency()).isEqualTo(-1);
        sweep.clock();
        assertThat(sweep.getNewFrequency()).isEqualTo(1280);
        assertThat(sweep.hasOverflowed()).isFalse();
    }

    @Test
    void clock_shouldCalculateNewFrequencyWithSubtraction() {
        FrequencySweep sweep = new FrequencySweep();
        sweep.writeNR10((byte) 0b0_010_1_010);
        sweep.trigger(1024);
        sweep.clock();
        sweep.clock();
        assertThat(sweep.getNewFrequency()).isEqualTo(768);
    }

    @Test
    void clock_overflow_shouldSignalOverflow() {
        FrequencySweep sweep = new FrequencySweep();
        sweep.writeNR10((byte) 0b0_001_0_001);
        sweep.trigger(1800);
        sweep.clock();
        assertThat(sweep.hasOverflowed()).isTrue();
    }

    @Test
    void clock_periodZero_shouldNotCalculate() {
        FrequencySweep sweep = new FrequencySweep();
        sweep.writeNR10((byte) 0b0_000_0_010);
        sweep.trigger(1024);
        for (int i = 0; i < 20; i++) {
            sweep.clock();
        }
        assertThat(sweep.hasOverflowed()).isFalse();
        assertThat(sweep.getShadowFrequency()).isEqualTo(1024);
    }

    @Test
    void clock_shiftZero_shouldNotUpdateFrequency() {
        FrequencySweep sweep = new FrequencySweep();
        sweep.writeNR10((byte) 0b0_010_0_000);
        sweep.trigger(1024);
        sweep.clock();
        sweep.clock();
        assertThat(sweep.getNewFrequency()).isEqualTo(-1);
    }

    @Test
    void trigger_shouldPerformOverflowCheckIfShiftNonZero() {
        FrequencySweep sweep = new FrequencySweep();
        sweep.writeNR10((byte) 0b0_001_0_001);
        sweep.trigger(2000);
        assertThat(sweep.hasOverflowed()).isTrue();
    }

    @Test
    void trigger_shouldNotOverflowCheckIfShiftZero() {
        FrequencySweep sweep = new FrequencySweep();
        sweep.writeNR10((byte) 0b0_001_0_000);
        sweep.trigger(2000);
        assertThat(sweep.hasOverflowed()).isFalse();
    }
}
