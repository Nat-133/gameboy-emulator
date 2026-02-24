package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Target;
import static org.gameboy.cpu.instructions.targets.Target.*;

public class Sub implements Instruction{
    private final Target right;

    private Sub(Target right) {
        this.right = right;
    }

    public static Sub sub_r8(R8 r8) {
        return new Sub((Target) r8);
    }

    public static Sub sub_a_imm8() {
        return new Sub(imm_8);
    }

    @Override
    public String representation() {
        return "SUB " + a.representation() + "," + this.right.representation();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte a = cpuStructure.registers().A();
        byte b = (byte) accessor.getValue(this.right);

        ArithmeticResult res = cpuStructure.alu().sub(a, b);
        cpuStructure.registers().setA(res.result());
        cpuStructure.registers().setFlags(res.flagChanges());
    }

    @Override
    public String toString() {
        return this.representation();
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
