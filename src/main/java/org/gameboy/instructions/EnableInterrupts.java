package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;

public class EnableInterrupts implements Instruction{
    private EnableInterrupts() {}

    public static EnableInterrupts ei() {
        return new EnableInterrupts();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
    }

    @Override
    public void postFetch(CpuStructure cpuStructure) {
        cpuStructure.registers().setIME(true);
    }

    @Override
    public String representation() {
        return "EI";
    }

    @Override
    public String toString() {
        return representation();
    }
}
