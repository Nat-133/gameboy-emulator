package org.gameboy;

import java.util.List;

import static org.gameboy.utils.BitUtilities.bit_range;

public class ArithmeticUnit {
    public static ArithmeticResult inc(byte value) {
        byte newValue = (byte) (value + 1);

        return new ArithmeticResult(
                newValue,
                List.of(
                        new FlagValue(Flag.Z, newValue == 0),
                        new FlagValue(Flag.N, false),
                        new FlagValue(Flag.H, bit_range(3, 0, value) == 0xf)
                )
        );
    }

    public record ArithmeticResult(byte result, List<FlagValue> flagChanges) {}

    public record FlagValue(Flag flag, boolean value) {
        public static FlagValue setFlag(Flag flag) {
            return new FlagValue(flag, true);
        }

        public static FlagValue unsetFlag(Flag flag) {
            return new FlagValue(flag, false);
        }
    }
}
