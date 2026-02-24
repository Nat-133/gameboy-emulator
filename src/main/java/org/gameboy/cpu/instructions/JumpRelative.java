package org.gameboy.cpu.instructions;

import org.gameboy.cpu.Flag;
import org.gameboy.cpu.components.CpuRegisters;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Condition;
import static org.gameboy.cpu.instructions.targets.Target.*;

public class JumpRelative implements Instruction{
    private final Condition cc;

    private JumpRelative(Condition cc) {
        this.cc = cc;
    }

    public static JumpRelative jr_cc(Condition cc) {
        return new JumpRelative(cc);
    }

    public static JumpRelative jr() {
        return new JumpRelative(null);
    }

    @Override
    public String toString() {
        return this.representation();
    }

    @Override
    public String representation() {
        return "JR "
                + (cc == null ? "" : cc.name() + ",")
                + "e8";
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor operationTargetAccessor = OperationTargetAccessor.from(cpuStructure);
        boolean shouldJump = evaluateCondition(cc, cpuStructure.registers());
        byte offset = (byte) operationTargetAccessor.getValue(imm_8);

        if (shouldJump) {
            short currentPC = cpuStructure.registers().PC();
            short new_pc = ControlFlow.signedAdditionWithIdu(currentPC, offset, false, cpuStructure);
            operationTargetAccessor.setValue(pc, new_pc);
        }
    }

    private boolean evaluateCondition(Condition condition, CpuRegisters registers) {
        return switch(condition) {
            case null -> true;
            case NZ -> !registers.getFlag(Flag.Z);
            case Z -> registers.getFlag(Flag.Z);
            case NC -> !registers.getFlag(Flag.C);
            case C -> registers.getFlag(Flag.C);
        };
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
