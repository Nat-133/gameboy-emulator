package org.gameboy.instructions;

import org.gameboy.components.CpuRegisters;
import org.gameboy.components.CpuStructure;
import org.gameboy.components.IncrementDecrementUnit;
import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.OperationTarget;

import static org.gameboy.instructions.targets.OperationTarget.SP;

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
}
