package org.gameboy.components;

import org.gameboy.Decoder;

public record CpuStructure(
        CpuRegisters registers,
        Memory memory,
        ArithmeticUnit alu,
        IncrementDecrementUnit idu,
        CpuClock clock,
        InterruptBus interruptBus,
        Decoder decoder
) {
}
