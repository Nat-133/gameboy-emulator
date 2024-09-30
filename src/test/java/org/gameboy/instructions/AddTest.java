package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.Flag;
import org.gameboy.components.ArithmeticUnit;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;
import org.gameboy.instructions.targets.WordGeneralRegister;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.gameboy.instructions.targets.WordGeneralRegister.*;
import static org.gameboy.utils.BitUtilities.*;

class AddTest {
    static Stream<Arguments> getR8ValuePairs() {
        return Stream.of(
                Arguments.of(0x12, ByteRegister.A, 0x12),
                Arguments.of(0xff, ByteRegister.B, 0x01),
                Arguments.of(0x0f, ByteRegister.C, 0x07),
                Arguments.of(0xf0, ByteRegister.D, 0x0f),
                Arguments.of(0xfa, ByteRegister.E, 0x09),
                Arguments.of(0x00, ByteRegister.H, 0x00),
                Arguments.of(0x00, ByteRegister.L, 0x01)
        );
    }

    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenAdd_thenResultIsCorrect(int a, ByteRegister r8, int b){
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8.convert(), (short) b);

        Add.add_a_r8(r8).execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) (a+b));
    }

    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenAdd_thenFlagsAreCorrect(int a, ByteRegister r8, int b){
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8.convert(), (short) b);

        Add.add_a_r8(r8).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new ArithmeticUnit.FlagChangesetBuilder()
                .with(Flag.H, lower_nibble((byte) a) + lower_nibble((byte) b) >= 0x10)
                .with(Flag.N, false)
                .with(Flag.C, a+b >= 0x100)
                .with(Flag.Z, (byte)(a+b) == 0)
                .build();
        expectedFlags.forEach(
                (flag, value) -> assertThat(cpuStructure.registers().getFlag(flag)).isEqualTo(value)
        );
    }

    @Test
    void givenTwoBytes_whenAddIndirectHl_thenResultIsCorrect(){
        byte a = (byte) 16;
        byte b = (byte) 25;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(15)
                .withIndirectHL(b)
                .withA(a)
                .build();

        Add.add_a_r8(ByteRegister.INDIRECT_HL).execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) (a+b));
    }

    @Test
    void givenTwoBytes_whenAddIndirectHl_thenFlagsAreCorrect(){
        byte a = (byte) 0x1a;
        byte b = (byte) 0x2b;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(15)
                .withIndirectHL(b)
                .withA(a)
                .build();

        Add.add_a_r8(ByteRegister.INDIRECT_HL).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new ArithmeticUnit.FlagChangesetBuilder()
                .with(Flag.H, true)
                .build();
        expectedFlags.forEach(
                (flag, value) -> assertThat(cpuStructure.registers().getFlag(flag)).isEqualTo(value)
        );
    }

    static Stream<Arguments> getR16ValuePairs() {
        return Stream.of(
                Arguments.of(0xffff, BC, 0x0001),
                Arguments.of(0xff0f, DE, 0x0101),
                Arguments.of(0x00ac, SP, 0x0062),
                Arguments.of(0x0200, HL, 0x0200)
        );
    }

    @ParameterizedTest
    @MethodSource("getR16ValuePairs")
    void givenTwoBytesAnd16BitRegister_whenAdd_thenResultIsCorrect(int a, WordGeneralRegister rr, int b) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(rr.convert(), (short) b);

        Add.add_hl_r16(rr).execute(cpuStructure);

        short expectedResult = (short) (a + b);
        assertThat(cpuStructure.registers().HL()).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("getR16ValuePairs")
    void givenTwoBytesAnd16BitRegister_whenAdd_thenFlagsAreCorrect(int a, WordGeneralRegister rr, int b) {
        int upper_a = a >> 8;
        int upper_b = b >> 8;
        int lower_a = uint((byte) a);
        int lower_b = uint((byte) b);
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(rr.convert(), (short) b);

        Add.add_hl_r16(rr).execute(cpuStructure);

        int carry = lower_a + lower_b > 0xff ? 1 : 0;
        Hashtable<Flag, Boolean> expectedFlags = new ArithmeticUnit.FlagChangesetBuilder()
                .with(Flag.H, lower_nibble((byte) upper_b) + lower_nibble((byte) upper_a) + carry > 0xf)
                .with(Flag.C, upper_a + upper_b + carry > 0xff)
                .with(Flag.Z, (byte) (upper_a + upper_b + carry) == 0)
                .with(Flag.N, false)
                .build();
        expectedFlags.forEach(
                (flag, value) -> assertThat(cpuStructure.registers().getFlag(flag)).isEqualTo(value)
        );
    }
}