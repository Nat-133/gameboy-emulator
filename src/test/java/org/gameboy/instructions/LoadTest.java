package org.gameboy.instructions;

import org.gameboy.components.*;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.utils.BitUtilities.*;

class LoadTest {
    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenImmediateByte_whenLoadIntoRegister_thenRegisterUpdatedCorrectly(ByteRegister register) {
        byte immediateByte = (byte) 0xFA;
        byte zero = (byte) 0;
        CpuRegisters registers = new CpuRegisters(zero, zero , zero, zero ,zero, zero, zero);
        Instruction instruction = Load.ld_r8_imm8(register);
        Memory memory = new Memory();
        memory.write((short) 0x0000, immediateByte);
        CpuStructure cpuStructure = new CpuStructure(registers, memory, new ArithmeticUnit(), new IncrementDecrementUnit());
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        instruction.execute(cpuStructure);

        byte registerValueAfter = (byte) accessor.getValue(register.convert());
        assertThat(registerValueAfter).isEqualTo(immediateByte);
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
        short immediateByte = (short) 0xFA;
        byte zero = (byte) 0;
        CpuRegisters registers = new CpuRegisters(zero, zero , zero, zero ,zero, zero, zero);
        Instruction instruction = Load.ld_r8_r8(destination, source);
        CpuStructure cpuStructure = new CpuStructure(registers, new Memory(), new ArithmeticUnit(), new IncrementDecrementUnit());
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        accessor.setValue(source.convert(), immediateByte);

        instruction.execute(cpuStructure);

        byte registerValueAfter = (byte) accessor.getValue(destination.convert());
        assertThat(registerValueAfter).isEqualTo(lower_byte(immediateByte));
    }

    @ParameterizedTest
    @ValueSource(bytes = {(byte) 0x00, (byte) 0xff, (byte) 0xaf, (byte) 0x67, (byte) 0x14})
    void givenMemoryLocation_whenLoadIntoA_withIndirectC_thenRegisterUpdatedCorrectly(byte memoryLocation) {
        Instruction instruction = Load.ld_A_indirectC();
        byte value = (byte) 0xFA;
        CpuRegisters registers = new CpuRegisters();
        Memory memory = new Memory();
        CpuStructure cpuStructure = new CpuStructure(registers, memory, new ArithmeticUnit(), new IncrementDecrementUnit());

        memory.write(set_lower_byte((short)0xff00, memoryLocation), value);
        registers.setC(memoryLocation);

        instruction.execute(cpuStructure);

        byte registerValueAfter = registers.A();
        assertThat(registerValueAfter).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(bytes = {(byte) 0x00, (byte) 0xff, (byte) 0xaf, (byte) 0x67, (byte) 0x14})
    void givenMemoryLocation_whenLoadAIntoIndirectC_thenMemoryUpdatedCorrectly(byte memoryLocation) {
        Instruction instruction = Load.ld_indirectC_A();
        byte value = (byte) 0xFA;
        CpuRegisters registers = new CpuRegisters();
        Memory memory = new Memory();
        CpuStructure cpuStructure = new CpuStructure(registers, memory, new ArithmeticUnit(), new IncrementDecrementUnit());

        registers.setC(memoryLocation);
        registers.setA(value);

        instruction.execute(cpuStructure);

        byte memoryValueAfter = memory.read(set_lower_byte((short) 0xff00, memoryLocation));

        assertThat(memoryValueAfter).isEqualTo(value);
    }
}