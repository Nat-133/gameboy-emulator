package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Target;
import static org.gameboy.cpu.instructions.targets.Target.*;

public class AddWithCarry implements Instruction{
    private final Target right;

    private AddWithCarry(Target right) {
        this.right = right;
    }

    public static AddWithCarry adc_a_r8(R8 r8) {
        return new AddWithCarry((Target) r8);
    }

    public static AddWithCarry adc_a_imm8() {
        return new AddWithCarry(imm_8);
    }

    @Override
    public String representation() {
        return "ADC " + a.representation() + "," + this.right.representation();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor accessor = OperationTargetAccessor.from(cpuStructure);
        byte a = cpuStructure.registers().A();
        byte b = (byte) accessor.getValue(this.right);

        ArithmeticResult res = cpuStructure.alu().add_carry(a, b, cpuStructure.registers().getFlag(Flag.C));
        cpuStructure.registers().setA(res.result());
        cpuStructure.registers().setFlags(res.flagChanges());
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
