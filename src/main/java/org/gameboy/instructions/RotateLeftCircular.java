package org.gameboy.instructions;

import org.gameboy.ArithmeticResult;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;

public class RotateLeftCircular implements Instruction{
    private final ByteRegister target;
    private final boolean isPrefixInstruction;

    private RotateLeftCircular(ByteRegister target, boolean isPrefixInstruction) {
        this.target = target;
        this.isPrefixInstruction = isPrefixInstruction;
    }

    public static RotateLeftCircular rlca() {
        return new RotateLeftCircular(ByteRegister.A, false);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte value = (byte) accessor.getValue(target.convert());
        ArithmeticResult result = cpuStructure.alu().rotate_left_circular(value);

        accessor.setValue(target.convert(), result.result());
        cpuStructure.registers().setFlags(result.flagChanges());
    }

    @Override
    public String representation() {
        return "RLC" + (isPrefixInstruction ? " " : "") + this.target.convert().representation();
    }
}
