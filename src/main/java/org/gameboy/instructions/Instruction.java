package org.gameboy.instructions;

import org.gameboy.OperationTargetAccessor;

public interface Instruction {
    String representation();

    void execute(OperationTargetAccessor operationTargetAccessor);
}
