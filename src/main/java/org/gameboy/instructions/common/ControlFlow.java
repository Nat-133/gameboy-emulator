package org.gameboy.instructions.common;

import org.gameboy.components.ArithmeticUnit.ArithmeticResult;
import org.gameboy.Flag;
import org.gameboy.components.CpuStructure;

import static org.gameboy.utils.BitUtilities.*;

public class ControlFlow {
    public static short signedAddition(short a, byte signedByte, boolean setFlags, CpuStructure cpuStructure) {
        byte msb = upper_byte(a);
        byte lsb = lower_byte(a);

        ArithmeticResult res = cpuStructure.alu().add(lsb, signedByte);
        boolean carry = res.flagChanges().getOrDefault(Flag.C, false);
        boolean negativeOffset = bit(signedByte, 7);

        if (carry && !negativeOffset) {
            msb = (byte) cpuStructure.idu().increment(msb);
        }
        else if (!carry && negativeOffset) {
            msb = (byte) cpuStructure.idu().decrement(msb);
        }
        lsb = res.result();

        if (setFlags) res.flagChanges().forEach((f,b) -> cpuStructure.registers().setFlags(b, f));
        return concat(msb, lsb);
    }

    public static void incrementPC(CpuStructure cpuStructure) {
        cpuStructure.registers().setPC(cpuStructure.idu().increment(cpuStructure.registers().PC()));
    }

    public static byte readImm8(CpuStructure cpuStructure) {
        byte value = cpuStructure.memory().read(cpuStructure.registers().PC());
        incrementPC(cpuStructure);
        return value;
    }

    public static short readImm16(CpuStructure cpuStructure) {
        byte lsb = readImm8(cpuStructure);

        byte msb = readImm8(cpuStructure);

        return concat(msb, lsb);
    }
}
