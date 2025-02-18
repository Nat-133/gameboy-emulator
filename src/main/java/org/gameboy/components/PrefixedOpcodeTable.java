package org.gameboy.components;

import org.gameboy.instructions.Instruction;
import org.gameboy.instructions.Nop;

import static org.gameboy.utils.BitUtilities.uint;

public class PrefixedOpcodeTable implements OpcodeTable {
    public Instruction lookup(byte opcode) {
        int x = uint(opcode) >> 6;
        int y = (uint(opcode) >> 3) & 0b00000111;
        int z = uint(opcode) & 0b00000111;

        return Nop.nop();
    }
}
