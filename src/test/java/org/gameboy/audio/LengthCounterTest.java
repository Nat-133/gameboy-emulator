package org.gameboy.audio;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LengthCounterTest {
    @Test
    void clock_whenEnabledAndCounterNonZero_shouldDecrement() {
        LengthCounter counter = new LengthCounter(64);
        counter.load(10);
        counter.setEnabled(true);
        boolean expired = counter.clock();
        assertThat(counter.getCounter()).isEqualTo(53);
        assertThat(expired).isFalse();
    }

    @Test
    void clock_whenDisabled_shouldNotDecrement() {
        LengthCounter counter = new LengthCounter(64);
        counter.load(10);
        boolean expired = counter.clock();
        assertThat(counter.getCounter()).isEqualTo(54);
        assertThat(expired).isFalse();
    }

    @Test
    void clock_whenCounterReachesZero_shouldReturnTrue() {
        LengthCounter counter = new LengthCounter(64);
        counter.load(63);
        counter.setEnabled(true);
        boolean expired = counter.clock();
        assertThat(counter.getCounter()).isEqualTo(0);
        assertThat(expired).isTrue();
    }

    @Test
    void clock_whenCounterAlreadyZero_shouldNotDecrement() {
        LengthCounter counter = new LengthCounter(64);
        counter.setEnabled(true);
        boolean expired = counter.clock();
        assertThat(counter.getCounter()).isEqualTo(0);
        assertThat(expired).isFalse();
    }

    @Test
    void trigger_whenCounterIsZero_shouldReloadWithMax() {
        LengthCounter counter = new LengthCounter(64);
        counter.trigger();
        assertThat(counter.getCounter()).isEqualTo(64);
    }

    @Test
    void trigger_whenCounterIsNonZero_shouldNotReload() {
        LengthCounter counter = new LengthCounter(64);
        counter.load(10);
        counter.trigger();
        assertThat(counter.getCounter()).isEqualTo(54);
    }

    @Test
    void load_shouldSetCounterToMaxMinusValue() {
        LengthCounter counter = new LengthCounter(256);
        counter.load(100);
        assertThat(counter.getCounter()).isEqualTo(156);
    }
}
