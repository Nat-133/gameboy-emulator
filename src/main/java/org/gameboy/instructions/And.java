package org.gameboy.instructions;

import org.gameboy.components.ArithmeticUnit.ArithmeticResult;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;
import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.OperationTarget;

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
        res.flagChanges().forEach((flag, value) -> cpuStructure.registers().setFlags(value, flag));
    }

    @Override
    public String toString() {
        return this.representation();
    }
}
