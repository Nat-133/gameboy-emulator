package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;

public class RotateRight implements Instruction{
    private final ByteRegister target;
    private final boolean isPrefixInstruction;

    private RotateRight(ByteRegister target, boolean isPrefixInstruction) {
        this.target = target;
        this.isPrefixInstruction = isPrefixInstruction;
    }

    public static RotateRight rra() {
        return new RotateRight(ByteRegister.A, false);
    }

    public static RotateRight rr_r8(ByteRegister target) {
        return new RotateRight(target, true);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte value = (byte) accessor.getValue(target.convert());
        boolean carryIn = cpuStructure.registers().getFlag(Flag.C);
        ArithmeticResult result = cpuStructure.alu().rotate_right(value, carryIn);

        accessor.setValue(target.convert(), result.result());
        
        FlagChangesetBuilder flagBuilder = new FlagChangesetBuilder(result.flagChanges());

        if (!isPrefixInstruction) {
            flagBuilder.with(Flag.Z, false);
        }
        cpuStructure.registers().setFlags(flagBuilder.build());
    }

    @Override
    public String representation() {
        return "RR" + (isPrefixInstruction ? " " : "") + this.target.convert().representation();
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
