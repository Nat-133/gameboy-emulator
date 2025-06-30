package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.common.Interrupt;
import org.gameboy.cpu.Cpu;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.gameboy.common.MemoryMapConstants.IE_ADDRESS;
import static org.gameboy.common.MemoryMapConstants.IF_ADDRESS;
import static org.gameboy.utils.BitUtilities.set_bit;

class HardwareInterruptTest {
    @Test
    void givenPC_whenHardwareInterruptCalled_thenOverlappedFetchRewound_andPCSavedToStack() {
        int sp = 0x1234;
        int pc = 0xabcd;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withSP(sp)
                .withPC(pc)
                .withIME(true)
                .build();

        HardwareInterrupt.callInterruptHandler(cpuStructure, Interrupt.JOYPAD);


        assertThatHex(cpuStructure.registers().SP()).isEqualTo((short) (sp - 2));
        assertThatHex(ControlFlow.popFromStack(cpuStructure)).isEqualTo((short) (pc - 1));
    }

    @ParameterizedTest
    @EnumSource(Interrupt.class)
    void givenHardwareInterruptCalled_thenCorrectIFReset(Interrupt interrupt) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(IE_ADDRESS, 0xff)
                .withMemory(IF_ADDRESS, set_bit((byte) 0, interrupt.index(), true))
                .withSP(0x1234)
                .withPC(0xabcd)
                .withIME(true)
                .build();

        HardwareInterrupt.callInterruptHandler(cpuStructure, interrupt);

        assertThatHex(cpuStructure.memory().read(IF_ADDRESS)).isEqualTo((byte) 0);
    }

    @ParameterizedTest
    @EnumSource(Interrupt.class)
    void givenHardwareInterruptCalled_thenInstructionRegisterHasCorrectHandler(Interrupt interrupt) {
        int opcode = 0xab;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(IE_ADDRESS, 0xff)
                .withMemory(interrupt.getInterruptHandlerAddress(), opcode)
                .withSP(0x1234)
                .withPC(0xabcd)
                .withIME(true)
                .build();

        HardwareInterrupt.callInterruptHandler(cpuStructure, interrupt);

        assertThatHex(cpuStructure.registers().instructionRegister()).isEqualTo((byte) opcode);
    }

    @Test
    void givenImeSet_whenHardwareInterruptCalled_thenImeUnset() {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withIME(true)
                .build();

        HardwareInterrupt.callInterruptHandler(cpuStructure, Interrupt.STAT);

        assertThat(cpuStructure.registers().IME()).isFalse();
    }

    @Test
    void givenInterruptSent_whenHandleInterrupt_thenFiveCyclesUsed() {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withUnprefixedOpcodeTable(ignored -> Nop.nop())
                .withIME(true)
                .withMemory(IE_ADDRESS, 0xff)
                .withMemory(IF_ADDRESS, 0xff)
                .build();
        long initialTime = cpuStructure.clock().getTime();

        new Cpu(cpuStructure).cycle();

        // nop + 5 for interrupt handler
        assertThatHex(cpuStructure.clock().getTime()).isEqualTo(initialTime + 6);
    }

}