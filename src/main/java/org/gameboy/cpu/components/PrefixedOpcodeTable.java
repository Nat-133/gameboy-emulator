package org.gameboy.cpu.components;

import org.gameboy.cpu.instructions.Instruction;

import static org.gameboy.cpu.instructions.Unimplemented.UNIMPLEMENTED;

public class PrefixedOpcodeTable implements OpcodeTable {
    public Instruction lookup(byte opcode) {
        return UNIMPLEMENTED;
    }
}
