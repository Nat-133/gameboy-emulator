package org.gameboy.instructions;

import org.gameboy.components.ArithmeticUnit.ArithmeticResult;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;
import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.OperationTarget;

public class Sub implements Instruction{
    private final GenericOperationTarget right;

    private Sub(GenericOperationTarget right) {
        this.right = right;
    }

    public static Sub sub_a_r8(ByteRegister r8) {
        return new Sub(r8.convert());
    }

    public static Sub sub_a_imm8() {
        return new Sub(OperationTarget.IMM_8.direct());
    }

    @Override
    public String representation() {
        return "SUB " + OperationTarget.A.direct().representation() + "," + this.right.representation();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte a = cpuStructure.registers().A();
        byte b = (byte) accessor.getValue(this.right);

        ArithmeticResult res = cpuStructure.alu().sub(a, b);
        cpuStructure.registers().setA(res.result());
        res.flagChanges().forEach((flag, value) -> cpuStructure.registers().setFlags(value, flag));
    }
}
