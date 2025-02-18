package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.Cpu;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertFlagsMatch;
import static org.gameboy.TestDecoderFactory.testDecoder;

class AndTest {
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
    void givenByteRegisterAndValues_whenAnd_thenResultIsCorrect(int a, ByteRegister r8, int b) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8.convert(), (short) b);

        And.and_r8(r8).execute(cpuStructure);

        assertThat(cpuStructure.registers().A()).isEqualTo((byte) (a & b));
    }

    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegisterAndValues_whenAnd_thenFlagsAreCorrect(int a, ByteRegister r8, int b) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(r8.convert(), (short) b);

        And.and_r8(r8).execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.H, true)
                .with(Flag.N, false)
                .with(Flag.C, false)
                .with(Flag.Z, (a & b) == 0)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }


    @ParameterizedTest
    @MethodSource("getR8ValuePairs")
    void givenByteRegister_whenAnd_thenClockIsCorrect(int ignored1, ByteRegister r8, int ignored2) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withUnprefixedOpcodeTable(testDecoder(And.and_r8(r8)))
                .build();
        Cpu cpu = new Cpu(cpuStructure);

        cpu.cycle();

        assertThat(cpuStructure.clock().getTime()).isEqualTo(1);
    }

    @Test
    void givenCpu_whenAndHl_thenClockIsCorrect() {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withUnprefixedOpcodeTable(testDecoder(And.and_r8(ByteRegister.INDIRECT_HL)))
                .build();
        Cpu cpu = new Cpu(cpuStructure);

        cpu.cycle();

        assertThat(cpuStructure.clock().getTime()).isEqualTo(2);
    }

    @Test
    void givenCpu_whenAndImm8_thenClockIsCorrect() {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withUnprefixedOpcodeTable(testDecoder(And.and_imm8()))
                .build();
        Cpu cpu = new Cpu(cpuStructure);

        cpu.cycle();

        assertThat(cpuStructure.clock().getTime()).isEqualTo(2);
    }
}