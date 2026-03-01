package org.gameboy.audio;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FrameSequencerTest {
    private int lengthClocks;
    private int sweepClocks;
    private int envelopeClocks;

    @Test
    void shouldClockLengthOnSteps0_2_4_6() {
        lengthClocks = 0; sweepClocks = 0; envelopeClocks = 0;
        FrameSequencer seq = new FrameSequencer(() -> lengthClocks++, () -> sweepClocks++, () -> envelopeClocks++);
        for (int i = 0; i < 65536; i++) seq.tCycle();
        assertThat(lengthClocks).isEqualTo(4);
    }

    @Test
    void shouldClockSweepOnSteps2_6() {
        lengthClocks = 0; sweepClocks = 0; envelopeClocks = 0;
        FrameSequencer seq = new FrameSequencer(() -> lengthClocks++, () -> sweepClocks++, () -> envelopeClocks++);
        for (int i = 0; i < 65536; i++) seq.tCycle();
        assertThat(sweepClocks).isEqualTo(2);
    }

    @Test
    void shouldClockEnvelopeOnStep7() {
        lengthClocks = 0; sweepClocks = 0; envelopeClocks = 0;
        FrameSequencer seq = new FrameSequencer(() -> lengthClocks++, () -> sweepClocks++, () -> envelopeClocks++);
        for (int i = 0; i < 65536; i++) seq.tCycle();
        assertThat(envelopeClocks).isEqualTo(1);
    }

    @Test
    void shouldStepEvery8192TCycles() {
        lengthClocks = 0;
        FrameSequencer seq = new FrameSequencer(() -> lengthClocks++, () -> {}, () -> {});
        for (int i = 0; i < 8191; i++) seq.tCycle();
        assertThat(lengthClocks).isEqualTo(0);
        seq.tCycle();
        assertThat(lengthClocks).isEqualTo(1);
    }
}
