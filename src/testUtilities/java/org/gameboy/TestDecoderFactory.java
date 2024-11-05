package org.gameboy;

import org.gameboy.instructions.Instruction;

public class TestDecoderFactory {
    public static Decoder testDecoder(Instruction returnValue) {
        return ignored -> returnValue;
    }
}
