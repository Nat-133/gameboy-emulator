package org.gameboy.instructions;

import org.gameboy.ArithmeticUnit;
import org.gameboy.ArithmeticUnit.ArithmeticResult;
import org.gameboy.OperationTargetAccessor;
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
    public void execute(OperationTargetAccessor operationTargetAccessor) {
        short value = operationTargetAccessor.getValue(target);

        short newValue;

        if (target.isByteTarget()) {
            ArithmeticResult result = ArithmeticUnit.dec((byte) value);

            newValue = result.result();

            result.flagChanges().forEach(
                    flagValue -> operationTargetAccessor.cpuRegisters.setFlags(flagValue.value(), flagValue.flag())
            );
        } else {
            newValue = (short) (value - 1);
        }

        operationTargetAccessor.setValue(target, newValue);
    }
}
