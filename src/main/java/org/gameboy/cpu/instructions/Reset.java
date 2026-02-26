package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import static org.gameboy.cpu.instructions.targets.Target.*;
import org.gameboy.utils.MultiBitValue.ThreeBitValue;

public class Reset implements Instruction{
    private final ThreeBitValue bitIndex;
    private final R8 target;

    private Reset(ThreeBitValue bitIndex, R8 target) {
        this.bitIndex = bitIndex;
        this.target = target;
    }

    public static Reset res_b_r8(ThreeBitValue bitIndex, R8 target) {
        return new Reset(bitIndex, target);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte value = (byte) accessor.getValue(target);
        ArithmeticResult result = cpuStructure.alu().set_bit(false, bitIndex.value(), value);

        accessor.setValue(target, result.result());
    }

    @Override
    public String representation() {
        return "RES " + bitIndex.value() + "," + this.target.representation();
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