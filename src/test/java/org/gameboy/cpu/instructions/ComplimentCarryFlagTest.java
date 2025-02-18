package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Hashtable;

import static org.gameboy.GameboyAssertions.assertFlagsMatch;

class ComplimentCarryFlagTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void givenCarryFlag_whenCompliment_thenCorrectFlagsSet(boolean carry_flag) {
        CpuStructureBuilder cpuStructureBuilder = new CpuStructureBuilder();
        cpuStructureBuilder = carry_flag
                ? cpuStructureBuilder.withExclusivelySetFlags(Flag.C)
                : cpuStructureBuilder.withExclusivelyUnsetFlags(Flag.C);
        CpuStructure cpuStructure = cpuStructureBuilder.build();

        ComplimentCarryFlag.ccf().execute(cpuStructure);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.N, false)
                .with(Flag.H, false)
                .with(Flag.C, !carry_flag)
                .build();
        assertFlagsMatch(expectedFlags, cpuStructure);
    }
}