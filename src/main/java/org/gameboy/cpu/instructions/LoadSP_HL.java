package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuRegisters;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.components.IncrementDecrementUnit;
import org.gameboy.cpu.instructions.targets.GenericOperationTarget;
import org.gameboy.cpu.instructions.targets.OperationTarget;

import static org.gameboy.cpu.instructions.targets.OperationTarget.SP;

public class LoadSP_HL implements Load {
    public static LoadSP_HL load_SP_HL() {
        return new LoadSP_HL();
    }

    @Override
    public GenericOperationTarget source() {
        return OperationTarget.HL.direct();
    }

    @Override
    public GenericOperationTarget destination() {
        return SP.direct();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        CpuRegisters registers = cpuStructure.registers();
        IncrementDecrementUnit idu = cpuStructure.idu();

        registers.setHL(idu.passthrough(registers.SP()));

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
