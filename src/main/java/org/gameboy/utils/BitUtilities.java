package org.gameboy.utils;

@SuppressWarnings("unused")
public class BitUtilities {
    public static int uint(short val) {
        return Short.toUnsignedInt(val);
    }

    public static int uint(byte val) {
        return Byte.toUnsignedInt(val);
    }

    public static byte rshift(byte value, int distance) {
        return (byte) (uint(value) >> distance);
    }

    public static byte lshift(byte value, int distance) {
        return (byte) (uint(value) << distance);
    }

    public static byte arithmetic_rshift(byte value, int distance) {
        return (byte) (value >> distance);
    }

    public static byte and(byte a, byte b) {
        return (byte) (a & b);
    }

    public static short and(short a, short b) {
        return (short) (a & b);
    }

    public static byte or(byte a, byte b) {
        return (byte) (a | b);
    }

    public static short or(short a, short b) {
        return (short) (a | b);
    }

    public static byte xor(byte a, byte b) {
        return (byte) (a ^ b);
    }

    public static short xor(short a, short b) {
        return (short) (a ^ b);
    }

    public static byte not(byte val) {
        return (byte) ~val;
    }

    public static short not(short val) {
        return (short) ~val;
    }

    public static short rshift(short value, int distance) {
        return (short) (uint(value) >> distance);
    }

    public static short lshift(short value, int distance) {
        return (short) (uint(value) << distance);
    }

    public static short arithmetic_rshift(short value, int distance) {
        return (short) (value >> distance);
    }

    public static int bit_range(int start, int end, byte value) {
        return and(rshift((byte) 0xFF, 8 - (start + 1 - end)), rshift(value, end));
    }

    public static int bit_range(int start, int end, short value) {
        return and(rshift((short) 0xFFFF, 16 - (start + 1 - end)), rshift(value, end));
    }

    public static byte upper_byte(short value) {
        return (byte) rshift(value, 8);
    }

    public static byte lower_byte(short value) {
        return (byte) value;
    }

    public static int lower_nibble(byte value) {
        return 0b00001111 & value;
    }

    public static int upper_nibble(byte value) {
        return uint(value) >>> 4;
    }

    public static short set_upper_byte(short oldShort, byte newByte) {
        return (short) ((uint(oldShort) & 0x00FF) | uint(newByte) << 8);
    }

    public static short set_lower_byte(short oldShort, byte newByte) {
        return (short) ((oldShort & 0xFF00) | uint(newByte));
    }

    public static short concat(byte higher, byte lower) {
        return set_upper_byte(lower, higher);
    }

    public static short set_values_from_mask(short value, int mask, boolean setTo) {
        if (setTo) {
            return (short) (value | mask);
        } else {
            return (short) (value & ~mask);
        }
    }

    public static boolean get_bit(short value, int i) {
        return (rshift(value, i) & 0b1) == 1;
    }

    public static short set_bit(short value, int i) {
        return or(value, lshift((short) 1, i));
    }

    public static byte set_bit(byte value, int i, boolean bitValue) {
        byte bitmask = lshift((byte) 1, i);
        if (bitValue) {
            return or(value, bitmask);
        }
        else {
            return and(value, not(bitmask));
        }
    }

    public static byte calculate_carry_from_add(byte a, byte b, byte res) {
        int intA = uint(a);
        int intB = uint(b);
        int intRes = uint(res);
        return (byte) ((intA&intB) | ((intA^intB) & ~intRes));
    }
}
