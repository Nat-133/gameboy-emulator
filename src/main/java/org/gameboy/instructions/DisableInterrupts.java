package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.ControlFlow;

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
}
