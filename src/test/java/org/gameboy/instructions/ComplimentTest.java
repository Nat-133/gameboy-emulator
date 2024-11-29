package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.Flag;
import org.gameboy.FlagChangesetBuilder;
import org.gameboy.components.CpuStructure;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertFlagsMatch;

class ComplimentTest {
    static Stream<Arguments> getComplimentaryValues() {
        return Stream.of(
                Arguments.of(0b01010101, 0b10101010),
                Arguments.of(0b00000000, 0b11111111),
                Arguments.of(0b11111111, 0b00000000),
                Arguments.of(0b11101000, 0b00010111)
        );
    }


    @ParameterizedTest
    @MethodSource("getComplimentaryValues")
    void givenByte_whenCompliment_thenFlagsAndResultAreCorrect(int a, int notA) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withSetFlags(Flag.Z)
                .withA(a)
                .build();

        Compliment.cpl().execute(cpuStructure);

        Hashtable<Flag,Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.N, true)
                .with(Flag.H, true)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
        assertThat(cpuStructure.registers().A()).isEqualTo((byte) notA);
    }
}