package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;

public class Prefix implements Instruction{
    private Prefix() {}

    public static Prefix prefix() {
        return new Prefix();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        cpuStructure.decoder().switchTables();

        byte ir = cpuStructure.memory().read(cpuStructure.registers().PC());
        cpuStructure.registers().setInstructionRegister(ir);
        ControlFlow.incrementPC(cpuStructure);

        cpuStructure.clock().tick();
    }

    @Override
    public boolean handlesFetch() {
        return true;
    }

    @Override
    public String representation() {
        return "PREFIX";
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
