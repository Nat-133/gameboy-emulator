package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;

import java.util.Hashtable;

import static org.gameboy.utils.BitUtilities.lower_nibble;
import static org.gameboy.utils.BitUtilities.uint;

public class DecimalAdjustAcumulator implements Instruction{
    private DecimalAdjustAcumulator() {
    }

    public static DecimalAdjustAcumulator daa() {
        return new DecimalAdjustAcumulator();
    }


    @Override
    public void execute(CpuStructure cpuStructure) {
        boolean full_carry = cpuStructure.registers().getFlag(Flag.C);
        boolean half_carry = cpuStructure.registers().getFlag(Flag.H);
        boolean subtraction = cpuStructure.registers().getFlag(Flag.N);
        byte a = cpuStructure.registers().A();
        int a_int = uint(a);

        int correction = 0;
        boolean set_carry = false;

        // lower nibble correction
        if (half_carry || (!subtraction && lower_nibble(a) > 0x9)) {
            correction |= 0x06;
        }

        // Upper nibble correction
        if (full_carry || (!subtraction && a_int > 0x99)) {
            correction |= 0x60;
            set_carry = true;
        }

        ArithmeticResult result;
        if (subtraction) {
            result = cpuStructure.alu().sub(a, (byte) correction);
        } else {
            result = cpuStructure.alu().add(a, (byte) correction);
        }

        Hashtable<Flag, Boolean> flagChanges = new FlagChangesetBuilder()
                .with(Flag.Z, result.result() == 0)
                .with(Flag.H, false)
                .with(Flag.C, set_carry)
                .build();

        cpuStructure.registers().setFlags(flagChanges);
        cpuStructure.registers().setA(result.result());
    }

    @Override
    public String representation() {
        return "DAA";
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
