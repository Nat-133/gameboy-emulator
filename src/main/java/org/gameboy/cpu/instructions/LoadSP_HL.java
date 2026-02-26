package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuRegisters;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.components.IncrementDecrementUnit;
import org.gameboy.cpu.instructions.targets.Target;
import static org.gameboy.cpu.instructions.targets.Target.*;

public class LoadSP_HL implements Load {
    public static LoadSP_HL load_SP_HL() {
        return new LoadSP_HL();
    }

    @Override
    public Target source() {
        return hl;
    }

    @Override
    public Target destination() {
        return sp;
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        CpuRegisters registers = cpuStructure.registers();
        IncrementDecrementUnit idu = cpuStructure.idu();

        registers.setSP(idu.passthrough(registers.HL()));

        cpuStructure.clock().tick();
    }

    @Override
    public String toString() {
        return representation();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Instruction other) {
            return this.representation().equals(other.representation());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.representation().hashCode();
    }
}
