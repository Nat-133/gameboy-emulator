package org.gameboy.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CpuClockTest {
    @Test
    void givenClock_whenCpuTick_thenTimeUpdated() {
        CpuClock clock = new CpuClock();
        long initialTime = clock.getTime();

        clock.tickCpu();

        long updatedTime = clock.getTime();
        assertEquals(initialTime + 1, updatedTime);
    }
}