package org.gameboy.cpu;

import org.gameboy.common.Interrupt;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.HardwareInterrupt;
import org.gameboy.cpu.instructions.Instruction;
import org.gameboy.utils.InstructionFormatConverter;

public class Cpu {
    private static final boolean LOGGING_ENABLED = false;

    private final CpuStructure cpuStructure;
    private short currentInstructionAddress = 0;
    private boolean firstInstruction = true;

    public Cpu(CpuStructure cpuStructure) {
        this.cpuStructure = cpuStructure;
    }

    public void cycle() {
        short instrAddr = currentInstructionAddress;
        Instruction instruction = decode(cpuStructure.registers().instructionRegister());

        instruction.execute(cpuStructure);

        fetch_cycle(instruction);

        if (LOGGING_ENABLED && !firstInstruction) {
            var regs = cpuStructure.registers();
            int tima = cpuStructure.memory().read((short) 0xFF05) & 0xFF;
            String refInstr = InstructionFormatConverter.toReferenceFormat(instruction.representation());
            System.out.printf("PC:%04X AF=%04X BC=%04X DE=%04X HL=%04X SP=%04X TIMA=%d | %s%n",
                instrAddr & 0xFFFF,
                regs.AF() & 0xFFFF,
                regs.BC() & 0xFFFF,
                regs.DE() & 0xFFFF,
                regs.HL() & 0xFFFF,
                regs.SP() & 0xFFFF,
                tima,
                refInstr);
        }
        firstInstruction = false;
    }

    private void fetch_cycle(Instruction instruction) {
        if (!instruction.handlesFetch()) {
            fetch();
            cpuStructure.clock().tick();

            if (handlePotentialInterrupt()) {
                // Interrupt was handled - update currentInstructionAddress
                // PC is now at interrupt_handler + 1, so PC-1 is the handler address
                currentInstructionAddress = (short)(cpuStructure.registers().PC() - 1);
            }

            instruction.postFetch(cpuStructure);
        } else {
            // Instruction handled its own fetch - update currentInstructionAddress
            // The instruction read from PC then incremented it, so fetched address is PC-1
            currentInstructionAddress = (short)(cpuStructure.registers().PC() - 1);
        }
    }

    private void fetch() {
        short pc = cpuStructure.registers().PC();
        currentInstructionAddress = pc;
        cpuStructure.registers().setPC(cpuStructure.idu().increment(pc));
        this.cpuStructure.registers().setInstructionRegister(cpuStructure.memory().read(pc));
    }

    private boolean handlePotentialInterrupt() {
        if (cpuStructure.registers().IME() && cpuStructure.interruptBus().hasInterrupts()) {
            Interrupt highestPriorityInterrupt = cpuStructure.interruptBus().activeInterrupts().getFirst();
            HardwareInterrupt.callInterruptHandler(cpuStructure, highestPriorityInterrupt);
            return true;
        }
        return false;
    }

    private Instruction decode(byte opcode) {
        return cpuStructure.decoder().decode(opcode);
    }
}
