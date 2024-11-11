package org.gameboy.instructions;

import org.gameboy.ArithmeticResult;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;
import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.OperationTarget;

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
}
