package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;

public class Prefix implements Instruction{
    private Prefix() {}

    public static Prefix prefix() {
        return new Prefix();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        cpuStructure.decoder().switchTables();
    }

    @Override
    public String representation() {
        return "PREFIX";
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
