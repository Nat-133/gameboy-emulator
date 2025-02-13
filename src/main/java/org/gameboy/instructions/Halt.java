package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;

public class Halt implements Instruction {
    private static final Halt HALT = new Halt();

    public static Halt halt() {
        return HALT;
    }

    private Halt() {}

    @Override
    public void execute(CpuStructure cpuStructure) {
        cpuStructure.idu().disableNextIncrement();

        cpuStructure.clock().stop();
        cpuStructure.interruptBus().waitForInterrupt();
        cpuStructure.clock().start();
    }

    @Override
    public String representation() {
        return "HALT";
    }
}
