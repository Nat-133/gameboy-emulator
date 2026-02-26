package org.gameboy.cpu.components;

import org.gameboy.cpu.instructions.*;
import org.gameboy.cpu.instructions.targets.Condition;
import static org.gameboy.cpu.instructions.targets.Target.*;
import org.gameboy.utils.MultiBitValue.OneBitValue;
import org.gameboy.utils.MultiBitValue.ThreeBitValue;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

import static org.gameboy.utils.MultiBitValue.ThreeBitValue.b110;

public class UnprefixedOpcodeTable implements OpcodeTable {
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
        ThreeBitValue y = get_y(opcode);
        ThreeBitValue z = get_z(opcode);

        return switch(get_x(opcode)) {
            case b00 -> decodeBlock0(y, z);
            case b01 -> decodeBlock1(y, z);
            case b10 -> decodeBlock2(y, z);
            case b11 -> decodeBlock3(y, z);
        };
    }

    private Instruction decodeBlock0(ThreeBitValue y, ThreeBitValue z) {
        OneBitValue q = OneBitValue.from(y, 0);
        TwoBitValue p = TwoBitValue.from(y, 1);

        return switch(z) {
            case b000 -> switch(y) {
                case b000 -> Nop.nop();
                case b001 -> Load.ld_imm16indirect_sp();
                case b010 -> Stop.stop();
                case b011 -> JumpRelative.jr();
                default -> JumpRelative.jr_cc(Condition.lookup(TwoBitValue.from(y, 0)));
            };
            case b001 -> switch(q){
                case b0 -> Load.ld_r16_imm16(R16.lookup(p));
                case b1 -> Add.add_hl_r16(R16.lookup(p));
            };
            case b010 -> {
                Mem16 register = Mem16.lookup(p);
                yield switch(q) {
                    case b0 -> Load.ld_mem16indirect_A(register);
                    case b1 -> Load.ld_A_mem16indirect(register);
                };
            }
            case b011 -> switch (q) {
                case b0 -> Inc.inc_r16(R16.lookup(p));
                case b1 -> Dec.dec_r16(R16.lookup(p));
            };
            case b100 -> Inc.inc_r8(R8.lookup(y));
            case b101 -> Dec.dec_r8(R8.lookup(y));
            case b110 -> Load.ld_r8_imm8(R8.lookup(y));
            case b111 -> switch(y) {
                case b000 -> RotateLeftCircular.rlca();
                case b001 -> RotateRightCircular.rrca();
                case b010 -> RotateLeft.rla();
                case b011 -> RotateRight.rra();
                case b100 -> DecimalAdjustAcumulator.daa();
                case b101 -> Compliment.cpl();
                case b110 -> SetCarryFlag.scf();
                case b111 -> ComplimentCarryFlag.ccf();
            };
        };
    }

    private Instruction decodeBlock1(ThreeBitValue y, ThreeBitValue z) {
        if (y == b110 && z == b110) {
            return Halt.halt();
        }

        return Load.ld_r8_r8(R8.lookup(y), R8.lookup(z));
    }

    private Instruction decodeBlock2(ThreeBitValue y, ThreeBitValue z) {
        R8 r8 = R8.lookup(z);
        return switch(y) {
            case b000 -> Add.add_a_r8(r8);
            case b001 -> AddWithCarry.adc_a_r8(r8);
            case b010 -> Sub.sub_r8(r8);
            case b011 -> SubWithCarry.sbc_a_r8(r8);
            case b100 -> And.and_r8(r8);
            case b101 -> Xor.xor_r8(r8);
            case b110 -> Or.or_r8(r8);
            case b111 -> Compare.cp_r8(r8);
        };
    }

    private Instruction decodeBlock3(ThreeBitValue y, ThreeBitValue z) {
        OneBitValue q = OneBitValue.from(y, 0);
        return switch (z) {
            case b000 -> switch(y) {
                case b000,
                     b001,
                     b010,
                     b011 -> ConditionalReturn.ret_cc(Condition.lookup(TwoBitValue.from(y, 0)));
                case b100 -> LoadHigher.ldh_imm8_A();
                case b101 -> Add.add_sp_e8();
                case b110 -> LoadHigher.ldh_A_imm8();
                case b111 -> Load.ld_HL_SP_OFFSET();
            };
            case b001 -> switch(q) {
                case b0 -> Pop.pop_stk16(Stk16.lookup(TwoBitValue.from(y, 1)));
                case b1 -> switch(TwoBitValue.from(y, 1)) {
                    case b00 -> Return.ret();
                    case b01 -> ReturnFromInterruptHandler.reti();
                    case b10 -> Jump.jp_HL();
                    case b11 -> Load.load_SP_HL();
                };
            };
            case b010 -> switch (y) {
                case b000, b001, b010, b011 -> Jump.jp_cc_nn(Condition.lookup(TwoBitValue.from(y, 0)));
                case b100 -> Load.ld_indirectC_A();
                case b101 -> Load.ld_imm16indirect_A();
                case b110 -> Load.ld_A_indirectC();
                case b111 -> Load.ld_A_imm16indirect();
            };
            case b011 -> switch (y) {
                case b000 -> Jump.jp_nn();
                case b001 -> Prefix.prefix();
                case b010 -> Illegal.illegal(y, z);
                case b011 -> Illegal.illegal(y, z);
                case b100 -> Illegal.illegal(y, z);
                case b101 -> Illegal.illegal(y, z);
                case b110 -> DisableInterrupts.di();
                case b111 -> EnableInterrupts.ei();
            };
            case b100 -> switch(y) {
                case b000, b001, b010, b011 -> Call.call_cc(Condition.lookup(TwoBitValue.from(y, 0)));
                case b100 -> Illegal.illegal(y, z);
                case b101 -> Illegal.illegal(y, z);
                case b110 -> Illegal.illegal(y, z);
                case b111 -> Illegal.illegal(y, z);
            };
            case b101 -> switch(q) {
                case b0 -> Push.push_stk16(Stk16.lookup(TwoBitValue.from(y, 1)));
                case b1 -> switch(TwoBitValue.from(y, 1)) {
                    case b00 -> Call.call();
                    case b01 -> Illegal.illegal(y, z);
                    case b10 -> Illegal.illegal(y, z);
                    case b11 -> Illegal.illegal(y, z);
                };
            };
            case b110 -> switch(y) {
                case b000 -> Add.add_a_imm8();
                case b001 -> AddWithCarry.adc_a_imm8();
                case b010 -> Sub.sub_a_imm8();
                case b011 -> SubWithCarry.sbc_a_imm8();
                case b100 -> And.and_imm8();
                case b101 -> Xor.xor_imm8();
                case b110 -> Or.or_imm8();
                case b111 -> Compare.cp_imm8();
            };
            case b111 -> switch(y) {
                case b000 -> Restart.rst_00H();
                case b001 -> Restart.rst_08H();
                case b010 -> Restart.rst_10H();
                case b011 -> Restart.rst_18H();
                case b100 -> Restart.rst_20H();
                case b101 -> Restart.rst_28H();
                case b110 -> Restart.rst_30H();
                case b111 -> Restart.rst_38H();
            };
        };
    }
}
