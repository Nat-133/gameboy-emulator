package org.gameboy.cpu.components;

import org.gameboy.cpu.instructions.Instruction;
import org.gameboy.cpu.instructions.Nop;

import static org.gameboy.cpu.utils.BitUtilities.uint;

public class PrefixedOpcodeTable implements OpcodeTable {
    public Instruction lookup(byte opcode) {
        int x = uint(opcode) >> 6;
        int y = (uint(opcode) >> 3) & 0b00000111;
        int z = uint(opcode) & 0b00000111;

        return Nop.nop();
    }
}
