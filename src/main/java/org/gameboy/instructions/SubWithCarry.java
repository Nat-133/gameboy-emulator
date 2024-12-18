package org.gameboy.instructions;

import org.gameboy.Flag;
import org.gameboy.ArithmeticResult;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;
import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.OperationTarget;

public class SubWithCarry implements Instruction{
    private final GenericOperationTarget right;

    private SubWithCarry(GenericOperationTarget right) {
        this.right = right;
    }

    public static SubWithCarry sbc_a_r8(ByteRegister r8) {
        return new SubWithCarry(r8.convert());
    }

    public static SubWithCarry sbc_a_imm8() {
        return new SubWithCarry(OperationTarget.IMM_8.direct());
    }

    @Override
    public String representation() {
        return "SBC " + OperationTarget.A.direct().representation() + "," + this.right.representation();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte a = cpuStructure.registers().A();
        byte b = (byte) accessor.getValue(this.right);

        ArithmeticResult res = cpuStructure.alu().sub_carry(a, b, cpuStructure.registers().getFlag(Flag.C));
        cpuStructure.registers().setA(res.result());
        cpuStructure.registers().setFlags(res.flagChanges());
    }

    @Override
    public String toString() {
        return this.representation();
    }
}
