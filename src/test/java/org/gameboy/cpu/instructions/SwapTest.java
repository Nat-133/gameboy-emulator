package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import static org.gameboy.cpu.instructions.targets.Target.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertFlagsMatch;
import static org.gameboy.GameboyAssertions.assertThatHex;

class SwapTest {
    static Stream<R8> r8Values() {
        return Arrays.stream(R8.LOOKUP_TABLE);
    }

    @ParameterizedTest
    @MethodSource("r8Values")
    void givenByteRegister_whenSwapWithNonZeroValue_thenNibblesSwapped(R8 r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.N, Flag.H, Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0x12;
        byte expectedValue = (byte) 0x21;
        accessor.setValue(r8, initialValue);

        Swap.swap_r8(r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8);
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
    @MethodSource("r8Values")
    void givenByteRegister_whenSwapWithZeroValue_thenZeroFlagSet(R8 r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.N, Flag.H, Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0x00;
        byte expectedValue = (byte) 0x00;
        accessor.setValue(r8, initialValue);

        Swap.swap_r8(r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8);
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
    @MethodSource("r8Values")
    void givenByteRegister_whenSwapWithF0Value_thenCorrectlySwapped(R8 r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.N, Flag.H, Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0xF0;
        byte expectedValue = (byte) 0x0F;
        accessor.setValue(r8, initialValue);

        Swap.swap_r8(r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8);
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
    @MethodSource("r8Values")
    void givenByteRegister_whenSwapWithABValue_thenCorrectlySwapped(R8 r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.N, Flag.H, Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0xAB;
        byte expectedValue = (byte) 0xBA;
        accessor.setValue(r8, initialValue);

        Swap.swap_r8(r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8);
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
    @MethodSource("r8Values")
    void givenByteRegister_whenSwap_thenAllFlagsExceptZeroAlwaysCleared(R8 r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.N, Flag.H, Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0x34;
        accessor.setValue(r8, initialValue);

        Swap.swap_r8(r8).execute(cpuStructure);

        assertThat(cpuStructure.registers().getFlag(Flag.N)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.H)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.C)).isFalse();
    }
}
