package org.gameboy.cpu.components;

import org.gameboy.cpu.instructions.Instruction;

public interface OpcodeTable {
    Instruction lookup(byte opcode);
}
