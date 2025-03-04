package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.utils.BitUtilities.lower_byte;
import static org.gameboy.utils.BitUtilities.set_lower_byte;

class LoadTest {
    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenImmediateByte_whenLoadIntoRegister_thenRegisterUpdatedCorrectly(ByteRegister register) {
        byte immediateByteValue = (byte) 0xfa;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(0x00fa)
                .withImm8(immediateByteValue)
                .build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        BasicLoad.ld_r8_imm8(register).execute(cpuStructure);

        byte registerValueAfter = (byte) accessor.getValue(register.convert());
        assertThat(registerValueAfter).isEqualTo(immediateByteValue);
    }

    static Stream<Arguments> byteRegisterPairs() {
        ByteRegister[] a = ByteRegister.values();
        ByteRegister[] b = ByteRegister.values();

        return Arrays.stream(a)
            .flatMap(firstArgument ->
                    Arrays.stream(b)
                            .map(secondArgument -> Arguments.of(firstArgument, secondArgument))
            );
    }

    @ParameterizedTest
    @MethodSource("byteRegisterPairs")
    void givenRegisterData_whenLoadIntoRegister_thenRegisterUpdatedCorrectly(ByteRegister destination, ByteRegister source) {
        short registerData = (short) 0xfa;
        CpuStructure cpuStructure = new CpuStructureBuilder().build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(source.convert(), registerData);

        BasicLoad.ld_r8_r8(destination, source).execute(cpuStructure);

        byte registerValueAfter = (byte) accessor.getValue(destination.convert());
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
}