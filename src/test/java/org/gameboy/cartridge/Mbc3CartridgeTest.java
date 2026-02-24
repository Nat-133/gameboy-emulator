package org.gameboy.cartridge;

import org.gameboy.common.Cartridge;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.gameboy.GameboyAssertions.assertThatHex;

public class Mbc3CartridgeTest {

    // Helper: create ROM data with MBC3+TIMER+RAM+BATTERY header (type 0x10)
    // ROM size code 0x05 = 64 banks (1MB), RAM size code 0x03 = 4 banks (32KB)
    private static byte[] mbc3Rom(int totalBanks) {
        byte[] rom = new byte[totalBanks * 0x4000];
        // Cartridge type: 0x10 = MBC3+TIMER+RAM+BATTERY
        rom[0x0147] = 0x10;
        // ROM size: compute code from bank count (2 << code = bankCount)
        rom[0x0148] = (byte) (Integer.numberOfTrailingZeros(totalBanks) - 1);
        // RAM size: code 0x03 = 4 banks (32KB)
        rom[0x0149] = 0x03;
        return rom;
    }

    private static Mbc3Cartridge createCartridge(byte[] rom) {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        return new Mbc3Cartridge(rom, time::get);
    }

    // --- ROM Bank 0 Reads (0x0000-0x3FFF) ---

    @Test
    public void givenRomData_whenReadFromBank0_thenReturnsBank0Data() {
        byte[] rom = mbc3Rom(4);
        rom[0x0000] = 0x31;
        rom[0x0100] = (byte) 0xC3;
        rom[0x3FFF] = 0x42;

        Cartridge cart = createCartridge(rom);

        assertThatHex(cart.read((short) 0x0000)).isEqualTo((byte) 0x31);
        assertThatHex(cart.read((short) 0x0100)).isEqualTo((byte) 0xC3);
        assertThatHex(cart.read((short) 0x3FFF)).isEqualTo((byte) 0x42);
    }

    // --- Switchable ROM Bank Reads (0x4000-0x7FFF) ---

    @Test
    public void givenDefaultState_whenReadFromSwitchableBank_thenReadsBank1() {
        byte[] rom = mbc3Rom(4);
        rom[0x4000] = (byte) 0xAB; // Bank 1, offset 0

        Cartridge cart = createCartridge(rom);

        assertThatHex(cart.read((short) 0x4000)).isEqualTo((byte) 0xAB);
    }

    @Test
    public void givenRomBankSetTo2_whenReadFromSwitchableBank_thenReadsBank2() {
        byte[] rom = mbc3Rom(4);
        rom[0x8000] = (byte) 0xCD; // Bank 2, offset 0

        Cartridge cart = createCartridge(rom);
        cart.write((short) 0x2000, (byte) 0x02); // Select bank 2

        assertThatHex(cart.read((short) 0x4000)).isEqualTo((byte) 0xCD);
    }

    @Test
    public void givenRomBankSetTo0_thenTreatedAsBank1() {
        byte[] rom = mbc3Rom(4);
        rom[0x4000] = (byte) 0xEF; // Bank 1, offset 0

        Cartridge cart = createCartridge(rom);
        cart.write((short) 0x2000, (byte) 0x00); // Write 0 — should map to 1

        assertThatHex(cart.read((short) 0x4000)).isEqualTo((byte) 0xEF);
    }

    @Test
    public void givenRomBankRegister_thenOnly7BitsUsed() {
        byte[] rom = mbc3Rom(128); // 128 banks
        rom[0x03 * 0x4000] = (byte) 0xBB; // Bank 3, offset 0

        Cartridge cart = createCartridge(rom);
        cart.write((short) 0x2000, (byte) 0x83); // 0x83 & 0x7F = 0x03

        assertThatHex(cart.read((short) 0x4000)).isEqualTo((byte) 0xBB);
    }

    @Test
    public void givenRomBankExceedsActualSize_thenBankMasked() {
        byte[] rom = mbc3Rom(4); // 4 banks -> mask 0x03
        rom[0x01 * 0x4000] = (byte) 0xDD; // Bank 1, offset 0

        Cartridge cart = createCartridge(rom);
        cart.write((short) 0x2000, (byte) 0x05); // 0x05 & 0x03 = 0x01

        assertThatHex(cart.read((short) 0x4000)).isEqualTo((byte) 0xDD);
    }

