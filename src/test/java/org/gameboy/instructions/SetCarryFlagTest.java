package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.Flag;
import org.gameboy.FlagChangesetBuilder;
import org.gameboy.components.CpuStructure;
import org.junit.jupiter.api.Test;

import java.util.Hashtable;

import static org.gameboy.GameboyAssertions.assertFlagsMatch;

class SetCarryFlagTest {
    @Test
    void whenSetCarryFlag_thenCorrectFlagsSet() {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelyUnsetFlags(Flag.C)
                .build();

        SetCarryFlag.scf().execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.N, false)
                .with(Flag.H, false)
                .with(Flag.C, true)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }
}