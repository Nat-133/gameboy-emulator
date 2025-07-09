package org.gameboy.cpu.components;

import org.gameboy.cpu.instructions.*;
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
        ByteRegister target = ByteRegister.lookup(z);

        return switch(x) {
            case b00 -> switch(y) {
                case b000 -> RotateLeftCircular.rlc_r8(target);
                case b001 -> RotateRightCircular.rrc_r8(target);
                case b010 -> RotateLeft.rl_r8(target);
                case b011 -> RotateRight.rr_r8(target);
                case b100 -> ShiftLeftArithmetic.sla_r8(target);
                case b101 -> ShiftRightArithmetic.sra_r8(target);
                case b110 -> Swap.swap_r8(target);
                case b111 -> ShiftRightLogical.srl_r8(target);
            };
            case b01 -> Bit.bit_b_r8(y, target);
            case b10 -> Reset.res_b_r8(y, target);
            case b11 -> UNIMPLEMENTED;
        };
    }
}
