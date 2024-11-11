package org.gameboy.components;

import org.gameboy.ArithmeticResult;
import org.gameboy.FlagChangesetBuilder;
import org.gameboy.utils.BitUtilities;

import static org.gameboy.Flag.*;
import static org.gameboy.utils.BitUtilities.bit;

public class ArithmeticUnit {
    public ArithmeticResult inc(byte value) {
        ArithmeticResult result = add(value, (byte) 1);

        return new ArithmeticResult(
                result.result(),
                new FlagChangesetBuilder(result.flagChanges())
                        .without(C)
                        .build()
        );
    }

    public ArithmeticResult dec(byte value) {
        ArithmeticResult result = sub(value, (byte) 1);

        return new ArithmeticResult(
                result.result(),
                new FlagChangesetBuilder(result.flagChanges())
                        .without(C)
                        .build()
        );
    }

    private ArithmeticResult calculate_sum(byte a, byte b, boolean carry, boolean isSubtract) {
        byte res = (byte) (a + b + (carry ? 1 : 0));
        byte carry_bits = BitUtilities.calculate_carry_from_add(a, b, res);
        carry_bits = isSubtract ? (byte) ~carry_bits : carry_bits;

        return new ArithmeticResult(
                res,
                new FlagChangesetBuilder()
                        .with(Z, res == 0)
                        .with(N, isSubtract)
                        .with(H, bit(carry_bits, 3))
                        .with(C, bit(carry_bits, 7))
                        .build()
        );
    }

    public ArithmeticResult add_carry(byte a, byte b, boolean carry) {
        return calculate_sum(a, b, carry, false);
    }

    public ArithmeticResult sub_carry(byte a, byte b, boolean carry) {
        byte b_twos_compliment = (byte) (~b);

        return calculate_sum(a, b_twos_compliment, !carry, true);
    }

    public ArithmeticResult add(byte a, byte b) {
        return add_carry(a, b, false);
    }

    public ArithmeticResult sub(byte a, byte b) {
        return sub_carry(a, b, false);
    }

    public ArithmeticResult and(byte a, byte b) {
        byte res = BitUtilities.and(a, b);

        return new ArithmeticResult(
                res,
                new FlagChangesetBuilder()
                        .with(Z, res == 0)
                        .with(N, false)
                        .with(H, true)
                        .with(C, false)
                        .build()
        );
    }

    public ArithmeticResult xor(byte a, byte b) {
        byte res = (byte) (a ^ b);
        return new ArithmeticResult(
                res,
                new FlagChangesetBuilder()
                        .withAll(false)
                        .with(Z, res == 0)
                        .build()
        );
    }

    public ArithmeticResult or(byte a, byte b) {
        byte res = (byte) (a | b);
        return new ArithmeticResult(
                res,
                new FlagChangesetBuilder()
                        .withAll(false)
                        .with(Z, res == 0)
                        .build()
        );
    }
}
