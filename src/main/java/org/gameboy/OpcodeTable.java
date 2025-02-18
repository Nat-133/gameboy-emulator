package org.gameboy;

import org.gameboy.instructions.Instruction;

public interface OpcodeTable {
    Instruction lookup(byte opcode);
}
