package org.gameboy;

public class IncrementDecrementUnit {
    public static short increment(short value) {
        return (short) (value + 1);
    }

    public static short decrement(short value) {
        return (short) (value - 1);
    }
}
