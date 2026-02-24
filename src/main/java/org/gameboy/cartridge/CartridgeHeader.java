package org.gameboy.cartridge;

import static org.gameboy.utils.BitUtilities.uint;

public class CartridgeHeader {
    private static final int CARTRIDGE_TYPE_OFFSET = 0x0147;
    private static final int ROM_SIZE_OFFSET = 0x0148;
    private static final int RAM_SIZE_OFFSET = 0x0149;

    private final int cartridgeType;
    private final int romBankCount;
    private final int ramBankCount;

    public CartridgeHeader(byte[] romData) {
        this.cartridgeType = uint(romData[CARTRIDGE_TYPE_OFFSET]);
        this.romBankCount = 2 << uint(romData[ROM_SIZE_OFFSET]);
        this.ramBankCount = parseRamBankCount(uint(romData[RAM_SIZE_OFFSET]));
    }

    public int cartridgeType() {
        return cartridgeType;
    }

    public int romBankCount() {
        return romBankCount;
    }

    public int romBankMask() {
        return romBankCount - 1;
    }

    public int ramBankCount() {
        return ramBankCount;
    }

    public int ramBankMask() {
        return ramBankCount == 0 ? 0 : ramBankCount - 1;
    }

    public boolean hasRtc() {
        return cartridgeType == 0x0F || cartridgeType == 0x10;
    }

    public boolean hasRam() {
        return cartridgeType == 0x10 || cartridgeType == 0x12 || cartridgeType == 0x13;
    }

    private static int parseRamBankCount(int ramSizeCode) {
        return switch (ramSizeCode) {
            case 0x00 -> 0;
            case 0x01 -> 0;
            case 0x02 -> 1;
            case 0x03 -> 4;
            case 0x04 -> 16;
            case 0x05 -> 8;
            default -> 0;
        };
    }
}
