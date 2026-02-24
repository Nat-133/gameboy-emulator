package org.gameboy.cartridge;

import org.gameboy.common.Cartridge;

import java.time.Instant;

public class CartridgeFactory {

    public static Cartridge fromRom(byte[] romData) {
        CartridgeHeader header = new CartridgeHeader(romData);
        int type = header.cartridgeType();

        return switch (type) {
            case 0x00 -> new RomOnlyCartridge(romData);
            case 0x0F, 0x10, 0x11, 0x12, 0x13 -> new Mbc3Cartridge(romData, Instant::now);
            default -> throw new UnsupportedOperationException(
                    "Unsupported cartridge type: 0x%02x".formatted(type));
        };
    }
}
