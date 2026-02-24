package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Target;

public class RotateLeft implements Instruction{
    private final Target.R8 target;
    private final boolean isPrefixInstruction;

    private RotateLeft(Target.R8 target, boolean isPrefixInstruction) {
        this.target = target;
        this.isPrefixInstruction = isPrefixInstruction;
    }

    public static RotateLeft rla() {
        return new RotateLeft(Target.a, false);
    }

    public static RotateLeft rl_r8(Target.R8 target) {
        return new RotateLeft(target, true);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte value = (byte) accessor.getValue(target);
        boolean carryIn = cpuStructure.registers().getFlag(Flag.C);
        ArithmeticResult result = cpuStructure.alu().rotate_left(value, carryIn);

        accessor.setValue(target, result.result());
        
        FlagChangesetBuilder flagBuilder = new FlagChangesetBuilder(result.flagChanges());

        if (!isPrefixInstruction) {
            flagBuilder.with(Flag.Z, false);
        }
        cpuStructure.registers().setFlags(flagBuilder.build());
    }

    @Override
    public String representation() {
        return "RL" + (isPrefixInstruction ? " " : "") + this.target.representation();
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
