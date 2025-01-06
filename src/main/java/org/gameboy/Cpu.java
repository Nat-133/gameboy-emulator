package org.gameboy;

import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.Instruction;

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
        this.cpuStructure.registers().setInstructionRegister(cpuStructure.memory().read(cpuStructure.registers().PC()));
    }

    private Instruction decode(byte opcode) {
        return activeDecoder.get().decode(opcode);
    }

    private void execute(Instruction instruction) {
        instruction.execute(cpuStructure);

        fetch();

        cpuStructure.clock().tick();
    }
}
