package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;

public class Halt implements Instruction {
    private static final Halt HALT = new Halt();

    public static Halt HALT() {
        return HALT;
    }

    private Halt() {}

    @Override
    public void execute(CpuStructure cpuStructure) {

    }

    @Override
    public String representation() {
        return "HALT";
    }
}
