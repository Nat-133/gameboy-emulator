package org.gameboy;

import org.gameboy.instructions.Instruction;
import org.gameboy.instructions.Load;
import org.gameboy.instructions.targets.ByteRegister;

import java.util.concurrent.atomic.AtomicReference;

public class Cpu {
    private final CpuRegisters registers;
    private final Memory memory;
    private final UnprefixedDecoder unprefixedDecoder;
    private final PrefixedDecoder prefixedDecoder;
    private final AtomicReference<Decoder> activeDecoder;

    public static void main(String[] args) {
        System.out.println("Hello, World!");
        Instruction instruction = Load.load_register_register(ByteRegister.A, ByteRegister.B);
        System.out.println(instruction.representation());
    }

    private Cpu(CpuRegisters registers, Memory memory, UnprefixedDecoder unprefixedDecoder, PrefixedDecoder prefixedDecoder) {
        this.registers = registers;
        this.memory = memory;
        this.unprefixedDecoder = unprefixedDecoder;
        this.prefixedDecoder = prefixedDecoder;
        this.activeDecoder = new AtomicReference<>(unprefixedDecoder);
    }

    private byte fetch()
    {
        registers.setInstructionRegister(memory.read(registers.PC()));
        return (byte) (registers.instructionRegister() >> 8);
    }

    private Instruction decode(byte opcode)
    {
        return activeDecoder.get().decode(opcode);
    }

    private void execute(Instruction instruction)
    {

    }
}
