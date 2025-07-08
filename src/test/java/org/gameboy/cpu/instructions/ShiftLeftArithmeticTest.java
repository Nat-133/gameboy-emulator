package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Hashtable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertFlagsMatch;
import static org.gameboy.GameboyAssertions.assertThatHex;

class ShiftLeftArithmeticTest {
    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegister_whenSlaWithCarrySet_thenStateIsCorrect(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b1010_1010;
        byte expectedValue = (byte) 0b0101_0100;
        accessor.setValue(r8.convert(), initialValue);

        ShiftLeftArithmetic.sla_r8(r8).execute(cpuStructure);

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
    void givenByteRegister_whenSLAwithCarryUnset_thenStateIsCorrect(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelyUnsetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b1010_1010;
        byte expectedValue = (byte) 0b0101_0100;
        accessor.setValue(r8.convert(), initialValue);

        ShiftLeftArithmetic.sla_r8(r8).execute(cpuStructure);

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
    void givenByteRegister_whenSLAwithNoMSBSet_thenCarryUnset(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b0111_0101;
        byte expectedValue = (byte) 0b1110_1010;
        accessor.setValue(r8.convert(), initialValue);

        ShiftLeftArithmetic.sla_r8(r8).execute(cpuStructure);

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
    void givenByteRegisterWithZeroValue_whenSLA_thenZeroFlagSet(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withUnsetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b0000_0000;
        byte expectedValue = (byte) 0b0000_0000;
        accessor.setValue(r8.convert(), initialValue);

        ShiftLeftArithmetic.sla_r8(r8).execute(cpuStructure);

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
    void givenByteRegister_whenSLA_thenNAndHFlagsAlwaysZero(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.N, Flag.H)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b1010_1010;
        accessor.setValue(r8.convert(), initialValue);

        ShiftLeftArithmetic.sla_r8(r8).execute(cpuStructure);

        assertThat(cpuStructure.registers().getFlag(Flag.N)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.H)).isFalse();
    }
}