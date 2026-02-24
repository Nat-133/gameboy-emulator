package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Target;

public class Dec implements Instruction{
    private final Target target;

    private Dec(Target target) {
        this.target = target;
    }

    public static Instruction dec_r8(Target.R8 target) {
        return new Dec(target);
    }

    public static Instruction dec_r16(Target.R16 target) {
        return new Dec(target);
    }

    @Override
    public String representation() {
        return "DEC " + target.representation();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor operationTargetAccessor = OperationTargetAccessor.from(cpuStructure);
        short value = operationTargetAccessor.getValue(target);

        short newValue;

        if (target instanceof Target.ByteTarget) {
            ArithmeticResult result = cpuStructure.alu().dec((byte) value);

            newValue = result.result();

            cpuStructure.registers().setFlags(result.flagChanges());
        } else {
            newValue = (short) (value - 1);
        }

        operationTargetAccessor.setValue(target, newValue);
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
