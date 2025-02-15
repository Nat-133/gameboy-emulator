package org.gameboy.instructions;

import org.gameboy.ArithmeticResult;
import org.gameboy.components.CpuStructure;

public class Compliment implements Instruction {

    private Compliment() {}

    public static Compliment cpl() {
        return new Compliment();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        ArithmeticResult res = cpuStructure.alu().compliment(cpuStructure.registers().A());
        cpuStructure.registers().setFlags(res.flagChanges());
        cpuStructure.registers().setA(res.result());
    }

    @Override
    public String representation() {
        return "CPL";
    }

    @Override
    public String toString() {
        return representation();
    }
}
