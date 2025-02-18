package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.gameboy.cpu.instructions.targets.GenericOperationTarget;
import org.gameboy.cpu.instructions.targets.OperationTarget;

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
