package org.gameboy.instructions;

import org.gameboy.OperationTargetAccessor;

public class Unimplemented implements Instruction{
    public static final Unimplemented UNIMPLEMENTED = new Unimplemented();

    @Override
    public String representation() {
        return "UNIMPLEMENTED";
    }

    @Override
    public void execute(OperationTargetAccessor operationTargetAccessor) {
        throw new UnsupportedOperationException("Unimplemented instruction");
    }

    @Override
    public String toString() {
        return representation();
    }
}
