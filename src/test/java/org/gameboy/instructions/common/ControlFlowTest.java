package org.gameboy.instructions.common;

import org.assertj.core.api.Assertions;
import org.gameboy.CpuStructureBuilder;
import org.gameboy.Flag;
import org.gameboy.components.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.utils.BitUtilities.lower_byte;
import static org.gameboy.utils.BitUtilities.upper_byte;

class ControlFlowTest {
    @Test
    void givenCpuStructure_whenReadImm8_thenResultCorrect_andPCIncremented() {
        short initialPC = 54;
        byte expectedImm8 = 67;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm8(expectedImm8)
                .build();

        byte actualImm8 = ControlFlow.readImm8(cpuStructure);

        assertThat(actualImm8).isEqualTo(expectedImm8);
        assertThat(cpuStructure.registers().PC()).isEqualTo((short) (initialPC + 1));
    }

    @Test
    void givenCpuStructure_whenReadImm16_thenResultCorrect_andPCIncrementedTwice() {
        short initialPC = 54;
        short expectedImm16 = (short) 0xabcd;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm16(expectedImm16)
                .build();

        short actualImm16 = ControlFlow.readImm16(cpuStructure);

        assertThat(actualImm16).isEqualTo(expectedImm16);
        assertThat(cpuStructure.registers().PC()).isEqualTo((short) (initialPC + 2));
    }

    static Stream<Arguments> signedAdditionTestCases() {
        return Stream.of(
                Arguments.of((short) 0xabcd, (byte) 0x34),
                Arguments.of((short) 0x7bcd, (byte) 0xa4),
                Arguments.of((short) 0x7bcd, (byte) 0x34),
                Arguments.of((short) 0xabcd, (byte) 0xf4)
        );
    }

    @ParameterizedTest
    @MethodSource("signedAdditionTestCases")
    void givenShortAndSignedByte_whenSignedAdditionWithIdu_thenResultCorrect(short a, byte b) {
        CpuStructure cpuStructure = new CpuStructureBuilder().build();
        short actual = ControlFlow.signedAdditionWithIdu(a, b, false, cpuStructure);

        short expected = (short) (a + b);
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("signedAdditionTestCases")
    void givenShortAndSignedByte_whenSignedAdditionOnlyAlu_thenResultCorrect(short a, byte b) {
        CpuStructure cpuStructure = new CpuStructureBuilder().build();
        short actual = ControlFlow.signedAdditionOnlyAlu(a, b, cpuStructure);

        short expected = (short) (a + b);
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("signedAdditionTestCases")
    void givenShortAndSignedByte_whenSignedAddition_thenFlagsCorrect(short a, byte b) {
        CpuStructure cpuStructure = new CpuStructureBuilder().build();

        ControlFlow.signedAdditionOnlyAlu(a, b, cpuStructure);

        Hashtable<Flag, Boolean> expectedFlagChanges = cpuStructure.alu().add(lower_byte(a), b).flagChanges();
        expectedFlagChanges.forEach(
                (flag, value) -> assertThat(cpuStructure.registers().getFlag(flag)).isEqualTo(value)
        );
    }
}