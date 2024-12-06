package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.Flag;
import org.gameboy.FlagChangesetBuilder;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.targets.ByteRegister;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertFlagsMatch;

class DecimalAdjustAcumulatorTest {

    static Stream<Arguments> getBytes() {
        return Stream.of(
                Arguments.of(Named.of("0x75", 0x75), Named.of("0x25", 0x25)),
                Arguments.of(Named.of("0x00", 0x00), Named.of("0x00", 0x00)),
                Arguments.of(Named.of("0x01", 0x01), Named.of("0x01", 0x01)),
                Arguments.of(Named.of("0x05", 0x05), Named.of("0x05", 0x05)),
                Arguments.of(Named.of("0x90", 0x90), Named.of("0x10", 0x10)),
                Arguments.of(Named.of("0x99", 0x99), Named.of("0x94", 0x94)),
                Arguments.of(Named.of("0x55", 0x55), Named.of("0x65", 0x65))
        );
    }

    @ParameterizedTest
    @MethodSource("getBytes")
    void givenTwoBytes_whenAddThenDecimalAdjust_thenResultIsDecimal(int a, int b) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .withB(b)
                .build();

        Add.add_a_r8(ByteRegister.B).execute(cpuStructure);
        DecimalAdjustAcumulator.daa().execute(cpuStructure);

        int expected_bcd = decimalAdd(a, b);
        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.C, expected_bcd >= 0x100)
                .with(Flag.Z, (expected_bcd & 0xff) == 0)
                .build();
        assertThat(cpuStructure.registers().A())
                .withFailMessage("Expecting 0x%x + 0x%x to be equal to 0x%x\nBut was 0x%x\n".formatted(a, b, expected_bcd, cpuStructure.registers().A()))
                .isEqualTo((byte) expected_bcd);
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @MethodSource("getBytes")
    void givenTwoBytes_whenSubThenDecimalAdjust_thenResultIsDecimal(int a, int b) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .withB(b)
                .build();

        Sub.sub_r8(ByteRegister.B).execute(cpuStructure);
        boolean carry_from_sub = cpuStructure.registers().getFlag(Flag.C);
        DecimalAdjustAcumulator.daa().execute(cpuStructure);

        int expected_bcd = decimalSub(a, b);
        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.C, expected_bcd >= 0x100 || carry_from_sub)
                .with(Flag.Z, (expected_bcd & 0xff) == 0)
                .with(Flag.H, false)
                .build();
        assertThat(cpuStructure.registers().A())
                .withFailMessage("Expecting 0x%x - 0x%x to be equal to 0x%x\nBut was 0x%x\n".formatted(a, b, expected_bcd, cpuStructure.registers().A()))
                .isEqualTo((byte) expected_bcd);
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    public int decimalAdd(int bcd_a, int bcd_b) {
        int a = Integer.parseInt("%x".formatted(bcd_a));
        int b = Integer.parseInt("%x".formatted(bcd_b));

        int result = a + b;

        //noinspection UnnecessaryLocalVariable
        int bcd_result = Integer.valueOf("%d".formatted(result), 16);
        return bcd_result;
    }

    public int decimalSub(int bcd_a, int bcd_b) {
        int a = Integer.parseInt("%x".formatted(bcd_a));
        int b = Integer.parseInt("%x".formatted(bcd_b));

        int result = (a - b + 100) % 100;

        //noinspection UnnecessaryLocalVariable
        int bcd_result = Integer.valueOf("%d".formatted(result), 16);
        return bcd_result;
    }
}