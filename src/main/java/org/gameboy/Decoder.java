package org.gameboy;

import org.gameboy.instructions.Instruction;

public interface Decoder {
    Instruction decode(byte opcode);
}
