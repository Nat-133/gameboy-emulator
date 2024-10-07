package org.gameboy.instructions;

import org.gameboy.Flag;
import org.gameboy.components.CpuRegisters;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.ControlFlow;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.Condition;
import org.gameboy.instructions.targets.OperationTarget;

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
    public String representation() {
        return "JR "
                + (cc == null ? "" : cc.name() + ",")
                + "e8";
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor operationTargetAccessor = OperationTargetAccessor.from(cpuStructure);
        byte offset = (byte) operationTargetAccessor.getValue(OperationTarget.IMM_8.direct());
        boolean shouldJump = evaluateCondition(cc, cpuStructure.registers());

        if (shouldJump) {
            short pc = cpuStructure.registers().PC();
            short new_pc = ControlFlow.signedAdditionWithIdu(pc, offset, false, cpuStructure);
            operationTargetAccessor.setValue(OperationTarget.PC.direct(), new_pc);
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
}
