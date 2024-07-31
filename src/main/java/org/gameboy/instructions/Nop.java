package org.gameboy.instructions;

import org.gameboy.OperationTargetAccessor;

public class Nop implements Instruction {
    private static final Nop NOP = new Nop();

    private Nop() {}

    public static Nop NOP() {
        return NOP;
    }

    public String representation() {
        return "NOP";
    }

    @Override
    public void execute(OperationTargetAccessor operationTargetAccessor) {

    }
}
