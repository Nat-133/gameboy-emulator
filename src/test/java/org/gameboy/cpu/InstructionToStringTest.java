package org.gameboy.cpu;

import org.gameboy.cpu.components.UnprefixedOpcodeTable;
import org.gameboy.cpu.instructions.Instruction;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class InstructionToStringTest {

    static Stream<Arguments> getAllUnprefixedInstructions()
    {
        UnprefixedOpcodeTable unprefixedOpcodeTable = new UnprefixedOpcodeTable();
        return IntStream.range(0, 0xFF+1)
                .mapToObj(i -> (byte)i)
                .map(unprefixedOpcodeTable::lookup)
                .map(instruction -> Arguments.of(Named.of(instruction.representation(), instruction)));
    }

    @ParameterizedTest(name="{0}")
    @MethodSource("getAllUnprefixedInstructions")
    public void givenInstruction_thenToStringIsEqualToRepresentation(Instruction instruction) {
        assertThat(instruction.toString()).withFailMessage("toString() not implemented for %s\n".formatted(instruction.getClass().getSimpleName())).isEqualTo(instruction.representation());
    }
}
