package org.gameboy.components;

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
