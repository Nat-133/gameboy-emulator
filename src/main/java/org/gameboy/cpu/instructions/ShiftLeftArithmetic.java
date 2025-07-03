package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;

public class ShiftLeftArithmetic implements Instruction{
    private final ByteRegister target;

    private ShiftLeftArithmetic(ByteRegister target) {
        this.target = target;
    }

    public static ShiftLeftArithmetic sla_r8(ByteRegister target) {
        return new ShiftLeftArithmetic(target);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte value = (byte) accessor.getValue(target.convert());
        ArithmeticResult result = cpuStructure.alu().arithmetic_shift_left(value);

        accessor.setValue(target.convert(), result.result());
        cpuStructure.registers().setFlags(result.flagChanges());
    }

    @Override
    public String representation() {
        return "SLA " + this.target.convert().representation();
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
