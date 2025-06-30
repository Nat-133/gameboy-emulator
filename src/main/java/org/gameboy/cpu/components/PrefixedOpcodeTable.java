package org.gameboy.cpu.components;

import org.gameboy.cpu.instructions.Instruction;
import org.gameboy.cpu.instructions.RotateLeft;
import org.gameboy.cpu.instructions.RotateLeftCircular;
import org.gameboy.cpu.instructions.RotateRightCircular;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.gameboy.utils.MultiBitValue.ThreeBitValue;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

import static org.gameboy.cpu.instructions.Unimplemented.UNIMPLEMENTED;

public class PrefixedOpcodeTable implements OpcodeTable {
    /*
     * |-opcode--------|
     * |7 6 5 4 3 2 1 0|
     * | x |  y  |  z  |
     * |   | p |q|     |
     */
    static TwoBitValue get_x(byte opcode) {
        return TwoBitValue.from(opcode, 6);
    }

    static ThreeBitValue get_y(byte opcode) {
        return ThreeBitValue.from(opcode, 3);
    }

    static ThreeBitValue get_z(byte opcode) {
        return ThreeBitValue.from(opcode, 0);
    }

    public Instruction lookup(byte opcode) {

        TwoBitValue x = get_x(opcode);
        ThreeBitValue y = get_y(opcode);
        ThreeBitValue z = get_z(opcode);

        return switch(x) {
            case b00 -> switch(y) {
                case b000 -> RotateLeftCircular.rlc_r8(ByteRegister.lookup(z));
                case b001 -> RotateRightCircular.rrc_r8(ByteRegister.lookup(z));
                case b010 -> RotateLeft.rl_r8(ByteRegister.lookup(z));
                case b011 -> UNIMPLEMENTED;
                case b100 -> UNIMPLEMENTED;
                case b101 -> UNIMPLEMENTED;
                case b110 -> UNIMPLEMENTED;
                case b111 -> UNIMPLEMENTED;
            };
            case b01 -> UNIMPLEMENTED;
            case b10 -> UNIMPLEMENTED;
            case b11 -> UNIMPLEMENTED;
        };
    }
}
