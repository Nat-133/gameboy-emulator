package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.gameboy.cpu.instructions.targets.GenericOperationTarget;
import org.gameboy.cpu.instructions.targets.OperationTarget;

public class And implements Instruction{
    private final GenericOperationTarget target;

    private And(GenericOperationTarget target) {
        this.target = target;
    }

    public static And and_r8(ByteRegister r8) {
        return new And(r8.convert());
    }

    public static And and_imm8() {
        return new And(OperationTarget.IMM_8.direct());
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
