package org.gameboy.instructions;

import org.gameboy.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;
import org.gameboy.instructions.targets.GenericOperationTarget;

public class Add implements Instruction {
    private final GenericOperationTarget left;
    private final GenericOperationTarget right;

    private Add(GenericOperationTarget left, GenericOperationTarget right) {
        this.left = left;
        this.right = right;
    }

    public static Add AddA_register(ByteRegister right) {
        return new Add(GenericOperationTarget.A, right.convert());
    }

    @Override
    public void execute(OperationTargetAccessor operationTargetAccessor) {

    }

    @Override
    public String representation() {
        return "ADD " + this.left.representation() + "," + this.right.representation();
    }
}
