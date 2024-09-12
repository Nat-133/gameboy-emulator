package org.gameboy.instructions.common;

import org.gameboy.components.ArithmeticUnit;
import org.gameboy.components.ArithmeticUnit.ArithmeticResult;
import org.gameboy.Flag;
import org.gameboy.components.IncrementDecrementUnit;

import java.util.function.BiConsumer;

import static org.gameboy.utils.BitUtilities.*;

public class ControlFlow {
    public static short signedAddition(short a, byte signedByte, BiConsumer<Boolean, Flag> flagSetOperation) {
        byte msb = upper_byte(a);
        byte lsb = lower_byte(a);

        ArithmeticResult res = ArithmeticUnit.add(lsb, signedByte);
        boolean carry = res.flagChanges().getOrDefault(Flag.C, false);
        boolean negativeOffset = bit(signedByte, 7);

        if (carry && !negativeOffset) {
            msb = (byte) IncrementDecrementUnit.increment(msb);
        }
        else if (!carry && negativeOffset) {
            msb = (byte) IncrementDecrementUnit.decrement(msb);
        }
        lsb = res.result();

        res.flagChanges().forEach((f,b) -> flagSetOperation.accept(b, f));
        return concat(msb, lsb);
    }
}
