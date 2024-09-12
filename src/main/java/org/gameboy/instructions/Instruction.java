package org.gameboy.instructions;

import org.gameboy.instructions.common.OperationTargetAccessor;

public interface Instruction {
    String representation();

    void execute(OperationTargetAccessor operationTargetAccessor);
}
