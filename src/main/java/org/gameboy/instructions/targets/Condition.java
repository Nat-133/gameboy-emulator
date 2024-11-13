package org.gameboy.instructions.targets;

import org.gameboy.utils.MultiBitValue.TwoBitValue;

public enum Condition {
    NZ,
    Z,
    NC,
    C;

    public static Condition lookup(TwoBitValue y) {
        return Condition.values()[y.value()];
    }
}
