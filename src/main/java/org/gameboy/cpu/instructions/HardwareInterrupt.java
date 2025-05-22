package org.gameboy.cpu.instructions;

import org.gameboy.common.Interrupt;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;

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

    @Override
    public String toString() {
        return "HARDWARE_INTERRUPT";
    }
}
