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

class SubWithCarryTest {
    static Stream<Arguments> getR8ValuePairs() {
        return Stream.of(
                Arguments.of(0x12, Target.a, 0x12),
                Arguments.of(0xff, Target.b, 0x80),
                Arguments.of(0x0f, Target.c, 0x07),
                Arguments.of(0xf0, Target.d, 0x0f),
                Arguments.of(0xfa, Target.e, 0xa9),
                Arguments.of(0x00, Target.h, 0x00),
                Arguments.of(0x00, Target.l, 0x01)
        );
    }

    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenSubWithoutCarry_thenResultIsCorrect(int a, Target.R8 r8, int b) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8, (short) b);

        SubWithCarry.sbc_a_r8(r8).execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) (a - b));
    }

    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenSubWithoutCarry_thenFlagsAreCorrect(int a, Target.R8 r8, int b) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8, (short) b);

        SubWithCarry.sbc_a_r8(r8).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.H, lower_nibble((byte) a) - lower_nibble((byte) b) < 0)
                .with(Flag.N, true)
                .with(Flag.C, a-b < 0)
                .with(Flag.Z, (byte) (a - b) == 0)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @Test
    void givenTwoBytes_whenSubIndirectHlWithoutCarry_thenResultIsCorrect() {
        byte a = (byte) 16;
        byte b = (byte) 25;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(15)
                .withIndirectHL(b)
                .withA(a)
                .build();

        SubWithCarry.sbc_a_r8(Target.indirect_hl).execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) (a - b));
    }

    @Test
    void givenTwoBytes_whenSubIndirectHlWithoutCarry_thenFlagsAreCorrect() {
        byte a = (byte) 0x1a;
        byte b = (byte) 0x2b;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(15)
                .withIndirectHL(b)
                .withA(a)
                .build();

        SubWithCarry.sbc_a_r8(Target.indirect_hl).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.H, true)
                .with(Flag.C, true)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }



    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenSubWithCarry_thenResultIsCorrect(int a, Target.R8 r8, int b) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.C)
                .withA(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8, (short) b);

        SubWithCarry.sbc_a_r8(r8).execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) (a - b - 1));
    }

    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenSubWithCarry_thenFlagsAreCorrect(int a, Target.R8 r8, int b) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.C)
                .withA(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8, (short) b);

        SubWithCarry.sbc_a_r8(r8).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.H, lower_nibble((byte) a) - lower_nibble((byte) b) - 1 < 0)
                .with(Flag.N, true)
                .with(Flag.C, a-b-1 < 0)
                .with(Flag.Z, (byte) (a - b - 1) == 0)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @Test
    void givenTwoBytes_whenSubIndirectHlWithCarry_thenResultIsCorrect() {
        byte a = (byte) 16;
        byte b = (byte) 25;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.C)
                .withHL(15)
                .withIndirectHL(b)
                .withA(a)
                .build();

        SubWithCarry.sbc_a_r8(Target.indirect_hl).execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) (a - b - 1));
    }

    @Test
    void givenTwoBytes_whenSubIndirectHlWithCarry_thenFlagsAreCorrect() {
        byte a = (byte) 0xab;
        byte b = (byte) 0x2b;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.C)
                .withHL(15)
                .withIndirectHL(b)
                .withA(a)
                .build();

        SubWithCarry.sbc_a_r8(Target.indirect_hl).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.H, true)
                .with(Flag.C, false)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }
}
