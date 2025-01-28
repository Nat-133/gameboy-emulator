package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;

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
}
