package org.gameboy.cartridge;

import org.gameboy.common.Cartridge;
import org.junit.jupiter.api.Test;

import static org.gameboy.GameboyAssertions.assertThatHex;

public class Mbc1CartridgeTest {

    // Helper: create ROM data with MBC1+RAM+BATTERY header (type 0x03)
    // romBankCount must be a power of 2
    private static byte[] mbc1Rom(int totalBanks) {
        byte[] rom = new byte[totalBanks * 0x4000];
        rom[0x0147] = 0x03; // MBC1+RAM+BATTERY
        rom[0x0148] = (byte) (Integer.numberOfTrailingZeros(totalBanks) - 1);
        rom[0x0149] = 0x03; // 4 RAM banks (32KB)
        return rom;
    }

    private static Mbc1Cartridge createCartridge(byte[] rom) {
        return new Mbc1Cartridge(rom);
    }

    // --- ROM Bank 0 Reads (0x0000-0x3FFF), Mode 0 ---

    @Test
    public void givenMode0_whenReadFromBank0_thenReturnsBank0Data() {
        byte[] rom = mbc1Rom(4);
        rom[0x0000] = 0x31;
        rom[0x0100] = (byte) 0xC3;
        rom[0x3FFF] = 0x42;

        Cartridge cart = createCartridge(rom);

        assertThatHex(cart.read((short) 0x0000)).isEqualTo((byte) 0x31);
        assertThatHex(cart.read((short) 0x0100)).isEqualTo((byte) 0xC3);
        assertThatHex(cart.read((short) 0x3FFF)).isEqualTo((byte) 0x42);
    }

    @Test
    public void givenMode0WithBank2Set_whenReadFromBank0Region_thenStillReturnsBank0() {
        byte[] rom = mbc1Rom(128); // 128 banks so BANK2 bits matter
        rom[0x0000] = (byte) 0xAA;

        Cartridge cart = createCartridge(rom);
        cart.write((short) 0x4000, (byte) 0x01); // BANK2 = 1

        assertThatHex(cart.read((short) 0x0000)).isEqualTo((byte) 0xAA);
    }
}
