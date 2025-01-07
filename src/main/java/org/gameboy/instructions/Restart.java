package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.ControlFlow;

import static org.gameboy.utils.BitUtilities.uint;

public class Restart implements Instruction {
    private final short address;

    private Restart(short address) {
        this.address = address;
    }

    public static Restart rst_00H() {
        return new Restart((short) 0x00);
    }

    public static Restart rst_08H() {
        return new Restart((short) 0x08);
    }

    public static Restart rst_10H() {
        return new Restart((short) 0x10);
    }

    public static Restart rst_18H() {
        return new Restart((short) 0x18);
    }

    public static Restart rst_20H() {
        return new Restart((short) 0x20);
    }

    public static Restart rst_28H() {
        return new Restart((short) 0x28);
    }

    public static Restart rst_30H() {
        return new Restart((short) 0x30);
    }

    public static Restart rst_38H() {
        return new Restart((short) 0x38);
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        ControlFlow.pushToStack(cpuStructure, cpuStructure.registers().PC());

        cpuStructure.registers().setPC(address);
    }

    @Override
    public String representation() {
        return "RST $%02x".formatted(uint(address));
    }

    @Override
    public String toString() {
        return representation();
    }
}
