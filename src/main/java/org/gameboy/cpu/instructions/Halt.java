package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;

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
