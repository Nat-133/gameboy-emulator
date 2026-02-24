package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import static org.gameboy.cpu.instructions.targets.Target.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.gameboy.utils.BitUtilities.lower_byte;
import static org.gameboy.utils.BitUtilities.set_lower_byte;

class LoadTest {
    static Stream<R8> r8Values() {
        return Arrays.stream(R8.LOOKUP_TABLE);
    }

    @ParameterizedTest
    @MethodSource("r8Values")
    void givenImmediateByte_whenLoadIntoRegister_thenRegisterUpdatedCorrectly(R8 register) {
        byte immediateByteValue = (byte) 0xfa;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(0x00fa)
                .withImm8(immediateByteValue)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        BasicLoad.ld_r8_imm8(register).execute(cpuStructure);

        byte registerValueAfter = (byte) accessor.getValue(register);
        assertThat(registerValueAfter).isEqualTo(immediateByteValue);
    }

    static Stream<Arguments> byteRegisterPairs() {
        R8[] a = R8.LOOKUP_TABLE;
        R8[] b = R8.LOOKUP_TABLE;

        return Arrays.stream(a)
            .flatMap(firstArgument ->
                    Arrays.stream(b)
                            .map(secondArgument -> Arguments.of(firstArgument, secondArgument))
            );
    }

    @ParameterizedTest
    @MethodSource("byteRegisterPairs")
    void givenRegisterData_whenLoadIntoRegister_thenRegisterUpdatedCorrectly(R8 destination, R8 source) {
        short registerData = (short) 0xfa;
        CpuStructure cpuStructure = new CpuStructureBuilder().build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(source, registerData);

        BasicLoad.ld_r8_r8(destination, source).execute(cpuStructure);

        byte registerValueAfter = (byte) accessor.getValue(destination);
        assertThat(registerValueAfter).isEqualTo(lower_byte(registerData));
    }

    @ParameterizedTest
    @ValueSource(bytes = {(byte) 0x00, (byte) 0xff, (byte) 0xaf, (byte) 0x67, (byte) 0x14})
    void givenMemoryLocation_whenLoadIntoA_withIndirectC_thenRegisterUpdatedCorrectly(byte memoryLocation) {
        Instruction instruction = BasicLoad.ld_A_indirectC();
        byte value = (byte) 0xfa;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withC(memoryLocation)
                .withHigherMemory(memoryLocation, value)
                .build();

        instruction.execute(cpuStructure);

        byte registerValueAfter = cpuStructure.registers().A();
        assertThat(registerValueAfter).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(bytes = {(byte) 0x00, (byte) 0xff, (byte) 0xaf, (byte) 0x67, (byte) 0x14})
    void givenMemoryLocation_whenLoadAIntoIndirectC_thenMemoryUpdatedCorrectly(byte memoryLocation) {
        byte value = (byte) 0xfa;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withC(memoryLocation)
                .withA(value)
                .build();

        BasicLoad.ld_indirectC_A().execute(cpuStructure);

        byte memoryValueAfter = cpuStructure.memory().read(set_lower_byte((short) 0xff00, memoryLocation));

        assertThat(memoryValueAfter).isEqualTo(value);
    }

    @Test
    void testLdHlSpPlusPositiveOffset() {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(0x0000, 0x01)  // n = 1 (immediate value to be read)
                .withSP(0xFFFE)
                .withPC(0x0000)
                .build();

        Load.ld_HL_SP_OFFSET().execute(cpuStructure);

        assertThatHex(cpuStructure.registers().HL()).isEqualTo((short) 0xFFFF);

        assertThat(cpuStructure.registers().getFlag(Flag.Z)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.N)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.H)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.C)).isFalse();

        assertThatHex(cpuStructure.registers().SP()).isEqualTo((short) 0xFFFE);
    }

    @Test
    void testLdHlSpPlusNegativeOffset() {
        // LD HL,SP+n with negative offset (-1)
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(0x0000, 0xFF)  // n = -1
                .withSP(0xFFFE)
                .withPC(0x0000)
                .build();

        Load.ld_HL_SP_OFFSET().execute(cpuStructure);

        assertThatHex(cpuStructure.registers().HL()).isEqualTo((short) 0xFFFD);

        assertThat(cpuStructure.registers().getFlag(Flag.Z)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.N)).isFalse();

        assertThatHex(cpuStructure.registers().SP()).isEqualTo((short) 0xFFFE);
    }

    @Test
    void testLdHlSpWithCarry() {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(0x0000, 0x02)  // n = 2
                .withSP(0x00FF)
                .withPC(0x0000)
                .build();

        Load.ld_HL_SP_OFFSET().execute(cpuStructure);

        assertThatHex(cpuStructure.registers().HL()).isEqualTo((short) 0x0101);

        assertThat(cpuStructure.registers().getFlag(Flag.Z)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.N)).isFalse();
        assertThat(cpuStructure.registers().getFlag(Flag.C)).isTrue();
    }

    @Test
    void testLdSpHl() {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withHL(0x1234)
                .withSP(0xFFFE)
                .build();

        Load.load_SP_HL().execute(cpuStructure);

        assertThatHex(cpuStructure.registers().SP()).isEqualTo((short) 0x1234);
        assertThatHex(cpuStructure.registers().HL()).isEqualTo((short) 0x1234);
    }
}
