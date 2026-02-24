package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.gameboy.GameboyAssertions.assertFlagsMatch;
import static org.gameboy.utils.BitUtilities.lower_nibble;
import static org.gameboy.utils.BitUtilities.uint;

class AddTest {
    static Stream<Arguments> getR8ValuePairs() {
        return Stream.of(
                Arguments.of(0x12, Target.a, 0x12),
                Arguments.of(0xff, Target.b, 0x01),
                Arguments.of(0x0f, Target.c, 0x07),
                Arguments.of(0xf0, Target.d, 0x0f),
                Arguments.of(0xfa, Target.e, 0x09),
                Arguments.of(0x00, Target.h, 0x00),
                Arguments.of(0x00, Target.l, 0x01)
        );
    }

    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenAdd_thenResultIsCorrect(int a, Target.R8 r8, int b){
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8, (short) b);

        Add.add_a_r8(r8).execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) (a+b));
    }

    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenAdd_thenFlagsAreCorrect(int a, Target.R8 r8, int b){
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8, (short) b);

        Add.add_a_r8(r8).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.H, lower_nibble((byte) a) + lower_nibble((byte) b) >= 0x10)
                .with(Flag.N, false)
                .with(Flag.C, a+b >= 0x100)
                .with(Flag.Z, (byte)(a+b) == 0)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
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

        Add.add_a_r8(Target.indirect_hl).execute(cpuStructure);

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

        Add.add_a_r8(Target.indirect_hl).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.H, true)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    static Stream<Arguments> getR16ValuePairs() {
        return Stream.of(
                Arguments.of(0xffff, Target.bc, 0x0001, false),
                Arguments.of(0xff0f, Target.de, 0x0101, true),
                Arguments.of(0x00ac, Target.sp, 0x0062, false),
                Arguments.of(0x0000, Target.sp, 0x0000, true),
                Arguments.of(0x0200, Target.hl, 0x0200, false)
        );
    }

    @ParameterizedTest
    @MethodSource("getR16ValuePairs")
    void givenTwoShortsAnd16BitRegister_whenAdd_thenResultIsCorrect(int a, Target.R16 rr, int b) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(rr, (short) b);

        Add.add_hl_r16(rr).execute(cpuStructure);

        short expectedResult = (short) (a + b);
        assertThat(cpuStructure.registers().HL()).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("getR16ValuePairs")
    void givenTwoShortsAnd16BitRegister_whenAdd_thenFlagsAreCorrect(int a, Target.R16 rr, int b, boolean zFlag) {
        int upper_a = a >> 8;
        int upper_b = b >> 8;
        int lower_a = uint((byte) a);
        int lower_b = uint((byte) b);
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(a)
                .withF(zFlag ? Flag.Z.getLocationMask() : 0)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(rr, (short) b);

        Add.add_hl_r16(rr).execute(cpuStructure);

        int carry = lower_a + lower_b > 0xff ? 1 : 0;

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.H, lower_nibble((byte) upper_b) + lower_nibble((byte) upper_a) + carry > 0xf)
                .with(Flag.C, upper_a + upper_b + carry > 0xff)
                .with(Flag.Z, zFlag)  // flag should be unchanged
                .with(Flag.N, false)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    static Stream<Arguments> getSP_IMM8_Values() {
        return Stream.of(
                Arguments.of(0xffff, 0x80),
                Arguments.of(0x0080, 0x80),
                Arguments.of(0xaf01, 0x81),
                Arguments.of(0x5a97, 0x72)
        );
    }

    @ParameterizedTest
    @MethodSource("getSP_IMM8_Values")
    void givenSP_whenAddE8_thenResultIsCorrect(int sp, int imm8) {
        int initialPC = 74;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withSP(sp)
                .withImm8(imm8)
                .build();

        Add.add_sp_e8().execute(cpuStructure);

        short expectedResult = (short) ((short) sp + (byte) imm8);
        assertThat(cpuStructure.registers().SP()).isEqualTo(expectedResult);
        assertThat(cpuStructure.registers().PC()).isEqualTo((short) (initialPC + 1));
    }

    @ParameterizedTest
    @MethodSource("getSP_IMM8_Values")
    void givenSP_whenAddE8_thenFlagsAreCorrect(int sp, int imm8) {
        int initialPC = 74;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withSP(sp)
                .withImm8(imm8)
                .withSetFlags(Flag.H, Flag.C, Flag.Z, Flag.N)
                .build();

        Add.add_sp_e8().execute(cpuStructure);

        int lowerByteRes = uint((byte) sp) + imm8;
        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.H, lower_nibble((byte) sp) + lower_nibble((byte) imm8) > 0xf)
                .with(Flag.C, lowerByteRes > 0xff)
                .with(Flag.Z, false)
                .with(Flag.N, false)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }
}
