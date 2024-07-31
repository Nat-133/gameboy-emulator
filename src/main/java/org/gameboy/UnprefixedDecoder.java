package org.gameboy;

import org.gameboy.instructions.Halt;
import org.gameboy.instructions.Instruction;
import org.gameboy.instructions.Load;
import org.gameboy.instructions.Nop;
import org.gameboy.instructions.targets.ByteRegister;
import org.gameboy.instructions.targets.WordGeneralRegister;
import org.gameboy.instructions.targets.WordMemoryRegister;
import org.gameboy.utils.MultiBitValue.OneBitValue;
import org.gameboy.utils.MultiBitValue.ThreeBitValue;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

import static org.gameboy.utils.BitUtilities.*;

public class UnprefixedDecoder implements Decoder {
    /*
     * |-opcode--------|
     * |7 6 5 4 3 2 1 0|
     * | x |  y  |  z  |
     * |   | p |q|     |
     */


    public Instruction decode(byte opcode) {
        int x = bit_range(7, 6, opcode);
        int y = bit_range(5, 3, opcode);
        int z = bit_range(2, 0, opcode);

        return switch(TwoBitValue.from(x)) {
            case b00 -> decodeBlock0(y, z);
            case b01 -> decodeBlock1(y, z);
            case b10 -> decodeBlock2(y, z);
            case b11 -> decodeBlock3(y, z);
        };
    }

    private Instruction decodeBlock0(int y, int z) {
        OneBitValue q = OneBitValue.from(y);
        TwoBitValue p = TwoBitValue.from(y >> 1);

        return switch(ThreeBitValue.from(z)) {
            case b000 -> switch(y) {
                case 0 -> Nop.NOP();
                case 1 -> Load.load_imm16indirect_sp();
                case 2 -> Nop.NOP(); // STOP
                case 3 -> Nop.NOP(); // JR imm8; imm8 signed
                default -> Nop.NOP(); // JR condition[y-4], imm8; imm8 signed
            };
            case b001 -> switch(q){
                case b0 -> Load.load_wordGeneralRegister_imm16(WordGeneralRegister.values()[p.ordinal()]);
                case b1 -> Nop.NOP(); // Add.add_HL_wordGeneralRegister
            };
            case b010 -> {
                WordMemoryRegister register = WordMemoryRegister.values()[p.ordinal()];
                yield switch(q) {
                    case b0 -> Load.load_wordMemoryRegisterIndirect_A(register);
                    case b1 -> Load.load_A_wordMemoryRegisterIndirect(register);
                };
            }
            case b011 -> Nop.NOP();
            case b100 -> Nop.NOP();
            case b101 -> Nop.NOP();
            case b110 -> Nop.NOP();
            case b111 -> Nop.NOP();
        };
    }

    private Instruction decodeBlock1(int y, int z) {
        if (y == 0b110 && z == 0b110) {
            return Halt.HALT();
        }

        return Load.load_register_register(ByteRegister.values()[y], ByteRegister.values()[z]);
    }

    private Instruction decodeBlock2(int y, int z) {
        return Nop.NOP();
    }

    private Instruction decodeBlock3(int y, int z) {
        return Nop.NOP();
    }
}