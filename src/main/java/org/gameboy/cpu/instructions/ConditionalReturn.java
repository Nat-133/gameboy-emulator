package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;
import org.gameboy.cpu.instructions.targets.Condition;

public class ConditionalReturn implements Instruction {
    private final Condition condition;

    private ConditionalReturn(Condition cc) {
        this.condition = cc;
    }

    public static ConditionalReturn ret_cc(Condition cc) {
        return new ConditionalReturn(cc);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        boolean shouldReturn = ControlFlow.evaluateCondition(condition, cpuStructure.registers());
        cpuStructure.clock().tick();

        if (shouldReturn) {
            short value = ControlFlow.popFromStack(cpuStructure);
            cpuStructure.registers().setPC(value);
            cpuStructure.clock().tick();
        }
    }

    @Override
    public String representation() {
        return "RET " + condition.name();
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
