package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.ControlFlow;

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
}
