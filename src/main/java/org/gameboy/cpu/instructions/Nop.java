package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;

public class Nop implements Instruction {
    private static final Nop NOP = new Nop();

    private Nop() {}

    public static Nop nop() {
        return NOP;
    }

    public String representation() {
        return "NOP";
    }

    @Override
    public void execute(CpuStructure cpuStructure) {

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
