package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.ControlFlow;
import org.gameboy.instructions.targets.Condition;
import org.gameboy.instructions.targets.OperationTarget;

public class Call implements Instruction{
    private final Condition condition;

    private Call(Condition cc) {
        this.condition = cc;
    }

    public static Call call() {
        return new Call(null);
    }

    public static Call call_cc(Condition cc) {
        return new Call(cc);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        short functionPointer = ControlFlow.readImm16(cpuStructure);

        // technically, the if statement should be in the previous clock tick.
        if (ControlFlow.evaluateCondition(condition, cpuStructure.registers())) {
            ControlFlow.pushToStack(cpuStructure, cpuStructure.registers().PC());

            cpuStructure.registers().setPC(functionPointer);
        }
    }

    @Override
    public String representation() {
        return "CALL "
                + (condition == null ? "" : condition.name() + ",")
                + OperationTarget.IMM_16.direct().representation();
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
