package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.GenericOperationTarget;
import org.gameboy.cpu.instructions.targets.WordStackRegister;

public class Push implements Instruction {
    private final GenericOperationTarget target;

    private Push(GenericOperationTarget target) {
        this.target = target;
    }

    public static Push push_stk16(WordStackRegister rr) {
        return new Push(rr.convert());
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);

        short value = accessor.getValue(target);
        ControlFlow.pushToStack(cpuStructure, value);
    }

    @Override
    public String representation() {
        return "PUSH " + target.representation();
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
