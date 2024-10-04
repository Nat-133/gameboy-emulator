package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.Flag;
import org.gameboy.components.ArithmeticUnit;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.gameboy.utils.BitUtilities.lower_nibble;

class SubTest {
    static Stream<Arguments> getR8ValuePairs() {
        return Stream.of(
                Arguments.of(0x12, ByteRegister.A, 0x12),
                Arguments.of(0xff, ByteRegister.B, 0x80),
                Arguments.of(0x0f, ByteRegister.C, 0x07),
                Arguments.of(0xf0, ByteRegister.D, 0x0f),
                Arguments.of(0xfa, ByteRegister.E, 0xa9),
                Arguments.of(0x00, ByteRegister.H, 0x00),
                Arguments.of(0x00, ByteRegister.L, 0x01)
        );
    }

    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenSub_thenResultIsCorrect(int a, ByteRegister r8, int b) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8.convert(), (short) b);

        Sub.sub_a_r8(r8).execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) (a - b));
    }

    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenSub_thenFlagsAreCorrect(int a, ByteRegister r8, int b) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8.convert(), (short) b);

        Sub.sub_a_r8(r8).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new ArithmeticUnit.FlagChangesetBuilder()
                .with(Flag.H, lower_nibble((byte) a) - lower_nibble((byte) b) < 0)
                .with(Flag.N, true)
                .with(Flag.C, a-b < 0)
                .with(Flag.Z, (byte) (a - b) == 0)
                .build();
        expectedFlags.forEach(
                (flag, value) -> assertThat(cpuStructure.registers().getFlag(flag))
                        .withFailMessage("Expecting flag %s to be %s, but was %s".formatted(flag, value, !value))
                        .isEqualTo(value)
        );
    }

    @Test
    void givenTwoBytes_whenSubIndirectHl_thenResultIsCorrect() {
        byte a = (byte) 16;
        byte b = (byte) 25;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(15)
                .withIndirectHL(b)
                .withA(a)
                .build();

        Sub.sub_a_r8(ByteRegister.INDIRECT_HL).execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) (a - b));
    }

    @Test
    void givenTwoBytes_whenSubIndirectHl_thenFlagsAreCorrect() {
        byte a = (byte) 0x1a;
        byte b = (byte) 0x2b;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(15)
                .withIndirectHL(b)
                .withA(a)
                .build();

        Sub.sub_a_r8(ByteRegister.INDIRECT_HL).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new ArithmeticUnit.FlagChangesetBuilder()
                .with(Flag.H, true)
                .with(Flag.C, true)
                .build();
        expectedFlags.forEach(
                (flag, value) -> assertThat(cpuStructure.registers().getFlag(flag)).isEqualTo(value)
        );
    }
}
