package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
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
                .with(Flag.Z, false)
                .with(Flag.N, false)
                .with(Flag.H, false)
                .with(Flag.C, (a & 0b0000_0001) != 0)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegister_whenRRCwithZeroCarry_thenStateIsCorrect(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withUnsetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue =  (byte) 0b0101_0101;
        byte expectedValue = (byte) 0b1010_1010;
        accessor.setValue(r8.convert(), initialValue);

        RotateRightCircular.rrc_r8(r8).execute(cpuStructure);

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
    void givenByteRegister_whenRRCwithOneCarry_thenStateIsCorrect(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withUnsetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue =  (byte) 0b1010_1010;
        byte expectedValue = (byte) 0b0101_0101;
        accessor.setValue(r8.convert(), initialValue);

        RotateRightCircular.rrc_r8(r8).execute(cpuStructure);

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
    void givenByteRegisterWithZeroValue_whenRRC_thenZeroFlagSet(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withUnsetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b0000_0000;
        byte expectedValue = (byte) 0b0000_0000;
        accessor.setValue(r8.convert(), initialValue);

        RotateRightCircular.rrc_r8(r8).execute(cpuStructure);

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
    void givenByteRegister_whenRRC_thenNAndHFlagsAlwaysZero(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.N, Flag.H)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b1010_1010;
        accessor.setValue(r8.convert(), initialValue);

        RotateRightCircular.rrc_r8(r8).execute(cpuStructure);

        assertThat(cpuStructure.registers().getFlag(Flag.N)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.H)).isFalse();
    }

    @Test
    void givenA_whenRRCA_thenZNAndHFlagsAlwaysZero() {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.Z, Flag.N, Flag.H)
                .withA(0b1010_1010)
                .build();

        RotateRightCircular.rrca().execute(cpuStructure);

        assertThat(cpuStructure.registers().getFlag(Flag.Z)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.N)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.H)).isFalse();
    }
}