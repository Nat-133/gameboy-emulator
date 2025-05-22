package org.gameboy.cpu;

import org.gameboy.common.Interrupt;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.HardwareInterrupt;
import org.gameboy.cpu.instructions.Instruction;

public class Cpu {
    private final CpuStructure cpuStructure;

    public Cpu(CpuStructure cpuStructure) {
        this.cpuStructure = cpuStructure;
    }

    public void cycle() {
        Instruction instruction = decode(cpuStructure.registers().instructionRegister());

        instruction.execute(cpuStructure);

        fetch_cycle(instruction);
    }

    private void fetch_cycle(Instruction instruction) {
        if (!instruction.handlesFetch()) {
            fetch();
            instruction.postFetch(cpuStructure);
            cpuStructure.clock().tick();

            handlePotentialInterrupt();
        }
    }

    private void fetch() {
        short pc = cpuStructure.registers().PC();
        cpuStructure.registers().setPC(cpuStructure.idu().increment(pc));
        this.cpuStructure.registers().setInstructionRegister(cpuStructure.memory().read(pc));
    }

    private void handlePotentialInterrupt() {
        if (cpuStructure.registers().IME() && cpuStructure.interruptBus().hasInterrupts()) {
            Interrupt highestPriorityInterrupt = cpuStructure.interruptBus().activeInterrupts().getFirst();
            HardwareInterrupt.callInterruptHandler(cpuStructure, highestPriorityInterrupt);
        }
    }

    private Instruction decode(byte opcode) {
        return cpuStructure.decoder().decode(opcode);
    }
}
