package org.gameboy.cartridge;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CartridgeHeaderTest {

    private static byte[] romWithHeader(byte cartridgeType, byte romSize, byte ramSize) {
        byte[] rom = new byte[0x8000];
        rom[0x0147] = cartridgeType;
        rom[0x0148] = romSize;
        rom[0x0149] = ramSize;
        return rom;
    }

    @Test
    public void givenRomOnlyType_thenCartridgeTypeIsCorrect() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x00, (byte) 0x00));

        assertThat(header.cartridgeType()).isEqualTo(0x00);
    }

    @Test
    public void givenMbc3RtcRamBattery_thenCartridgeTypeIsCorrect() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x10, (byte) 0x00, (byte) 0x00));

        assertThat(header.cartridgeType()).isEqualTo(0x10);
    }

    @Test
    public void givenRomSizeCode0_then2RomBanks() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x00, (byte) 0x00));

        assertThat(header.romBankCount()).isEqualTo(2);
    }

    @Test
    public void givenRomSizeCode1_then4RomBanks() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x01, (byte) 0x00));

        assertThat(header.romBankCount()).isEqualTo(4);
    }

    @Test
    public void givenRomSizeCode5_then64RomBanks() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x05, (byte) 0x00));

        assertThat(header.romBankCount()).isEqualTo(64);
    }

    @Test
    public void givenRomSizeCode6_then128RomBanks() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x06, (byte) 0x00));

        assertThat(header.romBankCount()).isEqualTo(128);
    }

    @Test
    public void givenRomBankCount_thenRomBankMaskIsCountMinusOne() {
        // 4 banks -> mask 0x03
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x01, (byte) 0x00));

        assertThat(header.romBankMask()).isEqualTo(0x03);
    }

    @Test
    public void givenRomSizeCode6_thenRomBankMaskIs0x7F() {
        // 128 banks -> mask 0x7F
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x06, (byte) 0x00));

        assertThat(header.romBankMask()).isEqualTo(0x7F);
    }

    @Test
    public void givenRamSizeCode0_then0RamBanks() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x00, (byte) 0x00));

        assertThat(header.ramBankCount()).isEqualTo(0);
    }

    @Test
    public void givenRamSizeCode2_then1RamBank() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x00, (byte) 0x02));

        assertThat(header.ramBankCount()).isEqualTo(1);
    }

    @Test
    public void givenRamSizeCode3_then4RamBanks() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x00, (byte) 0x03));

        assertThat(header.ramBankCount()).isEqualTo(4);
    }

    @Test
    public void givenRamSizeCode4_then16RamBanks() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x00, (byte) 0x04));

        assertThat(header.ramBankCount()).isEqualTo(16);
    }

    @Test
    public void givenRamSizeCode5_then8RamBanks() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x00, (byte) 0x05));

        assertThat(header.ramBankCount()).isEqualTo(8);
    }

    @Test
    public void givenRamBankCount4_thenRamBankMaskIs0x03() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x00, (byte) 0x03));

        assertThat(header.ramBankMask()).isEqualTo(0x03);
    }

    @Test
    public void givenRamBankCount0_thenRamBankMaskIs0() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x00, (byte) 0x00));

        assertThat(header.ramBankMask()).isEqualTo(0);
    }

    @Test
    public void givenMbc3WithRtc_thenHasRtcIsTrue() {
        // 0x0F = MBC3+TIMER+BATTERY
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x0F, (byte) 0x00, (byte) 0x00));

        assertThat(header.hasRtc()).isTrue();
    }

    @Test
    public void givenMbc3WithRtcRamBattery_thenHasRtcIsTrue() {
        // 0x10 = MBC3+TIMER+RAM+BATTERY
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x10, (byte) 0x00, (byte) 0x00));

        assertThat(header.hasRtc()).isTrue();
    }

    @Test
    public void givenMbc3Plain_thenHasRtcIsFalse() {
        // 0x11 = MBC3
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x11, (byte) 0x00, (byte) 0x00));

        assertThat(header.hasRtc()).isFalse();
    }

    @Test
    public void givenRomOnly_thenHasRtcIsFalse() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x00, (byte) 0x00));

        assertThat(header.hasRtc()).isFalse();
    }

    @Test
    public void givenMbc3WithRam_thenHasRamIsTrue() {
        // 0x12 = MBC3+RAM
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x12, (byte) 0x00, (byte) 0x03));

        assertThat(header.hasRam()).isTrue();
    }

    @Test
    public void givenMbc3RtcRamBattery_thenHasRamIsTrue() {
        // 0x10 = MBC3+TIMER+RAM+BATTERY
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x10, (byte) 0x00, (byte) 0x03));

        assertThat(header.hasRam()).isTrue();
    }

    @Test
    public void givenMbc3TimerBattery_thenHasRamIsFalse() {
        // 0x0F = MBC3+TIMER+BATTERY (no RAM)
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x0F, (byte) 0x00, (byte) 0x00));

        assertThat(header.hasRam()).isFalse();
    }

    @Test
    public void givenRomOnly_thenHasRamIsFalse() {
        CartridgeHeader header = new CartridgeHeader(romWithHeader((byte) 0x00, (byte) 0x00, (byte) 0x00));

        assertThat(header.hasRam()).isFalse();
    }
}
