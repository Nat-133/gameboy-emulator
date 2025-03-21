package org.gameboy;

import org.gameboy.cpu.Flag;
import org.gameboy.cpu.components.CpuStructure;

import java.util.Hashtable;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GameboyAssertions {
    public static void assertFlagsMatch(Hashtable<Flag, Boolean> expectedFlags, CpuStructure cpuStructure) {
        expectedFlags.forEach(
                (flag, value) -> assertThat(cpuStructure.registers().getFlag(flag))
                        .withFailMessage("Expecting flag %s to be %s, but was %s".formatted(flag, value, !value))
                        .isEqualTo(value)
        );
    }

    public static void assertFlagsMatch(Hashtable<Flag, Boolean> expectedFlags, Hashtable<Flag, Boolean> actualFlags) {
        assertThat(actualFlags).isEqualTo(expectedFlags);
    }

     public static <T extends Number> HexAssert<T> assertThatHex(T value) {
        return new HexAssert<>(value);
    }
}
