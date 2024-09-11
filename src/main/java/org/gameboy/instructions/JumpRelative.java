package org.gameboy.instructions;

import org.gameboy.*;
import org.gameboy.ArithmeticUnit.ArithmeticResult;
import org.gameboy.instructions.targets.Condition;
import org.gameboy.instructions.targets.OperationTarget;

import static org.gameboy.utils.BitUtilities.*;

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
    public void execute(OperationTargetAccessor operationTargetAccessor) {
        byte offset = (byte) operationTargetAccessor.getValue(OperationTarget.IMM_8.direct());
        boolean shouldJump = evaluateCondition(cc, operationTargetAccessor.cpuRegisters);

        if (shouldJump) {
            short pc = operationTargetAccessor.cpuRegisters.PC();
            byte pch = upper_byte(pc);
            byte pcl = lower_byte(pc);

            ArithmeticResult res = ArithmeticUnit.add(pcl, offset);
            boolean carry = res.flagChanges().getOrDefault(Flag.C, false);
            boolean negativeOffset = bit(offset, 7);

            if (carry && !negativeOffset) {
                pch = (byte) IncrementDecrementUnit.increment(pch);
            }
            else if (!carry && negativeOffset) {
                pch = (byte) IncrementDecrementUnit.decrement(pch);
            }
            pcl = res.result();

            short new_pc = concat(pch, pcl);

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
