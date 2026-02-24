package org.gameboy.cpu.instructions;

import org.gameboy.cpu.instructions.targets.Target;
import static org.gameboy.cpu.instructions.targets.Target.*;

public interface Load extends Instruction {
    static Load ld_r8_r8(R8 destination, R8 source) {
        return BasicLoad.ld_r8_r8(destination, source);
    }

    static Load ld_r8_imm8(R8 destination) {
        return BasicLoad.ld_r8_imm8(destination);
    }

    static Load ld_imm16indirect_sp() {
        return LoadMem_SP.ld_imm16indirect_sp();
    }

    static Load ld_r16_imm16(R16 register) {
        return BasicLoad.ld_r16_imm16(register);
    }

    static Load ld_A_mem16indirect(Mem16 indirectSource) {
        return BasicLoad.ld_A_mem16indirect(indirectSource);
    }

    static Load ld_mem16indirect_A(Mem16 indirectDestination) {
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

    Target source();

    Target destination();
}
