package org.gameboy.cartridge;

import org.gameboy.common.Cartridge;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CartridgeFactoryTest {

    private static byte[] romWithType(byte cartridgeType) {
        byte[] rom = new byte[0x8000];
        rom[0x0147] = cartridgeType;
        rom[0x0148] = 0x00; // 2 ROM banks
        rom[0x0149] = 0x00; // No RAM
        return rom;
    }

    @Test
    public void givenTypeCode0x00_thenCreatesRomOnlyCartridge() {
        Cartridge cart = CartridgeFactory.fromRom(romWithType((byte) 0x00));

        assertThat(cart).isInstanceOf(RomOnlyCartridge.class);
    }

    @Test
    public void givenTypeCode0x0F_thenCreatesMbc3Cartridge() {
        // MBC3+TIMER+BATTERY
        Cartridge cart = CartridgeFactory.fromRom(romWithType((byte) 0x0F));

        assertThat(cart).isInstanceOf(Mbc3Cartridge.class);
    }

    @Test
    public void givenTypeCode0x10_thenCreatesMbc3Cartridge() {
        // MBC3+TIMER+RAM+BATTERY
        Cartridge cart = CartridgeFactory.fromRom(romWithType((byte) 0x10));

        assertThat(cart).isInstanceOf(Mbc3Cartridge.class);
    }

    @Test
    public void givenTypeCode0x11_thenCreatesMbc3Cartridge() {
        // MBC3
        Cartridge cart = CartridgeFactory.fromRom(romWithType((byte) 0x11));

        assertThat(cart).isInstanceOf(Mbc3Cartridge.class);
    }

    @Test
    public void givenTypeCode0x12_thenCreatesMbc3Cartridge() {
        // MBC3+RAM
        Cartridge cart = CartridgeFactory.fromRom(romWithType((byte) 0x12));

        assertThat(cart).isInstanceOf(Mbc3Cartridge.class);
    }

    @Test
    public void givenTypeCode0x13_thenCreatesMbc3Cartridge() {
        // MBC3+RAM+BATTERY
        Cartridge cart = CartridgeFactory.fromRom(romWithType((byte) 0x13));

        assertThat(cart).isInstanceOf(Mbc3Cartridge.class);
    }

    @Test
    public void givenTypeCode0x01_thenCreatesMbc1Cartridge() {
        Cartridge cart = CartridgeFactory.fromRom(romWithType((byte) 0x01));

        assertThat(cart).isInstanceOf(Mbc1Cartridge.class);
    }

    @Test
    public void givenTypeCode0x02_thenCreatesMbc1Cartridge() {
        Cartridge cart = CartridgeFactory.fromRom(romWithType((byte) 0x02));

        assertThat(cart).isInstanceOf(Mbc1Cartridge.class);
    }

    @Test
    public void givenTypeCode0x03_thenCreatesMbc1Cartridge() {
        Cartridge cart = CartridgeFactory.fromRom(romWithType((byte) 0x03));

        assertThat(cart).isInstanceOf(Mbc1Cartridge.class);
    }

    @Test
    public void givenUnsupportedTypeCode_thenThrowsException() {
        assertThatThrownBy(() -> CartridgeFactory.fromRom(romWithType((byte) 0x04)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("0x04");
    }
}
