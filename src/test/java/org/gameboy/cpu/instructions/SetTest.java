package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Target;
import org.gameboy.utils.MultiBitValue.ThreeBitValue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.gameboy.utils.BitUtilities.get_bit;

class SetTest {
    static Stream<Target.R8> r8Values() {
        return Arrays.stream(Target.R8.LOOKUP_TABLE);
    }

    @ParameterizedTest
    @MethodSource("r8Values")
    void givenByteRegisterWithAllBitsClear_whenSetBit0_thenBit0SetAndValueCorrect(Target.R8 r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.Z, Flag.N, Flag.H, Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b0000_0000;
        byte expectedValue = (byte) 0b0000_0001;
        accessor.setValue(r8, initialValue);

        Set.set_b_r8(ThreeBitValue.b000, r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8);
        assertThatHex(actualValue).isEqualTo(expectedValue);

        assertThat(cpuStructure.registers().getFlag(Flag.Z)).isTrue();
        assertThat(cpuStructure.registers().getFlag(Flag.N)).isTrue();
        assertThat(cpuStructure.registers().getFlag(Flag.H)).isTrue();
        assertThat(cpuStructure.registers().getFlag(Flag.C)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("r8Values")
    void givenByteRegisterWithAllBitsClear_whenSetBit7_thenBit7SetAndValueCorrect(Target.R8 r8) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelyUnsetFlags(Flag.Z, Flag.N, Flag.H, Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        byte initialValue = (byte) 0b0000_0000;
        byte expectedValue = (byte) 0b1000_0000;
        accessor.setValue(r8, initialValue);

        Set.set_b_r8(ThreeBitValue.b111, r8).execute(cpuStructure);

        byte actualValue = (byte) accessor.getValue(r8);
        assertThatHex(actualValue).isEqualTo(expectedValue);

        assertThat(cpuStructure.registers().getFlag(Flag.Z)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.N)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.H)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.C)).isFalse();
    }

    static Stream<Arguments> getSetBitValues() {
        return IntStream.range(0, 8)
                .boxed()
                .map(bitIndex -> Arguments.of(
                        ThreeBitValue.values()[bitIndex],
                        bitIndex
                ));
    }

    @ParameterizedTest
    @MethodSource("getSetBitValues")
    void givenByteRegisterAndBitIndex_whenSet_thenBitSetAndFlagsUnchanged(ThreeBitValue threeBitIndex, int bitIndex) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.Z, Flag.N, Flag.H, Flag.C)
                .build();

        byte initialValue = (byte) 0b0000_0000;
        cpuStructure.registers().setA(initialValue);

        Set.set_b_r8(threeBitIndex, Target.a).execute(cpuStructure);

        assertThat(get_bit(cpuStructure.registers().A(), bitIndex)).isTrue();

        assertThat(cpuStructure.registers().getFlag(Flag.Z)).isTrue();
        assertThat(cpuStructure.registers().getFlag(Flag.N)).isTrue();
        assertThat(cpuStructure.registers().getFlag(Flag.H)).isTrue();
        assertThat(cpuStructure.registers().getFlag(Flag.C)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("getSetBitValues")
    void givenByteRegisterWithBitAlreadySet_whenSet_thenBitRemainsSet(ThreeBitValue threeBitIndex, int bitIndex) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .build();

        byte initialValue = (byte) 0b1111_1111;
        cpuStructure.registers().setA(initialValue);

        Set.set_b_r8(threeBitIndex, Target.a).execute(cpuStructure);

        assertThat(get_bit(cpuStructure.registers().A(), bitIndex)).isTrue();
        assertThatHex(cpuStructure.registers().A()).isEqualTo((byte) 0b1111_1111);
    }
}
