package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.WordStackRegister;

import static org.gameboy.instructions.common.ControlFlow.popFromStack;

public class Pop implements Instruction {
    private final GenericOperationTarget target;

    private Pop(GenericOperationTarget target) {
        this.target = target;
    }

    public static Pop pop_stk16(WordStackRegister stk16) {
        return new Pop(stk16.convert());
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        short value = popFromStack(cpuStructure);

        accessor.setValue(target, value);
    }

    @Override
    public String representation() {
        return "POP " + target.representation();
    }

    @Override
    public String toString() {
        return representation();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Instruction other) {
            return this.representation().equals(other.representation());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.representation().hashCode();
    }
}
