package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Target;

import static org.gameboy.cpu.instructions.common.ControlFlow.popFromStack;

public class Pop implements Instruction {
    private final Target.Stk16 target;

    private Pop(Target.Stk16 target) {
        this.target = target;
    }

    public static Pop pop_stk16(Target.Stk16 stk16) {
        return new Pop(stk16);
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
