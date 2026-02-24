package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Target;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;

class PopTest {
    static Stream<Target.Stk16> stk16Targets() {
        return Stream.of(Target.Stk16.LOOKUP_TABLE);
    }

    @ParameterizedTest
    @MethodSource("stk16Targets")
    void givenWord_whenPop_thenWordInRegisterAndStackCorrect(Target.Stk16 destination) {
        int sp = 0x6543;
        int expected = 0x1230;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withSP(sp)
                .withStack(expected)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        Pop.pop_stk16(destination).execute(cpuStructure);

        short actual = accessor.getValue(destination);

        assertThatHex(actual).isEqualTo((short) expected);
        assertThat(cpuStructure.registers().SP()).isEqualTo((short) (sp + 2));
    }
}
