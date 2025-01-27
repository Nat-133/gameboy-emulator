package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.components.CpuStructure;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;

class DisableInterruptsTest {
    @Test
    void givenDi_whenExecute_thenCpuStateCorrect() {
        int pc = 0xabcd;
        int imm8 = 0x56;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withIME(true)
                .withPC(pc)
                .withImm8(imm8)
                .build();

        DisableInterrupts.di().execute(cpuStructure);

        assertThatHex(cpuStructure.registers().PC()).isEqualTo((short) (pc + 1));
        assertThatHex(cpuStructure.registers().instructionRegister()).isEqualTo((byte) imm8);
        assertThat(cpuStructure.registers().getIME()).isFalse();
    }
}