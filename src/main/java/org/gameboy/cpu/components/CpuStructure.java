package org.gameboy.cpu.components;

import org.gameboy.memory.Memory;

public record CpuStructure(
        CpuRegisters registers,
        Memory memory,
        ArithmeticUnit alu,
        IncrementDecrementUnit idu,
        Clock clock,
        InterruptBus interruptBus,
        Decoder decoder
) {
}
