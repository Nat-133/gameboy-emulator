package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;
import org.gameboy.utils.MultiBitValue.ThreeBitValue;

public class Illegal implements Instruction{
    private final int value;

    private Illegal(ThreeBitValue y, ThreeBitValue z) {
        value = (3 << 6) + (y.value() << 3) + (z.value());
    }

    public static Illegal illegal(ThreeBitValue y, ThreeBitValue z) {
        return new Illegal(y, z);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
    }

    @Override
    public String representation() {
        return "ILLEGAL_%X".formatted(value);
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
