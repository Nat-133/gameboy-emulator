package org.gameboy.components;

import org.gameboy.instructions.Instruction;

public class Decoder {
    private final OpcodeTable unprefixedOpcodeTable;
    private final OpcodeTable prefixedOpcodeTable;
    private OpcodeTable activeTable;

    public Decoder(OpcodeTable unprefixedOpcodeTable, OpcodeTable prefixedOpcodeTable) {
        this.unprefixedOpcodeTable = unprefixedOpcodeTable;
        this.prefixedOpcodeTable = prefixedOpcodeTable;

        this.activeTable = unprefixedOpcodeTable;
    }

    public Instruction decode(byte opcode) {
        Instruction instruction = this.activeTable.lookup(opcode);
        this.activeTable = unprefixedOpcodeTable;
        return instruction;
    }

    public void switchTables() {
        this.activeTable = prefixedOpcodeTable;
    }
}
