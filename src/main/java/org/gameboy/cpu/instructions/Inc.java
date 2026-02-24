package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Target;
import static org.gameboy.cpu.instructions.targets.Target.*;

public class Inc implements Instruction{
    private final Target target;

    private Inc(Target target) {
        this.target = target;
    }

    public static Instruction inc_r8(R8 target) {
        return new Inc((Target) target);
    }

    public static Instruction inc_r16(R16 target) {
        return new Inc((Target) target);
    }

    @Override
    public String representation() {
        return "INC " + target.representation();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor operationTargetAccessor = OperationTargetAccessor.from(cpuStructure);
        short value = operationTargetAccessor.getValue(target);

        short newValue;

        if (target instanceof ByteTarget) {
            ArithmeticResult result = cpuStructure.alu().inc((byte) value);

            newValue = result.result();

            cpuStructure.registers().setFlags(result.flagChanges());
        } else {
            newValue = (short) (value + 1);
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
