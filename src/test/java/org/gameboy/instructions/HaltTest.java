package org.gameboy.instructions;

import org.gameboy.Cpu;
import org.gameboy.CpuStructureBuilder;
import org.gameboy.components.Clock;
import org.gameboy.components.CpuStructure;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.gameboy.MemoryMapConstants.*;
import static org.gameboy.TestUtils.waitFor;

class HaltTest {
    @Test
    void givenInterruptWaiting_whenHalt_thenClockStoppedAndImmediatelyStarted() {
        TestClock clock = new TestClock();
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(IF_ADDRESS, 0x0f)
                .withMemory(IE_ADDRESS, 0x0f)
                .withIME(true)
                .withClock(clock)
                .build();

        Halt.halt().execute(cpuStructure);

        assertThat(clock.isStopped).isFalse();
        assertThat(clock.hasStopped).isTrue();
        assertThat(clock.hasStarted).isTrue();
    }

    @Test
    void givenIMESet_whenHaltWithInterruptPending_thenPCIncrementedNormally() {
        int pc = 0xabcd;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(IF_ADDRESS, 0x0f)
                .withMemory(IE_ADDRESS, 0x0f)
                .withInstructionRegister(0x01)
                .withIME(true)
                .withPC(pc)
                .build();
        Cpu cpu = new Cpu(cpuStructure);

        cpu.cycle();

        assertThatHex(cpuStructure.registers().PC()).isEqualTo((short) (SERIAL_HANDLER_ADDRESS + 1));
    }

    @Test
    void givenIMEUnset_whenHaltBeforeInterruptPending_thenPCIncrementedNormally() throws TimeoutException {
        int pc = 0xabcd;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(IF_ADDRESS, 0x00)
                .withMemory(IE_ADDRESS, 0x0f)
                .withUnprefixedOpcodeTable(opcode -> Halt.halt())
                .withIME(false)
                .withPC(pc)
                .build();
        Cpu cpu = new Cpu(cpuStructure);

        Thread waitThread = new Thread(cpu::cycle);
        Thread writeThread = new Thread(() -> cpuStructure.memory().write(IF_ADDRESS, (byte) 0x0f));

        waitThread.start();
        waitFor(() -> waitThread.getState() == Thread.State.WAITING || waitThread.getState() == Thread.State.BLOCKED);

        writeThread.start();
        waitFor(() -> !writeThread.isAlive());

        waitFor(() -> !waitThread.isAlive());
        assertThat(waitThread.isAlive()).isFalse();
        Halt.halt().execute(cpuStructure);

        assertThatHex(cpuStructure.registers().PC()).isEqualTo((short) (pc + 1));

    }

    @Test
    void givenIMEUnset_whenHaltAndInterruptPending_thenPCFailsToIncrement() {
        int pc = 0xabcd;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(IF_ADDRESS, 0x0f)
                .withMemory(IE_ADDRESS, 0x0f)
                .withUnprefixedOpcodeTable(opcode -> Halt.halt())
                .withIME(false)
                .withPC(pc)
                .build();
        Cpu cpu = new Cpu(cpuStructure);

        cpu.cycle();

        assertThatHex(cpuStructure.registers().PC()).isEqualTo((short) (pc));
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