package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.TestInstruction;
import org.gameboy.common.MemoryMapConstants;
import org.gameboy.cpu.Cpu;
import org.gameboy.cpu.components.CpuStructure;
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

    @Test
    void givenPendingInterrupt_whenPrefixExecuted_thenInterruptDoesNotFireBetweenPrefixAndOperand() {
        // The correct CB operand at memory[0x0000] — should be decoded from the prefixed table
        byte cbOperand = 0x42;

        AtomicBoolean correctPrefixedCalled = new AtomicBoolean(false);
        TestInstruction correctPrefixedInstruction = new TestInstruction("correct prefixed instruction")
                .withBehaviour(structure -> correctPrefixedCalled.set(true));

        // The handler byte at 0x0040 is 0xAB. If the bug is present, 0xAB gets decoded
        // from the prefixed table instead of the correct operand 0x42.
        byte handlerByte = (byte) 0xAB;

        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withInstructionRegister(0xCB)    // Start with PREFIX in IR
                .withMemory(0x0000, cbOperand)    // The real CB operand
                .withUnprefixedOpcodeTable(opcode -> {
                    int unsigned = Byte.toUnsignedInt(opcode);
                    if (unsigned == 0xCB) return Prefix.prefix();
                    return Nop.nop();
                })
                .withPrefixedOpcodeTable(opcode -> {
                    // Only the correct CB operand maps to our tracked instruction
                    if (opcode == cbOperand) return correctPrefixedInstruction;
                    return new TestInstruction("wrong prefixed instruction");
                })
                .withIME(true)
                .withMemory(MemoryMapConstants.IE_ADDRESS, 0xFF)
                .withMemory(MemoryMapConstants.IF_ADDRESS, 0x01)              // VBLANK pending
                .withMemory(MemoryMapConstants.VBLANK_HANDLER_ADDRESS, handlerByte) // handler opcode
                .withSP(0xDFF0)
                .build();

        Cpu cpu = new Cpu(cpuStructure);

        // Cycle 1: Decode IR=0xCB → PREFIX → execute (switchTables + fetch CB operand)
        // BUG: without fix, interrupt fires during fetch_cycle, overwrites IR with
        // handler byte (0xAB) which gets decoded from CB table instead of 0x42
        cpu.cycle();

        // Cycle 2: Should decode the correct CB operand (0x42) from the prefixed table
        cpu.cycle();

        // The correct prefixed instruction (for operand 0x42) should have executed
        // If the bug is present, 0xAB (handler byte) would have been decoded from
        // the prefixed table instead, and correctPrefixedCalled would be false
        assertThat(correctPrefixedCalled.get()).isTrue();
    }
}