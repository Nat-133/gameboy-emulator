package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.components.CpuStructure;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;

class ReturnFromInterruptHandlerTest {
    @Test
    void givenStack_whenReturnFromFunction_thenCpuStateCorrect() {
        int sp = 0xabcd;
        int expected_pc = 0x1234;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withIME(false)
                .withSP(sp)
                .withStack(expected_pc)
                .build();

        ReturnFromInterruptHandler.reti().execute(cpuStructure);
        ReturnFromInterruptHandler.reti().postFetch(cpuStructure);

        short expected_sp = (short) (sp + 2);
        assertThatHex(cpuStructure.registers().PC()).isEqualTo((short) expected_pc);
        assertThatHex(cpuStructure.registers().SP()).isEqualTo(expected_sp);
        assertThat(cpuStructure.registers().IME()).isTrue();
    }
}