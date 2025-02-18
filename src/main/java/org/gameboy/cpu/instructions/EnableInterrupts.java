package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;

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
