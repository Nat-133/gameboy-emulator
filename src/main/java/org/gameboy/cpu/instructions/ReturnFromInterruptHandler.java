package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;

public class ReturnFromInterruptHandler implements Instruction {
    private ReturnFromInterruptHandler() {
    }

    public static ReturnFromInterruptHandler reti() {
        return new ReturnFromInterruptHandler();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        short value = ControlFlow.popFromStack(cpuStructure);
        cpuStructure.registers().setPC(value);
        cpuStructure.registers().setIME(true);
        cpuStructure.clock().tick();
    }

    @Override
    public String representation() {
        return "RETI";
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
