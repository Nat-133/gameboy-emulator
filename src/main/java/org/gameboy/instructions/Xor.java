package org.gameboy.instructions;

import org.gameboy.ArithmeticResult;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;
import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.OperationTarget;

public class Xor implements Instruction{
    private final GenericOperationTarget target;

    private Xor(GenericOperationTarget target) {
        this.target = target;
    }

    public static Xor xor_r8(ByteRegister register) {
        return new Xor(register.convert());
    }

    public static Xor xor_imm8() {
        return new Xor(OperationTarget.IMM_8.direct());
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte a = cpuStructure.registers().A();
        byte b = (byte) accessor.getValue(target);

        ArithmeticResult res = cpuStructure.alu().xor(a, b);

        cpuStructure.registers().setA(res.result());
        cpuStructure.registers().setFlags(res.flagChanges());
    }

    @Override
    public String representation() {
        return "XOR A," + target.representation();
    }

    @Override
    public String toString() {
        return representation();
    }
}
