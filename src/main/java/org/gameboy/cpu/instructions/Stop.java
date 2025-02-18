package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;

public class Stop implements Instruction {
    private Stop() {}

    public static Stop stop() {
        return new Stop();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        ControlFlow.incrementPC(cpuStructure);

        cpuStructure.clock().tick();
        cpuStructure.clock().tick();
        cpuStructure.clock().tick();
    }

    @Override
    public String representation() {
        return "STOP";
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
