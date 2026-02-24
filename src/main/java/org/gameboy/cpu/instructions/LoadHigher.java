package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Target;

public class LoadHigher implements Instruction{
    private final Target destination;
    private final Target source;

    private LoadHigher(Target destination, Target source) {
        this.destination = destination;
        this.source = source;
    }

    public static Instruction ldh_imm8_A() {
        return new LoadHigher(Target.indirect_imm_8, Target.a);
    }

    public static Instruction ldh_A_imm8() {
        return new LoadHigher(Target.a, Target.indirect_imm_8);
    }

    @Override
    public String representation() {
        return "LDH " + destination.representation() + "," + source.representation();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor operationTargetAccessor = OperationTargetAccessor.from(cpuStructure);
        operationTargetAccessor.setValue(destination, operationTargetAccessor.getValue(source));
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
