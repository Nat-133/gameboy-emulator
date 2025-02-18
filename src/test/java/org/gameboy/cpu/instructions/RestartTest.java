package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.gameboy.GameboyAssertions.assertThatHex;

class RestartTest {
    static Stream<Arguments> getRestartValues() {
        return Stream.of(
                Arguments.of(Restart.rst_08H(), 0x08),
                Arguments.of(Restart.rst_10H(), 0x10),
                Arguments.of(Restart.rst_18H(), 0x18),
                Arguments.of(Restart.rst_20H(), 0x20),
                Arguments.of(Restart.rst_28H(), 0x28),
                Arguments.of(Restart.rst_30H(), 0x30),
                Arguments.of(Restart.rst_38H(), 0x38)
        );
    }

    @ParameterizedTest
    @MethodSource("getRestartValues")
    void givenRestart_whenExecute_thenStackAndPcAreCorrect(Restart restartInstruction, int jumpAddress) {
        int pc = 0x1234;
        int sp = 0xabcd;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(pc)
                .withSP(sp)
                .build();

        restartInstruction.execute(cpuStructure);

        assertThatHex(cpuStructure.registers().PC()).isEqualTo((short) jumpAddress);
        assertThatHex(cpuStructure.registers().SP()).isEqualTo((short) (sp - 2));

        short topOfStack = ControlFlow.popFromStack(cpuStructure);
        assertThatHex(topOfStack).isEqualTo((short) pc);
    }
}