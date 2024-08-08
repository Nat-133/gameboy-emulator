package org.gameboy;

public class CpuRegisters {
    private short af;
    private short bc;
    private short de;
    private short hl;

    private short sp;
    private short pc;

    private short instructionRegister;

    public CpuRegisters(
            short af,
            short bc,
            short de,
            short hl,
            short sp,
            short pc,
            short instructionRegister) {
        this.af = af;
        this.bc = bc;
        this.de = de;
        this.hl = hl;
        this.sp = sp;
        this.pc = pc;

        this.instructionRegister = instructionRegister;
    }

    public CpuRegisters() {
        this(
                (short) 0x0000,
                (short) 0x0000,
                (short) 0x0000,
                (short) 0x0000,
                (short) 0x0000,
                (short) 0x0000,
                (short) 0x0000
        );
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

    public byte F() {
        return (byte) (af);
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

    private void setF(byte value) {
        af = (short) (af & 0xFF00);
        af = (short) (af | value);
    }

    private void setZ(boolean value) {

    }

    public short instructionRegister() {
        return instructionRegister;
    }

    public void setInstructionRegister(short instructionRegister) {
        this.instructionRegister = instructionRegister;
    }
}
