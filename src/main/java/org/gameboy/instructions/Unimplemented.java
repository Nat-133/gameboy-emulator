package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;

public class Unimplemented implements Instruction{
    public static final Unimplemented UNIMPLEMENTED = new Unimplemented();

    @Override
    public String representation() {
        return "UNIMPLEMENTED";
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        throw new UnsupportedOperationException("Unimplemented instruction");
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
