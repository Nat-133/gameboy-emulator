package org.gameboy;

import org.gameboy.instructions.*;
import org.gameboy.instructions.targets.ByteRegister;
import org.gameboy.instructions.targets.WordGeneralRegister;
import org.gameboy.instructions.targets.WordMemoryRegister;
import org.gameboy.utils.MultiBitValue.OneBitValue;
import org.gameboy.utils.MultiBitValue.ThreeBitValue;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

import static org.gameboy.instructions.Unimplemented.UNIMPLEMENTED;
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
                case 1 -> Load.ld_imm16indirect_sp();
                case 2 -> UNIMPLEMENTED; // STOP
                case 3 -> UNIMPLEMENTED; // JR imm8; imm8 signed
                default -> UNIMPLEMENTED; // JR condition[y-4], imm8; imm8 signed
            };
            case b001 -> switch(q){
                case b0 -> Load.ld_r16_imm16(WordGeneralRegister.values()[p.ordinal()]);
                case b1 -> UNIMPLEMENTED; // Add.add_HL_wordGeneralRegister
            };
            case b010 -> {
                WordMemoryRegister register = WordMemoryRegister.values()[p.ordinal()];
                yield switch(q) {
                    case b0 -> Load.ld_mem16indirect_A(register); // Load.load_wordMemoryRegisterIndirect_A(register);
                    case b1 -> Load.ld_A_mem16indirect(register); // Load.load_A_wordMemoryRegisterIndirect(register);
                };
            }
            case b011 -> switch (q) {
                case b0 -> Inc.inc_r16(WordGeneralRegister.values()[p.ordinal()]);
                case b1 -> Dec.dec_r16(WordGeneralRegister.values()[p.ordinal()]);
            };
            case b100 -> Inc.inc_r8(ByteRegister.values()[y]);
            case b101 -> Dec.dec_r8(ByteRegister.values()[y]);
            case b110 -> Load.ld_r8_imm8(ByteRegister.values()[y]);
            case b111 -> UNIMPLEMENTED;
        };
    }

    private Instruction decodeBlock1(int y, int z) {
        if (y == 0b110 && z == 0b110) {
            return Halt.HALT();
        }

        return Load.ld_r8_r8(ByteRegister.values()[y], ByteRegister.values()[z]);
    }

    private Instruction decodeBlock2(int y, int z) {
        return UNIMPLEMENTED;
    }

    private Instruction decodeBlock3(int y, int z) {
        return switch (ThreeBitValue.from(z)) {
            case b000 -> switch(ThreeBitValue.from(y)) {
                case b000 -> UNIMPLEMENTED;
                case b001 -> UNIMPLEMENTED;
                case b010 -> UNIMPLEMENTED;
                case b011 -> UNIMPLEMENTED;
                case b100 -> LoadHigher.ldh_imm8_A();
                case b101 -> UNIMPLEMENTED;
                case b110 -> LoadHigher.ldh_A_imm8();
                case b111 -> Load.load_HL_SP_OFFSET();
            };
            case b001 -> switch(ThreeBitValue.from(y)) {
                case b000 -> UNIMPLEMENTED;
                case b001 -> UNIMPLEMENTED;
                case b010 -> UNIMPLEMENTED;
                case b011 -> UNIMPLEMENTED;
                case b100 -> UNIMPLEMENTED;
                case b101 -> UNIMPLEMENTED;
                case b110 -> UNIMPLEMENTED;
                case b111 -> Load.load_SL_HL();
            };
            case b010 -> switch (ThreeBitValue.from(y)) {
                case b000 -> UNIMPLEMENTED;
                case b001 -> UNIMPLEMENTED;
                case b010 -> UNIMPLEMENTED;
                case b011 -> UNIMPLEMENTED;
                case b100 -> Load.ld_indirectC_A();
                case b101 -> Load.ld_imm16indirect_A();
                case b110 -> Load.ld_A_indirectC();
                case b111 -> Load.ld_A_imm16indirect();
            };
            case b011 -> UNIMPLEMENTED;
            case b100 -> UNIMPLEMENTED;
            case b101 -> UNIMPLEMENTED;
            case b110 -> UNIMPLEMENTED;
            case b111 -> UNIMPLEMENTED;
        };
    }
}
