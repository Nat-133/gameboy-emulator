package org.gameboy.instructions;

import org.gameboy.ArithmeticResult;
import org.gameboy.Flag;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;

public class RotateLeft implements Instruction{
    private final ByteRegister target;
    private final boolean isPrefixInstruction;

    private RotateLeft(ByteRegister target, boolean isPrefixInstruction) {
        this.target = target;
        this.isPrefixInstruction = isPrefixInstruction;
    }

    public static RotateLeft rla() {
        return new RotateLeft(ByteRegister.A, false);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte value = (byte) accessor.getValue(target.convert());
        boolean carryIn = cpuStructure.registers().getFlag(Flag.C);
        ArithmeticResult result = cpuStructure.alu().rotate_left(value, carryIn);

        accessor.setValue(target.convert(), result.result());
        cpuStructure.registers().setFlags(result.flagChanges());
    }

    @Override
    public String representation() {
        return "RL" + (isPrefixInstruction ? " " : "") + this.target.convert().representation();
    }
}
