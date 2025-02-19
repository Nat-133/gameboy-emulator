package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;
import org.gameboy.cpu.instructions.targets.GenericOperationTarget;

import static org.gameboy.cpu.instructions.targets.OperationTarget.IMM_16;
import static org.gameboy.cpu.instructions.targets.OperationTarget.SP;

public class LoadMem_SP implements Load {

    public static LoadMem_SP ld_imm16indirect_sp() {
        return new LoadMem_SP();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        short address = ControlFlow.readImm16(cpuStructure);

        short sp = cpuStructure.registers().SP();

        ControlFlow.writeWordToMem(address, sp, cpuStructure);
    }

    @Override
    public GenericOperationTarget source() {
        return SP.direct();
    }

    @Override
    public GenericOperationTarget destination() {
        return IMM_16.indirect();
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
