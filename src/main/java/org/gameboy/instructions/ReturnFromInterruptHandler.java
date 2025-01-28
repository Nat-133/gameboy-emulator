package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.ControlFlow;

public class ReturnFromInterruptHandler implements Instruction {
    private ReturnFromInterruptHandler() {
    }

    public static ReturnFromInterruptHandler reti() {
        return new ReturnFromInterruptHandler();
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        short value = ControlFlow.popFromStack(cpuStructure);
        cpuStructure.registers().setPC(value);
        cpuStructure.clock().tick();
    }

    @Override
    public void postFetch(CpuStructure cpuStructure) {
        cpuStructure.registers().setIME(true);
    }

    @Override
    public String representation() {
        return "RETI";
    }

    @Override
    public String toString() {
        return representation();
    }
}
