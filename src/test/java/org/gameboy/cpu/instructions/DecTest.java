package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.FlagValue;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.gameboy.cpu.instructions.targets.WordGeneralRegister;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.FlagValue.setFlag;
import static org.gameboy.FlagValue.unsetFlag;
import static org.gameboy.cpu.Flag.*;

class DecTest {
    @ParameterizedTest
    @EnumSource(ByteRegister.class)
    void givenByteRegisterWithZeroValue_whenDec_thenRegisterUpdatedCorrectly(ByteRegister register) {
        CpuStructure cpuStructure = new CpuStructureBuilder().build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        Dec.dec_r8(register).execute(cpuStructure);

        byte registerValueAfter = (byte) accessor.getValue(register.convert());
        assertThat(registerValueAfter).isEqualTo((byte) 0xff);
    }

    @ParameterizedTest
    @EnumSource(WordGeneralRegister.class)
    void givenByteRegisterWithZeroValue_whenDec_thenRegisterUpdatedCorrectly(WordGeneralRegister register) {
        CpuStructure cpuStructure = new CpuStructureBuilder().build();
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        Dec.dec_r16(register).execute(cpuStructure);

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
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(value)
                .build();

        Dec.dec_r8(ByteRegister.A).execute(cpuStructure);

        List<FlagValue> actualFlags = expectedFlags.stream()
                .map(FlagValue::getKey)
                .map(flag -> new FlagValue(flag, cpuStructure.registers().getFlag(flag)))
                .toList();

        assertThat(actualFlags).containsExactlyElementsOf(expectedFlags);
    }

    @ParameterizedTest
    @EnumSource
    void givenShort_whenDec_thenNoFlags(WordGeneralRegister register) {
        CpuStructure cpuStructure = new CpuStructureBuilder().build();

        Dec.dec_r16(register).execute(cpuStructure);

        List<Boolean> actualFlags = Arrays.stream(Flag.values()).map(cpuStructure.registers()::getFlag).toList();

        assertThat(actualFlags).doesNotContain(true);
    }
}