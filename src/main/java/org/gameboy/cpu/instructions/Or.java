package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.gameboy.cpu.instructions.targets.GenericOperationTarget;
import org.gameboy.cpu.instructions.targets.OperationTarget;

public class Or implements Instruction{
    private final GenericOperationTarget target;

    private Or(GenericOperationTarget target) {
        this.target = target;
    }

    public static Or or_r8(ByteRegister register) {
        return new Or(register.convert());
    }

    public static Or or_imm8() {
        return new Or(OperationTarget.IMM_8.direct());
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte a = cpuStructure.registers().A();
        byte b = (byte) accessor.getValue(target);

        ArithmeticResult res = cpuStructure.alu().or(a, b);

        cpuStructure.registers().setA(res.result());
        cpuStructure.registers().setFlags(res.flagChanges());
    }

    @Override
    public String representation() {
        return "OR A," + target.representation();
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
