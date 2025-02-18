package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.gameboy.cpu.instructions.targets.GenericOperationTarget;
import org.gameboy.cpu.instructions.targets.WordGeneralRegister;

public class Dec implements Instruction{
    private final GenericOperationTarget target;

    private Dec(GenericOperationTarget target) {
        this.target = target;
    }

    public static Instruction dec_r8(ByteRegister target) {
        return new Dec(target.convert());
    }

    public static Instruction dec_r16(WordGeneralRegister target) {
        return new Dec(target.convert());
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

        if (target.isByteTarget()) {
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
