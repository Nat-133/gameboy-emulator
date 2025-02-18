package org.gameboy.components;

import org.gameboy.OpcodeTable;

public record CpuStructure(
        CpuRegisters registers,
        Memory memory,
        ArithmeticUnit alu,
        IncrementDecrementUnit idu,
        Clock clock,
        InterruptBus interruptBus,
        OpcodeTable opcodeTable
) {
}
