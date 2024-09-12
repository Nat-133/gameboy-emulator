package org.gameboy.instructions;

import org.gameboy.components.ArithmeticUnit;
import org.gameboy.components.ArithmeticUnit.ArithmeticResult;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;
import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.OperationTarget;

public class Add implements Instruction {
    private final GenericOperationTarget left;
    private final GenericOperationTarget right;

    private Add(GenericOperationTarget left, GenericOperationTarget right) {
        this.left = left;
        this.right = right;
    }

    public static Add AddA_register(ByteRegister right) {
        return new Add(OperationTarget.A.direct(), right.convert());
    }

    @Override
    public void execute(OperationTargetAccessor operationTargetAccessor) {
        if (!this.left.isByteTarget() && !this.right.isByteTarget()) {
            // 16-bit addition
        }
        else if (!this.left.isByteTarget()) { // && this.right.isByteTarget()
            // 16-bit + 8-bit signed
        }
        else {
            executeEightBitAddition(operationTargetAccessor);
        }
    }

    private void executeEightBitAddition(OperationTargetAccessor operationTargetAccessor) {
        byte leftValue = (byte) operationTargetAccessor.getValue(this.left);
        byte rightValue = (byte) operationTargetAccessor.getValue(this.right);

        ArithmeticResult result = ArithmeticUnit.add(leftValue, rightValue);
        operationTargetAccessor.setValue(this.left, result.result());

        result.flagChanges().forEach(
                (flag, change) -> operationTargetAccessor.cpuRegisters.setFlags(change, flag)
        );
    }

    private void executeSignedAddition(OperationTargetAccessor operationTargetAccessor) {
        short leftValue = operationTargetAccessor.getValue(this.left);
        byte rightValue = (byte) operationTargetAccessor.getValue(this.right);
    }

    private void executeSixteenBitAddition(OperationTargetAccessor operationTargetAccessor) {
        short leftValue = operationTargetAccessor.getValue(this.left);
        short rightValue = operationTargetAccessor.getValue(this.right);

    }

    @Override
    public String representation() {
        return "ADD " + this.left.representation() + "," + this.right.representation();
    }
}
