package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertFlagsMatch;

class RotateRightCircularTest {
    static Stream<Arguments> getRotateRightCircularValues() {
        return Stream.of(
                Arguments.of(0b01010101, 0b10101010),
                Arguments.of(0b00000000, 0b00000000),
                Arguments.of(0b11111111, 0b11111111),
                Arguments.of(0b11101000, 0b01110100)
        );
    }

    @ParameterizedTest
    @MethodSource("getRotateRightCircularValues")
    void givenA_whenRRCA_thenResultAndFlagsCorrect(int a, int expectedResult) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .build();

        RotateRightCircular.rrca().execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) expectedResult);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .withAll(false)
                .with(Flag.C, (a & 0b0000_0001) != 0)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

}