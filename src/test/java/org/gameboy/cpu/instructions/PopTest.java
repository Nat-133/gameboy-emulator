package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.WordStackRegister;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;

class PopTest {
    @ParameterizedTest
    @EnumSource(WordStackRegister.class)
    void givenWord_whenPop_thenWordInRegisterAndStackCorrect(WordStackRegister destination) {
        int sp = 0x6543;
        int expected = 0x1234;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withSP(sp)
                .withStack(expected)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        Pop.pop_stk16(destination).execute(cpuStructure);

        short actual = accessor.getValue(destination.convert());

        assertThatHex(actual).isEqualTo((short) expected);
        assertThat(cpuStructure.registers().SP()).isEqualTo((short) (sp + 2));
    }
}
