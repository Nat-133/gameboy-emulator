package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.utils.BitUtilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertFlagsMatch;

class RotateLeftTest {
    static Stream<Arguments> getRotateLeftValues() {
        return Stream.of(
                Arguments.of(0b01010101, 0b1010101_0),
                Arguments.of(0b00000000, 0b0000000_0),
                Arguments.of(0b11111111, 0b1111111_0),
                Arguments.of(0b11101000, 0b1101000_0)
        );
    }

    @ParameterizedTest
    @MethodSource("getRotateLeftValues")
    void givenA_andCarrySet_whenRLA_thenResultAndFlagsCorrect(int a, int mostSignificant7Bits) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.C)
                .withA(a)
                .build();

        RotateLeft.rla().execute(cpuStructure);

        byte expectedResult = BitUtilities.set_bit((byte) mostSignificant7Bits, 0, true);
        assertThat(cpuStructure.registers().A()).isEqualTo(expectedResult);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .withAll(false)
                .with(Flag.C, (a & 0b1000_0000) != 0)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @MethodSource("getRotateLeftValues")
    void givenA_andCarryUnset_whenRLA_thenResultAndFlagsCorrect(int a, int mostSignificant7Bits) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelyUnsetFlags(Flag.C)
                .withA(a)
                .build();

        RotateLeft.rla().execute(cpuStructure);

        byte expectedResult = BitUtilities.set_bit((byte) mostSignificant7Bits, 0, false);
        assertThat(cpuStructure.registers().A()).isEqualTo(expectedResult);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .withAll(false)
                .with(Flag.C, (a & 0b1000_0000) != 0)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }
}