    // --- RAM Enable/Disable ---

    @Test
    public void givenRamDisabled_whenReadFromRam_thenReturns0xFF() {
        byte[] rom = mbc3Rom(4);
        Cartridge cart = createCartridge(rom);

        assertThatHex(cart.read((short) 0xA000)).isEqualTo((byte) 0xFF);
    }

    @Test
    public void givenRamEnabled_whenWriteAndRead_thenReturnsWrittenValue() {
        byte[] rom = mbc3Rom(4);
        Cartridge cart = createCartridge(rom);

        cart.write((short) 0x0000, (byte) 0x0A); // Enable RAM
        cart.write((short) 0xA000, (byte) 0x42); // Write to RAM

        assertThatHex(cart.read((short) 0xA000)).isEqualTo((byte) 0x42);
    }

    @Test
    public void givenRamEnabledThenDisabled_whenRead_thenReturns0xFF() {
        byte[] rom = mbc3Rom(4);
        Cartridge cart = createCartridge(rom);

        cart.write((short) 0x0000, (byte) 0x0A); // Enable
        cart.write((short) 0xA000, (byte) 0x42); // Write
        cart.write((short) 0x0000, (byte) 0x00); // Disable

        assertThatHex(cart.read((short) 0xA000)).isEqualTo((byte) 0xFF);
    }

    @Test
    public void givenRamDisabled_whenWriteToRam_thenWriteIgnored() {
        byte[] rom = mbc3Rom(4);
        Cartridge cart = createCartridge(rom);

        cart.write((short) 0xA000, (byte) 0x42); // Write while disabled
        cart.write((short) 0x0000, (byte) 0x0A); // Enable

        assertThatHex(cart.read((short) 0xA000)).isEqualTo((byte) 0x00); // Not written
    }

    @Test
    public void givenNon0x0AValue_thenRamRemainsDisabled() {
        byte[] rom = mbc3Rom(4);
        Cartridge cart = createCartridge(rom);

        cart.write((short) 0x0000, (byte) 0x0B); // Not 0x0A

        cart.write((short) 0xA000, (byte) 0x42);
        assertThatHex(cart.read((short) 0xA000)).isEqualTo((byte) 0xFF);
    }

    // --- RAM Banking ---

    @Test
    public void givenRamBank1Selected_whenWriteAndRead_thenAccessesBank1() {
        byte[] rom = mbc3Rom(4);
        Cartridge cart = createCartridge(rom);

        cart.write((short) 0x0000, (byte) 0x0A); // Enable RAM

        // Write to bank 0
        cart.write((short) 0xA000, (byte) 0x11);

        // Switch to bank 1
        cart.write((short) 0x4000, (byte) 0x01);
        cart.write((short) 0xA000, (byte) 0x22);

        // Read bank 1
        assertThatHex(cart.read((short) 0xA000)).isEqualTo((byte) 0x22);

        // Switch back to bank 0
        cart.write((short) 0x4000, (byte) 0x00);
        assertThatHex(cart.read((short) 0xA000)).isEqualTo((byte) 0x11);
    }

    @Test
    public void givenCartridgeWith1RamBank_whenSelectBank1_thenMaskedToBank0() {
        byte[] rom = new byte[4 * 0x4000];
        rom[0x0147] = 0x12; // MBC3+RAM
        rom[0x0148] = 0x01; // 4 ROM banks
        rom[0x0149] = 0x02; // 1 RAM bank (8KB) -> mask 0x00
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);

        Cartridge cart = new Mbc3Cartridge(rom, time::get);

        cart.write((short) 0x0000, (byte) 0x0A); // Enable

        cart.write((short) 0x4000, (byte) 0x00); // Bank 0
        cart.write((short) 0xA000, (byte) 0xAA);

