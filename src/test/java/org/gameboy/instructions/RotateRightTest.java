package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.Flag;
import org.gameboy.FlagChangesetBuilder;
import org.gameboy.components.CpuStructure;
import org.gameboy.utils.BitUtilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertFlagsMatch;

class RotateRightTest {
    static Stream<Arguments> getRotateRightValues() {
        return Stream.of(
                Arguments.of(0b01010101, 0b0_0101010),
                Arguments.of(0b00000000, 0b0_0000000),
                Arguments.of(0b11111111, 0b0_1111111),
                Arguments.of(0b11101000, 0b0_1110100)
        );
    }

    @ParameterizedTest
    @MethodSource("getRotateRightValues")
    void givenA_andCarrySet_whenRRA_thenResultAndFlagsCorrect(int a, int expected7Bits) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.C)
                .withA(a)
                .build();

        RotateRight.rra().execute(cpuStructure);

        byte expectedResult = BitUtilities.set_bit((byte) expected7Bits, 7, true);
        assertThat(cpuStructure.registers().A()).isEqualTo(expectedResult);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .withAll(false)
                .with(Flag.C, (a & 0b0000_0001) != 0)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @MethodSource("getRotateRightValues")
    void givenA_andCarryUnset_whenRRA_thenResultAndFlagsCorrect(int a, int expected7Bits) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelyUnsetFlags(Flag.C)
                .withA(a)
                .build();

        RotateRight.rra().execute(cpuStructure);

        byte expectedResult = BitUtilities.set_bit((byte) expected7Bits, 7, false);
        assertThat(cpuStructure.registers().A()).isEqualTo(expectedResult);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .withAll(false)
                .with(Flag.C, (a & 0b0000_0001) != 0)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }
}