package org.gameboy.instructions;

import org.gameboy.components.ArithmeticUnit;
import org.gameboy.components.ArithmeticUnit.ArithmeticResult;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;
import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.WordGeneralRegister;

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

            result.flagChanges().forEach(
                    (flag, change) -> operationTargetAccessor.cpuRegisters.setFlags(change, flag)
            );
        } else {
            newValue = (short) (value - 1);
        }

        operationTargetAccessor.setValue(target, newValue);
    }
}
