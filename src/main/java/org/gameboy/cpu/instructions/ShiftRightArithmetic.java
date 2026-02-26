package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import static org.gameboy.cpu.instructions.targets.Target.*;

public class ShiftRightArithmetic implements Instruction{
    private final R8 target;

    private ShiftRightArithmetic(R8 target) {
        this.target = target;
    }

    public static ShiftRightArithmetic sra_r8(R8 target) {
        return new ShiftRightArithmetic(target);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte value = (byte) accessor.getValue(target);
        ArithmeticResult result = cpuStructure.alu().arithmetic_shift_right(value);

        accessor.setValue(target, result.result());
        
        FlagChangesetBuilder flagBuilder = new FlagChangesetBuilder(result.flagChanges());

        cpuStructure.registers().setFlags(flagBuilder.build());
    }

    @Override
    public String representation() {
        return "SRA " + this.target.representation();
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
