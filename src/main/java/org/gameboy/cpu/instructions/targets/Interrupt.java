package org.gameboy.cpu.instructions.targets;

import static org.gameboy.cpu.MemoryMapConstants.*;

public enum Interrupt {
    VBLANK,
    STAT,
    TIMER,
    SERIAL,
    JOYPAD;

    public int index() {
        return switch (this) {
            case VBLANK -> 0;
            case STAT   -> 1;
            case TIMER  -> 2;
            case SERIAL -> 3;
            case JOYPAD -> 4;
        };
    }

    public short getInterruptHandlerAddress() {
        return switch(this) {
            case VBLANK -> VBLANK_HANDLER_ADDRESS;
            case STAT -> STAT_HANDLER_ADDRESS;
            case TIMER -> TIMER_HANDLER_ADDRESS;
            case SERIAL -> SERIAL_HANDLER_ADDRESS;
            case JOYPAD -> JOYPAD_HANDLER_ADDRESS;
        };
    }
}
