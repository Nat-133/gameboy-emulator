package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import static org.gameboy.cpu.instructions.targets.Target.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.gameboy.utils.BitUtilities.lower_byte;
import static org.gameboy.utils.BitUtilities.upper_byte;

class PushTest {
    static Stream<Stk16> stk16Targets() {
        return Stream.of(Stk16.LOOKUP_TABLE);
    }

    @ParameterizedTest
    @MethodSource("stk16Targets")
    void givenWord_whenPush_thenWordInRegisterAndStackCorrect(Stk16 destination) {
        int sp = 0x6543;
        short expected = 0x1234;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withSP(sp)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(destination, expected);
        expected = accessor.getValue(destination);

        Push.push_stk16(destination).execute(cpuStructure);

        byte actual_lsb = cpuStructure.memory().read((short) (sp - 2));
        byte actual_msb = cpuStructure.memory().read((short) (sp - 1));

        assertThatHex(actual_lsb).isEqualTo(lower_byte(expected));
        assertThatHex(actual_msb).isEqualTo(upper_byte(expected));
        assertThat(cpuStructure.registers().SP()).isEqualTo((short) (sp - 2));
    }

    @Test
    void givenWord_whenPush_thenPopReturnsWord() {
        int sp = 0x6543;
        short expected = 0x1234;
        Stk16 from = bc;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withSP(sp)
                .withBC(expected)
                .build();

        Push.push_stk16(from).execute(cpuStructure);
        short pop = ControlFlow.popFromStack(cpuStructure);

        assertThatHex(pop).isEqualTo(expected);
    }
}
