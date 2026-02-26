package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Target;
import static org.gameboy.cpu.instructions.targets.Target.*;

public class And implements Instruction{
    private final Target target;

    private And(Target target) {
        this.target = target;
    }

    public static And and_r8(R8 r8) {
        return new And(r8);
    }

    public static And and_imm8() {
        return new And(imm_8);
    }

    @Override
    public String representation() {
        return "AND A," + target.representation();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte a = cpuStructure.registers().A();
        byte b = (byte) accessor.getValue(this.target);

        ArithmeticResult res = cpuStructure.alu().and(a, b);
        cpuStructure.registers().setA(res.result());
        cpuStructure.registers().setFlags(res.flagChanges());
    }

    @Override
    public String toString() {
        return this.representation();
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
