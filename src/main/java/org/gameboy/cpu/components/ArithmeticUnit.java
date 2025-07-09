package org.gameboy.cpu.components;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.utils.BitUtilities;

import static org.gameboy.cpu.Flag.*;
import static org.gameboy.utils.BitUtilities.*;

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
                        .with(H, get_bit(carry_bits, 3))
                        .with(C, get_bit(carry_bits, 7))
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

    public ArithmeticResult rotate_right_circular(byte value) {
        boolean carry = get_bit(value, 0);
        byte res = set_bit(rshift(value, 1), 7, carry);
        return new ArithmeticResult(
                res,
                new FlagChangesetBuilder()
                        .withAll(false)
                        .with(C, carry)
                        .with(Z, res == 0)
                        .build()
        );
    }

    public ArithmeticResult rotate_right(byte value, boolean carry_in) {
        byte res = set_bit(rshift(value, 1), 7, carry_in);
        return new ArithmeticResult(
                res,
                new FlagChangesetBuilder().withAll(false)
                        .with(Flag.C, get_bit(value, 0))
                        .with(Flag.Z, res == 0)
                        .build()
        );
    }

    public ArithmeticResult rotate_left_circular(byte value) {
        boolean carry = get_bit(value, 7);
        byte res = set_bit(lshift(value, 1), 0, carry);
        return new ArithmeticResult(
                res,
                new FlagChangesetBuilder()
                        .withAll(false)
                        .with(C, carry)
                        .with(Z, res == 0)
                        .build()
        );
    }

    public ArithmeticResult rotate_left(byte value, boolean carry_in) {
        byte res = set_bit(lshift(value, 1), 0, carry_in);
        return new ArithmeticResult(
                res,
                new FlagChangesetBuilder()
                        .withAll(false)
                        .with(Flag.C, get_bit(value, 7))
                        .with(Flag.Z, res == 0)
                        .build()
        );
    }

    public ArithmeticResult arithmetic_shift_left(byte value) {
        byte res = lshift(value, 1);
        return new ArithmeticResult(
                res,
                new FlagChangesetBuilder()
                        .withAll(false)
                        .with(Flag.C, get_bit(value, 7))
                        .with(Flag.Z, res == 0)
                        .build()
        );
    }

    public ArithmeticResult arithmetic_shift_right(byte value) {
        byte res = arithmetic_rshift(value, 1);
        return new ArithmeticResult(
                res,
                new FlagChangesetBuilder()
                        .withAll(false)
                        .with(Flag.C, get_bit(value, 0))
                        .with(Flag.Z, res == 0)
                        .build()
        );
    }

    public ArithmeticResult logical_shift_right(byte value) {
        byte res = rshift(value, 1);
        return new ArithmeticResult(
                res,
                new FlagChangesetBuilder()
                        .withAll(false)
                        .with(Flag.C, get_bit(value, 0))
                        .with(Flag.Z, res == 0)
                        .build()
        );
    }

    public ArithmeticResult compliment(byte value) {
        return new ArithmeticResult(
                BitUtilities.not(value),
                new FlagChangesetBuilder()
                        .with(Flag.N, true)
                        .with(Flag.H, true)
                        .build()
        );
    }

    public ArithmeticResult set_carry_flag() {
        return new ArithmeticResult(
                (byte) 0,
                new FlagChangesetBuilder()
                        .with(Flag.N, false)
                        .with(Flag.H, false)
                        .with(Flag.C, true)
                        .build()
        );
    }

    public ArithmeticResult compliment_carry_flag(boolean carry_flag) {
        return new ArithmeticResult(
                (byte) 0,
                new FlagChangesetBuilder()
                        .with(Flag.N, false)
                        .with(Flag.H, false)
                        .with(Flag.C, !carry_flag)
                        .build()
        );
    }

    public ArithmeticResult swap(byte value) {
        int lower_nibble = BitUtilities.bit_range(3, 0, value);
        int upper_nibble = BitUtilities.bit_range(7, 4, value);
        
        byte res = (byte) ((lower_nibble<<4) | upper_nibble);
        
        return new ArithmeticResult(
                res,
                new FlagChangesetBuilder()
                        .withAll(false)
                        .with(Z, res == 0)
                        .build()
        );
    }

    public ArithmeticResult bit_test(int bitIndex, byte value) {
        boolean bitValue = get_bit(value, bitIndex);
        
        return new ArithmeticResult(
                (byte) 0,
                new FlagChangesetBuilder()
                        .with(Z, !bitValue)
                        .with(N, false)
                        .with(H, true)
                        .build()
        );
    }
}
