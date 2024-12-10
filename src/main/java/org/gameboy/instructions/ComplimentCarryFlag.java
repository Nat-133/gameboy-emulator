package org.gameboy.instructions;

import org.gameboy.ArithmeticResult;
import org.gameboy.Flag;
import org.gameboy.components.CpuStructure;

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
}
