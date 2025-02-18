package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.components.CpuStructure;

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
