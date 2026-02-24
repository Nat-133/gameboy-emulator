package org.gameboy.cartridge;

import org.gameboy.common.Cartridge;

import java.time.Instant;
import java.util.function.Supplier;

import static org.gameboy.utils.BitUtilities.uint;

public class Mbc3Cartridge implements Cartridge {
    private final byte[] rom;
    private final byte[] ram;
    private final RealTimeClock rtc;
    private final int romBankMask;
    private final int ramBankMask;
    private final boolean hasRtc;

    private int romBank = 1;
    private int ramRtcBank = 0;
    private boolean ramRtcEnabled = false;

    public Mbc3Cartridge(byte[] romData, Supplier<Instant> clock) {
        CartridgeHeader header = new CartridgeHeader(romData);
        this.rom = romData;
        this.romBankMask = header.romBankMask();
        this.ramBankMask = header.ramBankMask();
        this.hasRtc = header.hasRtc();

        int ramSize = header.ramBankCount() * 0x2000;
        this.ram = new byte[ramSize];

        this.rtc = hasRtc ? new RealTimeClock(clock) : null;
    }

    @Override
    public byte read(short address) {
        int addr = uint(address);

        if (addr <= 0x3FFF) {
            return rom[addr];
        }

        if (addr <= 0x7FFF) {
            int effectiveBank = romBank & romBankMask;
            int romOffset = effectiveBank * 0x4000 + (addr - 0x4000);
            if (romOffset < rom.length) {
                return rom[romOffset];
            }
            return (byte) 0xFF;
        }

        if (addr >= 0xA000 && addr <= 0xBFFF) {
            if (!ramRtcEnabled) {
                return (byte) 0xFF;
            }

            if (ramRtcBank <= 0x03) {
                int effectiveRamBank = ramBankMask > 0 ? ramRtcBank & ramBankMask : 0;
                int ramOffset = effectiveRamBank * 0x2000 + (addr - 0xA000);
                if (ramOffset < ram.length) {
                    return ram[ramOffset];
                }
                return (byte) 0xFF;
            }

            if (ramRtcBank >= 0x08 && ramRtcBank <= 0x0C && rtc != null) {
                return rtc.read(ramRtcBank);
            }

            return (byte) 0xFF;
        }

        return (byte) 0xFF;
    }

    @Override
    public void write(short address, byte value) {
        int addr = uint(address);
        int val = Byte.toUnsignedInt(value);

        if (addr <= 0x1FFF) {
            ramRtcEnabled = (val == 0x0A);
            return;
        }

        if (addr <= 0x3FFF) {
            int bank = val & 0x7F;
            romBank = (bank == 0) ? 1 : bank;
            return;
        }

        if (addr <= 0x5FFF) {
            ramRtcBank = val;
            return;
        }

        if (addr <= 0x7FFF) {
            if (rtc != null) {
                rtc.writeLatch(value);
            }
            return;
        }

        if (addr >= 0xA000 && addr <= 0xBFFF) {
            if (!ramRtcEnabled) {
                return;
            }

            if (ramRtcBank <= 0x03) {
                int effectiveRamBank = ramBankMask > 0 ? ramRtcBank & ramBankMask : 0;
                int ramOffset = effectiveRamBank * 0x2000 + (addr - 0xA000);
                if (ramOffset < ram.length) {
                    ram[ramOffset] = value;
                }
                return;
            }

            if (ramRtcBank >= 0x08 && ramRtcBank <= 0x0C && rtc != null) {
                rtc.write(ramRtcBank, value);
            }
        }
    }
}
