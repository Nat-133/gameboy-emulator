package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.components.Clock;
import org.gameboy.components.CpuStructure;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.MemoryMapConstants.IE_ADDRESS;
import static org.gameboy.MemoryMapConstants.IF_ADDRESS;

class HaltTest {
    @Test
    void givenInterruptWaiting_whenHalt_thenClockStoppedAndImmediatelyStarted() {
        TestClock clock = new TestClock();
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(IF_ADDRESS, 0x0f)
                .withMemory(IE_ADDRESS, 0x0f)
                .withClock(clock)
                .build();

        Halt.halt().execute(cpuStructure);

        assertThat(clock.isStopped).isFalse();
        assertThat(clock.hasStopped).isTrue();
        assertThat(clock.hasStarted).isTrue();
    }

    private static class TestClock implements Clock {
        public boolean hasStopped = false;
        public boolean hasStarted = false;
        public boolean isStopped = false;

        @Override
        public void tick() {

        }

        @Override
        public long getTime() {
            return 0;
        }

        @Override
        public void stop() {
            this.hasStopped = true;
            this.isStopped = true;
        }

        @Override
        public void start() {
            this.hasStarted = true;
            this.isStopped = false;
        }
    }
}