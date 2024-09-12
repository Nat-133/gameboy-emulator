package org.gameboy.instructions;

import org.gameboy.instructions.common.OperationTargetAccessor;

public class Halt implements Instruction {
    private static final Halt HALT = new Halt();

    public static Halt HALT() {
        return HALT;
    }

    private Halt() {}

    @Override
    public void execute(OperationTargetAccessor operationTargetAccessor) {

    }

    @Override
    public String representation() {
        return "HALT";
    }
}
