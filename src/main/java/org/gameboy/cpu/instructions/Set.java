package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.gameboy.utils.MultiBitValue.ThreeBitValue;

public class Set implements Instruction{
    private final ThreeBitValue bitIndex;
    private final ByteRegister target;

    private Set(ThreeBitValue bitIndex, ByteRegister target) {
        this.bitIndex = bitIndex;
        this.target = target;
    }

    public static Set set_b_r8(ThreeBitValue bitIndex, ByteRegister target) {
        return new Set(bitIndex, target);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte value = (byte) accessor.getValue(target.convert());
        ArithmeticResult result = cpuStructure.alu().set_bit(true, bitIndex.value(), value);

        accessor.setValue(target.convert(), result.result());
    }

    @Override
    public String representation() {
        return "SET " + bitIndex.value() + "," + this.target.convert().representation();
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