package org.gameboy;

import org.gameboy.components.CpuStructure;

import java.util.Hashtable;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GameboyAssertions {
    public static void assertFlagsMatch(Hashtable<Flag, Boolean> expectedFlags, CpuStructure cpuStructure) {
        expectedFlags.forEach(
                (flag, value) -> assertThat(cpuStructure.registers().getFlag(flag)).isEqualTo(value)
        );
    }
}
