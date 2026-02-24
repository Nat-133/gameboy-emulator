package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Target;

public class Compare implements Instruction{
    private final Target target;

    private Compare(Target target) {
        this.target = target;
    }

    public static Compare cp_r8(Target.R8 register) {
        return new Compare(register);
    }

    public static Compare cp_imm8() {
        return new Compare(Target.imm_8);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte a = cpuStructure.registers().A();
        byte b = (byte) accessor.getValue(this.target);

        ArithmeticResult res = cpuStructure.alu().sub(a, b);
        cpuStructure.registers().setFlags(res.flagChanges());
    }

    @Override
    public String representation() {
        return "CP A," + target.representation();
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
