package org.gameboy.cpu.instructions.common;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.components.CpuStructure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.gameboy.cpu.utils.BitUtilities.lower_byte;
import static org.gameboy.cpu.utils.BitUtilities.upper_byte;

class ControlFlowTest {
    @Test
    void givenCpuStructure_whenReadIndirectPCAndIncrement_thenResultCorrect_andPCIncremented() {
        short initialPC = 54;
        byte expectedImm8 = 67;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm8(expectedImm8)
                .build();

        byte actualImm8 = ControlFlow.readIndirectPCAndIncrement(cpuStructure);

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

    @Test
    void givenShort_whenPushToStack_thenStateIsCorrect() {
        int sp = 0xff16;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withSP(sp)
                .build();

        short value = (short) 0xabcd;
        ControlFlow.pushToStack(cpuStructure, value);

        assertThatHex(cpuStructure.memory().read((short) (sp - 1))).isEqualTo(upper_byte(value));
        assertThatHex(cpuStructure.memory().read((short) (sp - 2))).isEqualTo(lower_byte(value));

        assertThatHex(cpuStructure.registers().SP()).isEqualTo((short) (sp - 2));
        assertThat(cpuStructure.clock().getTime()).isEqualTo(3);
    }

    @Test
    void givenShort_whenPopFromStack_thenStateIsCorrect() {
        int sp = 0xff16;
        int expected = 0x1234;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withSP(sp)
                .withStack(expected)
                .build();

        short actual = ControlFlow.popFromStack(cpuStructure);

        assertThatHex(actual).isEqualTo((short) expected);

        assertThatHex(cpuStructure.registers().SP()).isEqualTo((short) (sp + 2));
        assertThat(cpuStructure.clock().getTime()).isEqualTo(2);
    }
}