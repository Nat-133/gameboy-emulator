package org.gameboy.cpu.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CpuClockTest {
    @Test
    void givenClock_whenCpuTick_thenTimeUpdated() {
        CpuClock clock = new CpuClock();
        long initialTime = clock.getTime();

        clock.tick();

        long updatedTime = clock.getTime();
        assertEquals(initialTime + 1, updatedTime);
    }
}