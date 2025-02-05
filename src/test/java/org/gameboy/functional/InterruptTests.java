package org.gameboy.functional;

import org.gameboy.Cpu;
import org.gameboy.CpuStructureBuilder;
import org.gameboy.UnprefixedDecoder;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.targets.Interrupt;
import org.gameboy.utils.BitUtilities;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.gameboy.MemoryMapConstants.*;

public class InterruptTests {

    public static final int RETI_OPCODE = 0xd9;
    public static final byte JOYPAD_INTERRUPT_SET = BitUtilities.set_bit((byte) 0, Interrupt.JOYPAD.index(), true);

    @Test
    void givenInterruptHandlerIsOnlyRETI_whenInterruptReturns_thenCpuReturnsToPreInterruptState() {
        int pc = 0x1234;
        int sp = 0x9081;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(pc)
                .withSP(sp)
                .withMemory(JOYPAD_HANDLER_ADDRESS, RETI_OPCODE)
                .withMemory(IE_ADDRESS, 0xff)
                .build();
        Cpu cpu = new Cpu(cpuStructure, new UnprefixedDecoder(), new UnprefixedDecoder());

        cpu.cycle();  // NOP ==> pc_1 <- pc_0 + 1
        cpuStructure.memory().write(IF_ADDRESS, JOYPAD_INTERRUPT_SET);  // Request Interrupt
        cpu.cycle();  // NOP, followed by interrupt handler routine ==> pc_2 <- HANDLER_ADDRESS
        cpu.cycle();  // RETI ==> pc_3 <- pc_1, + 1
        cpu.cycle();  // NOP ==> pc_4 <- pc_3 + 1 === pc_1 + 2 === pc_0 + 3

        assertThatHex(cpuStructure.registers().PC()).isEqualTo((short) (pc + 3));
        assertThatHex(cpuStructure.registers().SP()).isEqualTo((short) sp);
        assertThat(cpuStructure.registers().IME()).isTrue();
        assertThat(cpuStructure.interruptBus().activeInterrupts()).doesNotContain(Interrupt.JOYPAD);
    }
}
