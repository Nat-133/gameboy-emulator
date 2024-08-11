package org.gameboy.instructions;

import org.gameboy.CpuRegisters;
import org.gameboy.Memory;
import org.gameboy.OperationTargetAccessor;
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
        short immediateByte = (short) 0xFA01;
        byte zero = (byte) 0;
        CpuRegisters registers = new CpuRegisters(zero, zero , zero, zero ,zero, zero, immediateByte);
        Instruction instruction = Load.ld_r8_imm8(register);
        OperationTargetAccessor accessor = new OperationTargetAccessor(new Memory(), registers);

        instruction.execute(accessor);

        byte registerValueAfter = (byte) accessor.getValue(register.convert());
        assertThat(registerValueAfter).isEqualTo(upper_byte(immediateByte));
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
        OperationTargetAccessor accessor = new OperationTargetAccessor(new Memory(), registers);
        accessor.setValue(source.convert(), immediateByte);

        instruction.execute(accessor);

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
        OperationTargetAccessor accessor = new OperationTargetAccessor(memory, registers);

        memory.write(set_lower_byte((short)0xff00, memoryLocation), value);
        registers.setC(memoryLocation);

        instruction.execute(accessor);

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
        OperationTargetAccessor accessor = new OperationTargetAccessor(memory, registers);

        registers.setC(memoryLocation);
        registers.setA(value);

        instruction.execute(accessor);

        byte memoryValueAfter = (byte)memory.read(set_lower_byte((short) 0xff00, memoryLocation));

        assertThat(memoryValueAfter).isEqualTo(value);
    }
}