package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.ControlFlow;
import org.gameboy.instructions.targets.Interrupt;

public class HardwareInterrupt {
    public static void callInterruptHandler(CpuStructure cpuStructure, Interrupt interrupt) {
        cpuStructure.registers().setIME(false);
        cpuStructure.interruptBus().deactivateInterrupt(interrupt);

        cpuStructure.registers().setPC(cpuStructure.idu().decrement(cpuStructure.registers().PC()));

        cpuStructure.clock().tick();

        ControlFlow.pushToStack(cpuStructure, cpuStructure.registers().PC());

        cpuStructure.registers().setPC(interrupt.getInterruptHandlerAddress());
        byte nextInstruction = ControlFlow.readIndirectPCAndIncrement(cpuStructure);
        cpuStructure.registers().setInstructionRegister(nextInstruction);
    }

    public String representation() {
        return "HARDWARE_INTERRUPT";
    }

    @Override
    public String toString() {
        return representation();
    }
}
