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
        boolean IME = cpuStructure.registers().IME();
        boolean interruptsPending = cpuStructure.interruptBus().hasInterrupts();

        if (!IME && interruptsPending) {
            cpuStructure.idu().disableNextIncrement();
        }

        cpuStructure.clock().stop();
        cpuStructure.interruptBus().waitForInterrupt();
        cpuStructure.clock().start();
    }

    @Override
    public String representation() {
        return "HALT";
    }

    @Override
    public String toString() {
        return representation();
    }
}
