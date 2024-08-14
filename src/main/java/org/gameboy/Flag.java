package org.gameboy;

public enum Flag {
    Z,  // zero
    N,  // subtraction
    H,  // half-carry
    C;  // carry

    public int getLocationMask() {
        return switch (this) {
            case Z -> 0b1000_0000;
            case N -> 0b0100_0000;
            case H -> 0b0010_0000;
            case C -> 0b0001_0000;
        };
    }
}
