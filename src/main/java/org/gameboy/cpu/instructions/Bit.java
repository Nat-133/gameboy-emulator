package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.gameboy.utils.MultiBitValue.ThreeBitValue;

public class Bit implements Instruction{
    private final ThreeBitValue bitIndex;
    private final ByteRegister target;

    private Bit(ThreeBitValue bitIndex, ByteRegister target) {
        this.bitIndex = bitIndex;
        this.target = target;
    }

    public static Bit bit_b_r8(ThreeBitValue bitIndex, ByteRegister target) {
        return new Bit(bitIndex, target);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte value = (byte) accessor.getValue(target.convert());
        ArithmeticResult result = cpuStructure.alu().bit_test(bitIndex.value(), value);

        FlagChangesetBuilder flagBuilder = new FlagChangesetBuilder(result.flagChanges());

        cpuStructure.registers().setFlags(flagBuilder.build());
    }

    @Override
    public String representation() {
        return "BIT " + bitIndex.value() + "," + this.target.convert().representation();
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