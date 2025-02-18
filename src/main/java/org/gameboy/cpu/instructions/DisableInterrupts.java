package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;

public class DisableInterrupts implements Instruction{
    private DisableInterrupts() {}

    public static DisableInterrupts di() {
        return new DisableInterrupts();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        byte ir = cpuStructure.memory().read(cpuStructure.registers().PC());
        cpuStructure.registers().setInstructionRegister(ir);
        ControlFlow.incrementPC(cpuStructure);
        cpuStructure.registers().setIME(false);

        cpuStructure.clock().tick();
    }

    @Override
    public String representation() {
        return "DI";
    }

    @Override
    public boolean handlesFetch() {
        return true;
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
