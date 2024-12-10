package org.gameboy.instructions;

import org.gameboy.ArithmeticResult;
import org.gameboy.components.CpuStructure;

public class SetCarryFlag implements Instruction{
    private SetCarryFlag() {}

    public static SetCarryFlag scf() {
        return new SetCarryFlag();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        ArithmeticResult result = cpuStructure.alu().set_carry_flag();

        cpuStructure.registers().setFlags(result.flagChanges());
    }

    @Override
    public String representation() {
        return "SCF";
    }

    @Override
    public String toString() {
        return representation();
    }
}
