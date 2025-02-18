package org.gameboy.cpu.components;

public class IncrementDecrementUnit {

    private boolean disableNextIncrement;

    public IncrementDecrementUnit() {
        disableNextIncrement = false;
    }

    public short increment(short value) {
        if (disableNextIncrement) {
            disableNextIncrement = false;
            return value;
        }

        return (short) (value + 1);
    }

    public short decrement(short value) {
        return (short) (value - 1);
    }

    public short passthrough(short value) {
        return value;
    }

    public void disableNextIncrement() {
        this.disableNextIncrement = true;
    }
}
