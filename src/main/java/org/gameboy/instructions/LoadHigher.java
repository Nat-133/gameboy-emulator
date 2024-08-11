package org.gameboy.instructions;

import org.gameboy.OperationTargetAccessor;
import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.OperationTarget;

import static org.gameboy.instructions.targets.OperationTarget.A;
import static org.gameboy.instructions.targets.OperationTarget.IMM_8;

public class LoadHigher implements Instruction{
    private final GenericOperationTarget destination;
    private final GenericOperationTarget source;

    private LoadHigher(GenericOperationTarget destination, GenericOperationTarget source) {
        this.destination = destination;
        this.source = source;
    }

    public static Instruction ldh_imm8_A() {
        return new LoadHigher(IMM_8.indirect(), A.direct());
    }

    public static Instruction ldh_A_imm8() {
        return new LoadHigher(A.direct(), IMM_8.indirect());
    }

    @Override
    public String representation() {
        return "LDH " + destination.representation() + "," + source.representation();
    }

    @Override
    public void execute(OperationTargetAccessor operationTargetAccessor) {
        operationTargetAccessor.setValue(destination, operationTargetAccessor.getValue(source));
    }

    @Override
    public String toString() {
        return representation();
    }
}
