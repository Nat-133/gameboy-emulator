package org.gameboy.utils;

public class MultiBitValue {
    public enum OneBitValue {
        b0,
        b1;

        public static OneBitValue from(int value) {
            return OneBitValue.values()[value & 0b1];
        }
    }
    public enum TwoBitValue {
        b00,
        b01,
        b10,
        b11;

        public static TwoBitValue from(int value) {
            return TwoBitValue.values()[value & 0b11];
        }
    }

    public enum ThreeBitValue {
        b000,
        b001,
        b010,
        b011,
        b100,
        b101,
        b110,
        b111;

        public static ThreeBitValue from(int value) {
            return ThreeBitValue.values()[value & 0b111];
        }
    }
}
