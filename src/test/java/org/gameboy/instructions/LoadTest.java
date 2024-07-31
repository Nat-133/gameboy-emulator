package org.gameboy.instructions;

import org.gameboy.CpuRegisters;
import org.gameboy.Memory;
import org.gameboy.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.utils.BitUtilities.lower_byte;
import static org.gameboy.utils.BitUtilities.upper_byte;

class LoadTest {
    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenImmediateByte_whenLoadIntoRegister_thenRegisterUpdatedCorrectly(ByteRegister register) {
        short immediateByte = (short) 0xFA01;
        byte zero = (byte) 0;
        CpuRegisters registers = new CpuRegisters(zero, zero , zero, zero ,zero, zero, immediateByte);
        Instruction instruction = Load.load_register_imm8(register);
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
        Instruction instruction = Load.load_register_register(destination, source);
        OperationTargetAccessor accessor = new OperationTargetAccessor(new Memory(), registers);
        accessor.setValue(source.convert(), immediateByte);

        instruction.execute(accessor);

        byte registerValueAfter = (byte) accessor.getValue(destination.convert());
        assertThat(registerValueAfter).isEqualTo(lower_byte(immediateByte));
    }
}