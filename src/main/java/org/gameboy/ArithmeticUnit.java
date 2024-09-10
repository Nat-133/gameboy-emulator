package org.gameboy;

import org.gameboy.utils.BitUtilities;

import java.util.Hashtable;

import static org.gameboy.Flag.*;
import static org.gameboy.utils.BitUtilities.bit;

public class ArithmeticUnit {
    public static ArithmeticResult inc(byte value) {
        ArithmeticResult result = add(value, (byte) 1);

        return new ArithmeticResult(
                result.result,
                new FlagChangeSetBuilder(result.flagChanges)
                        .without(C)
                        .build()
        );
    }

    public static ArithmeticResult dec(byte value) {
        ArithmeticResult result = sub(value, (byte) 1);


        return new ArithmeticResult(
                result.result,
                new FlagChangeSetBuilder(result.flagChanges)
                        .without(C)
                        .build()
        );
    }

    private static ArithmeticResult calculate_sum(byte a, byte b, boolean carry, boolean isSubtract) {
        byte res = (byte) (a + b + (carry ? 1 : 0));
        byte carry_bits = BitUtilities.calculate_carry_from_add(a, b, res);
        carry_bits = isSubtract ? (byte) ~carry_bits : carry_bits;

        return new ArithmeticResult(
                res,
                new FlagChangeSetBuilder()
                        .with(Z, res == 0)
                        .with(N, isSubtract)
                        .with(H, bit(carry_bits, 3))
                        .with(C, bit(carry_bits, 7))
                        .build()
        );
    }

    public static ArithmeticResult add(byte a, byte b) {
        return calculate_sum(a, b, false, false);
    }

    public static ArithmeticResult sub(byte a, byte b) {
        byte b_twos_compliment = (byte) (~b);

        return calculate_sum(a, b_twos_compliment, true, true);
    }

    public record ArithmeticResult(byte result, Hashtable<Flag, Boolean> flagChanges) {}

    public record FlagValue(Flag flag, boolean value) {
        public static FlagValue setFlag(Flag flag) {
            return new FlagValue(flag, true);
        }

        public static FlagValue unsetFlag(Flag flag) {
            return new FlagValue(flag, false);
        }
    }

    public static class FlagChangeSetBuilder {
        private final Hashtable<Flag, Boolean> changes;

        public FlagChangeSetBuilder() {
            this(new Hashtable<>(4, 1f));
        }

        public FlagChangeSetBuilder(Hashtable<Flag, Boolean> changes){
            this.changes = new Hashtable<>(changes);
        }

        public FlagChangeSetBuilder with(Flag flag, boolean value) {
            changes.put(flag, value);
            return this;
        }

        public FlagChangeSetBuilder without(Flag flag) {
            changes.remove(flag);
            return this;
        }

        public Hashtable<Flag, Boolean> build() {
            return changes;
        }
    }
}
