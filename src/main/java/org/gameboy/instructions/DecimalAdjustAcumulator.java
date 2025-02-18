package org.gameboy.instructions;

import org.gameboy.ArithmeticResult;
import org.gameboy.Flag;
import org.gameboy.FlagChangesetBuilder;
import org.gameboy.components.CpuStructure;

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

        int a_adjustment_lower = (!subtraction && lower_nibble(a) > 0x9) || half_carry ? 0x06 : 0x00;
        int a_adjustment_upper = (!subtraction && uint(a) > 0x99)        || full_carry ? 0x60 : 0x00;

        byte a_adjustment = (byte) (a_adjustment_upper | a_adjustment_lower);

        ArithmeticResult result = subtraction
                ? cpuStructure.alu().sub(a, a_adjustment)
                : cpuStructure.alu().add(a, a_adjustment);
        boolean new_carry = result.flagChanges().get(Flag.C);
        Hashtable<Flag, Boolean> flagChanges = new FlagChangesetBuilder(result.flagChanges())
                .with(Flag.H, false)
                .with(Flag.C, full_carry || new_carry)
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
