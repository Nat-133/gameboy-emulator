package org.gameboy.components;

public class IncrementDecrementUnit {
    public short increment(short value) {
        return (short) (value + 1);
    }

    public short decrement(short value) {
        return (short) (value - 1);
    }
}
