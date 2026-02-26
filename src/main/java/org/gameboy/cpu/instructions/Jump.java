package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;
import org.gameboy.cpu.instructions.targets.Condition;
import org.gameboy.cpu.instructions.targets.Target;
import static org.gameboy.cpu.instructions.targets.Target.*;
import org.gameboy.utils.BitUtilities;

import static org.gameboy.cpu.instructions.common.ControlFlow.evaluateCondition;

public class Jump implements Instruction{
    private final Condition cc;
    private final Target target;

    private Jump(Condition cc, Target target) {
        this.cc = cc;
        this.target = target;
    }

    public static Jump jp_nn() {
        return new Jump(null, imm_16);
    }

    public static Jump jp_HL() {
        return new Jump(null, hl);
    }

    public static Jump jp_cc_nn(Condition cc) {
        return new Jump(cc, imm_16);
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
        if (this.target == hl) {
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
        byte Z = ControlFlow.readIndirectPCAndIncrement(cpuStructure);

        boolean doJump = evaluateCondition(cc, cpuStructure.registers());
        byte W = ControlFlow.readIndirectPCAndIncrement(cpuStructure);
        short imm16 = BitUtilities.concat(W, Z);

        if (doJump) {
            cpuStructure.registers().setPC(imm16);
            cpuStructure.clock().tick();
        }
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
