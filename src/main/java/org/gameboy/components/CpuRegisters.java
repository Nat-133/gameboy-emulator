package org.gameboy.components;

import org.gameboy.Flag;

import java.util.Arrays;

import static org.gameboy.utils.BitUtilities.apply_mask;

public class CpuRegisters {
    private short af;
    private short bc;
    private short de;
    private short hl;

    private short sp;
    private short pc;

    private byte instructionRegister;

    public CpuRegisters(
            short af,
            short bc,
            short de,
            short hl,
            short sp,
            short pc,
            byte instructionRegister) {
        this.af = af;
        this.bc = bc;
        this.de = de;
        this.hl = hl;
        this.sp = sp;
        this.pc = pc;

        this.instructionRegister = instructionRegister;
    }

    public short BC() {
        return bc;
    }

    public short DE() {
        return de;
    }

    public short HL() {
        return hl;
    }

    public short AF() {
        return af;
    }

    public short SP() {
        return sp;
    }

    public short PC() {
        return pc;
    }

    public byte A() {
        return (byte) (af >> 8);
    }

    public byte B() {
        return (byte) (bc >> 8);
    }

    public byte C() {
        return (byte) (bc);
    }

    public byte D() {
        return (byte) (de >> 8);
    }

    public byte E() {
        return (byte) (de);
    }

    public byte H() {
        return (byte) (hl >> 8);
    }

    public byte L() {
        return (byte) (hl);
    }

    public void setBC(short value) {
        bc = value;
    }

    public void setDE(short value) {
        de = value;
    }

    public void setHL(short value) {
        hl = value;
    }

    public void setAF(short value) {
        af = value;
    }

    public void setSP(short value) {
        sp = value;
    }

    public void setPC(short value) {
        pc = value;
    }

    public void setA(byte value) {
        af = (short) (af & 0x00FF);
        af = (short) ((value << 8) | af);
    }

    public void setB(byte value) {
        bc = (short) (bc & 0x00FF);
        bc = (short) ((value << 8) | bc);
    }

    public void setC(byte value) {
        bc = (short) (bc & 0xFF00);
        bc = (short) (bc | value);
    }

    public void setD(byte value) {
        de = (short) (de & 0x00FF);
        de = (short) ((value << 8) | de);
    }

    public void setE(byte value) {
        de = (short) (de & 0xFF00);
        de = (short) (de | value);
    }

    public void setH(byte value) {
        hl = (short) (hl & 0x00FF);
        hl = (short) ((value << 8) | hl);
    }

    public void setL(byte value) {
        hl = (short) (hl & 0xFF00);
        hl = (short) (hl | value);
    }

    public void setFlags(boolean value, Flag... flags) {
        int mask = Arrays.stream(flags)
                .map(Flag::getLocationMask)
                .reduce(0, (a, b) -> a | b);

        af = apply_mask(af, mask, !value);
    }

    public boolean getFlag(Flag flag) {
        return apply_mask(af, ~flag.getLocationMask(), true) != 0;
    }

    public byte instructionRegister() {
        return instructionRegister;
    }

    public void setInstructionRegister(byte instructionRegister) {
        this.instructionRegister = instructionRegister;
    }
}
