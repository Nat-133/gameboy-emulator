package org.gameboy.common;

public class MemoryMapConstants {
    public static final short IE_ADDRESS = (short) 0xffff;
    public static final short IF_ADDRESS = (short) 0xff0f;

    public static final short VBLANK_HANDLER_ADDRESS = (short) 0x0040;
    public static final short STAT_HANDLER_ADDRESS = (short) 0x0048;
    public static final short TIMER_HANDLER_ADDRESS = (short) 0x0050;
    public static final short SERIAL_HANDLER_ADDRESS = (short) 0x0058;
    public static final short JOYPAD_HANDLER_ADDRESS = (short) 0x0060;

    public static final int TILE_MAP_A_ADDRESS = 0x9800;
    public static final int TILE_MAP_B_ADDRESS = 0x9C00;
}
