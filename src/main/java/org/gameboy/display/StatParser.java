package org.gameboy.display;

import org.gameboy.utils.MultiBitValue.TwoBitValue;

import static org.gameboy.utils.BitUtilities.get_bit;
import static org.gameboy.utils.BitUtilities.set_bit;

public class StatParser {
    static boolean lyCompareInterruptEnabled(byte stat) {
        return get_bit(stat, 6);
    }

    static boolean oamInterruptEnabled(byte stat) {
        return get_bit(stat, 5);
    }

    static boolean vblankInterruptEnabled(byte stat) {
        return get_bit(stat, 4);
    }

    static boolean hblankInterruptEnabled(byte stat) {
        return get_bit(stat, 3);
    }

    static boolean coincidenceFlag(byte stat) {
        return get_bit(stat, 2);
    }

    static byte setCoincidenceFlag(byte stat, boolean value) {
        return set_bit(stat, 2, value);
    }

    static byte setPpuMode(PpuMode mode, byte stat) {
        TwoBitValue value = switch(mode) {
            case H_BLANK -> TwoBitValue.b00;
            case V_BLANK -> TwoBitValue.b01;
            case OAM_SCANNING -> TwoBitValue.b10;
            case DRAWING -> TwoBitValue.b11;
        };

        return (byte) ((stat & 0b1111_1100) | value.value());
    }

    public enum PpuMode {
        H_BLANK,
        V_BLANK,
        OAM_SCANNING,
        DRAWING
    }
}
