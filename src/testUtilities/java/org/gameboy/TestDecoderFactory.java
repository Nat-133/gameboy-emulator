package org.gameboy;

import org.gameboy.instructions.Instruction;

public class TestDecoderFactory {
    public static OpcodeTable testDecoder(Instruction returnValue) {
        return ignored -> returnValue;
    }
}
