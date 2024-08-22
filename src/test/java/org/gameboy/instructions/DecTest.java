package org.gameboy.instructions;

import org.gameboy.ArithmeticUnit.FlagValue;
import org.gameboy.CpuRegisters;
import org.gameboy.Flag;
import org.gameboy.Memory;
import org.gameboy.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;
import org.gameboy.instructions.targets.WordGeneralRegister;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.ArithmeticUnit.FlagValue.setFlag;
import static org.gameboy.ArithmeticUnit.FlagValue.unsetFlag;
import static org.gameboy.Flag.*;
import static org.gameboy.utils.BitUtilities.set_upper_byte;

class DecTest {
    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegisterWithZeroValue_whenDec_thenRegisterUpdatedCorrectly(ByteRegister register) {
        Instruction instruction = Dec.dec_r8(register);
        CpuRegisters registers = new CpuRegisters();
        OperationTargetAccessor accessor = new OperationTargetAccessor(new Memory(), registers);

        instruction.execute(accessor);

        byte registerValueAfter = (byte) accessor.getValue(register.convert());
        assertThat(registerValueAfter).isEqualTo((byte) 0xff);
    }

    @ParameterizedTest
    @EnumSource(WordGeneralRegister.class)
    void givenByteRegisterWithZeroValue_whenDec_thenRegisterUpdatedCorrectly(WordGeneralRegister register) {
        Instruction instruction = Dec.dec_r16(register);
        short zero = (short) 0x0000;
        CpuRegisters registers = new CpuRegisters(zero, zero, zero, zero, zero, zero, (byte)0);
        OperationTargetAccessor accessor = new OperationTargetAccessor(new Memory(), registers);

        instruction.execute(accessor);

        short registerValueAfter = accessor.getValue(register.convert());
        assertThat(registerValueAfter).isEqualTo((short) 0xffff);
    }

    static Stream<Arguments> getDecValues() {
        return Stream.of(
                Arguments.of((byte) 0x00, List.of(unsetFlag(Z), setFlag(N), setFlag(H))),
                Arguments.of((byte) 0x0f, List.of(unsetFlag(Z), setFlag(N), unsetFlag(H))),
                Arguments.of((byte) 0xff, List.of(unsetFlag(Z), setFlag(N), unsetFlag(H))),
                Arguments.of((byte) 0xf0, List.of(unsetFlag(Z), setFlag(N), setFlag(H)))
        );
    }

    @ParameterizedTest(name="{0}")
    @MethodSource("getDecValues")
    void givenByte_whenDec_thenFlagsCorrect(byte value, List<FlagValue> expectedFlags) {
        short zero = (short) 0;
        Instruction instruction = Dec.dec_r8(ByteRegister.A);
        CpuRegisters registers = new CpuRegisters(set_upper_byte(zero, value), zero, zero, zero, zero, zero, (byte) zero);
        OperationTargetAccessor accessor = new OperationTargetAccessor(new Memory(), registers);

        instruction.execute(accessor);

        List<FlagValue> actualFlags = expectedFlags.stream()
                .map(FlagValue::flag)
                .map(flag -> new FlagValue(flag, registers.getFlag(flag)))
                .toList();

        assertThat(actualFlags).containsExactlyElementsOf(expectedFlags);
    }

    @ParameterizedTest
    @EnumSource
    void givenShort_whenDec_thenNoFlags(WordGeneralRegister register) {
        Instruction instruction = Dec.dec_r16(register);
        short zero = (short) 0x0000;
        CpuRegisters registers = new CpuRegisters(zero, zero, zero, zero, zero, zero, (byte) zero);
        OperationTargetAccessor accessor = new OperationTargetAccessor(new Memory(), registers);

        instruction.execute(accessor);

        List<Boolean> actualFlags = Arrays.stream(Flag.values()).map(registers::getFlag).toList();

        assertThat(actualFlags).doesNotContain(true);
    }
}