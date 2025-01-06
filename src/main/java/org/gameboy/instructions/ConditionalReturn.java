package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.ControlFlow;
import org.gameboy.instructions.targets.Condition;

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
}
