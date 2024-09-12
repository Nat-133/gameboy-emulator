package org.gameboy.instructions.common;

import org.gameboy.components.ArithmeticUnit;
import org.gameboy.components.ArithmeticUnit.ArithmeticResult;
import org.gameboy.Flag;
import org.gameboy.components.CpuStructure;
import org.gameboy.components.IncrementDecrementUnit;

import java.util.function.BiConsumer;

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
}
