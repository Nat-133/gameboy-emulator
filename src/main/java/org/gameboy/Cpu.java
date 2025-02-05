package org.gameboy;

import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.HardwareInterrupt;
import org.gameboy.instructions.Instruction;
import org.gameboy.instructions.targets.Interrupt;

import java.util.concurrent.atomic.AtomicReference;

public class Cpu {
    private final Decoder unprefixedDecoder;
    private final Decoder prefixedDecoder;
    private final AtomicReference<Decoder> activeDecoder;
    private final CpuStructure cpuStructure;

    public Cpu(CpuStructure cpuStructure, Decoder unprefixedDecoder, Decoder prefixedDecoder) {
        this.cpuStructure = cpuStructure;
        this.unprefixedDecoder = unprefixedDecoder;
        this.prefixedDecoder = prefixedDecoder;
        this.activeDecoder = new AtomicReference<>(unprefixedDecoder);
    }

    public void cycle() {
        Instruction instruction = decode(cpuStructure.registers().instructionRegister());

        execute(instruction);
    }

    private void fetch() {
        short pc = cpuStructure.registers().PC();
        cpuStructure.registers().setPC(cpuStructure.idu().increment(pc));
        this.cpuStructure.registers().setInstructionRegister(cpuStructure.memory().read(pc));

        if (!cpuStructure.interruptBus().activeInterrupts().isEmpty()) {
            cpuStructure.clock().tick();

            Interrupt highestPriorityInterrupt = cpuStructure.interruptBus().activeInterrupts().getFirst();
            HardwareInterrupt.callInterruptHandler(cpuStructure, highestPriorityInterrupt);
        }
    }

    private Instruction decode(byte opcode) {
        return activeDecoder.get().decode(opcode);
    }

    private void execute(Instruction instruction) {
        instruction.execute(cpuStructure);

        if (!instruction.handlesFetch()) {
            fetch();

            instruction.postFetch(cpuStructure);

            cpuStructure.clock().tick();
        }
    }
}
