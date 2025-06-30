package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;

public class RotateRightCircular implements Instruction{
    private final ByteRegister target;
    private final boolean isPrefixInstruction;

    private RotateRightCircular(ByteRegister target, boolean isPrefixInstruction) {
        this.target = target;
        this.isPrefixInstruction = isPrefixInstruction;
    }

    public static RotateRightCircular rrca() {
        return new RotateRightCircular(ByteRegister.A, false);
    }

    public static RotateRightCircular rrc_r8(ByteRegister target) {
        return new RotateRightCircular(target, true);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte value = (byte) accessor.getValue(target.convert());
        ArithmeticResult result = cpuStructure.alu().rotate_right_circular(value);

        accessor.setValue(target.convert(), result.result());
        cpuStructure.registers().setFlags(result.flagChanges());
    }

    @Override
    public String representation() {
        return "RRC" + (isPrefixInstruction ? " " : "") + this.target.convert().representation();
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
