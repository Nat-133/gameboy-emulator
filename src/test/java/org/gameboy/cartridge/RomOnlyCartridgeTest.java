package org.gameboy.cartridge;

import org.gameboy.common.Cartridge;
import org.junit.jupiter.api.Test;

import static org.gameboy.GameboyAssertions.assertThatHex;

public class RomOnlyCartridgeTest {

    @Test
    public void givenRomData_whenReadFromRomBank0_thenReturnsCorrectByte() {
        byte[] rom = new byte[0x8000];
        rom[0x0000] = 0x31;
        rom[0x0100] = (byte) 0xC3;
        rom[0x3FFF] = 0x42;

        Cartridge cartridge = new RomOnlyCartridge(rom);

        assertThatHex(cartridge.read((short) 0x0000)).isEqualTo((byte) 0x31);
        assertThatHex(cartridge.read((short) 0x0100)).isEqualTo((byte) 0xC3);
        assertThatHex(cartridge.read((short) 0x3FFF)).isEqualTo((byte) 0x42);
    }

    @Test
    public void givenRomData_whenReadFromRomBank1_thenReturnsCorrectByte() {
        byte[] rom = new byte[0x8000];
        rom[0x4000] = (byte) 0xAB;
        rom[0x7FFF] = (byte) 0xCD;

        Cartridge cartridge = new RomOnlyCartridge(rom);

        assertThatHex(cartridge.read((short) 0x4000)).isEqualTo((byte) 0xAB);
        assertThatHex(cartridge.read((short) 0x7FFF)).isEqualTo((byte) 0xCD);
    }

    @Test
    public void givenRomOnlyCartridge_whenReadFromExternalRam_thenReturns0xFF() {
        Cartridge cartridge = new RomOnlyCartridge(new byte[0x8000]);

        assertThatHex(cartridge.read((short) 0xA000)).isEqualTo((byte) 0xFF);
        assertThatHex(cartridge.read((short) 0xBFFF)).isEqualTo((byte) 0xFF);
    }

    @Test
    public void givenRomOnlyCartridge_whenWriteToRom_thenWriteIsIgnored() {
        byte[] rom = new byte[0x8000];
        rom[0x1000] = 0x42;

        Cartridge cartridge = new RomOnlyCartridge(rom);
        cartridge.write((short) 0x1000, (byte) 0xFF);

        assertThatHex(cartridge.read((short) 0x1000)).isEqualTo((byte) 0x42);
    }

    @Test
    public void givenRomOnlyCartridge_whenWriteToExternalRam_thenWriteIsIgnored() {
        Cartridge cartridge = new RomOnlyCartridge(new byte[0x8000]);
        cartridge.write((short) 0xA000, (byte) 0x42);

        assertThatHex(cartridge.read((short) 0xA000)).isEqualTo((byte) 0xFF);
    }

    @Test
    public void givenShortRom_whenReadBeyondRomData_thenReturnsZero() {
        byte[] shortRom = new byte[0x100];
        shortRom[0x00] = 0x31;

        Cartridge cartridge = new RomOnlyCartridge(shortRom);

        assertThatHex(cartridge.read((short) 0x0000)).isEqualTo((byte) 0x31);
        assertThatHex(cartridge.read((short) 0x0100)).isEqualTo((byte) 0x00);
        assertThatHex(cartridge.read((short) 0x4000)).isEqualTo((byte) 0x00);
    }
}
