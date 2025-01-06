package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.targets.Condition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.gameboy.TestUtils.getConditionFlags;

class ConditionalReturnTest {
    @ParameterizedTest
    @EnumSource(Condition.class)
    void givenConditionSatisfied_whenConditionalReturnOnCondition_thenConditionalReturn(Condition cc) {
        int sp = 0xabcd;
        int expected_pc = 0x1234;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(getConditionFlags(cc, true))
                .withSP(sp)
                .withStack(expected_pc)
                .build();

        ConditionalReturn.ret_cc(cc).execute(cpuStructure);

        short expected_sp = (short) (sp + 2);
        assertThatHex(cpuStructure.registers().PC()).isEqualTo((short) expected_pc);
        assertThatHex(cpuStructure.registers().SP()).isEqualTo(expected_sp);
    }

    @ParameterizedTest
    @EnumSource(Condition.class)
    void givenConditionUnsatisfied_whenConditionalReturnOnCondition_thenDoNotConditionalReturn(Condition cc) {
        int expected_sp = 0xabcd;
        int stack = 0x1234;
        int expected_pc = 0x5712;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(getConditionFlags(cc, false))
                .withPC(expected_pc)
                .withSP(expected_sp)
                .withStack(stack)
                .build();

        ConditionalReturn.ret_cc(cc).execute(cpuStructure);

        assertThatHex(cpuStructure.registers().PC()).isEqualTo((short) expected_pc);
        assertThatHex(cpuStructure.registers().SP()).isEqualTo((short) expected_sp);
    }

}