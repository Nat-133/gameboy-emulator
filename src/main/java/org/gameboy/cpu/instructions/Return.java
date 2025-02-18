package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;

public class Return implements Instruction {
    private Return() {
    }

    public static Return ret() {
        return new Return();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        short value = ControlFlow.popFromStack(cpuStructure);
        cpuStructure.registers().setPC(value);
        cpuStructure.clock().tick();
    }

    @Override
    public String representation() {
        return "RET";
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
