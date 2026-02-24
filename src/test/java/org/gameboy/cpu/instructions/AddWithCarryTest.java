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

class AddWithCarryTest {
    static Stream<Arguments> getR8ValuePairs() {
        return Stream.of(
                Arguments.of(0x12, Target.a, 0x12),
                Arguments.of(0xff, Target.b, 0x01),
                Arguments.of(0x0f, Target.c, 0x07),
                Arguments.of(0xf0, Target.d, 0x0f),
                Arguments.of(0xfa, Target.e, 0x09),
                Arguments.of(0x00, Target.h, 0x00),
                Arguments.of(0x08, Target.l, 0x07)
        );
    }

    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenAdcWithNoCarry_thenResultIsCorrect(int a, Target.R8 r8, int b){
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8, (short) b);

        AddWithCarry.adc_a_r8(r8).execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) (a+b));
    }

    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenAdcWithNoCarry_thenFlagsAreCorrect(int a, Target.R8 r8, int b){
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8, (short) b);

        AddWithCarry.adc_a_r8(r8).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.H, lower_nibble((byte) a) + lower_nibble((byte) b) >= 0x10)
                .with(Flag.N, false)
                .with(Flag.C, a+b >= 0x100)
                .with(Flag.Z, (byte)(a+b) == 0)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @Test
    void givenTwoBytes_whenAdcIndirectHlWithNoCarry_thenResultIsCorrect(){
        byte a = (byte) 16;
        byte b = (byte) 25;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(15)
                .withIndirectHL(b)
                .withA(a)
                .build();

        AddWithCarry.adc_a_r8(Target.indirect_hl).execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) (a+b));
    }

    @Test
    void givenTwoBytes_whenAdcIndirectHlWithNoCarry_thenFlagsAreCorrect(){
        byte a = (byte) 0x1a;
        byte b = (byte) 0x2b;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(15)
                .withIndirectHL(b)
                .withA(a)
                .build();

        AddWithCarry.adc_a_r8(Target.indirect_hl).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.H, true)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenAdcWithCarry_thenResultIsCorrect(int a, Target.R8 r8, int b){
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .withExclusivelySetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8, (short) b);

        AddWithCarry.adc_a_r8(r8).execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) (a+b+1));
    }

    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenAdcWithCarry_thenFlagsAreCorrect(int a, Target.R8 r8, int b){
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .withExclusivelySetFlags(Flag.C)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8, (short) b);

        AddWithCarry.adc_a_r8(r8).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.H, lower_nibble((byte) a) + lower_nibble((byte) b) + 1 >= 0x10)
                .with(Flag.N, false)
                .with(Flag.C, a+b+1 >= 0x100)
                .with(Flag.Z, (byte)(a+b+1) == 0)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @Test
    void givenTwoBytes_whenAdcIndirectHlWithCarry_thenResultIsCorrect(){
        byte a = (byte) 16;
        byte b = (byte) 25;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(15)
                .withIndirectHL(b)
                .withA(a)
                .withExclusivelySetFlags(Flag.C)
                .build();

        AddWithCarry.adc_a_r8(Target.indirect_hl).execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) (a+b+1));
    }

    @Test
    void givenTwoBytes_whenAdcIndirectHlWithCarry_thenFlagsAreCorrect(){
        byte a = (byte) 0x18;
        byte b = (byte) 0x27;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(15)
                .withIndirectHL(b)
                .withA(a)
                .withExclusivelySetFlags(Flag.C)
                .build();

        AddWithCarry.adc_a_r8(Target.indirect_hl).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.H, true)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }
}
