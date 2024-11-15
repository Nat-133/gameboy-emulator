package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.Flag;
import org.gameboy.FlagChangesetBuilder;
import org.gameboy.components.CpuStructure;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertFlagsMatch;

class RotateLeftCircularTest {
    static Stream<Arguments> getRotateLeftCircularValues() {
        return Stream.of(
                Arguments.of(0b01010101, 0b10101010),
                Arguments.of(0b00000000, 0b00000000),
                Arguments.of(0b11111111, 0b11111111),
                Arguments.of(0b11101000, 0b11010001)
        );
    }

    @ParameterizedTest
    @MethodSource("getRotateLeftCircularValues")
    void givenA_whenRLCA_thenResultAndFlagsCorrect(int a, int expectedResult) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .build();

        RotateLeftCircular.rlca().execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) expectedResult);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.C, (a & 0b1000_0000) != 0)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

}