        cart.write((short) 0x4000, (byte) 0x01); // Bank 1 & 0x00 = 0 (masked to bank 0)
        assertThatHex(cart.read((short) 0xA000)).isEqualTo((byte) 0xAA);
    }

    // --- RTC Register Access ---

    @Test
    public void givenRtcBankSelected_whenRamEnabled_thenAccessesRtcRegister() {
        byte[] rom = mbc3Rom(4);
        Cartridge cart = createCartridge(rom);

        cart.write((short) 0x0000, (byte) 0x0A); // Enable RAM/RTC
        cart.write((short) 0x4000, (byte) 0x08); // Select RTC seconds register

        // RTC starts at 0, latched values should be 0
        assertThatHex(cart.read((short) 0xA000)).isEqualTo((byte) 0x00);
    }

    @Test
    public void givenRtcBankSelected_whenWrite_thenWritesToRtcRegister() {
        byte[] rom = mbc3Rom(4);
        Cartridge cart = createCartridge(rom);

        cart.write((short) 0x0000, (byte) 0x0A); // Enable
        cart.write((short) 0x4000, (byte) 0x08); // Select RTC seconds

        // Write to RTC seconds
        cart.write((short) 0xA000, (byte) 30);

        // Latch and read
        cart.write((short) 0x6000, (byte) 0x00);
        cart.write((short) 0x6000, (byte) 0x01);

        assertThatHex(cart.read((short) 0xA000)).isEqualTo((byte) 30);
    }

    @Test
    public void givenRtcDisabled_whenReadRtcBank_thenReturns0xFF() {
        byte[] rom = mbc3Rom(4);
        Cartridge cart = createCartridge(rom);

        cart.write((short) 0x4000, (byte) 0x08); // Select RTC — but RAM/RTC not enabled

        assertThatHex(cart.read((short) 0xA000)).isEqualTo((byte) 0xFF);
    }

    // --- Latch Clock via Cartridge Write ---

    @Test
    public void givenLatchSequenceViaCartridge_thenRtcLatches() {
        byte[] rom = mbc3Rom(4);
        Cartridge cart = createCartridge(rom);

        cart.write((short) 0x0000, (byte) 0x0A); // Enable
        cart.write((short) 0x4000, (byte) 0x08); // Select RTC seconds

        // Write 30 to live seconds
        cart.write((short) 0xA000, (byte) 30);

        // Latch via cartridge write
        cart.write((short) 0x6000, (byte) 0x00);
        cart.write((short) 0x6000, (byte) 0x01);

        assertThatHex(cart.read((short) 0xA000)).isEqualTo((byte) 30);
    }

    // --- No RTC Cartridge ---

    @Test
    public void givenMbc3WithoutRtc_whenSelectRtcBank_thenReturns0xFF() {
        byte[] rom = new byte[4 * 0x4000];
        rom[0x0147] = 0x11; // MBC3 (no timer)
        rom[0x0148] = 0x01; // 4 banks
        rom[0x0149] = 0x03; // 4 RAM banks
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);

        Cartridge cart = new Mbc3Cartridge(rom, time::get);

        cart.write((short) 0x0000, (byte) 0x0A); // Enable
        cart.write((short) 0x4000, (byte) 0x08); // Select RTC seconds

        assertThatHex(cart.read((short) 0xA000)).isEqualTo((byte) 0xFF);
    }

    // --- Edge: Read/Write at boundary addresses ---

    @Test
    public void givenRomBank0_whenReadAtEndOfBank_thenCorrect() {
        byte[] rom = mbc3Rom(4);
        rom[0x3FFF] = (byte) 0xFE;

        Cartridge cart = createCartridge(rom);

        assertThatHex(cart.read((short) 0x3FFF)).isEqualTo((byte) 0xFE);
    }

    @Test
    public void givenSwitchableBank_whenReadAtEndOfBank_thenCorrect() {
        byte[] rom = mbc3Rom(4);
        rom[0x7FFF] = (byte) 0xFD; // Bank 1, last byte

        Cartridge cart = createCartridge(rom);

        assertThatHex(cart.read((short) 0x7FFF)).isEqualTo((byte) 0xFD);
    }

    @Test
    public void givenRam_whenReadAtEndOfRam_thenCorrect() {
        byte[] rom = mbc3Rom(4);
        Cartridge cart = createCartridge(rom);

        cart.write((short) 0x0000, (byte) 0x0A); // Enable
        cart.write((short) 0xBFFF, (byte) 0xFC); // Write to end of RAM bank

        assertThatHex(cart.read((short) 0xBFFF)).isEqualTo((byte) 0xFC);
    }
}
