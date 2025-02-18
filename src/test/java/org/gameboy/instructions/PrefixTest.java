package org.gameboy.instructions;

import org.gameboy.Cpu;
import org.gameboy.CpuStructureBuilder;
import org.gameboy.TestInstruction;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PrefixTest {
    @Test
    void givenPrefixInstruction_whenCpuCycle_thenNextInstructionIsFromUnprefixedDecoder() {
        AtomicBoolean prefixedInstructionCalled = new AtomicBoolean(false);
        TestInstruction prefixedInstruction = new TestInstruction("prefixed instruction")
                .withBehaviour(structure -> prefixedInstructionCalled.set(true));

        Cpu cpu = new Cpu(new CpuStructureBuilder()
                .withUnprefixedOpcodeTable(opcode -> Prefix.prefix())
                .withPrefixedOpcodeTable(opcode -> prefixedInstruction)
                .build()
        );

        cpu.cycle();
        assertThat(prefixedInstructionCalled.get()).isFalse();

        cpu.cycle();
        assertThat(prefixedInstructionCalled.get()).isTrue();
    }
}