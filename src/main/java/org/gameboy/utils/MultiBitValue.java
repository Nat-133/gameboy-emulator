package org.gameboy.utils;

public class MultiBitValue {
    public interface BitValue {
        int value();
    }

    public enum OneBitValue implements BitValue {
        b0,
        b1;

        public static OneBitValue from(int value) {
            return from(value, 0);
        }

        public static OneBitValue from(int value, int startBit) {
            return OneBitValue.values()[(value >>> startBit) & 0b1];
        }

        public static OneBitValue from(BitValue value, int startBit) {
            return OneBitValue.from(value.value(), startBit);
        }

        public int value() {
            return this.ordinal();
        }
    }

    public enum TwoBitValue implements BitValue {
        b00,
        b01,
        b10,
        b11;

        public static TwoBitValue from(int value) {
            return TwoBitValue.from(value, 0);
        }

        public static TwoBitValue from(int value, int startBit) {
            return TwoBitValue.values()[(value >>> startBit) & 0b11];
        }

        public static TwoBitValue from(BitValue value, int startBit) {
            return TwoBitValue.from(value.value(), startBit);
        }

        public int value() {
            return this.ordinal();
        }
    }

    public enum ThreeBitValue implements BitValue {
        b000,
        b001,
        b010,
        b011,
        b100,
        b101,
        b110,
        b111;

        public static ThreeBitValue from(int value) {
            return ThreeBitValue.from(value, 0);
        }

        public static ThreeBitValue from(int value, int startBit) {
            return ThreeBitValue.values()[(value >>> startBit) & 0b111];
        }

        public int value() {
            return this.ordinal();
        }
    }
}
