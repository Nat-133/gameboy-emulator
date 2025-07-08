package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.gameboy.utils.BitUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertFlagsMatch;
import static org.gameboy.GameboyAssertions.assertThatHex;

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
                .with(Flag.Z, false)
                .with(Flag.N, false)
                .with(Flag.H, false)
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
                .with(Flag.Z, false)
                .with(Flag.N, false)
                .with(Flag.H, false)
                .with(Flag.C, (a & 0b0000_0001) != 0)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegister_whenRRwithCarrySet_thenStateIsCorrect(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b0101_0101;
        byte expectedValue = (byte) 0b1010_1010;
        accessor.setValue(r8.convert(), initialValue);

        RotateRight.rr_r8(r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8.convert());
        assertThatHex(actualValue).isEqualTo(expectedValue);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.Z, expectedValue == 0)
                .with(Flag.N, false)
                .with(Flag.H, false)
                .with(Flag.C, true)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegister_whenRRwithCarryUnset_thenStateIsCorrect(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelyUnsetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b0101_0101;
        byte expectedValue = (byte) 0b0010_1010;
        accessor.setValue(r8.convert(), initialValue);

        RotateRight.rr_r8(r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8.convert());
        assertThatHex(actualValue).isEqualTo(expectedValue);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.Z, expectedValue == 0)
                .with(Flag.N, false)
                .with(Flag.H, false)
                .with(Flag.C, true)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegister_whenRRwithNoLSBSet_thenCarryUnset(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b1010_1010;
        byte expectedValue = (byte) 0b1101_0101;
        accessor.setValue(r8.convert(), initialValue);

        RotateRight.rr_r8(r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8.convert());
        assertThatHex(actualValue).isEqualTo(expectedValue);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.Z, expectedValue == 0)
                .with(Flag.N, false)
                .with(Flag.H, false)
                .with(Flag.C, false)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegisterWithZeroValue_whenRR_thenZeroFlagSet(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withUnsetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b0000_0000;
        byte expectedValue = (byte) 0b0000_0000;
        accessor.setValue(r8.convert(), initialValue);

        RotateRight.rr_r8(r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8.convert());
        assertThatHex(actualValue).isEqualTo(expectedValue);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.Z, true)
                .with(Flag.N, false)
                .with(Flag.H, false)
                .with(Flag.C, false)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegister_whenRR_thenNAndHFlagsAlwaysZero(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.N, Flag.H)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b1010_1010;
        accessor.setValue(r8.convert(), initialValue);

        RotateRight.rr_r8(r8).execute(cpuStructure);

        assertThat(cpuStructure.registers().getFlag(Flag.N)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.H)).isFalse();
    }

    @Test
    void givenA_whenRRA_thenZNAndHFlagsAlwaysZero() {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.Z, Flag.N, Flag.H)
                .withA(0b1010_1010)
                .build();

        RotateRight.rra().execute(cpuStructure);

        assertThat(cpuStructure.registers().getFlag(Flag.Z)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.N)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.H)).isFalse();
    }
}