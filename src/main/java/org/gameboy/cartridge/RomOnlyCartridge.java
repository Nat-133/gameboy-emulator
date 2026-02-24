package org.gameboy.cartridge;

import org.gameboy.common.Cartridge;

import static org.gameboy.utils.BitUtilities.uint;

public class RomOnlyCartridge implements Cartridge {
    private final byte[] rom;

    public RomOnlyCartridge(byte[] romData) {
        this.rom = new byte[0x8000];
        System.arraycopy(romData, 0, this.rom, 0, Math.min(romData.length, 0x8000));
    }

    @Override
    public byte read(short address) {
        int addr = uint(address);
        if (addr <= 0x7FFF) {
            return rom[addr];
        }
        return (byte) 0xFF;
    }

    @Override
    public void write(short address, byte value) {
        // ROM-only cartridge: all writes are ignored
    }
}
