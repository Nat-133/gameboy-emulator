package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.gameboy.utils.MultiBitValue.ThreeBitValue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertFlagsMatch;

class BitTest {
    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegisterWithBitSet_whenBitTestBit0_thenZeroFlagUnset(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.Z, Flag.N)
                .withExclusivelyUnsetFlags(Flag.H, Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b0000_0001;
        accessor.setValue(r8.convert(), initialValue);

        Bit.bit_b_r8(ThreeBitValue.b000, r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8.convert());
        assertThat(actualValue).isEqualTo(initialValue);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.Z, false)
                .with(Flag.N, false)
                .with(Flag.H, true)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegisterWithBitUnset_whenBitTestBit0_thenZeroFlagSet(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelyUnsetFlags(Flag.Z, Flag.N, Flag.H)
                .withExclusivelySetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b1111_1110;
        accessor.setValue(r8.convert(), initialValue);

        Bit.bit_b_r8(ThreeBitValue.b000, r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8.convert());
        assertThat(actualValue).isEqualTo(initialValue);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.Z, true)
                .with(Flag.N, false)
                .with(Flag.H, true)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegisterWithBitSet_whenBitTestBit7_thenZeroFlagUnset(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.Z, Flag.N)
                .withExclusivelyUnsetFlags(Flag.H, Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b1000_0000;
        accessor.setValue(r8.convert(), initialValue);

        Bit.bit_b_r8(ThreeBitValue.b111, r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8.convert());
        assertThat(actualValue).isEqualTo(initialValue);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.Z, false)
                .with(Flag.N, false)
                .with(Flag.H, true)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegisterWithBitUnset_whenBitTestBit7_thenZeroFlagSet(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelyUnsetFlags(Flag.Z, Flag.N, Flag.H)
                .withExclusivelySetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b0111_1111;
        accessor.setValue(r8.convert(), initialValue);

        Bit.bit_b_r8(ThreeBitValue.b111, r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8.convert());
        assertThat(actualValue).isEqualTo(initialValue);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.Z, true)
                .with(Flag.N, false)
                .with(Flag.H, true)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    static Stream<Arguments> getBitTestValues() {
        return Stream.of(
                Arguments.of(ThreeBitValue.b000, (byte) 0b0000_0001, false),
                Arguments.of(ThreeBitValue.b000, (byte) 0b1111_1110, true),
                Arguments.of(ThreeBitValue.b001, (byte) 0b0000_0010, false),
                Arguments.of(ThreeBitValue.b001, (byte) 0b1111_1101, true),
                Arguments.of(ThreeBitValue.b010, (byte) 0b0000_0100, false),
                Arguments.of(ThreeBitValue.b010, (byte) 0b1111_1011, true),
                Arguments.of(ThreeBitValue.b011, (byte) 0b0000_1000, false),
                Arguments.of(ThreeBitValue.b011, (byte) 0b1111_0111, true),
                Arguments.of(ThreeBitValue.b100, (byte) 0b0001_0000, false),
                Arguments.of(ThreeBitValue.b100, (byte) 0b1110_1111, true),
                Arguments.of(ThreeBitValue.b101, (byte) 0b0010_0000, false),
                Arguments.of(ThreeBitValue.b101, (byte) 0b1101_1111, true),
                Arguments.of(ThreeBitValue.b110, (byte) 0b0100_0000, false),
                Arguments.of(ThreeBitValue.b110, (byte) 0b1011_1111, true),
                Arguments.of(ThreeBitValue.b111, (byte) 0b1000_0000, false),
                Arguments.of(ThreeBitValue.b111, (byte) 0b0111_1111, true)
        );
    }

    @ParameterizedTest
    @MethodSource("getBitTestValues")
    void givenByteRegisterAndBitIndex_whenBitTest_thenFlagsCorrectAndValueUnchanged(ThreeBitValue bitIndex, byte value, boolean expectedZeroFlag) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.N)
                .withExclusivelyUnsetFlags(Flag.H)
                .withA(value)
                .build();

        Bit.bit_b_r8(bitIndex, ByteRegister.A).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.Z, expectedZeroFlag)
                .with(Flag.N, false)
                .with(Flag.H, true)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegister_whenBitTest_thenCarryFlagUnchanged(ByteRegister r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        accessor.setValue(r8.convert(), (byte) 0b1010_1010);

        Bit.bit_b_r8(ThreeBitValue.b000, r8).execute(cpuStructure);

        assertThat(cpuStructure.registers().getFlag(Flag.C)).isTrue();
    }
}