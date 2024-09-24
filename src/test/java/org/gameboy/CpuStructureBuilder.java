package org.gameboy;

import org.gameboy.components.*;
import org.gameboy.utils.BitUtilities;

import static org.gameboy.utils.BitUtilities.lower_byte;
import static org.gameboy.utils.BitUtilities.upper_byte;

@SuppressWarnings("Unused")
public class CpuStructureBuilder {
    private short af;
    private short bc;
    private short de;
    private short hl;
    private short sp;
    private short pc;
    private byte instructionRegister;

    private final Memory memory;

    public CpuStructureBuilder() {
        this.af = 0x0000;
        this.bc = 0x0000;
        this.de = 0x0000;
        this.hl = 0x0000;
        this.sp = 0x0000;
        this.pc = 0x0000;
        this.instructionRegister = 0x00;

        this.memory = new Memory();
    }

    public CpuStructureBuilder withAF(int af) {
        this.af = (short) af;
        return this;
    }

    public CpuStructureBuilder withA(int a) {
        this.af = BitUtilities.set_upper_byte(af, (byte) a);
        return this;
    }

    public CpuStructureBuilder withF(int f) {
        this.af = BitUtilities.set_upper_byte(af, (byte) f);
        return this;
    }

    public CpuStructureBuilder withBC(int bc) {
        this.bc = (short) bc;
        return this;
    }

    public CpuStructureBuilder withC(int c) {
        this.bc = BitUtilities.set_lower_byte(bc, (byte) c);
        return this;
    }

    public CpuStructureBuilder withB(int b) {
        this.bc = BitUtilities.set_upper_byte(bc, (byte) b);
        return this;
    }

    public CpuStructureBuilder withDE(int de) {
        this.de = (short) de;
        return this;
    }

    public CpuStructureBuilder withD(int d) {
        this.de = BitUtilities.set_upper_byte(de, (byte) d);
        return this;
    }

    public CpuStructureBuilder withE(int e) {
        this.de = BitUtilities.set_lower_byte(de, (byte) e);
        return this;
    }

    public CpuStructureBuilder withHL(int hl) {
        this.hl = (short) hl;
        return this;
    }

    public CpuStructureBuilder withL(int l) {
        this.hl = BitUtilities.set_lower_byte(hl, (byte) l);
        return this;
    }

    public CpuStructureBuilder withH(int h) {
        this.hl = BitUtilities.set_upper_byte(hl, (byte) h);
        return this;
    }

    public CpuStructureBuilder withSP(int sp) {
        this.sp = (short) sp;
        return this;
    }

    public CpuStructureBuilder withPC(int pc) {
        this.pc = (short) pc;
        return this;
    }

    public CpuStructureBuilder withInstructionRegister(int instructionRegister) {
        this.instructionRegister = (byte) instructionRegister;
        return this;
    }

    public CpuStructureBuilder withAllRegistersSet(int value) {
        return this.withAF(value)
                .withBC(value)
                .withDE(value)
                .withHL(value)
                .withSP(value)
                .withPC(value)
                .withInstructionRegister(value);
    }

    public CpuStructureBuilder withImm8(int imm8) {
        this.memory.write(this.pc, (byte) imm8);
        return this;
    }

    public CpuStructureBuilder withImm16(int imm16) {
        short val = (short) imm16;
        this.memory.write(this.pc, lower_byte(val));
        this.memory.write(this.pc, upper_byte(val));
        return this;
    }

    public CpuStructureBuilder withMemory(int address, int value) {
        this.memory.write((short) address, (byte) value);
        return this;
    }

    public CpuStructureBuilder withHigherMemory(int address, int value) {
        short location = BitUtilities.set_lower_byte((short) 0xff00, (byte) address);
        this.memory.write(location, (byte) value);
        return this;
    }

    public CpuStructureBuilder withExclusivelySetFlags(Flag... flags) {
        this.af = BitUtilities.set_lower_byte(this.af, (byte) 0x00);
        return this.withSetFlags(flags);
    }

    public CpuStructureBuilder withSetFlags(Flag... flags) {
        for (Flag flag : flags) {
            this.af = BitUtilities.apply_mask(af, flag.getLocationMask(), false);
        }

        return this;
    }

    public CpuStructureBuilder withExclusivelyUnsetFlags(Flag... flags) {
        this.af = BitUtilities.set_lower_byte(this.af, (byte) 0xf0);
        return this.withUnsetFlags(flags);
    }

    public CpuStructureBuilder withUnsetFlags(Flag... flags) {
        for (Flag flag : flags) {
            this.af = BitUtilities.apply_mask(af, flag.getLocationMask(), true);
        }

        return this;
    }

    public CpuStructure build() {
        return new CpuStructure(
                new CpuRegisters(af, bc, de, hl, sp, pc, instructionRegister),
                memory,
                new ArithmeticUnit(),
                new IncrementDecrementUnit()
        );
    }
}
