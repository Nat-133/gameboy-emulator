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

class SwapTest {
    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegister_whenSwapWithNonZeroValue_thenNibblesSwapped(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.N, Flag.H, Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0x12;
        byte expectedValue = (byte) 0x21;
        accessor.setValue(r8.convert(), initialValue);

        Swap.swap_r8(r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8.convert());
        assertThatHex(actualValue).isEqualTo(expectedValue);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.Z, false)
                .with(Flag.N, false)
                .with(Flag.H, false)
                .with(Flag.C, false)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegister_whenSwapWithZeroValue_thenZeroFlagSet(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.N, Flag.H, Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0x00;
        byte expectedValue = (byte) 0x00;
        accessor.setValue(r8.convert(), initialValue);

        Swap.swap_r8(r8).execute(cpuStructure);

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
    void givenByteRegister_whenSwapWithF0Value_thenCorrectlySwapped(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.N, Flag.H, Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0xF0;
        byte expectedValue = (byte) 0x0F;
        accessor.setValue(r8.convert(), initialValue);

        Swap.swap_r8(r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8.convert());
        assertThatHex(actualValue).isEqualTo(expectedValue);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.Z, false)
                .with(Flag.N, false)
                .with(Flag.H, false)
                .with(Flag.C, false)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegister_whenSwapWithABValue_thenCorrectlySwapped(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.N, Flag.H, Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0xAB;
        byte expectedValue = (byte) 0xBA;
        accessor.setValue(r8.convert(), initialValue);

        Swap.swap_r8(r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8.convert());
        assertThatHex(actualValue).isEqualTo(expectedValue);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.Z, false)
                .with(Flag.N, false)
                .with(Flag.H, false)
                .with(Flag.C, false)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegister_whenSwap_thenAllFlagsExceptZeroAlwaysCleared(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.N, Flag.H, Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0x34;
        accessor.setValue(r8.convert(), initialValue);

        Swap.swap_r8(r8).execute(cpuStructure);

        assertThat(cpuStructure.registers().getFlag(Flag.N)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.H)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.C)).isFalse();
    }
}