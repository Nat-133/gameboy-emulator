package org.gameboy;

import org.gameboy.utils.BitUtilities;

import java.util.List;

import static org.gameboy.Flag.*;
import static org.gameboy.utils.BitUtilities.bit;

public class ArithmeticUnit {
    public static ArithmeticResult inc(byte value) {
        ArithmeticResult result = add(value, (byte) 1);

        return new ArithmeticResult(
                result.result,
                result.flagChanges.stream().filter(c -> c.flag != C).toList()
        );
    }

    public static ArithmeticResult dec(byte value) {
        ArithmeticResult result = sub(value, (byte) 1);

        return new ArithmeticResult(
                result.result,
                result.flagChanges.stream().filter(c -> c.flag != C).toList()
        );
    }

    private static ArithmeticResult calculate_sum(byte a, byte b, boolean carry, boolean isSubtract) {
        byte res = (byte) (a + b + (carry ? 1 : 0));
        byte carry_bits = BitUtilities.calculate_carry_from_add(a, b, res);
        carry_bits = isSubtract ? (byte) ~carry_bits : carry_bits;

        return new ArithmeticResult(
                res,
                List.of(
                        new FlagValue(Z, res == 0),
                        new FlagValue(N, isSubtract),
                        new FlagValue(H, bit(carry_bits, 3)),
                        new FlagValue(C, bit(carry_bits, 7))
                )
        );
    }

    public static ArithmeticResult add(byte a, byte b) {
        return calculate_sum(a, b, false, false);
    }

    public static ArithmeticResult sub(byte a, byte b) {
        byte b_twos_compliment = (byte) (~b);

        return calculate_sum(a, b_twos_compliment, true, true);
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
