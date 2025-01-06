package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.components.CpuStructure;
import org.junit.jupiter.api.Test;

import static org.gameboy.GameboyAssertions.assertThatHex;

class ReturnTest {
    @Test
    void givenStack_whenReturnFromFunction_thenCpuStateCorrect() {
        int sp = 0xabcd;
        int expected_pc = 0x1234;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withSP(sp)
                .withStack(expected_pc)
                .build();

        Return.ret().execute(cpuStructure);

        short expected_sp = (short) (sp + 2);
        assertThatHex(cpuStructure.registers().PC()).isEqualTo((short) expected_pc);
        assertThatHex(cpuStructure.registers().SP()).isEqualTo(expected_sp);
    }
}