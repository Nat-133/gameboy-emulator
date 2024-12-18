package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.WordStackRegister;
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

        Pop.pop_rr(destination).execute(cpuStructure);

        short actual = accessor.getValue(destination.convert());

        assertThatHex(actual).isEqualTo((short) expected);
        assertThat(cpuStructure.registers().SP()).isEqualTo((short) (sp + 2));
    }
}
