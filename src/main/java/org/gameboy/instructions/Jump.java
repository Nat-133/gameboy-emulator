package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.ControlFlow;
import org.gameboy.instructions.targets.Condition;
import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.OperationTarget;
import org.gameboy.utils.BitUtilities;

import static org.gameboy.instructions.common.ControlFlow.evaluateCondition;

public class Jump implements Instruction{
    private final Condition cc;
    private final GenericOperationTarget target;

    private Jump(Condition cc, GenericOperationTarget target) {
        this.cc = cc;
        this.target = target;
    }

    public static Jump jp_nn() {
        return new Jump(null, OperationTarget.IMM_16.direct());
    }

    public static Jump jp_HL() {
        return new Jump(null, OperationTarget.HL.direct());
    }

    public static Jump jp_cc_nn(Condition cc) {
        return new Jump(cc, OperationTarget.IMM_16.direct());
    }

    @Override
    public String toString() {
        return this.representation();
    }

    @Override
    public String representation() {
        return "JP "
                + (cc == null ? "" : cc.name() + ",")
                + this.target;
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        if (this.target.equals(OperationTarget.HL.direct())) {
            executeJpHL(cpuStructure);
        }
        else {
            executeJpImm16(cpuStructure);
        }
    }

    private static void executeJpHL(CpuStructure cpuStructure) {
        cpuStructure.registers().setPC(cpuStructure.registers().HL());
    }

    private void executeJpImm16(CpuStructure cpuStructure) {
        byte Z = ControlFlow.readImm8(cpuStructure);

        boolean doJump = evaluateCondition(cc, cpuStructure.registers());
        byte W = ControlFlow.readImm8(cpuStructure);
        short imm16 = BitUtilities.concat(W, Z);

        if (doJump) {
            cpuStructure.registers().setPC(imm16);
            cpuStructure.clock().tickCpu();
        }
    }
}
