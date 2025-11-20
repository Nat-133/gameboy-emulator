package org.gameboy.components;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InternalTimerCounterFallingEdgeTest {
    private InternalTimerCounter counter;

    @BeforeEach
    void setUp() {
        counter = new InternalTimerCounter(0);
    }

    @Test
    void shouldNotifyListenerOnBit0FallingEdge() {
        AtomicInteger callCount = new AtomicInteger(0);
        counter.onFallingEdge(0, callCount::incrementAndGet);

        // Bit 0: 0->1 (rising), then 1->0 (falling) on second tCycle
        counter.tCycle(); // counter = 1, bit 0 rose
        assertThat(callCount.get()).isEqualTo(0);

        counter.tCycle(); // counter = 2, bit 0 fell
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void shouldNotifyListenerOnBit7FallingEdge() {
        AtomicInteger callCount = new AtomicInteger(0);
        counter.onFallingEdge(7, callCount::incrementAndGet);

        // Bit 7 falls when counter goes from 0xFF to 0x100, and 0x1FF to 0x200, etc.
        // First, advance to 0xFF
        for (int i = 0; i < 0xFF; i++) {
            counter.tCycle();
        }
        assertThat(callCount.get()).isEqualTo(0);

        // Next tCycle: 0xFF -> 0x100, bit 7 falls (was 1, now 0)
        counter.tCycle();
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    void shouldNotNotifyOnRisingEdge() {
        AtomicInteger callCount = new AtomicInteger(0);
        counter.onFallingEdge(0, callCount::incrementAndGet);

        counter.tCycle(); // 0->1, rising edge on bit 0
        assertThat(callCount.get()).isEqualTo(0); // Should NOT notify
    }

    @Test
    void shouldSupportMultipleListenersOnDifferentBits() {
        AtomicInteger bit0Count = new AtomicInteger(0);
        AtomicInteger bit1Count = new AtomicInteger(0);

        counter.onFallingEdge(0, bit0Count::incrementAndGet);
        counter.onFallingEdge(1, bit1Count::incrementAndGet);

        // Run 4 tCycles: bit 0 falls at 2, 4; bit 1 falls at 4
        for (int i = 0; i < 4; i++) {
            counter.tCycle();
        }

        assertThat(bit0Count.get()).isEqualTo(2); // Fell at counter 2 and 4
        assertThat(bit1Count.get()).isEqualTo(1); // Fell at counter 4
    }

    @Test
    void shouldNotifyOnWrapAround() {
        AtomicInteger callCount = new AtomicInteger(0);
        counter.onFallingEdge(15, callCount::incrementAndGet);

        // Advance to 0xFFFF
        for (int i = 0; i < 0xFFFF; i++) {
            counter.tCycle();
        }
        assertThat(callCount.get()).isEqualTo(0);

        // Wrap: 0xFFFF -> 0x0000, bit 15 falls
        counter.tCycle();
        assertThat(callCount.get()).isEqualTo(1);
    }
}
