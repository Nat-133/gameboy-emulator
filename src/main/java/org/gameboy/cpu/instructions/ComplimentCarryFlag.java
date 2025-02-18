package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.components.CpuStructure;

public class ComplimentCarryFlag implements Instruction{
    private ComplimentCarryFlag() {}

    public static ComplimentCarryFlag ccf() {
        return new ComplimentCarryFlag();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        boolean carry_flag = cpuStructure.registers().getFlag(Flag.C);
        ArithmeticResult result = cpuStructure.alu().compliment_carry_flag(carry_flag);

        cpuStructure.registers().setFlags(result.flagChanges());
    }

    @Override
    public String representation() {
        return "CCF";
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
