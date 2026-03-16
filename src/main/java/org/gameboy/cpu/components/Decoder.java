package org.gameboy.cpu.components;

import com.google.inject.Inject;
import org.gameboy.cpu.annotations.Prefixed;
import org.gameboy.cpu.annotations.Unprefixed;
import org.gameboy.cpu.instructions.Instruction;

public class Decoder {
    private final OpcodeTable unprefixedOpcodeTable;
    private final OpcodeTable prefixedOpcodeTable;
    private OpcodeTable activeTable;

    @Inject
    public Decoder(@Unprefixed OpcodeTable unprefixedOpcodeTable, @Prefixed OpcodeTable prefixedOpcodeTable) {
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
