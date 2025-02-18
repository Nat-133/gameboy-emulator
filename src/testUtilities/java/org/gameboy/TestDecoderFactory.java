package org.gameboy;

import org.gameboy.cpu.components.OpcodeTable;
import org.gameboy.cpu.instructions.Instruction;

public class TestDecoderFactory {
    public static OpcodeTable testDecoder(Instruction returnValue) {
        return ignored -> returnValue;
    }
}
