package org.gameboy.instructions;

import org.gameboy.instructions.targets.ByteRegister;
import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.WordGeneralRegister;
import org.gameboy.instructions.targets.WordMemoryRegister;

public interface Load extends Instruction {
    static Load ld_r8_r8(ByteRegister destination, ByteRegister source) {
        return BasicLoad.ld_r8_r8(destination, source);
    }

    static Load ld_r8_imm8(ByteRegister destination) {
        return BasicLoad.ld_r8_imm8(destination);
    }

    static Load ld_imm16indirect_sp() {
        return LoadMem_SP.ld_imm16indirect_sp();
    }

    static Load ld_r16_imm16(WordGeneralRegister register) {
        return BasicLoad.ld_r16_imm16(register);
    }

    static Load ld_A_mem16indirect(WordMemoryRegister indirectSource) {
        return BasicLoad.ld_A_mem16indirect(indirectSource);
    }

    static Load ld_mem16indirect_A(WordMemoryRegister indirectDestination) {
        return BasicLoad.ld_mem16indirect_A(indirectDestination);
    }

    static Load ld_indirectC_A() {
        return BasicLoad.ld_indirectC_A();
    }

    static Load ld_A_indirectC() {
        return BasicLoad.ld_A_indirectC();
    }

    static Load ld_HL_SP_OFFSET() {
        return BasicLoad.ld_HL_SP_OFFSET();
    }

    static Load ld_imm16indirect_A() {
        return BasicLoad.ld_imm16indirect_A();
    }

    static Load ld_A_imm16indirect() {
        return BasicLoad.ld_A_imm16indirect();
    }

    static Load load_SP_HL() {
        return LoadSP_HL.load_SP_HL();
    }

    @Override
    default String representation() {
        return "LD " + this.destination().representation() + "," + this.source().representation();
    }

    GenericOperationTarget source();

    GenericOperationTarget destination();
}
